package com.namcobandaigames.dragonballtap.apk;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

// ══════════════════════════════════════════════════════════════════════
// VideoTexture — ORQUESTRADOR. Mantém o nome histórico (é o que
// ExtrasReader/TCBManajer já esperam e chamam), mas por dentro delega
// toda a mecânica de ExoPlayer/GL para VideoPlayerGL, e todo o "o que foi
// pedido" para VideoRequest — esta classe só decide O QUE FAZER com isso:
// idempotência, BGM/SE mute, $WAIT, trava de posição, thumbnail (consulta
// só, já pré-calculada em GameData).
//
// PRINCÍPIOS DE DESIGN (aprendidos dos bugs da versão anterior):
//   1. Idempotência NUNCA depende de estado do ExoPlayer (isPlaying,
//      isBusy). Depende só da identidade do VideoRequest atual — se já
//      existe uma tentativa em andamento para o MESMO (acttype,
//      videoIndex), ignora o novo pedido, ponto final. Isso elimina a
//      corrida que causava reinícios repetidos.
//   2. holdPat() protege obj.time baseado em "existe uma tentativa de
//      vídeo ativa" (_currentRequest != null e ainda não finalizada
//      explicitamente), não em estado oscilante do player.
//   3. Todo "fim de vídeo" (natural, skip, novo vídeo por cima, fim de
//      janela sem loop, objeto destruído, $END_ANO) passa por um ÚNICO
//      método de encerramento (finishPlayback), que sempre desfaz BGM
//      mute / SE mute / trava de posição / trava de jogo — nunca duas
//      vezes o mesmo caminho de saída com lógica duplicada.
// ══════════════════════════════════════════════════════════════════════
public final class VideoTexture {

    private final VideoPlayerGL _player = new VideoPlayerGL();

    // Pedido atualmente em andamento (null = nenhum vídeo ativo/pedido).
    // Ver princípio de design 1 acima — esta é a fonte da verdade de
    // idempotência, não estado do ExoPlayer.
    private VideoRequest _currentRequest = null;

    // true assim que o vídeo tocou pela 1ª vez nesta sessão (para não
    // mostrar a thumbnail de novo em oscilações de isPlaying após o
    // início real).
    private volatile boolean _hasEverPlayed = false;

    private long _windowEndMs = -1;
    private boolean _windowLoops = false;

    private long _skipHoldStartMillis = 0;

    private boolean _holdsActionStop = false;

    private int _lockedX;
    private int _lockedY;

    private boolean _bgmMutedByThisVideo = false;
    private boolean _seMutedByThisVideo = false;

    private int _thumbTexId = 0;

    private static final int HOLD_TIME_VALUE = 60;

    public VideoTexture() {
        _player.setListener(new VideoPlayerGL.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    _hasEverPlayed = true;
                }
            }

