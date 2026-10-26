package com.namcobandaigames.dragonballtap.apk;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.ByteArrayDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

// ══════════════════════════════════════════════════════════════════════
// VideoPlayerGL — ciclo de vida PURO do ExoPlayer + desenho GL. Não sabe
// NADA sobre $VID, $WAIT, BGM, ano/pat, ObjReq — é só "tocar bytes de
// vídeo numa textura GL", reutilizável independente de qualquer regra de
// jogo. Toda a lógica de negócio (o que fazer quando o vídeo começa/
// termina, idempotência, wait, mute) vive em VideoPlaybackController, que
// USA esta classe como ferramenta.
//
// THREADING — preservado do design original (ver histórico): ExoPlayer só
// pode ser criado/controlado na MAIN THREAD, mas todo o resto do jogo
// roda na GLThread. Toda operação no player é despachada via
// _mainHandler.post(...), e cada startPlayer() carrega um token de
// geração (_generation) — se um novo pedido invalidar um pedido antigo
// ainda "em trânsito" na fila da main thread, o Runnable antigo aborta
// sem tocar no player atual. Isso é o que evita um setup antigo pisar
// por cima de um setup mais novo.
// ══════════════════════════════════════════════════════════════════════
final class VideoPlayerGL {

    private static final String VERTEX_SHADER =
            "attribute vec2 a_position;\n" +
            "attribute vec2 a_texCoord;\n" +
            "uniform mat4 u_mvpMatrix;\n" +
            "uniform mat4 u_texMatrix;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main() {\n" +
            "    gl_Position = u_mvpMatrix * vec4(a_position, 0.0, 1.0);\n" +
            "    vec4 uv = u_texMatrix * vec4(a_texCoord, 0.0, 1.0);\n" +
            "    v_texCoord = uv.xy;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES u_texture;\n" +
            "uniform float u_alpha;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_texture, v_texCoord);\n" +
            "    gl_FragColor = vec4(texColor.rgb, texColor.a * u_alpha);\n" +
            "}\n";

