package com.namcobandaigames.dragonballtap.apk;

import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class GameData {

    public static final int SOUND_MAX = 20;
    public static final int TYPE_ACT = 1;
    public static final int TYPE_BIN = 2;
    public static final int TYPE_CNV = 0;
    public static final int TYPE_DAC = 3;
    public static final int TYPE_MAX = 4;
    protected final byte[][] data = new byte[4][];
    protected int iSoundCount = 0;
    protected final ArrayList imageList = new ArrayList();
    protected final ArrayList videoList = new ArrayList();
    public byte[][] vid = new byte[0][];

    // NOVO — fila PRÓPRIA de texturas animadas (webp), separada de
    // imageList/cnv de propósito: a função extra FUNC_ANIM_TEX (ver
    // ExtrasReader / $GIF) referencia um índice DIRETO nesta fila, sem
    // passar pela tradução via cnv que os PNGs manuais usam. Mesmo
    // padrão "ArrayList durante o load -> array no final" que
    // vid/videoList já seguem.
    protected final ArrayList animTexList = new ArrayList();
    public AnimatedTexture[] animTex = new AnimatedTexture[0];
    // NOVO — paralelo a vid[] por índice: textureId da thumbnail (frame 0)
    // de cada vídeo, já extraída e subida no CARREGAMENTO do .pac (não mais
    // na hora do gatilho $VID em VideoTexture.playFromDac). Isso elimina o
    // delay de extração (~3-4ms) do caminho crítico de "gatilho -> aparece
    // na tela", movendo esse custo para o momento de loading, que já
    // tolera uma pequena pausa. 0 = sem thumbnail (extração falhou, ou
    // ainda não carregada).
    public int[] vidThumbTexId = new int[0];
    // Espelha videoList durante o carregamento — convertido para
    // vidThumbTexId (array) ao final, mesmo padrão que videoList/vid já
    // usava.
    protected final ArrayList videoThumbTexIdList = new ArrayList();
    public short[] piGameData = null;
    public int[] piGameDataPos = null;
    public int[] piGameDataXSize = null;
    public int[] piGameDataYSize = null;
    protected final byte[][] sound = new byte[20][];
    protected SpriteData sprite = null;
    protected String NAME;

    public static class SpriteData {

        protected ArrayList _imageList = new ArrayList();
        protected byte[][] pData = new byte[1][];
        protected ArrayList _animTexList = new ArrayList(); // NOVO — fila própria de
                                                            // AnimatedTexture (webp) deste sprite
        public AnimatedTexture[] _animTex = new AnimatedTexture[0]; // NOVO

        public byte[] getData() {
            return this.pData[0];
        }

        public boolean Init(GlobalWork gw, int off, ByteBuffer src) {
            return false;
        }

        // ── Init(byte[]) modificado ─────────────────────────────────────────────
        public boolean Init(GlobalWork gw, int off, byte[] src) {
            try {
                Clear(gw);
                if (src == null) {
                    return false;
                }
                int dataCount = (src[off] & 255) | ((src[off + 1] & 255) << 8);
                for (int i = 0; i < dataCount; i++) {
                    int pos = off + (i << 4) + 2;
                    int dataSize = (src[pos + 4] & 255)
                            | ((src[pos + 5] & 255) << 8)
                            | ((src[pos + 6] & 255) << 16)
                            | ((src[pos + 7] & 255) << 24);
                    int dataPos = off + ((dataCount << 4) + 2) + ((src[pos + 0] & 255)
                                    | ((src[pos + 1] & 255) << 8)
                                    | ((src[pos + 2] & 255) << 16)
                                    | ((src[pos + 3] & 255) << 24));
                    pos += 8;
                    // png
                    if (src[pos] == (byte) 112 && src[pos + 1] == (byte) 110 && src[
                                    pos + 2] == (byte) 103) {
                        AndroidGLTexture tex = new AndroidGLTexture();
                        if (!tex.loadTexture(gw.gl, src, dataPos, dataSize, true)) {
                            return false;
                        } else {
                            this._imageList.add(tex);
                        }
                        // bin
                    } else if (src[pos] == (byte) 98 && src[pos + 1] == (byte) 105 && src[
                                    pos + 2] == (byte) 110) {
                        this.pData[0] = new byte[dataSize];
                        if (this.pData[0] == null) {
                            return false;
                        }
                        System.arraycopy(src, dataPos, this.pData[0], 0, dataSize);
                        // web (webp) — NOVO
                        // Magic de 3 bytes 'w'(119) 'e'(101) 'b'(98), mesmo padrão
                        // de checagem crua que png/bin já usam aqui (SpriteData
                        // não usa os TYPE_* de 4 bytes de GameData.decodeData —
                        // tem seu próprio formato de magic curto). Bytes crus do
                        // .webp começam em dataPos, tamanho dataSize — igual ao
                        // bloco "webp" de GameData.decodeData, sem prefixo de modo
                        // (só existe o caminho pré-decodificado — ver
                        // AnimatedTexture.java).
                    } else if (src[pos] == (byte) 119 && src[pos + 1] == (byte) 101 && src[
                                    pos + 2] == (byte) 98) {
                        byte[] webpBytes = new byte[dataSize];
                        System.arraycopy(src, dataPos, webpBytes, 0, dataSize);

                        AnimatedTexture animTex = new AnimatedWebpTexturePreloaded();
                        if (animTex.init(gw.gl, webpBytes)) {
                            this._animTexList.add(animTex);
                            this._animTex = (AnimatedTexture[]) this._animTexList.toArray(
                                            new AnimatedTexture[this._animTexList.size()]);
                        }
                        // Diferente de png/bin: uma falha aqui NÃO aborta o
                        // Init inteiro (sem "return false") — um webp que falhou
                        // ao decodificar não deveria impedir o resto do sprite
                        // (PNGs/bin já lidos) de funcionar. Mesmo espírito de
                        // GameData.decodeData, que só loga e segue.
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public void Clear(GlobalWork gw) {
            int i;
            if (this.pData != null) {
                for (i = 0; i < this.pData.length; i++) {
                    this.pData[i] = null;
                }
            }
            for (i = 0; i < this._imageList.size(); i++) {
                ((AndroidGLTexture) this._imageList.get(i)).Dispose(gw.gl);
            }
            this._imageList.clear();

            // NOVO — sem isto, toda vez que Init(byte[]) roda de novo sobre
            // este SpriteData (Clear() é chamado no início de todo Init),
            // as AnimatedTexture antigas nunca eram liberadas (textureId
            // de GPU nunca deletado — vazamento de VRAM) e a fila
            // _animTexList só crescia, nunca resetava, duplicando
            // entradas a cada recarregamento (ex.: Compiler recarregando
            // o mesmo sprite repetidamente durante testes de dev).
            for (i = 0; i < this._animTexList.size(); i++) {
                ((AnimatedTexture) this._animTexList.get(i)).Dispose(gw.gl);
            }
            this._animTexList.clear();
            this._animTex = new AnimatedTexture[0];
        }

        public void Dispose(GlobalWork gw) {
            Clear(gw);
            this.pData = (byte[][]) null;
            this._imageList = null;
            this._animTexList = null; // NOVO — mesmo padrão de _imageList logo acima
        }

        public int getImage(int no) {
            if (no >= 0 && no < this._imageList.size()) {
                AndroidGLTexture tex = (AndroidGLTexture) this._imageList.get(no);
                if (tex != null) {
                    return tex.GetImage();
                }
            }
            return 0;
        }

        public int getImageWidth(int no) {
            if (no >= 0 && no < this._imageList.size()) {
                AndroidGLTexture tex = (AndroidGLTexture) this._imageList.get(no);
                if (tex != null) {
                    return tex.GetWidth();
                }
            }
            return 1;
        }

        public int getImageHeight(int no) {
            if (no >= 0 && no < this._imageList.size()) {
                AndroidGLTexture tex = (AndroidGLTexture) this._imageList.get(no);
                if (tex != null) {
                    return tex.GetHeight();
                }
            }
            return 1;
        }

        // Consultado por TCBManajer.DrawSprite quando $M==4 — mesma convenção de
        // segurança de índice que getImage/getImageWidth/getImageHeight já usam.
        public AnimatedTexture getAnimTex(int no) {
            if (no >= 0 && no < this._animTex.length) {
                return this._animTex[no];
            }
            return null;
        }
    }

    public boolean Init(GlobalWork gw, String name, int cnvType, int loadFilter) {
        NAME = name;
        boolean z = false;
        Dispose(gw);
        if (name != null && name.length() > 0) {
            try {
                this._viaName = true; // sinaliza ao Init(byte[]) que a origem é um .pac de assets
                z = Init(gw, Utility.getBytes(gw.context.getAssets().open(name + ".pac")), cnvType, loadFilter);
            } catch (IOException e) {
//                e.printStackTrace();
            } finally {
                this._viaName = false;
            }
        }
        return z;
    }

    public boolean Init(GlobalWork gw, byte[] src, int cnvType, int loadFilter) {
        Dispose(gw);
        // ── Suporte à recarga pós-perda de contexto EGL ────────────────────
        boolean fromNamed = this._viaName;
        this._viaName = false;
        registerForGLReload(this);
        this.iCnvTypeSaved = cnvType;
        this.iLoadFilterSaved = loadFilter;
        if (fromNamed) {
            this.pSrcKeep = null; // recarrega reabrindo assets/NAME.pac
        } else {
            // Chamada direta com bytes (ex.: conteúdo baixado): não existe
            // .pac em assets para reabrir — mantém a referência para a
            // recarga. (São os mesmos bytes já vivos no chamador.)
            this.NAME = null;
            this.pSrcKeep = src;
        }
        // ───────────────────────────────────────────────────────────────────
        if (src == null) {
            return false;
        }
        int dataCount = (src[0] & 255) | ((src[1] & 255) << 8);
        for (int i = 0; i < dataCount; i++) {
            int pos = (i << 4) + 2;
            int dataSize = (src[pos + 4] & 255)
                    | ((src[pos + 5] & 255) << 8)
                    | ((src[pos + 6] & 255) << 16)
                    | ((src[pos + 7] & 255) << 24);
            int dataPos = ((dataCount << 4) + 2)
                    + ((src[pos + 0] & 255)
                            | ((src[pos + 1] & 255) << 8)
                            | ((src[pos + 2] & 255) << 16)
                            | ((src[pos + 3] & 255) << 24));
            pos += 8;
            int type = ((src[pos] & 255) << 24) | ((src[pos + 1] & 255) << 16) | ((src[
                                    pos + 2] & 255) << 8) | (src[pos + 3] & 255);
            if (!decodeData(gw, src, dataPos, dataSize, type, cnvType, loadFilter)) {
                return false;
            }
        }
        this.vid = (byte[][]) this.videoList.toArray(new byte[this.videoList.size()][]);
        return true;
    }

    // protected (era private): o Compiler (com.zue.gd.util, subclasse) chama
    // direto em vez de achar o método por reflexão.
    protected boolean decodeData(GlobalWork gw, byte[] src, int dataPos, int dataSize, int dataType, int cnvType, int loadFilter) {
        switch (dataType) {
            case 0x706e6700: // png
                if ((loadFilter & 1) == 0) {
                    AndroidGLTexture tex = new AndroidGLTexture();
                    if (!tex.loadTexture(gw.gl, src, dataPos, dataSize, true)) {
                        this.imageList.add(tex);
                    } else {
                        this.imageList.add(tex);
                    }
                }
                break;
            case 0x61637400: // act
                if ((loadFilter & 2) == 0) {
                    if (cnvType != 1) {
                        this.data[1] = null;
                        this.data[1] = new byte[dataSize];
                        System.arraycopy(src, dataPos, this.data[1], 0, dataSize);
                    } else if (!binCnv(1, src, dataPos, dataSize)) {
                        Dispose(gw);
                        return false;
                    }
                }
                break;
            case 0x62696e00: // bin
                if ((loadFilter & 4) == 0) {
                    if (cnvType != 2) {
                        this.data[2] = null;
                        this.data[2] = new byte[dataSize];
                        System.arraycopy(src, dataPos, this.data[2], 0, dataSize);
                    } else if (!binCnv(2, src, dataPos, dataSize)) {
                        Dispose(gw);
                        return false;
                    }
                }
                break;
            case 0x636e7600: // cnv
                if ((loadFilter & 8) == 0) {
                    if (cnvType != 0) {
                        this.data[0] = null;
                        this.data[0] = new byte[dataSize];
                        System.arraycopy(src, dataPos, this.data[0], 0, dataSize);
                    } else if (!binCnv(3, src, dataPos, dataSize)) {
                        Dispose(gw);
                        return false;
                    }
                }
                break;
            case 0x64616300: // dac
                if ((loadFilter & 16) == 0) {
                    if (cnvType != 3) {
                        this.data[3] = null;
                        this.data[3] = new byte[dataSize];
                        System.arraycopy(src, dataPos, this.data[3], 0, dataSize);
                    } else if (!binCnv(4, src, dataPos, dataSize)) {
                        Dispose(gw);
                        return false;
                    }
                }
                break;
            case 0x73707200: // spr
                if ((loadFilter & 32) == 0) {
                    if (this.sprite != null) {
                        this.sprite.Dispose(gw);
                        this.sprite = null;
                    }
                    if (dataSize > 0) {
                        this.sprite = new SpriteData();
                        this.sprite.Init(gw, dataPos, src);
                    }
                }
                break;
            case 0x76696400: // vid
                if ((loadFilter & 64) == 0) {
                    byte[] raw = new byte[dataSize];
                    System.arraycopy(src, dataPos, raw, 0, dataSize);
                    this.videoList.add(raw);
                    this.vid = (byte[][]) this.videoList.toArray(new byte[this.videoList.size()][]);

                    // NOVO: extrai e sobe a thumbnail JÁ AQUI, no carregamento
                    // do .pac — não mais na hora do gatilho $VID
                    // (VideoTexture.playFromDac). Síncrono/bloqueante é
                    // aceitável aqui: decodeData já roda na thread GL durante
                    // o carregamento (tela de loading), que já tolera uma
                    // pequena pausa — diferente de rodar isso durante o jogo
                    // já em andamento, que travaria frames visíveis.
                    int texId = extractAndUploadThumbnail(gw, raw);
                    this.videoThumbTexIdList.add(texId);
                    this.vidThumbTexId = toIntArray(this.videoThumbTexIdList);
                }
                break;
            case 0x77617600: // wav
                // sound[] tem SOUND_MAX posições: um pack com mais WAVs (fácil de
                // acontecer num mod) estourava o array e abortava o load inteiro.
                if (this.iSoundCount >= this.sound.length) {
                    Log.e("GameData " + (NAME == null ? "" : NAME), "mais de " + SOUND_MAX + " WAVs no pack — WAV ignorado");
                    break;
                }
                this.sound[this.iSoundCount] = null;
                this.sound[this.iSoundCount] = new byte[dataSize];
                if (this.sound[this.iSoundCount] != null) {
                    System.arraycopy(src, dataPos, this.sound[this.iSoundCount++], 0, dataSize);
                }
                break;
            case 0x77656200: // "web" (webp) — NOVO
                // Layout do payload: bytes crus do arquivo .webp animado,
                // do início ao fim — SEM byte de modo (só existe um modo:
                // pré-decodificado — ver AnimatedWebpTexturePreloaded).
                //
                // Decisão: o modo não pode viver no gatilho $GIF (lido só
                // em tempo de ACT, tarde demais para decidir COMO
                // decodificar algo que já precisa estar pronto desde o
                // load do .pac) nem alternar em runtime (decodeData roda
                // no load inteiro, antes de saber se/quando algum ACT vai
                // de fato referenciar este webp) — então não há
                // "streamed sob demanda" que valha a pena: o custo de
                // abrir o decoder já é pago aqui de qualquer forma. Por
                // isso só existe o caminho pré-decodificado: N texturas
                // reais, update() em runtime só troca qual índice é
                // devolvido, zero decodificação depois do load.
                //
                // LIMITE DE USO: isto é para animações CURTAS/PEQUENAS
                // (idle de personagem, ícones, efeitos) — N frames em
                // resolução de sprite. Para cutscenes grandes/longas
                // (alta resolução, muitos segundos), o custo de N
                // texturas na GPU é proibitivo; use VideoTexture/$VID
                // (ExtrasReader.FUNC_PLAY_VIDEO), que já existe
                // exatamente para este caso.
                //
                // Não passa por imageList/cnv — vai para a fila própria
                // animTexList. A tradução "$GIF: N" -> textura é feita
                // inteiramente por ExtrasReader/TCBManajer via
                // getAnimTex(N), sem tocar no cnv dos PNGs manuais.
                if ((loadFilter & 1) == 0) {
                    byte[] webpBytes = new byte[dataSize];
                    System.arraycopy(src, dataPos, webpBytes, 0, dataSize);

                    AnimatedTexture animTex = new AnimatedWebpTexturePreloaded();
                    if (!animTex.init(gw.gl, webpBytes)) {
                    } else {
                        this.animTexList.add(animTex);
                        this.animTex = (AnimatedTexture[]) this.animTexList.toArray(
                                        new AnimatedTexture[this.animTexList.size()]);
                    }
                }
                break;
        }
        return true;
    }

    // Consultado por TCBManajer.DrawImageAnimTex — mesma convenção de
    // segurança de índice que getImage/getImageWidth/getImageHeight já
    // usam (devolve null em vez de lançar, chamador decide o fallback).
    public AnimatedTexture getAnimTex(int no) {
        if (no >= 0 && no < this.animTex.length) {
            return this.animTex[no];
        }
        return null;
    }

    /**
     * Sobe um PNG em memória para a textura dada. AndroidGLTexture.loadTexture
     * é protected (só visível neste pacote); o Compiler, em com.zue.gd.util,
     * usa este método em vez de reflexão.
     */
    public static boolean loadPng(GlobalWork gw, AndroidGLTexture tex, byte[] png) {
        return tex.loadTexture(gw.gl, png, 0, png.length, true);
    }

    public int getSoundCount() {
        return this.iSoundCount;
    }

    public byte[] getSoundData(int no) {
        return this.sound[no];
    }

    public byte[] getData(int type) {
//        if (NAME != null && NAME.startsWith("back") && type == 0) {
//            try {
//                throw new RuntimeException("CNV TYPE 0");
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
        return this.data[type];
    }

    public SpriteData getSprite() {
        return this.sprite;
    }

    // NOVO: consultado por VideoTexture (via TCBManajer) na hora de
    // desenhar, no lugar da extração feita na hora do gatilho. Mesmo
    // padrão de segurança de índice que getImage/getImageWidth já usam.
    public int getVideoThumbTexId(int videoIndex) {
        if (videoIndex >= 0 && videoIndex < this.vidThumbTexId.length) {
            return this.vidThumbTexId[videoIndex];
        }
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════════
    // NOVO: extração + upload de thumbnail de vídeo (frame 0), movida para
    // cá a partir de VideoTexture.ensureThumbnail/uploadThumbnail. Aqui
    // roda de forma SÍNCRONA (sem thread separada) porque o contexto de
    // chamada (carregamento do .pac) já tolera uma pequena pausa —
    // diferente do uso original em VideoTexture, que rodava durante o
    // jogo já em andamento e por isso precisava ser assíncrono.
    // ══════════════════════════════════════════════════════════════════════

    // Retorna o textureId gerado, ou 0 se a extração falhar (vídeo
    // corrompido, formato não suportado pelo MediaMetadataRetriever, etc.)
    // — 0 é o mesmo valor de "textura ausente" usado em getImage/AndroidGLTexture.
    private int extractAndUploadThumbnail(GlobalWork gw, byte[] videoData) {
        android.graphics.Bitmap bmp = null;
        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        try {
            retriever.setDataSource(new ByteArrayMediaDataSourceForThumb(videoData));
            // getFrameAtTime(0): pega o frame mais próximo do tempo 0 — não
            // precisa ser EXATAMENTE o frame 0 do encode (I-frame mais
            // próximo do início já é visualmente idêntico ao primeiro
            // frame, para fins de placeholder de transição).
            bmp = retriever.getFrameAtTime(0);
        } catch (Exception e) {
            Logue.StackTrace(e);
            bmp = null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }

        if (bmp == null) {
            return 0;
        }

        int texId = 0;
        try {
            int[] ids = new int[1];
            android.opengl.GLES20.glGenTextures(1, ids, 0);
            texId = ids[0];

            android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, texId);
            android.opengl.GLES20.glTexParameterf(android.opengl.GLES20.GL_TEXTURE_2D,
                    android.opengl.GLES20.GL_TEXTURE_MIN_FILTER, android.opengl.GLES20.GL_LINEAR);
            android.opengl.GLES20.glTexParameterf(android.opengl.GLES20.GL_TEXTURE_2D,
                    android.opengl.GLES20.GL_TEXTURE_MAG_FILTER, android.opengl.GLES20.GL_LINEAR);
            android.opengl.GLES20.glTexParameterf(android.opengl.GLES20.GL_TEXTURE_2D,
                    android.opengl.GLES20.GL_TEXTURE_WRAP_S, android.opengl.GLES20.GL_CLAMP_TO_EDGE);
            android.opengl.GLES20.glTexParameterf(android.opengl.GLES20.GL_TEXTURE_2D,
                    android.opengl.GLES20.GL_TEXTURE_WRAP_T, android.opengl.GLES20.GL_CLAMP_TO_EDGE);
            android.opengl.GLUtils.texImage2D(android.opengl.GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        } finally {
            bmp.recycle();
        }

        return texId;
    }

    private static int[] toIntArray(ArrayList list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (Integer) list.get(i);
        }
        return out;
    }

    // Mesma fonte de dados "clássica" que VideoTexture usava, para o
    // MediaMetadataRetriever ler direto de um byte[] em memória. Duplicada
    // aqui (não compartilhada com VideoTexture) para manter as duas
    // classes desacopladas — a extração de thumbnail agora é
    // responsabilidade exclusiva de GameData.
    private static class ByteArrayMediaDataSourceForThumb extends android.media.MediaDataSource {
        private final byte[] data;

        ByteArrayMediaDataSourceForThumb(byte[] data) {
            this.data = data;
        }

        @Override
        public int readAt(long position, byte[] buffer, int offset, int size) {
            if (position >= data.length) {
                return -1;
            }
            int available = data.length - (int) position;
            int toRead = Math.min(size, available);
            System.arraycopy(data, (int) position, buffer, offset, toRead);
            return toRead;
        }

        @Override
        public long getSize() {
            return data.length;
        }

        @Override
        public void close() {}
    }

// ══════════════════════════════════════════════════════════════════════
    // Tick global de texturas animadas (webp)
    //
    // Registro estático fraco, mesmo padrão de _glRegistry logo abaixo —
    // mas aqui não é sobre GLReload, é sobre AVANÇAR FRAME. Chamado 1x por
    // frame de DESENHO (TCBManajer.DrawExec), não por frame de jogo/tick de
    // ACT — cada AnimatedTexture decide sozinha (via seu próprio guard de
    // tempo) se já é hora de decodificar/subir o próximo frame do webp, ou
    // se ainda não venceu o delay e não faz nada. Registrar aqui é o que
    // permite a mesma instância de AnimatedTexture (referenciada por
    // getAnimTex(N) de N objetos diferentes ao mesmo tempo) avançar UMA
    // vez só por frame de desenho, independente de quantos ObjReq a usam —
    // é assim que se obtém "comportamento igual a um PNG": todo mundo que
    // referencia o mesmo índice vê exatamente o mesmo frame ao mesmo tempo.
    // ══════════════════════════════════════════════════════════════════════
    private static final java.util.ArrayList<
            java.lang.ref.WeakReference<
                    AnimatedTexture>> _animTexRegistry = new java.util.ArrayList<
            java.lang.ref.WeakReference<AnimatedTexture>>();

    // Package-private (não private): chamado a partir de
    // AnimatedWebpTexturePreloaded.init(), no momento em que cada
    // instância é criada — mesmo pacote, sem precisar de getter/setter.
    //
    // Deduplica por IDENTIDADE (==, não equals): reloadGL() chama init()
    // de novo na MESMA instância após perda de contexto EGL — sem esta
    // checagem, cada ciclo de pause/resume do app acumularia mais uma
    // WeakReference da mesma AnimatedTexture, fazendo update() ser
    // chamado múltiplas vezes por frame nela (inofensivo para a
    // correção, já que o guard de tempo dentro de update() ainda
    // impede duplo-avanço real, mas cresce sem limite à toa).
    static void registerAnimatedTexture(AnimatedTexture t) {
        synchronized (_animTexRegistry) {
            for (int i = _animTexRegistry.size() - 1; i >= 0; i--) {
                AnimatedTexture existing = _animTexRegistry.get(i).get();
                if (existing == null) {
                    _animTexRegistry.remove(i);
                } else if (existing == t) {
                    return; // já registrada
                }
            }
            _animTexRegistry.add(new java.lang.ref.WeakReference<AnimatedTexture>(t));
        }
    }

    // Chamado 1x no topo de TCBManajer.DrawExec, antes do loop de desenho.
    public static void updateAllAnimatedTextures() {
        synchronized (_animTexRegistry) {
            for (int i = _animTexRegistry.size() - 1; i >= 0; i--) {
                AnimatedTexture t = _animTexRegistry.get(i).get();
                if (t == null) {
                    _animTexRegistry.remove(i);
                } else {
                    t.update();
                }
            }
        }
    }

// ══════════════════════════════════════════════════════════════════════
    // Recarga pós-perda de contexto EGL
    //
    // Quando o Android destrói o contexto (ex.: app em background), todas as
    // texturas GL morrem, mas os objetos AndroidGLTexture continuam vivos com
    // IDs velhos. Depois de AndroidGLTexture.invalidateAll() zerar os IDs,
    // GLReloadAll() reabre cada .pac e repõe os pixels NOS MESMOS objetos,
    // na MESMA ordem — os índices usados pelo jogo continuam válidos.
    // ══════════════════════════════════════════════════════════════════════
    private boolean _viaName = false;
    private int iCnvTypeSaved = -1;
    private int iLoadFilterSaved = 0;
    private byte[] pSrcKeep = null;

    private static final java.util.ArrayList<
            java.lang.ref.WeakReference<GameData>> _glRegistry = new java.util.ArrayList<
            java.lang.ref.WeakReference<GameData>>();

    // protected (era private): o Compiler registra os GameData que ele monta
    // a partir da pasta do projeto, que não passam por Init(byte[]).
    protected static void registerForGLReload(GameData gd) {
        synchronized (_glRegistry) {
            for (int i = _glRegistry.size() - 1; i >= 0; i--) {
                GameData g = _glRegistry.get(i).get();
                if (g == null) {
                    _glRegistry.remove(i);
                } else if (g == gd) {
                    return; // já registrado
                }
            }
            _glRegistry.add(new java.lang.ref.WeakReference<GameData>(gd));
        }
    }

    // Chamado na thread GL (TCBManajer.GLContextRecreated) com o contexto
    // novo já corrente.
    public static void GLReloadAll(GlobalWork gw) {
        java.util.ArrayList<GameData> alive = new java.util.ArrayList<GameData>();
        synchronized (_glRegistry) {
            for (int i = _glRegistry.size() - 1; i >= 0; i--) {
                GameData g = _glRegistry.get(i).get();
                if (g == null) {
                    _glRegistry.remove(i);
                } else {
                    alive.add(g);
                }
            }
        }
        for (int i = 0; i < alive.size(); i++) {
            try {
                alive.get(i).GLReload(gw);
            } catch (Exception e) {
                Logue.StackTrace(e);
            }
        }
    }

    public boolean GLReload(GlobalWork gw) {
        if (this.imageList.isEmpty() && this.sprite == null && this.videoList.isEmpty()
                && this.animTexList.isEmpty()) {
            return true; // nada gráfico carregado neste pac
        }
        byte[] src = null;
        if (this.NAME != null && this.NAME.length() > 0) {
            try {
                src = Utility.getBytes(gw.context.getAssets().open(this.NAME + ".pac"));
            } catch (IOException e) {
            }
        }
        if (src == null) {
            src = this.pSrcKeep;
        }
        if (src == null) {
            return false;
        }
        // Mesma varredura do Init(byte[]), mas processando SÓ png/spr e
        // reutilizando os objetos existentes (ordem/índices preservados).
        int k = 0;
        int dataCount = (src[0] & 255) | ((src[1] & 255) << 8);
        for (int i = 0; i < dataCount; i++) {
            int pos = (i << 4) + 2;
            int dataSize = (src[pos + 4] & 255)
                    | ((src[pos + 5] & 255) << 8)
                    | ((src[pos + 6] & 255) << 16)
                    | ((src[pos + 7] & 255) << 24);
            int dataPos = ((dataCount << 4) + 2)
                    + ((src[pos + 0] & 255)
                            | ((src[pos + 1] & 255) << 8)
                            | ((src[pos + 2] & 255) << 16)
                            | ((src[pos + 3] & 255) << 24));
            pos += 8;
            int type = ((src[pos] & 255) << 24) | ((src[pos + 1] & 255) << 16)
                    | ((src[pos + 2] & 255) << 8) | (src[pos + 3] & 255);
            switch (type) {
                case 0x706e6700: // png: repõe os pixels no MESMO AndroidGLTexture
                    if ((this.iLoadFilterSaved & 1) == 0) {
                        if (k < this.imageList.size()) {
                            ((AndroidGLTexture) this.imageList.get(k))
                                    .loadTexture(gw.gl, src, dataPos, dataSize, true);
                        }
                        k++;
                    }
                    break;
                case 0x73707200: // spr: reprocessa no MESMO SpriteData
                    if ((this.iLoadFilterSaved & 32) == 0 && this.sprite != null && dataSize > 0) {
                        this.sprite.Init(gw, dataPos, src);
                    }
                    break;
                    // act/bin/cnv/dac/wav: dados de CPU — sobreviveram intactos.
            }
        }

        reloadDerivedGL(gw, this.iLoadFilterSaved);
        return true;
    }

    /**
     * Recria o que é DERIVADO de dados de CPU já em memória: thumbnails de
     * vídeo (de videoList) e texturas webp (cada AnimatedTexture guarda seus
     * bytes). Separado de GLReload para o Compiler reaproveitar depois de
     * recarregar png/spr da pasta do projeto.
     */
    protected void reloadDerivedGL(GlobalWork gw, int loadFilter) {
        // NOVO: re-extrai e re-sobe as thumbnails de vídeo — os textureIds
        // antigos morreram junto com o contexto EGL, mas os bytes crus em
        // videoList sobreviveram (são dados de CPU, como dac/bin/cnv/act).
        // Reconstrói vidThumbTexId do zero, na MESMA ordem de videoList —
        // os índices continuam válidos para quem já usa
        // getVideoThumbTexId(videoIndex).
        if ((loadFilter & 64) == 0 && !this.videoList.isEmpty()) {
            this.videoThumbTexIdList.clear();
            for (Object rawObj : this.videoList) {
                byte[] raw = (byte[]) rawObj;
                int texId = extractAndUploadThumbnail(gw, raw);
                this.videoThumbTexIdList.add(texId);
            }
            this.vidThumbTexId = toIntArray(this.videoThumbTexIdList);
        }

        // NOVO: recria as texturas GL de cada AnimatedTexture (webp) — o(s)
        // textureId(s) antigo(s) morreram junto com o contexto EGL, mas os
        // bytes crus do .webp foram mantidos por cada instância (mesmo
        // princípio de pSrcKeep/videoList: dados de CPU sobrevivem,
        // recursos de GPU não). reloadGL() reprocessa a partir desses
        // bytes e recria o(s) textureId(s) — a REFERÊNCIA de objeto
        // (a própria AnimatedTexture) continua a mesma, então getAnimTex(N)
        // permanece válido sem precisar reindexar nada.
        //
        // Mesmo bit de loadFilter que decodeData usa para decidir se carrega
        // webp (1, igual ao png). Antes aqui era 128: com o bit 128 ligado e o
        // 1 desligado, os webp eram carregados mas nunca recarregados.
        if ((loadFilter & 1) == 0) {
            for (Object t : this.animTexList) {
                ((AnimatedTexture) t).reloadGL(gw.gl);
            }
        }
    }

    private boolean binCnv(int t, byte[] src, int offset, int size) {

        Log.e("BIN_CNV " + (NAME == null ? "" : NAME), t == 1 ? "ACT" : t == 2 ? "BIN" : t == 3 ? "CNV" : "DAC");
        this.piGameData = null;
        this.piGameDataPos = null;
        this.piGameDataXSize = null;
        this.piGameDataYSize = null;
        try {
            int i;
            this.piGameData = new short[size];
            for (i = 0; i < size; i++) {
                this.piGameData[i] = (short) (src[offset + i] & 255);
            }
            int m = (this.piGameData[1] << 8) | this.piGameData[0];
            this.piGameDataPos = new int[m];
            this.piGameDataXSize = new int[m];
            this.piGameDataYSize = new int[m];
            for (i = 0; i < m; i++) {
                int pos = (i * 8) + 2;
                this.piGameDataPos[i] = (((this.piGameData[pos + 3] << 24) | (this.piGameData[
                                                pos + 2] << 16)) | (this.piGameData[
                                        pos + 1] << 8)) | this.piGameData[pos + 0];
                pos += 4;
                this.piGameDataXSize[i] = (this.piGameData[pos + 1] << 8) | this.piGameData[
                        pos + 0];
                pos += 2;
                this.piGameDataYSize[i] = (this.piGameData[pos + 1] << 8) | this.piGameData[
                        pos + 0];
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void Dispose(GlobalWork gw) {
        int i;
        this.pSrcKeep = null; // libera bytes mantidos para recarga pós-perda de contexto
        this.piGameData = null;
        this.piGameDataPos = null;
        this.piGameDataXSize = null;
        this.piGameDataYSize = null;
        if (this.sprite != null) {
            this.sprite.Dispose(gw);
            this.sprite = null;
        }
        for (i = 0; i < this.data.length; i++) {
            this.data[i] = null;
        }
        for (i = 0; i < this.imageList.size(); i++) {
            ((AndroidGLTexture) this.imageList.get(i)).Dispose(gw.gl);
        }
        this.imageList.clear();

        // NOVO: libera as texturas animadas (webp) — cada uma cuida de
        // deletar seu(s) próprio(s) textureId(s) e fechar o decoder nativo
        // (streamed) ou nenhum decoder (preloaded, já fechado no load).
        for (i = 0; i < this.animTexList.size(); i++) {
            ((AnimatedTexture) this.animTexList.get(i)).Dispose(gw.gl);
        }
        this.animTexList.clear();
        this.animTex = new AnimatedTexture[0];

        // NOVO: libera as texturas de thumbnail de vídeo antes de limpar os
        // arrays — senão os textureIds vazam na GPU (nunca seriam
        // deletados, já que nenhum outro código conhece esses IDs).
        if (this.vidThumbTexId != null && this.vidThumbTexId.length > 0) {
            int[] toDelete = new int[this.vidThumbTexId.length];
            int count = 0;
            for (int texId : this.vidThumbTexId) {
                if (texId != 0) {
                    toDelete[count++] = texId;
                }
            }
            if (count > 0) {
                int[] trimmed = new int[count];
                System.arraycopy(toDelete, 0, trimmed, 0, count);
                android.opengl.GLES20.glDeleteTextures(count, trimmed, 0);
            }
        }
        this.videoThumbTexIdList.clear();
        this.vidThumbTexId = new int[0];

        this.videoList.clear();
        this.vid = new byte[0][];
        this.iSoundCount = 0;
        for (i = 0; i < this.sound.length; i++) {
            this.sound[i] = null;
        }
    }

    public int getImage(int no) {
        if (no >= 0 && no < this.imageList.size()) {
            AndroidGLTexture tex = (AndroidGLTexture) this.imageList.get(no);
            if (tex != null) {
                return tex.GetImage();
            }
        }
        return 0;
    }

    public int getImageWidth(int no) {
        if (no >= 0 && no < this.imageList.size()) {
            AndroidGLTexture tex = (AndroidGLTexture) this.imageList.get(no);
            if (tex != null) {
                return tex.GetWidth();
            }
        }
        return 1;
    }

    public int getImageHeight(int no) {
        if (no >= 0 && no < this.imageList.size()) {
            AndroidGLTexture tex = (AndroidGLTexture) this.imageList.get(no);
            if (tex != null) {
                return tex.GetHeight();
            }
        }
        return 1;
    }

}
