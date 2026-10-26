package com.namcobandaigames.dragonballtap.apk;

import android.opengl.GLES20;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

// ══════════════════════════════════════════════════════════════════════
// AnimatedWebpTexturePreloaded — ÚNICA implementação de AnimatedTexture.
//
// Uso alvo: webps curtos/poucos frames (ex.: idle de personagem, ícones,
// efeitos pequenos) onde o custo de manter N texturas na GPU é
// desprezível, e o benefício é ZERO decodificação/upload em runtime
// depois do load — update() só avança um índice inteiro.
//
// Troca: memória de GPU = N × (largura × altura × 4 bytes), reservada
// integralmente no load. LIMITE DE USO: para webps grandes/alta
// resolução/muitos frames (cutscenes), isto NÃO é adequado — a memória
// de GPU explode (ex.: 720p × 300 frames ≈ 1GB só nesta textura). Use
// VideoTexture/$VID (ExtrasReader.FUNC_PLAY_VIDEO) para esses casos —
// ver AnimatedTexture.java para a explicação completa de por que não
// existe um modo "streamed" alternativo aqui.
//
// O decoder nativo é aberto, usado para decodificar TODOS os frames de
// uma vez, e FECHADO ao final do init() — ele não precisa continuar
// vivo, porque depois do load só se troca qual textureId já existente é
// devolvido, nunca mais se decodifica nada.
// ══════════════════════════════════════════════════════════════════════
public final class AnimatedWebpTexturePreloaded extends AnimatedTexture {

    private int[] _frameTexIds;
    private int[] _frameDelaysMs;
    private int _width, _height;
    private int _currentFrame = 0;
    private long _lastFrameTimeMs;

    // Mantido só para reloadGL() pós-perda de contexto EGL — os
    // textureIds antigos morrem junto com o contexto, mas os bytes crus
    // do .webp sobrevivem (dados de CPU), permitindo reprocessar do zero.
    private byte[] _webpBytesKept;

    @Override
    public boolean init(GL10 gl, byte[] webpBytes) {
        _webpBytesKept = webpBytes;

        long handle = WebpAnimNative.open(webpBytes);
        if (handle == 0) {
            return false;
        }

        try {
            _width = WebpAnimNative.getWidth(handle);
            _height = WebpAnimNative.getHeight(handle);
            int frameCount = WebpAnimNative.getFrameCount(handle);

            if (_width <= 0 || _height <= 0 || frameCount <= 0) {
                return false;
            }

            // Libera texturas de uma tentativa anterior, se reloadGL()
            // está chamando init() de novo por cima de um estado já
            // existente (evita vazar textureIds da GPU).
            deleteFrameTextures();

            _frameTexIds = new int[frameCount];
            _frameDelaysMs = new int[frameCount];

            ByteBuffer buf = ByteBuffer.allocateDirect(_width * _height * 4)
                    .order(ByteOrder.nativeOrder());

            int[] genIds = new int[1];
            for (int i = 0; i < frameCount; i++) {
                buf.position(0);
                int delay = WebpAnimNative.nextFrame(handle, buf);
                buf.position(0);

                // Erro de decodificação no meio do loop: trunca aqui em
                // vez de deixar frames "lixo" no array — a animação toca
                // só até o último frame válido, e faz loop a partir dele.
                if (delay < 0) {
                    _frameTexIds = java.util.Arrays.copyOf(_frameTexIds, i);
                    _frameDelaysMs = java.util.Arrays.copyOf(_frameDelaysMs, i);
                    break;
                }

                GLES20.glGenTextures(1, genIds, 0);
                int texId = genIds[0];

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                        _width, _height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);

                _frameTexIds[i] = texId;
                _frameDelaysMs[i] = Math.max(delay, 1); // nunca 0 — evita troca a cada tick por engano
            }

            if (_frameTexIds.length == 0) {
                return false; // nenhum frame decodificado com sucesso
            }

            _currentFrame = 0;
            _lastFrameTimeMs = SystemClock.elapsedRealtime();

            // Registra esta instância no tick global (GameData.
            // updateAllAnimatedTextures, chamado 1x por frame de desenho
            // em TCBManajer.DrawExec) — sem isto, update() nunca seria
            // chamado e a textura ficaria parada no frame 0 para sempre.
            GameData.registerAnimatedTexture(this);
            return true;

        } finally {
            // Decoder não precisa continuar vivo — tudo já está na GPU.
            WebpAnimNative.close(handle);
        }
    }

    @Override
    public void update() {
        if (_frameTexIds == null || _frameTexIds.length <= 1) {
            return; // nada a animar (0 ou 1 frame só)
        }
        long now = SystemClock.elapsedRealtime();
        if (now - _lastFrameTimeMs < _frameDelaysMs[_currentFrame]) {
            return; // delay do frame atual ainda não venceu — não faz nada
        }
        _currentFrame = (_currentFrame + 1) % _frameTexIds.length;
        _lastFrameTimeMs = now;
    }

    @Override
    public int GetImage() {
        return (_frameTexIds != null && _frameTexIds.length > 0) ? _frameTexIds[_currentFrame] : 0;
    }

    @Override
    public int GetWidth() {
        return _width;
    }

    @Override
    public int GetHeight() {
        return _height;
    }

    @Override
    public void reloadGL(GL10 gl) {
        // Todos os textureIds antigos já morreram junto com o contexto
        // EGL perdido (não são mais válidos, nem precisam de
        // glDeleteTextures) — reprocessa do zero a partir dos bytes
        // mantidos. _currentFrame é recalculado a partir de 0 dentro de
        // init(); se quiser preservar em qual frame a animação estava no
        // momento da perda de contexto, seria necessário guardar
        // _currentFrame antes de chamar init() e restaurá-lo depois —
        // não feito aqui por simplicidade (perda de contexto é rara o
        // bastante para o "salto" visual ser imperceptível).
        if (_webpBytesKept != null) {
            init(gl, _webpBytesKept);
        }
    }

    @Override
    public void Dispose(GL10 gl) {
        deleteFrameTextures();
        _webpBytesKept = null;
    }

    private void deleteFrameTextures() {
        if (_frameTexIds != null && _frameTexIds.length > 0) {
            GLES20.glDeleteTextures(_frameTexIds.length, _frameTexIds, 0);
        }
        _frameTexIds = null;
        _frameDelaysMs = null;
    }
}