    private static final String THUMB_FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform float u_alpha;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_texture, v_texCoord);\n" +
            "    gl_FragColor = vec4(texColor.rgb, texColor.a * u_alpha);\n" +
            "}\n";

    private int _program = 0;
    private int _aPosition = -1;
    private int _aTexCoord = -1;
    private int _uMvpMatrix = -1;
    private int _uTexMatrix = -1;
    private int _uTexture = -1;
    private int _uAlpha = -1;

    // Programa IRMÃO, só pra desenhar a thumbnail (sampler2D comum, não
    // samplerExternalOES) — precisa ser separado porque uma unit
    // configurada como externalOES nunca pode ser lida por um sampler2D,
    // e vice-versa. Vertex shader idêntico (mesma geometria/matrizes), só
    // o fragment shader muda o tipo de sampler.
    private int _thumbProgram = 0;
    private int _thumbAPosition = -1;
    private int _thumbATexCoord = -1;
    private int _thumbUMvpMatrix = -1;
    private int _thumbUTexMatrix = -1;
    private int _thumbUTexture = -1;
    private int _thumbUAlpha = -1;

    private final int[] _texId = new int[1];
    private SurfaceTexture _surfaceTexture;
    private Surface _surface;
    private volatile ExoPlayer _player;
    private Context _appContext;

    private final Handler _mainHandler = new Handler(Looper.getMainLooper());
    private volatile int _cachedPlaybackState = Player.STATE_IDLE;
    private volatile boolean _cachedIsPlaying = false;
    private volatile long _cachedPositionMs = 0;

    private final float[] _texMatrix = new float[16];
    private final float[] _identity = new float[16];
    private volatile boolean _frameAvailable = false;

    private final float[] _orthoMatrix = new float[16];
    private int _orthoW = -1;
    private int _orthoH = -1;

    private final FloatBuffer _fsVerts;
    private final FloatBuffer _posVerts;
    private final FloatBuffer _quadUV;
    private final FloatBuffer _thumbQuadUV; // UV invertido em Y — ver renderThumbQuad
    private final float[] _modelMatrix = new float[16];
    private final float[] _mvpOut = new float[16];

    // Tamanho real do vídeo, atualizado via onVideoSizeChanged — usado
    // por VideoPlaybackController para resolver $W/$H == 0 ("original").
    private volatile int _actualVideoWidth = 0;
    private volatile int _actualVideoHeight = 0;

    // Token de geração — ver comentário de threading no topo da classe.
    private volatile int _generation = 0;

    // Callback opcional para o orquestrador saber de mudanças de estado
    // sem precisar dar poll a cada frame. Todos os métodos rodam na MAIN
    // THREAD (mesma thread dos callbacks do Player.Listener) — o
    // orquestrador é responsável por qualquer sincronização necessária.
    interface Listener {
        void onPlaybackStateChanged(int state);
        void onIsPlayingChanged(boolean isPlaying);
        void onVideoSizeChanged(int width, int height);
    }

    private Listener _listener;

    void setListener(Listener listener) {
        _listener = listener;
    }

    VideoPlayerGL() {
        Matrix.setIdentityM(_identity, 0);

        float[] fs = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
        ByteBuffer bb = ByteBuffer.allocateDirect(fs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        _fsVerts = bb.asFloatBuffer();
        _fsVerts.put(fs).position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(8 * 4);
        bb2.order(ByteOrder.nativeOrder());
        _posVerts = bb2.asFloatBuffer();

        float[] uv = {0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f};
        ByteBuffer bb3 = ByteBuffer.allocateDirect(uv.length * 4);
        bb3.order(ByteOrder.nativeOrder());
        _quadUV = bb3.asFloatBuffer();
        _quadUV.put(uv).position(0);

        // NOVO: UV com Y invertido, usado só para a thumbnail. O vídeo
        // real (SurfaceTexture/samplerExternalOES) já recebe a correção
        // de orientação de graça via _texMatrix (getTransformMatrix()),
        // mas a thumbnail (Bitmap → glTexImage2D) não passa por essa
        // matriz — ela usa u_texMatrix = identidade (ver renderThumbQuad).
        // Um android.graphics.Bitmap tem a linha 0 no TOPO da imagem,
        // enquanto a convenção de UV do OpenGL tem v=0 na base — sem essa
        // inversão, a thumbnail aparece de cabeça para baixo.
        float[] thumbUv = {0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f};
        ByteBuffer bb4 = ByteBuffer.allocateDirect(thumbUv.length * 4);
        bb4.order(ByteOrder.nativeOrder());
        _thumbQuadUV = bb4.asFloatBuffer();
        _thumbQuadUV.put(thumbUv).position(0);
    }

    // Chamado 1x na thread GL, com o contexto ES 2.0 já ativo. Idempotente.
    void init(Context appContext) {
        if (_program != 0) return;
        _appContext = appContext.getApplicationContext();

        int vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        _program = GLES20.glCreateProgram();
        GLES20.glAttachShader(_program, vs);
        GLES20.glAttachShader(_program, fs);
        GLES20.glLinkProgram(_program);
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);

        _aPosition = GLES20.glGetAttribLocation(_program, "a_position");
        _aTexCoord = GLES20.glGetAttribLocation(_program, "a_texCoord");
        _uMvpMatrix = GLES20.glGetUniformLocation(_program, "u_mvpMatrix");
        _uTexMatrix = GLES20.glGetUniformLocation(_program, "u_texMatrix");
        _uTexture = GLES20.glGetUniformLocation(_program, "u_texture");
        _uAlpha = GLES20.glGetUniformLocation(_program, "u_alpha");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1); // unit dedicada — nunca a mesma do Graphics2D (unit 0)
        GLES20.glGenTextures(1, _texId, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, _texId[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        _surfaceTexture = new SurfaceTexture(_texId[0]);
        _surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture st) {
                _frameAvailable = true;
            }
        });
        _surface = new Surface(_surfaceTexture);

        // ── Programa irmão pra thumbnail (sampler2D comum) ──────────────
        int tvs = compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER); // mesmo vertex shader
        int tfs = compile(GLES20.GL_FRAGMENT_SHADER, THUMB_FRAGMENT_SHADER);
        _thumbProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(_thumbProgram, tvs);
        GLES20.glAttachShader(_thumbProgram, tfs);
        GLES20.glLinkProgram(_thumbProgram);
        GLES20.glDeleteShader(tvs);
        GLES20.glDeleteShader(tfs);

        _thumbAPosition = GLES20.glGetAttribLocation(_thumbProgram, "a_position");
        _thumbATexCoord = GLES20.glGetAttribLocation(_thumbProgram, "a_texCoord");
        _thumbUMvpMatrix = GLES20.glGetUniformLocation(_thumbProgram, "u_mvpMatrix");
        _thumbUTexMatrix = GLES20.glGetUniformLocation(_thumbProgram, "u_texMatrix");
        _thumbUTexture = GLES20.glGetUniformLocation(_thumbProgram, "u_texture");
        _thumbUAlpha = GLES20.glGetUniformLocation(_thumbProgram, "u_alpha");
        // Nota: o vertex shader compartilhado multiplica a_texCoord por
        // u_texMatrix — como uniforms não setados em GLSL ES começam
        // zerados (não identidade!), renderThumbQuad() PRECISA setar
        // u_texMatrix = identidade explicitamente a cada draw.
    }

    private int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }

    // ── Reprodução ────────────────────────────────────────────────────────

    boolean playBytes(byte[] data, boolean loopWholeVideo) {
        try {
            final byte[] dataCopy = data;
            DataSource.Factory factory = new DataSource.Factory() {
                @Override
                public DataSource createDataSource() {
                    return new ByteArrayDataSource(dataCopy);
                }
            };
            MediaItem item = MediaItem.fromUri(Uri.parse("bytes://video/" + System.identityHashCode(data)));
            MediaSource source = new ProgressiveMediaSource.Factory(factory).createMediaSource(item);
            return startPlayer(source, loopWholeVideo);
        } catch (Exception e) {
            return false;
        }
    }

    boolean playAsset(Context ctx, String assetName, boolean loopWholeVideo) {
        try {
            AssetFileDescriptor afd = ctx.getAssets().openFd(assetName);
            afd.close();
            MediaItem item = MediaItem.fromUri(Uri.parse("asset:///" + assetName));
            MediaSource source = new ProgressiveMediaSource.Factory(
                    new androidx.media3.datasource.DefaultDataSource.Factory(ctx))
                    .createMediaSource(item);
            return startPlayer(source, loopWholeVideo);
        } catch (IOException e) {
            return false;
        }
    }

    // true desde o instante em que startPlayer() é chamado (pedido feito)
    // até stop() — independe de o ExoPlayer já existir de fato (ele só é
    // criado dentro do post() na main thread, que pode rodar alguns
    // milissegundos depois do pedido). Cobre o intervalo em que
    // _cachedPlaybackState ainda é STATE_IDLE mas já existe uma intenção
    // real de tocar — sem isto, isBusy() (no orquestrador) veria "false"
    // logo após o pedido, e finishPlayback() mataria o vídeo antes dele
    // sequer começar a preparar de verdade.
    private volatile boolean _hasPendingOrActivePlayer = false;

    private boolean startPlayer(final MediaSource source, final boolean loopWholeVideo) {
        try {
            _cachedPlaybackState = Player.STATE_IDLE;
            _cachedIsPlaying = false;
            _cachedPositionMs = 0;
            _hasPendingOrActivePlayer = true;

            final int myGeneration = ++_generation;

            _mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (myGeneration != _generation) {
                        return; // invalidado antes mesmo de começar a rodar
                    }
                    try {
                        ExoPlayer player = new ExoPlayer.Builder(_appContext).build();
                        player.setVideoSurface(_surface);
                        player.setRepeatMode(loopWholeVideo ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
                        player.addListener(new InternalPlayerListener());
                        player.setMediaSource(source);
                        player.prepare();
                        player.setPlayWhenReady(true);

                        if (myGeneration != _generation) {
                            try { player.stop(); } catch (Exception ignored) {}
                            try { player.release(); } catch (Exception ignored) {}
                            return;
                        }

                        _player = player;
                        _mainHandler.post(_positionPoller);
                    } catch (Exception e) {
                        Logue.texto("VideoPlayerGL: EXCEÇÃO em startPlayer (main thread): " + e);
                    }
                }
            });
            return true;
        } catch (Exception e) {
            Logue.texto("VideoPlayerGL: EXCEÇÃO em startPlayer: " + e);
            return false;
        }
    }

    void stop() {
        _generation++;
        _cachedPlaybackState = Player.STATE_IDLE;
        _cachedIsPlaying = false;
        _cachedPositionMs = 0;
        _actualVideoWidth = 0;
        _actualVideoHeight = 0;
        _hasPendingOrActivePlayer = false;

        _mainHandler.removeCallbacks(_positionPoller);
        final ExoPlayer toRelease = _player;
        _player = null;
        if (toRelease != null) {
            _mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    try { toRelease.stop(); } catch (Exception ignored) {}
                    try { toRelease.release(); } catch (Exception ignored) {}
                }
            });
        }
    }

    void pause() {
        _mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (_player != null) {
                    _player.setPlayWhenReady(false);
                }
            }
        });
    }

    void resume() {
        _mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (_player != null) {
                    _player.setPlayWhenReady(true);
                }
            }
        });
    }

    void setVolume(final float volume) {
        _mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (_player != null) {
                    _player.setVolume(volume);
                }
            }
        });
    }

    void seekTo(final long ms) {
        _mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (_player != null) {
                    try {
                        _player.seekTo(ms);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    boolean isPlaying() {
        return _cachedIsPlaying;
    }

    // true desde o pedido de play até stop() — ver comentário no campo
    // _hasPendingOrActivePlayer acima. É isto (não getPlaybackState()
    // sozinho) que o orquestrador deve considerar para decidir se o
    // vídeo ainda é "relevante" logo após o pedido, antes do 1º
    // BUFFERING/READY confirmado.
    boolean hasPendingOrActivePlayer() {
        return _hasPendingOrActivePlayer;
    }

    int getPlaybackState() {
        return _cachedPlaybackState;
    }

    long getCachedPositionMs() {
        return _cachedPositionMs;
    }

    int getActualVideoWidth() {
        return _actualVideoWidth;
    }

    int getActualVideoHeight() {
        return _actualVideoHeight;
    }

    private final Runnable _positionPoller = new Runnable() {
        @Override
        public void run() {
            if (_player != null) {
                try {
                    _cachedPositionMs = _player.getCurrentPosition();
                } catch (Exception ignored) {
                }
                _mainHandler.postDelayed(this, 50);
            }
        }
    };

    void update() {
        if (_frameAvailable) {
            _frameAvailable = false;
            _surfaceTexture.updateTexImage();
            _surfaceTexture.getTransformMatrix(_texMatrix);
        }
    }

    void ensureOrtho(GlobalWork gw) {
        if (_orthoW != gw.iScreenScalWidth || _orthoH != gw.iScreenScalHeight) {
            _orthoW = gw.iScreenScalWidth;
            _orthoH = gw.iScreenScalHeight;
            float l = -(_orthoW / 2f);
            float r = _orthoW / 2f;
            float b = -(_orthoH / 2f);
            float t = _orthoH / 2f;
            Matrix.orthoM(_orthoMatrix, 0, l, r, b, t, -100f, 100f);
        }
    }

    float[] getOrthoMatrix() {
        return _orthoMatrix;
    }

    FloatBuffer getFullscreenVerts() {
        return _fsVerts;
    }

    float[] getIdentityMatrix() {
        return _identity;
    }

    // flipH/flipV: espelha o quad no eixo X/Y, respectivamente — mesma
    // técnica que qualquer sprite 2D usa (escala negativa no eixo
    // espelhado). Passar false/false reproduz exatamente o comportamento
    // anterior (sem flip).
    float[] buildPositionedMvp(float cx, float cy, float rotationDegrees, float w, float h,
                                boolean flipH, boolean flipV) {
        _posVerts.clear();
        _posVerts.put(new float[]{
                -0.5f, -0.5f,
                0.5f, -0.5f,
                -0.5f, 0.5f,
                0.5f, 0.5f
        });
        _posVerts.position(0);

        Matrix.setIdentityM(_modelMatrix, 0);
        Matrix.translateM(_modelMatrix, 0, cx, -cy, 0f);
        Matrix.rotateM(_modelMatrix, 0, rotationDegrees, 0f, 0f, 1f);
        Matrix.scaleM(_modelMatrix, 0, flipH ? -w : w, flipV ? -h : h, 1f);

        Matrix.multiplyMM(_mvpOut, 0, _orthoMatrix, 0, _modelMatrix, 0);
        return _mvpOut;
    }

    FloatBuffer getPositionedVerts() {
        return _posVerts;
    }

    void renderQuad(FloatBuffer verts, float[] mvp, float alpha) {
        GLES20.glUseProgram(_program);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, _texId[0]);
        GLES20.glUniform1i(_uTexture, 1);
        GLES20.glUniformMatrix4fv(_uTexMatrix, 1, false, _texMatrix, 0);
        GLES20.glUniformMatrix4fv(_uMvpMatrix, 1, false, mvp, 0);
        GLES20.glUniform1f(_uAlpha, alpha);

        GLES20.glEnableVertexAttribArray(_aPosition);
        verts.position(0);
        GLES20.glVertexAttribPointer(_aPosition, 2, GLES20.GL_FLOAT, false, 0, verts);

        GLES20.glEnableVertexAttribArray(_aTexCoord);
        _quadUV.position(0);
        GLES20.glVertexAttribPointer(_aTexCoord, 2, GLES20.GL_FLOAT, false, 0, _quadUV);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(_aPosition);
        GLES20.glDisableVertexAttribArray(_aTexCoord);
    }

    // texId aqui é o textureId da THUMBNAIL (já extraída/subida por
    // GameData no carregamento do .pac) — VideoPlayerGL não gera nem é
    // dono dessa textura, só sabe desenhá-la com o shader/programa certo
    // (sampler2D comum, diferente do samplerExternalOES do vídeo real).
    void renderThumbQuad(int texId, FloatBuffer verts, float[] mvp, float alpha) {
        GLES20.glUseProgram(_thumbProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2); // unit própria — não conflita com a unit 1 do vídeo (OES)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLES20.glUniform1i(_thumbUTexture, 2);
        GLES20.glUniformMatrix4fv(_thumbUTexMatrix, 1, false, _identity, 0);
        GLES20.glUniformMatrix4fv(_thumbUMvpMatrix, 1, false, mvp, 0);
        GLES20.glUniform1f(_thumbUAlpha, alpha);

        GLES20.glEnableVertexAttribArray(_thumbAPosition);
        verts.position(0);
        GLES20.glVertexAttribPointer(_thumbAPosition, 2, GLES20.GL_FLOAT, false, 0, verts);

        GLES20.glEnableVertexAttribArray(_thumbATexCoord);
        _thumbQuadUV.position(0);
        GLES20.glVertexAttribPointer(_thumbATexCoord, 2, GLES20.GL_FLOAT, false, 0, _thumbQuadUV);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(_thumbAPosition);
        GLES20.glDisableVertexAttribArray(_thumbATexCoord);
    }

    private class InternalPlayerListener implements Player.Listener {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            _cachedPlaybackState = playbackState;

            // CORRIGIDO: _hasPendingOrActivePlayer só existe para cobrir a
            // janela curta entre "pedido de play" (startPlayer chamado) e
            // "1º estado real confirmado pelo ExoPlayer" — sem isso,
            // isBusy() veria false logo após o pedido e mataria o vídeo
            // antes dele preparar (ver histórico de correção anterior).
            // Mas, uma vez que QUALQUER estado real chegou aqui (IDLE,
            // BUFFERING, READY ou ENDED), esse flag já cumpriu seu papel —
            // se ele continuar true para sempre, isBusy() nunca mais cai
            // para false quando o vídeo termina (STATE_ENDED), porque
            // NADA além de stop() o zera, e stop() só é chamado de
            // dentro de finishPlayback(), que só roda quando isBusy() já
            // é false — uma referência circular que travava o
            // encerramento (BGM/trava de jogo nunca eram desfeitos).
            _hasPendingOrActivePlayer = false;

            if (_listener != null) {
                _listener.onPlaybackStateChanged(playbackState);
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            _cachedIsPlaying = isPlaying;
            if (_listener != null) {
                _listener.onIsPlayingChanged(isPlaying);
            }
        }

        @Override
        public void onVideoSizeChanged(androidx.media3.common.VideoSize videoSize) {
            _actualVideoWidth = videoSize.width;
            _actualVideoHeight = videoSize.height;
            if (_listener != null) {
                _listener.onVideoSizeChanged(videoSize.width, videoSize.height);
            }
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            Logue.texto("VideoPlayerGL: onPlayerError: " + error);
        }

        @Override public void onEvents(Player player, Player.Events events) {}
        @Override public void onTimelineChanged(androidx.media3.common.Timeline timeline, int reason) {}
        @Override public void onMediaItemTransition(MediaItem mediaItem, int reason) {}
        @Override public void onTracksChanged(androidx.media3.common.Tracks tracks) {}
        @Override public void onMediaMetadataChanged(androidx.media3.common.MediaMetadata mediaMetadata) {}
        @Override public void onPlaylistMetadataChanged(androidx.media3.common.MediaMetadata mediaMetadata) {}
        @Override public void onIsLoadingChanged(boolean isLoading) {}
        @Override public void onLoadingChanged(boolean isLoading) {}
        @Override public void onAvailableCommandsChanged(Player.Commands availableCommands) {}
        @Override public void onTrackSelectionParametersChanged(androidx.media3.common.TrackSelectionParameters parameters) {}
        @Override public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {}
        @Override public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {}
        @Override public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {}
        @Override public void onRepeatModeChanged(int repeatMode) {}
        @Override public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}
        @Override public void onPlayerErrorChanged(PlaybackException error) {}
        @Override public void onPositionDiscontinuity(int reason) {}
        @Override public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {}
        @Override public void onPlaybackParametersChanged(androidx.media3.common.PlaybackParameters playbackParameters) {}
        @Override public void onSeekBackIncrementChanged(long seekBackIncrementMs) {}
        @Override public void onSeekForwardIncrementChanged(long seekForwardIncrementMs) {}
        @Override public void onMaxSeekToPreviousPositionChanged(long maxSeekToPreviousPositionMs) {}
        @Override public void onAudioSessionIdChanged(int audioSessionId) {}
        @Override public void onAudioAttributesChanged(androidx.media3.common.AudioAttributes audioAttributes) {}
        @Override public void onVolumeChanged(float volume) {}
        @Override public void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {}
        @Override public void onDeviceInfoChanged(androidx.media3.common.DeviceInfo deviceInfo) {}
        @Override public void onDeviceVolumeChanged(int deviceVolume, boolean muted) {}
        @Override public void onSurfaceSizeChanged(int width, int height) {}
        @Override public void onRenderedFirstFrame() {}
        @Override public void onCues(java.util.List<androidx.media3.common.text.Cue> cues) {}
        @Override public void onCues(androidx.media3.common.text.CueGroup cueGroup) {}
        @Override public void onMetadata(androidx.media3.common.Metadata metadata) {}
    }

    void invalidateGL() {
        stop();
        if (_surface != null) {
            _surface.release();
            _surface = null;
        }
        if (_surfaceTexture != null) {
            _surfaceTexture.release();
            _surfaceTexture = null;
        }
        _program = 0;
        _thumbProgram = 0;
        _texId[0] = 0;
    }
}
