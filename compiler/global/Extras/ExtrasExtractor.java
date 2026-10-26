package com.zue.gd.util;

// ══════════════════════════════════════════════════════════════════════
// ExtrasExtractor — lado do Compiler, DECOMPILAÇÃO do gatilho de funções
// extras (o que no binário é o "OPT 225") de volta para o formato fonte
// legível, espelhando o que ExtrasBuilder escreveu.
//
// Ponto de entrada único chamado por DacExtractor, no bloco de OPT
// (Flag 64):
//   ExtrasExtractor.isExtrasFlag(optFlag)     — pergunta se é o gatilho
//                                                especial de extras
//   ExtrasExtractor.extractAndPrint(...)      — se for, lê os bytes e
//                                                imprime o formato fonte
//
// DacExtractor NÃO conhece o valor 225, funcCount, nem nenhum código de
// função — tudo isso vive só aqui. Se um dia as funções extras forem
// descontinuadas, o único ajuste necessário é isExtrasFlag(...) sempre
// devolver false — nenhuma mudança em DacExtractor é necessária (o fluxo
// normal de extração de OPT, já existente, assume novamente).
//
// BUG PRÉ-EXISTENTE QUE ISTO CORRIGE: antes desta classe, DacExtractor
// sempre lia 4 bytes fixos como optData mesmo quando optFlag==225 —
// nesse caso o "optData" lido eram, na verdade, os 2 primeiros bytes do
// payload de extras interpretados incorretamente, e a leitura de TODOS
// os campos seguintes do MPT saía desalinhada a partir daí.
//
// FORMATO BINÁRIO LIDO (deve bater EXATAMENTE com o que ExtrasBuilder,
// no lado do Compiler/escrita, gera — ver aquela classe para o
// contrato completo):
//   [1 byte]  funcCount
//   [funcCount bytes] códigos de função, na ordem de execução
//   [payload de cada função, concatenado, na mesma ordem]
//
// SAÍDA (formato fonte impresso, espelhando ExtrasParser):
//   Se a lista incluir o código de "OPT real" (250): imprime uma linha
//     $OPT:{...} normal, exatamente como o fluxo padrão já imprimia.
//   Se a lista incluir FUNC_PLAY_VIDEO (60): imprime uma linha
//     $VID:{ $SKIP:, $FLAGS:, $W:, $H: }, no mesmo formato que
//     ExtrasParser.handleVidLine sabe reler depois.
//
// DEPENDÊNCIA PENDENTE EM DacExtractor.java: o campo Printer.buf é
// private, então esta classe não consegue replicar a checagem
// "if (printer.buf.length() > 0) printer.println();" que os outros
// blocos ($OPT, $RAZ) já fazem antes de escrever, para não colar a nova
// tag na mesma linha de uma tag anterior. É necessário adicionar em
// DacExtractor.Printer um método público equivalente:
//
//     public final boolean hasPendingLine() {
//         return this.buf.length() > 0;
//     }
//
// Sem esse método, ExtrasExtractor não compila (buf é inacessível daqui).
// ══════════════════════════════════════════════════════════════════════
public final class ExtrasExtractor {

    private ExtrasExtractor() {
    } // classe utilitária, só métodos estáticos

    // Códigos de função — DEVEM bater exatamente com as constantes
    // homônimas em ExtrasBuilder.java (lado da escrita) e ExtrasReader.java
    // (lado do jogo/runtime). Não há verificação automática entre os três
    // lados; qualquer divergência desalinha o parsing silenciosamente.
    private static final int FUNC_PLAY_VIDEO = 60;
    private static final int FUNC_ANIM_TEX = 61; // NOVO — deve bater com ExtrasReader/ExtrasBuilder
    private static final int FUNC_REAL_OPT = 250;

    // ── Bits de $FLAGS — DEVEM bater exatamente com VideoRequest.java e
    //    ExtrasBuilder.java. TODOS derivados agora. ─────────────────────
    private static final int FLAG_FIXO           = 1;
    private static final int FLAG_FULLSCREEN     = 8;
    private static final int FLAG_STOP           = 16;
    private static final int FLAG_HAS_STOP_POINT = 32;
    private static final int FLAG_MUTE_BGM       = 64;
    private static final int FLAG_MUTE_SE        = 128;
    private static final int FLAG_MUTE_VIDEO     = 256;
    private static final int FLAG_HAS_NEW_BGM    = 512;
    private static final int FLAG_END_ANO        = 1024;

    // Chamado por DacExtractor para decidir se o optFlag lido é o gatilho
    // especial de funções extras, ou uma OPT normal de gameplay. Encapsula
    // o valor mágico 225 — nenhum outro arquivo do extractor deveria
    // comparar contra 225 diretamente.
    public static boolean isExtrasFlag(int optFlag) {
        return optFlag == 225;
    }

