package com.namcobandaigames.dragonballtap.apk;

import javax.microedition.khronos.opengles.GL10;

// ══════════════════════════════════════════════════════════════════════
// AnimatedTexture — contrato comum para texturas animadas (webp) que
// vivem na fila própria GameData.animTexList/animTex[] (SEPARADA de
// imageList/cnv — ver GameData.decodeData, marcador "webp", e
// ExtrasReader.FUNC_ANIM_TEX / $GIF).
//
// Por que uma classe base em vez de extends AndroidGLTexture direto:
// AndroidGLTexture não foi revisada aqui (não faz parte destes arquivos),
// então em vez de assumir seu construtor/campos internos, esta classe
// expõe exatamente os métodos que o resto do sistema já consome
// (GetImage/GetWidth/GetHeight, usados por TCBManajer.DrawImageAnimTex
// exatamente como usaria em AndroidGLTexture) mais os 3 métodos NOVOS
// que só uma textura animada precisa: update() (avança/decide se avança
// o frame), reloadGL() (recria GL após perda de contexto EGL) e
// Dispose() (libera GPU + decoder nativo).
//
// ÚNICA implementação concreta: AnimatedWebpTexturePreloaded (decodifica
// TODOS os frames no load, N texturas reais, update() só troca qual
// índice é devolvido). Um modo "streamed" (1 textura, decodificação sob
// demanda) foi cogitado e descartado: GameData.decodeData roda no load
// do .pac inteiro, ANTES de qualquer ACT decidir se/quando um $GIF vai
// de fato referenciar aquele webp — ou seja, o custo de abrir o decoder
// já é pago no load de qualquer forma, não há "adiar decodificação até
// o uso" possível neste ponto. Sem esse benefício, um modo streamed só
// economizaria VRAM à custa de decode/upload periódico em runtime — para
// os casos-alvo deste sistema (animações curtas/pequenas: idle, ícones,
// efeitos), a VRAM extra de N texturas é desprezível, então não compensa
// a complexidade extra. Cutscenes grandes/longas continuam pelo caminho
// de vídeo já existente (VideoTexture/$VID), que É feito para isso.
//
// Esta classe continua sendo uma classe base (em vez de
// AnimatedWebpTexturePreloaded direto) só para deixar a porta aberta a
// uma futura reintrodução de um modo alternativo, sem precisar mudar
// nada em TCBManajer/GameData (que já tratam AnimatedTexture de forma
// polimórfica) — não há nenhum plano concreto de fazer isso agora.
//
// PÚBLICA (classe e todos os métodos): Compiler.java (pacote
// com.zue.gd.util, diferente de com.namcobandaigames.dragonballtap.apk)
// precisa instanciar AnimatedWebpTexturePreloaded e chamar init()/
// GetImage()/GetWidth()/GetHeight() diretamente em CompilerSpriteData —
// visibilidade package-private não é suficiente neste caso. Java exige
// que TODA subclasse mantenha a mesma visibilidade (ou mais aberta) dos
// métodos herdados; por isso AnimatedWebpTexturePreloaded também precisa
// declarar cada override como public, não só esta classe base.
// ══════════════════════════════════════════════════════════════════════
public abstract class AnimatedTexture {

    // Decodifica o cabeçalho do webp, prepara a(s) textura(s) GL e sobe o
    // frame inicial. Chamado 1x, no momento do load (GameData.decodeData).
    // Devolve false em caso de falha (webp inválido/corrompido, decoder
    // nativo não conseguiu abrir) — GameData já loga e simplesmente não
    // adiciona a instância à fila neste caso.
    public abstract boolean init(GL10 gl, byte[] webpBytes);

    // Chamado 1x por frame de DESENHO (TCBManajer.DrawExec, via
    // GameData.updateAllAnimatedTextures) — NÃO por frame de jogo/ACT.
    // Cada implementação decide sozinha, com base em tempo real
    // (SystemClock.elapsedRealtime()), se já é hora de avançar para o
    // próximo frame do webp, ou se o delay do frame atual ainda não
    // venceu (nesse caso não faz NADA — nem decodifica nem troca
    // textureId — ver princípio de "não atualizar se não mudou").
    public abstract void update();

    // Contrato consumido por TCBManajer.DrawImageAnimTex, EXATAMENTE como
    // consumiria de um AndroidGLTexture normal (mesmo nome de método,
    // mesma convenção: 0 = sem imagem válida).
    public abstract int GetImage();
    public abstract int GetWidth();
    public abstract int GetHeight();

    // Perda de contexto EGL (app foi para background e voltou, Android
    // destruiu o contexto): todo textureId desta instância morreu, mas os
    // bytes crus do .webp foram mantidos internamente por cada
    // implementação (mesmo princípio de GameData.pSrcKeep) — reloadGL()
    // reprocessa a partir deles e recria o(s) textureId(s). A REFERÊNCIA
    // de objeto (esta própria instância) não muda, então
    // GameData.getAnimTex(N) continua válido sem reindexar nada.
    // Chamado por GameData.GLReload(gw), 1x por instância na fila.
    public abstract void reloadGL(GL10 gl);

    // Libera textureId(s) da GPU e fecha o decoder nativo, se houver.
    // Chamado por GameData.Dispose(gw), 1x por instância na fila.
    public abstract void Dispose(GL10 gl);
}