            @Override
            public void onVideoSizeChanged(int width, int height) {
            }
        });

        synchronized (_registry) {
            _registry.add(new WeakReference<VideoTexture>(this));
        }
    }

    public int playFromDac(GlobalWork gw, ObjReq obj, TCBManajer tm, byte[] dac, int dataPtr) {
        int videoIndex = obj.img;

        // NOVO: zera obj.img logo após capturar o índice do vídeo — evita
        // que DrawExec (no ramo de sprite normal, obj.img == -1 já é a
        // convenção existente de "sem imagem") tente carregar/desenhar a
        // textura estática por baixo do vídeo desnecessariamente. Seguro
        // fazer isso aqui porque videoIndex já guardou o valor original
        // ANTES desta linha — nada mais neste método (nem chamadas
        // subsequentes) depende de obj.img, só de videoIndex.
        obj.img = -1;

        int acttype = obj.acttype & 0x1F;

        int canSkip = readShortLE(dac, dataPtr); dataPtr += 2;
        int flags = readShortLE(dac, dataPtr); dataPtr += 2;
        int width = readShortLE(dac, dataPtr); dataPtr += 2;
        int height = readShortLE(dac, dataPtr); dataPtr += 2;

        boolean stop = (flags & VideoRequest.FLAG_STOP) != 0;
        boolean hasStopPoint = (flags & VideoRequest.FLAG_HAS_STOP_POINT) != 0;
        int stopAno = 0, stopMpt = 0;
        if (hasStopPoint) {
            stopAno = readShortLE(dac, dataPtr); dataPtr += 2;
            stopMpt = dac[dataPtr++] & 255;
        }

        boolean hasNewBgm = (flags & VideoRequest.FLAG_HAS_NEW_BGM) != 0;
        int newBgmId = 0;
        if (hasNewBgm) {
            newBgmId = dac[dataPtr++] & 255;
        }

        int rawSeconds = obj.time;
        boolean loopRequested = (obj.wActFlag & 1) != 0;

        int requestX = (obj.x >> 16) + gw.iScreenDrawOffsetX + obj.ox;
        int requestY = (obj.y >> 16) + gw.iScreenDrawOffsetY + obj.oy;

        VideoRequest request = new VideoRequest(acttype, videoIndex, canSkip, flags, width, height,
                stop, hasStopPoint, stopAno, stopMpt, hasNewBgm, newBgmId,
                rawSeconds, loopRequested, requestX, requestY);

        if (_currentRequest != null && request.isSameVideoAs(_currentRequest)) {
            return dataPtr;
        }

        startPlayback(gw, obj, tm, request);
        return dataPtr;
    }

    private static int readShortLE(byte[] dac, int pos) {
        return (short) ((dac[pos + 1] & 255) << 8 | (dac[pos] & 255));
    }

    private void startPlayback(GlobalWork gw, ObjReq obj, TCBManajer tm, VideoRequest request) {
        if (_currentRequest != null) {
            finishPlayback(gw);
        }

        _currentRequest = request;
        _hasEverPlayed = false;

        if (request.rawSeconds > 0) {
            _windowEndMs = request.rawSeconds * 1000L;
            _windowLoops = request.loopRequested;
        } else {
            _windowEndMs = -1;
            _windowLoops = false;
        }
        boolean loopWholeVideo = request.loopRequested && request.rawSeconds == 0;

        if (request.hasFlag(VideoRequest.FLAG_FIXO)) {
            _lockedX = request.requestX;
            _lockedY = request.requestY;
        }

        if (request.hasNewBgm) {
            tm.GdtBGM(gw, request.newBgmId);
        }

        if (request.hasFlag(VideoRequest.FLAG_MUTE_BGM)) {
            SoundEffect.getInstance().pauseBgm();
            _bgmMutedByThisVideo = true;
        }
        if (request.hasFlag(VideoRequest.FLAG_MUTE_SE)) {
            SoundEffect.getInstance().pauseSE();
            _seMutedByThisVideo = true;
        }

        _player.init(gw.context);

        byte[] data = tm.GetVideoData(request.acttype, request.videoIndex);
        _thumbTexId = tm.GetVideoThumbTexId(request.acttype, request.videoIndex);

        boolean started = false;
        if (data != null) {
            started = _player.playBytes(data, loopWholeVideo);
        }
        if (!started) {
            started = _player.playAsset(gw.context, "vid_" + request.videoIndex + ".mp4", loopWholeVideo);
        }

        if (request.hasFlag(VideoRequest.FLAG_MUTE_VIDEO)) {
            _player.setVolume(0f);
        }

        if (started && request.stopsImmediately()) {
            TCBManajer.bActionStop = true;
            _holdsActionStop = true;
        }
    }

    private void finishPlayback(GlobalWork gw) {
        _player.stop();

        if (_bgmMutedByThisVideo) {
            SoundEffect.getInstance().resumeBgm();
            _bgmMutedByThisVideo = false;
        }
        if (_seMutedByThisVideo) {
            SoundEffect.getInstance().resumeSE();
            _seMutedByThisVideo = false;
        }
        if (_holdsActionStop) {
            TCBManajer.bActionStop = false;
            _holdsActionStop = false;
        }

        _windowEndMs = -1;
        _windowLoops = false;
        _hasEverPlayed = false;
    }

    public void stop() {
        finishPlayback(null);
        _currentRequest = null;
    }

    public boolean holdPat(GlobalWork gw, ObjReq obj) {
        if (_currentRequest == null) {
            return false;
        }

        checkWindow();

        boolean stillActive = isBusy();

        if (stillActive) {
            obj.time = HOLD_TIME_VALUE;
            obj.timeMax = HOLD_TIME_VALUE;
            if (_currentRequest.stopPointIsActive()
                    && obj.ano == _currentRequest.stopAno
                    && obj.pat == _currentRequest.stopMpt) {
                TCBManajer.bActionStop = true;
                _holdsActionStop = true;
            }
            return true;
        } else {
            obj.time = 0;
            obj.timeMax = 0;
            obj.wActFlag |= 2;
            finishPlayback(gw);
            _currentRequest = null;
            return false;
        }
    }

    public void onAnoChanging(GlobalWork gw) {
        if (_currentRequest != null && _currentRequest.hasFlag(VideoRequest.FLAG_END_ANO)) {
            finishPlayback(gw);
            _currentRequest = null;
        }
    }

    // CORRIGIDO: precisa considerar hasPendingOrActivePlayer(), não só o
    // estado confirmado do ExoPlayer — sem isto, holdPat() via isBusy()
    // == false no exato frame seguinte a playFromDac() (o ExoPlayer ainda
    // não teve tempo de sair de STATE_IDLE), e finishPlayback() matava o
    // vídeo antes dele sequer começar a preparar de verdade. Mesma classe
    // de corrida já identificada e corrigida na versão anterior do
    // VideoTexture — reintroduzida por engano na reescrita, corrigida
    // aqui de novo.
    private boolean isBusy() {
        return _player.hasPendingOrActivePlayer()
                || _player.getPlaybackState() == androidx.media3.common.Player.STATE_BUFFERING
                || _player.getPlaybackState() == androidx.media3.common.Player.STATE_READY
                || _player.isPlaying();
    }

    private void checkWindow() {
        if (_windowEndMs < 0) return;
        if (_player.getCachedPositionMs() < _windowEndMs) return;

        if (_windowLoops) {
            _player.seekTo(0);
        } else {
            _player.stop();
            _windowEndMs = -1;
        }
    }

    public boolean checkSkipHold(boolean touchInsideArea) {
        if (_currentRequest == null || _currentRequest.canSkipSeconds <= 0) return false;

        if (touchInsideArea) {
            if (_skipHoldStartMillis == 0) {
                _skipHoldStartMillis = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - _skipHoldStartMillis >= _currentRequest.canSkipSeconds * 1000L) {
                _skipHoldStartMillis = 0;
                stop();
                return true;
            }
        } else {
            _skipHoldStartMillis = 0;
        }
        return false;
    }

    public boolean isPlaying() {
        return _player.isPlaying();
    }

    // ── Consultas para DrawExec (equivalentes aos antigos campos públicos
    //    dacFlags/dacWidth/dacHeight) ───────────────────────────────────
    // DrawExec precisa saber, ANTES de chamar draw(), se o vídeo é
    // fullscreen (para decidir o cálculo de touchInsideVideo) e qual o
    // tamanho para calcular a área de toque. Isso não depende de estado
    // do player, só do VideoRequest atual — retorna 0/false com segurança
    // se não houver request (não deveria ser chamado nesse caso, já que
    // getActiveVideo() só retorna não-null quando hasActiveRequest()).
    public int getDacFlags() {
        return _currentRequest != null ? _currentRequest.flags : 0;
    }

    public boolean isFullscreen() {
        return _currentRequest != null && _currentRequest.hasFlag(VideoRequest.FLAG_FULLSCREEN);
    }

    public int getDacWidth() {
        if (_currentRequest == null) return 0;
        if (_currentRequest.width == 0 && _player.getActualVideoWidth() > 0) {
            return _player.getActualVideoWidth();
        }
        return _currentRequest.width;
    }

    public int getDacHeight() {
        if (_currentRequest == null) return 0;
        if (_currentRequest.height == 0 && _player.getActualVideoHeight() > 0) {
            return _player.getActualVideoHeight();
        }
        return _currentRequest.height;
    }

    // Usado por ExtrasReader.getActiveVideo() para decidir se DrawExec
    // deve continuar chamando holdPat()/draw() para este objeto. Baseado
    // na EXISTÊNCIA de um pedido (_currentRequest != null), não em estado
    // oscilante do ExoPlayer — ver princípio de design 1 no topo do
    // arquivo. Uma vez que playFromDac() aceitou um pedido, isto
    // permanece true até holdPat()/stop() explicitamente encerrar.
    public boolean hasActiveRequest() {
        return _currentRequest != null;
    }

    public void update() {
        _player.update();
    }

    public void drawFullscreen(int screenWidth, int screenHeight, float alpha) {
        android.opengl.GLES20.glViewport(0, 0, screenWidth, screenHeight);
        if (_player.isPlaying()) {
            _player.renderQuad(_player.getFullscreenVerts(), _player.getIdentityMatrix(), alpha);
        } else if (!_hasEverPlayed && isPreparingOrBuffering() && _thumbTexId != 0) {
            // Frame 0 no lugar da tela preta, só na preparação INICIAL —
            // depois que o vídeo já tocou 1x, uma oscilação momentânea de
            // isPlaying()==false (rebuffering, seek de loop) não volta a
            // mostrar a thumbnail: simplesmente não desenha nada neste
            // frame, mantendo o último frame real na tela.
            renderThumbFullscreen(alpha);
        }
    }

    // flipH/flipV: já resolvidos pelo CHAMADOR (DrawExec), incluindo a
    // herança do pai via XOR — VideoTexture não conhece obj.parent, então
    // toda a lógica de "flip próprio XOR flip do pai" precisa ser feita
    // antes de chamar este método, exatamente como já é feito para
    // sprites normais em DrawExec.
    public void draw(GlobalWork gw, int x, int y, int width, int height, float rotationDegrees,
                      float alpha, boolean flipH, boolean flipV) {
        boolean playing = _player.isPlaying();
        boolean showThumb = !_hasEverPlayed && isPreparingOrBuffering() && _thumbTexId != 0;
        if (!playing && !showThumb) {
            return;
        }

        _player.ensureOrtho(gw);

        // FLAG_FIXO: ignora x/y recebidos (que mudariam a cada frame
        // conforme o objeto se move) e usa a posição congelada no
        // momento em que o vídeo começou. Quando esta flag está ativa, o
        // vídeo se comporta "como fullscreen" no sentido de ignorar
        // qualquer herança de posição/flip/zoom do pai — só o
        // deslocamento do próprio $MID (já embutido em requestX/Y, ver
        // playFromDac) e o tamanho de $VID valem.
        boolean lockedPosition = _currentRequest != null
                && _currentRequest.hasFlag(VideoRequest.FLAG_FIXO);

        int drawX = x, drawY = y;
        boolean effectiveFlipH = flipH, effectiveFlipV = flipV;
        if (lockedPosition) {
            drawX = _lockedX;
            drawY = _lockedY;
            effectiveFlipH = false;
            effectiveFlipV = false;
        }

        // $W/$H == 0: usa a resolução real do vídeo, se já disponível
        // (via onVideoSizeChanged) — senão mantém o tamanho pedido pelo
        // chamador (DrawExec já resolve isso a partir de dacWidth/dacHeight
        // fora daqui; aqui só cobrimos o caso de o request pedir 0,0
        // explicitamente e a resolução real já ter chegado).
        int drawWidth = width, drawHeight = height;
        if (_currentRequest != null) {
            if (_currentRequest.width == 0 && _player.getActualVideoWidth() > 0) {
                drawWidth = _player.getActualVideoWidth();
            }
            if (_currentRequest.height == 0 && _player.getActualVideoHeight() > 0) {
                drawHeight = _player.getActualVideoHeight();
            }
        }

        float cx = drawX + drawWidth / 2f;
        float cy = drawY + drawHeight / 2f;
        float[] mvp = _player.buildPositionedMvp(cx, cy, rotationDegrees, drawWidth, drawHeight,
                effectiveFlipH, effectiveFlipV);

        if (playing) {
            _player.renderQuad(_player.getPositionedVerts(), mvp, alpha);
        } else {
            renderThumbPositioned(mvp, alpha);
        }
    }

    // true quando existe um pedido de vídeo em andamento mas ainda não
    // confirmado como tocando de fato (preparação/buffering inicial) —
    // é nesse intervalo que a thumbnail deve aparecer no lugar do vídeo.
    private boolean isPreparingOrBuffering() {
        return _currentRequest != null && !_player.isPlaying();
    }

    // (a preencher junto da migração da thumbnail: renderThumbFullscreen/
    // renderThumbPositioned precisam do programa/shader de thumbnail, que
    // hoje vive dentro de VideoPlayerGL — ver próxima iteração)
    private void renderThumbFullscreen(float alpha) {
        _player.renderThumbQuad(_thumbTexId, _player.getFullscreenVerts(), _player.getIdentityMatrix(), alpha);
    }

    private void renderThumbPositioned(float[] mvp, float alpha) {
        _player.renderThumbQuad(_thumbTexId, _player.getPositionedVerts(), mvp, alpha);
    }

    // ── Pausa/retomada (app indo para background/voltando) ──────────────
    // Diferente de stop(): não encerra o pedido (_currentRequest
    // permanece), só congela/descongela o ExoPlayer — o vídeo continua
    // de onde parou ao retomar. Usado pelo tie-in de onPause/onResume do
    // AndroidGLView (ver pauseAll/resumeAll abaixo).
    public void pause() {
        _player.pause();
    }

    public void resume() {
        _player.resume();
    }

    // Chamado quando o contexto EGL é perdido/recriado — libera Surface/
    // SurfaceTexture/programas GL. Depois disto, esta instância não pode
    // mais ser usada para desenhar sem antes chamar playFromDac() de novo
    // (que reconstrói tudo via VideoPlayerGL.init()).
    public void invalidateGL() {
        _player.invalidateGL();
    }

    // ── Registro global de instâncias vivas ──────────────────────────────
    // Uma VideoTexture por obj.extras (potencialmente várias tocando ao
    // mesmo tempo). Permite pausar/retomar todas de uma vez (app pause/
    // resume) e invalidar todas de uma vez (perda de contexto EGL), sem
    // que AndroidGLView/TCBManajer precisem rastrear cada instância.
    private static final ArrayList<WeakReference<VideoTexture>> _registry =
            new ArrayList<WeakReference<VideoTexture>>();

    public static void pauseAll() {
        synchronized (_registry) {
            for (int i = _registry.size() - 1; i >= 0; i--) {
                VideoTexture v = _registry.get(i).get();
                if (v == null) _registry.remove(i);
                else v.pause();
            }
        }
    }

    public static void resumeAll() {
        synchronized (_registry) {
            for (int i = _registry.size() - 1; i >= 0; i--) {
                VideoTexture v = _registry.get(i).get();
                if (v == null) _registry.remove(i);
                else v.resume();
            }
        }
    }

    public static void invalidateAllGL() {
        synchronized (_registry) {
            for (int i = _registry.size() - 1; i >= 0; i--) {
                VideoTexture v = _registry.get(i).get();
                if (v == null) _registry.remove(i);
                else v.invalidateGL();
            }
        }
    }
}