    // Lê o payload completo do gatilho de extras a partir do reader (que já
    // deve estar posicionado logo após o optFlag=225 de 2 bytes, consumido
    // pelo chamador antes desta chamada), e IMPRIME o formato fonte
    // correspondente via printer — uma linha por função reconhecida, na
    // mesma ordem em que aparecem no binário.
    //
    // Não retorna nada: DacExtractor não precisa do dataPtr de volta porque
    // BufReader mantém sua própria posição interna (this.pos), avançada a
    // cada read() — diferente do DacWriterCore/BinWriter no lado da
    // escrita, que usa um dataPtr explícito.
    public static void extractAndPrint(DacExtractor.BufReader reader, DacExtractor.Printer printer) {
        int funcCount = reader.readUnsignedByte();

        int[] funcCodes = new int[funcCount];
        for (int i = 0; i < funcCount; i++) {
            funcCodes[i] = reader.readUnsignedByte();
        }

        for (int i = 0; i < funcCount; i++) {
            printFunctionPayload(reader, printer, funcCodes[i]);
        }
    }

    // ── Leitura + impressão do payload de cada função conhecida ──────────
    private static void printFunctionPayload(DacExtractor.BufReader reader,
                                               DacExtractor.Printer printer, int funcCode) {
        switch (funcCode) {
            case FUNC_REAL_OPT: {
                int optFlag = reader.readShortLE();
                int d0 = reader.readUnsignedByte();
                int d1 = reader.readUnsignedByte();
                int d2 = reader.readUnsignedByte();
                int d3 = reader.readUnsignedByte();

                DacExtractor.Vars v = printer.vars;
                v.pos = 0;
                v.addInt(optFlag).addInt(d0).addInt(d1).addInt(d2).addInt(d3);
                if (printer.hasPendingLine()) {
                    printer.println();
                }
                printer.writef("$OPT:{$FLAG: %d, $DATA: B[%d,%d,%d,%d]}, ");
                printer.println();
                break;
            }

            case FUNC_PLAY_VIDEO: {
                int skip = reader.readShortLE();
                int flags = reader.readShortLE();
                int w = reader.readShortLE();
                int h = reader.readShortLE();

                // Payloads condicionais, na MESMA ordem em que
                // ExtrasBuilder os escreveu: ponto de $STOP antes de
                // $NEW_BGM.
                boolean stopEnabled = (flags & FLAG_STOP) != 0;
                boolean hasStopPoint = (flags & FLAG_HAS_STOP_POINT) != 0;
                int stopAno = 0, stopMpt = 0;
                if (hasStopPoint) {
                    stopAno = reader.readShortLE();
                    stopMpt = reader.readUnsignedByte();
                }

                boolean hasNewBgm = (flags & FLAG_HAS_NEW_BGM) != 0;
                int newBgmId = 0;
                if (hasNewBgm) {
                    newBgmId = reader.readUnsignedByte();
                }

                boolean muteBgm = (flags & FLAG_MUTE_BGM) != 0;
                boolean muteSe = (flags & FLAG_MUTE_SE) != 0;
                boolean muteVideo = (flags & FLAG_MUTE_VIDEO) != 0;
                boolean endAno = (flags & FLAG_END_ANO) != 0;
                boolean fullscreen = (flags & FLAG_FULLSCREEN) != 0;
                boolean fixo = (flags & FLAG_FIXO) != 0;

                if (printer.hasPendingLine()) {
                    printer.println();
                }

                DacExtractor.Vars v = printer.vars;
                v.pos = 0;
                v.addInt(skip).addInt(w).addInt(h);
                printer.writef("$VID:{ $SKIP: %d, $W: %d, $H: %d, ");

                v.pos = 0;
                v.add(fixo ? "yes" : "not").add(fullscreen ? "yes" : "not");
                printer.writef("$FIXO: %s, $FULLSCREEN: %s, ");

                v.pos = 0;
                v.add(muteBgm ? "yes" : "not").add(muteSe ? "yes" : "not")
                        .add(muteVideo ? "yes" : "not").add(endAno ? "yes" : "not");
                printer.writef("$MUTE_BGM: %s, $MUTE_SE: %s, $MUTE_VIDEO: %s, $END_ANO: %s, ");

                if (hasNewBgm) {
                    v.pos = 0;
                    v.addInt(newBgmId);
                    printer.writef("$NEW_BGM: %d, ");
                } else {
                    printer.writef("$NEW_BGM: not, ");
                }

                // $STOP:{ yes/not, $ANO: X/not, $MPT: N } — unificado.
                if (stopEnabled) {
                    if (hasStopPoint) {
                        v.pos = 0;
                        v.addInt(stopAno).addInt(stopMpt);
                        printer.writef("$STOP:{ yes, $ANO: %d, $MPT: %d } ");
                    } else {
                        printer.writef("$STOP:{ yes, $ANO: not, $MPT: 0 } ");
                    }
                } else {
                    printer.writef("$STOP:{ not } ");
                }

                printer.writef("}, ");
                printer.println();
                break;
            }

            case FUNC_ANIM_TEX: {
                int gifIndex = reader.readShortLE();

                if (printer.hasPendingLine()) {
                    printer.println();
                }

                DacExtractor.Vars v = printer.vars;
                v.pos = 0;
                v.addInt(gifIndex);
                printer.writef("$GIF:{ $ID: %d }, ");
                printer.println();
                break;
            }

            default:
                // Código desconhecido: nada a ler com segurança além do
                // próprio byte de código (já consumido pelo chamador).
                // Isso significa que payloads de códigos não reconhecidos
                // por esta versão do extractor DEVEM ter tamanho zero, ou
                // a leitura desalinha a partir daqui — mesmo cuidado que
                // já vale para ExtrasReader no lado do jogo.
                break;
        }
    }
}
