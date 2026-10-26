package com.namcobandaigames.dragonballtap.apk;

import java.nio.ByteBuffer;

// ══════════════════════════════════════════════════════════════════════
// WebpAnimNative — wrapper fino sobre o WebPAnimDecoder da libwebp
// (biblioteca C, via JNI). Usado por AnimatedWebpTexturePreloaded — que
// abre o decoder, decodifica TODOS os frames de uma vez no load, e o
// fecha em seguida (o decoder não precisa continuar vivo em runtime,
// já que depois do load só se troca qual textureId já existente é
// devolvido — ver AnimatedTexture.java para o porquê de não existir um
// modo alternativo que mantivesse o decoder aberto).
//
// Handle: cada open() devolve um "long" opaco (ponteiro nativo para uma
// struct C que guarda o WebPAnimDecoder* + os bytes crus mantidos vivos —
// ver comentário em open() sobre por que os bytes precisam sobreviver).
// close() SEMPRE deve ser chamado quando a instância Java correspondente
// morre/é substituída, ou o handle nativo (e a memória que ele segura)
// vaza — nenhum finalizer cobre isso, é responsabilidade explícita de
// AnimatedTexture.Dispose()/reloadGL().
//
// THREADING: todas as chamadas nativas aqui assumem execução na mesma
// thread que já possui o contexto GL corrente (a GL thread do
// GLSurfaceView) — não há sincronização interna. Isso é seguro porque
// GameData.decodeData, GameData.updateAllAnimatedTextures e
// GameData.GLReload já rodam todos na GL thread (mesmo padrão que
// AndroidGLTexture/VideoPlayerGL já assumem).
// ══════════════════════════════════════════════════════════════════════
public final class WebpAnimNative {

    static {
        System.loadLibrary("webpanim");
    }

    private WebpAnimNative() {
    } // classe utilitária, só métodos estáticos

    // Abre o decoder a partir dos bytes crus do arquivo .webp (o mesmo
    // byte[] que GameData.decodeData extraiu do .pac). A implementação
    // nativa COPIA esses bytes para memória própria (não mantém
    // referência ao array Java) — necessário porque o WebPAnimDecoder
    // precisa que o buffer de origem permaneça válido durante toda a
    // vida do decoder, e um byte[] Java pode ser movido/coletado pelo GC
    // a qualquer momento entre chamadas.
    //
    // Devolve 0 em caso de falha (arquivo corrompido/não é webp válido/
    // não tem animação) — chamador deve tratar 0 como "não inicializou".
    public static native long open(byte[] webpBytes);

    // Dimensões do canvas (mesmas para TODOS os frames — WebP animado usa
    // disposal/blend sobre um canvas de tamanho fixo, frames individuais
    // podem ser menores mas o decoder já compõe tudo no tamanho do canvas
    // antes de devolver, com MODE_RGBA). Só válidas após open() != 0.
    public static native int getWidth(long handle);
    public static native int getHeight(long handle);

    // Quantidade total de frames da animação (loop completo).
    public static native int getFrameCount(long handle);

    // Decodifica o PRÓXIMO frame (avança o decoder internamente) e
    // escreve os pixels RGBA (width*height*4 bytes) diretamente no
    // ByteBuffer DIRECT fornecido — outBuffer.position(0) antes da
    // chamada, permanece em 0 depois (chamador reposiciona se for ler).
    // Deve ter sido alocado com ByteBuffer.allocateDirect(width*height*4)
    // e ordem nativa (ByteOrder.nativeOrder()) — a implementação nativa
    // escreve via memcpy direto no endereço do buffer, sem cópia
    // intermediária Java.
    //
    // LOOP AUTOMÁTICO: se o decoder já entregou o último frame do loop
    // atual, esta chamada reinicia sozinha (WebPAnimDecoderReset) antes
    // de decodificar — ou seja, nextFrame() nunca "acaba"; chamar em
    // sequência indefinidamente reproduz a animação em loop infinito.
    //
    // Devolve a DURAÇÃO (em milissegundos) que ESTE frame recém-decodificado
    // deve permanecer visível antes do próximo — ou -1 em caso de erro
    // interno do decoder (chamador deve tratar como "não avançar",
    // mantendo o frame anterior).
    public static native int nextFrame(long handle, ByteBuffer outBuffer);

    // Libera o WebPAnimDecoder e a cópia interna dos bytes do webp.
    // Idempotente do lado Java: chamadores devem zerar seu próprio campo
    // de handle após chamar, e nunca chamar duas vezes com o mesmo valor
    // não-zero (o lado nativo não protege contra double-free).
    public static native void close(long handle);
}
