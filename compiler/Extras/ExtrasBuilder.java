package com.zue.gd.util;

import java.util.List;

// ══════════════════════════════════════════════════════════════════════
// ExtrasBuilder — lado do Compiler, ESCRITA dos bytes do gatilho de
// funções extras (o que no binário é o "OPT 225").
//
// Ponto de entrada único chamado por DacWriterCore.buildMpt():
//   ExtrasBuilder.hasExtras(mpt)     — pergunta se este MPT precisa do
//                                      formato especial
//   ExtrasBuilder.write(w, mpt)      — se precisar, escreve todos os bytes
//
// DacWriterCore NÃO conhece o valor 225, funcCount, nem nenhum código de
// função — tudo isso vive só aqui. Se um dia as funções extras forem
// descontinuadas, o único ajuste necessário é hasExtras(...) sempre
// devolver false — nenhuma mudança em DacWriterCore é necessária (o
// fluxo normal de escrita de OPT, já existente, assume novamente).
//
// FORMATO BINÁRIO ESCRITO por write() (o método é dono do payload INTEIRO,
// do primeiro ao último byte — DacWriterCore não escreve nada antes nem
// depois desta chamada, além do que já fazia para os outros campos do MPT):
//   [2 bytes] wOptFlag = 225
//   [1 byte]  funcCount
//   [funcCount bytes] códigos de função, na ordem de execução
//   [payload de cada função, concatenado, na mesma ordem]
//
// ORDEM DAS FUNÇÕES: se o MPT tiver uma OPT normal (mpt.opt.used, com
// flag != 225 — ou seja, o autor do formato fonte escreveu $OPT normal)
// E também função(ões) extra(s) ($VID, etc.), a função "ler OPT real"
// (código 250) é SEMPRE inserida primeiro na lista, seguida das funções
// extras na ordem em que foram declaradas no formato fonte. Isso garante
// que a lógica de gameplay nativa (que pode depender de wOptFlag/OptData
// já setados) nunca "perde" para as funções extras dentro do mesmo frame.
// ══════════════════════════════════════════════════════════════════════
public final class ExtrasBuilder {

    private ExtrasBuilder() {
    } // classe utilitária, só métodos estáticos

    // Códigos de função — DEVEM bater exatamente com as constantes
    // homônimas em ExtrasReader.java (lado do jogo/runtime). Não há
    // verificação automática entre os dois lados; qualquer divergência
    // aqui desalinha o parsing silenciosamente.
    private static final int FUNC_PLAY_VIDEO = 60;
    private static final int FUNC_ANIM_TEX = 61; // NOVO — deve bater com ExtrasReader.FUNC_ANIM_TEX
    private static final int FUNC_REAL_OPT = 250;

    // ── Bits de $FLAGS — DEVEM bater exatamente com VideoRequest.java
    //    (lado do jogo/runtime). TODOS derivados agora — não existe mais
    //    nenhum bit manual/campo $FLAGS no formato fonte (ver histórico
    //    de decisão: os antigos bits manuais 1/2/4 foram analisados e
    //    removidos por serem redundantes com comportamento natural do
    //    motor, ou por preferir sincronização manual no binário). ────────
    private static final int FLAG_FIXO           = 1;
    private static final int FLAG_FULLSCREEN     = 8;
    private static final int FLAG_STOP           = 16;
    private static final int FLAG_HAS_STOP_POINT = 32;
    private static final int FLAG_MUTE_BGM       = 64;
    private static final int FLAG_MUTE_SE        = 128;
    private static final int FLAG_MUTE_VIDEO     = 256;
    private static final int FLAG_HAS_NEW_BGM    = 512;
    private static final int FLAG_END_ANO        = 1024;

    // ── Interface mínima de escrita, para não depender da classe privada
    //    DacWriterCore.BinWriter. DacWriterCore passa um adaptador fino
    //    (ver exemplo de uso no ponto de integração, mais abaixo). ────────
    public interface ByteSink {
        void writeByte(int v);
        void writeShortLE(int v);
    }

    // Chamado por DacWriterCore para decidir se este MPT precisa do
    // formato especial de funções extras, em vez do fluxo normal de OPT.
    //
    // Verdadeiro se: há função(ões) extra(s) pendente(s) para este MPT
    // (registradas por ExtrasParser durante o parse do formato fonte).
    // Uma OPT normal sozinha, SEM função extra junto, NUNCA aciona este
    // formato — continua indo pelo fluxo original de DacWriterCore.
    public static boolean hasExtras(MptInstance mpt) {
        return !ExtrasParser.getFunctionsFor(mpt).isEmpty();
    }

    // Escreve o payload COMPLETO do gatilho de extras, INCLUINDO o
    // wOptFlag=225 inicial de 2 bytes — DacWriterCore não precisa (e não
    // deve) escrever esse valor separadamente antes de chamar este método.
    // Isso elimina uma dependência implícita entre as duas classes: antes,
    // era responsabilidade do CHAMADOR escrever 225 antes de chamar write(),
    // o que é frágil (fácil esquecer, como de fato aconteceu na primeira
    // integração). Agora ExtrasBuilder é dono de TODO o payload do gatilho,
    // do primeiro ao último byte.
    public static void write(ByteSink w, MptInstance mpt) {
        w.writeShortLE(225); // marca o gatilho — único lugar que escreve este valor

        List<ExtrasParser.ExtraFunction> extras = ExtrasParser.getFunctionsFor(mpt);

        boolean needsRealOpt = mpt.opt.used; // $OPT normal presente neste MPT?

        int funcCount = extras.size() + (needsRealOpt ? 1 : 0);
        w.writeByte(funcCount);

        // 1. Lista de códigos, na ordem de execução: OPT real primeiro
        //    (se presente), depois as funções extras na ordem declarada.
        if (needsRealOpt) {
            w.writeByte(FUNC_REAL_OPT);
        }
        for (ExtrasParser.ExtraFunction f : extras) {
            w.writeByte(f.code());
        }

        // 2. Payloads, na MESMA ordem da lista de códigos acima.
        if (needsRealOpt) {
            w.writeShortLE(mpt.opt.flag);
            w.writeByte(mpt.opt.data[0]);
            w.writeByte(mpt.opt.data[1]);
            w.writeByte(mpt.opt.data[2]);
            w.writeByte(mpt.opt.data[3]);
        }
        for (ExtrasParser.ExtraFunction f : extras) {
            writeFunctionPayload(w, f);
        }
    }

    // ── Escrita do payload de cada função conhecida ──────────────────────
    // Precisa bater exatamente com a ordem/tamanho que ExtrasReader/
    // VideoTexture (lado do jogo) espera ler para o mesmo código de
    // função — ver VideoRequest.java para a tabela de bits completa.
    private static void writeFunctionPayload(ByteSink w, ExtrasParser.ExtraFunction f) {
        if (f instanceof ExtrasParser.VidFunction) {
            ExtrasParser.VidFunction vid = (ExtrasParser.VidFunction) f;

            // Monta o flags: TODO bit é derivado dos campos nomeados —
            // não existe mais nenhum valor numérico vindo do fonte para
            // combinar aqui.
            int finalFlags = 0;
            if (vid.fixo) finalFlags |= FLAG_FIXO;
            if (vid.fullscreen) finalFlags |= FLAG_FULLSCREEN;
            if (vid.stopEnabled) finalFlags |= FLAG_STOP;
            if (vid.stopEnabled && vid.hasStopPoint) finalFlags |= FLAG_HAS_STOP_POINT;
            if (vid.muteBgm) finalFlags |= FLAG_MUTE_BGM;
            if (vid.muteSe) finalFlags |= FLAG_MUTE_SE;
            if (vid.muteVideo) finalFlags |= FLAG_MUTE_VIDEO;
            if (vid.hasNewBgm) finalFlags |= FLAG_HAS_NEW_BGM;
            if (vid.endAno) finalFlags |= FLAG_END_ANO;

            w.writeShortLE(vid.skip);
            w.writeShortLE(finalFlags);
            w.writeShortLE(vid.width);
            w.writeShortLE(vid.height);

            // Payloads condicionais, na MESMA ordem em que
            // VideoTexture.playFromDac() os lê: ponto de $STOP antes de
            // $NEW_BGM.
            if (vid.stopEnabled && vid.hasStopPoint) {
                w.writeShortLE(vid.stopAno);
                w.writeByte(vid.stopMpt);
            }
            if (vid.hasNewBgm) {
                w.writeByte(vid.newBgmId);
            }
        }
        // (novas funções extras: adicionar "else if" aqui, escrevendo o
        // payload na mesma ordem/tamanho que o case correspondente em
        // ExtrasReader.processFunction() espera ler)
        else if (f instanceof ExtrasParser.GifFunction) {
            ExtrasParser.GifFunction gif = (ExtrasParser.GifFunction) f;
            w.writeShortLE(gif.gifIndex); // payload mínimo: só o índice — ver ExtrasReader.FUNC_ANIM_TEX
        }
    }
}
