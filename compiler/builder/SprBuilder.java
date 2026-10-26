package com.zue.gd.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

// ══════════════════════════════════════════════════════════════════════
// SprBuilder — compila NNN.bin (formato fonte legível) em NNN.bin
// (formato binário lido por SpriteData.Init/TCBManajer.DrawSprite).
//
// FORMATO FONTE (por camadas com timeline — análogo a $ANO/$MPT do DAC,
// aplicado a sprites):
//
//   $MOVIE_0: {$INFO:{ $PSZ:36, $PCO:2, $SSZ:0, $SCO:0, $FCO:300, $LP:0, $TP:0 },
//      $LAYER_0: {                              // uma trilha independente
//         $ANIM_0: {$START: 0,                  // sem $END — deriva da
//                                                // soma de $TIME dos $FR
//            $FR_0:{
//               $TIME: 30, $IMG: 5,
//               $CROP:[$X:0, $Y:0, $W:400, $H:400],
//               $MID:[$X:-25, $Y:-37, $W:100, $H:150],
//               $TRANSITION: yes,                // yes=interpola suave, not=seco
//               $RAZ:{$OPACITY: 0, $MODE: 0},
//            },
//            $FR_1:{...},
//         },
//         $ANIM_1: {$START: 220, ...},           // gap entre 31..220: $IMG:-1
//      },
//      $LAYER_1: {...},
//   },
//
// $CROP com W<0 e H<0 (qualquer valor negativo serve de sinalizador) =
// "use a imagem inteira" (lida do PNG/WEBP correspondente na pasta).
// $MID com W<0 e H<0 = "use o tamanho do $CROP resolvido" (ou da imagem
// inteira, se $CROP também for auto) — $MID.X/$MID.Y sempre valem como
// escritos, nunca são auto.
//
// TRANSIÇÃO: $TIME de um $FR é "quantos ticks para CHEGAR neste estado a
// partir do anterior" (mesma semântica do DAC). $TRANSITION:yes interpola
// linearmente (mid/crop/opacity) ao longo desses ticks; $TRANSITION:not
// mantém o valor anterior constante até o último tick, então salta seco.
// img/mode NUNCA interpolam — valem o valor do $FR de destino durante
// toda a transição quando smooth, ou ficam no valor anterior até o
// último tick quando not (mesma regra dos demais campos não numéricos).
//
// BINÁRIO FINAL: inalterado — N registros de 1 tick cada (img:2 + vt:16
// + uv:16 + alpha:1 + mode:1 = 36 bytes por registro, preenchido com
// zero até completar $PSZ). SpriteData/DrawSprite/SprExtractor não
// precisam saber que o texto de origem mudou.
// ══════════════════════════════════════════════════════════════════════
public final class SprBuilder {

    // ── Patterns do $INFO (inalterados) ─────────────────────────────────
    private static final Pattern PSZ_P = Pattern.compile("\\$PSZ:\\s*(\\d+)");
    private static final Pattern PCO_P = Pattern.compile("\\$PCO:\\s*(\\d+)");
    private static final Pattern SSZ_P = Pattern.compile("\\$SSZ:\\s*(\\d+)");
    private static final Pattern SCO_P = Pattern.compile("\\$SCO:\\s*(\\d+)");
    private static final Pattern FCO_P = Pattern.compile("\\$FCO:\\s*(\\d+)");
    private static final Pattern LP_P  = Pattern.compile("\\$LP:\\s*(\\d+)");
    private static final Pattern TP_P  = Pattern.compile("\\$TP:\\s*(\\d+)");

    // ── Patterns do formato $LAYER/$ANIM/$FR ─────────────────────────────
    private static final Pattern START_P      = Pattern.compile("\\$START\\s*:\\s*(\\d+)");
    private static final Pattern TIME_P       = Pattern.compile("\\$TIME\\s*:\\s*(\\d+)");
    private static final Pattern IMG_P        = Pattern.compile("\\$IMG\\s*:\\s*(-?\\d+)");
    // Aceita tanto o formato NOVO ($TRANSITION: 0.35, um double 0.0-1.0
    // = ponto de corte) quanto o formato ANTIGO ($TRANSITION: yes/not,
    // preservado por compatibilidade retroativa com .bin já extraídos
    // antes desta mudança — ver parseTransition para o mapeamento exato).
    private static final Pattern TRANSITION_P = Pattern.compile("\\$TRANSITION\\s*:\\s*(yes|not|-?\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPACITY_P    = Pattern.compile("\\$OPACITY\\s*:\\s*(\\d+)");
    private static final Pattern MODE_P       = Pattern.compile("\\$MODE\\s*:\\s*(\\d+)");
    private static final Pattern ROT_P        = Pattern.compile("\\$ROT\\s*:\\s*(-?\\d+(?:\\.\\d+)?)"); // NOVO — graus, aceita decimal
    private static final Pattern FLIP_P       = Pattern.compile("\\$FLIP\\s*:\\s*\\{([^}]*)\\}"); // NOVO — bloco {$X:.., $Y:..} — chave de fechamento escapada (\\}), não só a de abertura: o motor ICU do Android (usado em regex no runtime real, diferente do java.util.regex desktop) rejeita "}" não-escapado quando "{" já foi escapado antes, mesmo sendo sintaxe válida no desktop
    private static final Pattern FIELD_FLIP_X_P = Pattern.compile("\\$X\\s*:\\s*(yes|not|1|0)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_FLIP_Y_P = Pattern.compile("\\$Y\\s*:\\s*(yes|not|1|0)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CROP_P       = Pattern.compile("\\$CROP\\s*:\\s*\\[([^\\]]+)\\]"); // fechamento \\] escapado — ver nota em FLIP_P sobre rigidez do ICU regex do Android
    private static final Pattern MID_P        = Pattern.compile("\\$MID\\s*:\\s*\\[([^\\]]+)\\]");  // idem
    private static final Pattern FIELD_X_P    = Pattern.compile("\\$X\\s*:\\s*(-?\\d+)");
    private static final Pattern FIELD_Y_P    = Pattern.compile("\\$Y\\s*:\\s*(-?\\d+)");
    private static final Pattern FIELD_W_P    = Pattern.compile("\\$W\\s*:\\s*(-?\\d+)");
    private static final Pattern FIELD_H_P    = Pattern.compile("\\$H\\s*:\\s*(-?\\d+)");

    private SprBuilder() {}

    // ══════════════════════════════════════════════════════════════════
    // Estruturas em memória (parse -> resolução de auto -> expansão -> bytes)
    // ══════════════════════════════════════════════════════════════════

    // Um $FR já com $CROP/$MID RESOLVIDOS (auto convertido em valor real)
    // — criado durante a expansão, não durante o parse puro (ver
    // resolveFrKeyframe), porque a resolução depende de ler dimensões de
    // arquivo, o que só faz sentido fazer uma vez por $FR, não repetido.
    private static final class ResolvedFr {
        int img;
        double midX, midY, midW, midH;
        double rot; // NOVO — graus, 0 = sem rotação
        boolean flipX, flipY; // NOVO
        double cropX, cropY, cropW, cropH;
        int opacity;
        int mode;
    }

    // Um $FR cru, exatamente como escrito no texto (antes de resolver
    // $CROP/$MID automáticos).
    private static final class RawFr {
        int sourceIndex; // índice original ($FR_N) — só para mensagens de erro, não afeta o binário
        int time;
        int img;
        double cropX, cropY, cropW, cropH; // podem ser negativos = auto
        double midX, midY, midW, midH;     // idem
        double easeParam; // NOVO — 0.0-1.0, ponto de corte dentro do intervalo (era boolean transitionSmooth)
        int opacity;
        int mode;
        double rot; // NOVO — graus, default 0 se $ROT ausente do $RAZ
        boolean flipX, flipY; // NOVO — default false (not) se $FLIP ausente do $RAZ
    }

    private static final class AnimBlock {
        int sourceIndex; // índice original ($ANIM_N) — só para mensagens de erro, não afeta o binário
        int start;
        List<RawFr> frames = new ArrayList<RawFr>();
    }

    private static final class LayerBlock {
        List<AnimBlock> anims = new ArrayList<AnimBlock>();
    }

    // ══════════════════════════════════════════════════════════════════
    // Entrada pública — contrato do GAME COMPILER (diferente do
    // appCompiler): esta pasta .spr/ já é uma pasta DESEMPACOTADA em
    // disco (extraída por SprExtractor.process) — build() gera o
    // arquivo .bin BINÁRIO a partir do .bin.txt (texto fonte legível
    // $LAYER/$ANIM/$FR), como arquivos SEPARADOS.
    //
    // IMPORTANTE: .bin.txt e .bin NUNCA podem ser o mesmo arquivo — .bin
    // é a extensão que o JOGO lê em tempo de execução (ver
    // Compiler.loadFromProjectFolder, que só reconhece ".bin" como
    // binário de sprite via decodeData/SpriteData.Init), então .bin.txt
    // precisa existir como fonte separada e editável, sem nunca ser
    // sobrescrita pelo binário compilado.
    //
    // NÃO monta nenhum mini-pac aqui — quem remonta o .pac final a
    // partir da pasta inteira (incluindo os .png/.webp intocados e o
    // .bin recém-gerado) é RealPacBuilder.packFolderToBytes, que já lê
    // genericamente qualquer arquivo não-.txt/.json da pasta, sem
    // precisar de nenhuma lógica específica de sprite ali.
    // ══════════════════════════════════════════════════════════════════
    public static void build(File sprDir) throws Exception {
        build(sprDir, null);
    }

    /**
     * Mesmo comportamento de build(File), com um ErrorReporter opcional (pode
     * ser null — nesse caso custo extra é zero) que coleta problemas de
     * sintaxe/valor de cada .bin.txt processado, sem nunca abortar a
     * compilação — o .bin sempre é gerado, mesmo com erros reportados.
     */
    public static void build(File sprDir, ErrorReporter reporter) throws Exception {
        if (sprDir == null || !sprDir.isDirectory()) return;

        File[] files = sprDir.listFiles(new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".bin.txt");
            }
        });

        if (files == null || files.length == 0) return;

        Arrays.sort(files, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                return a.getName().compareTo(b.getName());
            }
        });

        for (File f : files) {
            byte[] compiled = buildBinFromText(f, sprDir, reporter);
            File outFile = new File(sprDir, stripTxtExtension(f.getName()));
            writeFile(outFile, compiled); // ".bin" gerado, SEPARADO do ".bin.txt" fonte
        }
    }

    // Remove só o ".txt" final (ex.: "000.bin.txt" -> "000.bin") —
    // preserva o ".bin" no meio do nome, que é a extensão real que o
    // jogo espera.
    private static String stripTxtExtension(String name) {
        int idx = name.toLowerCase().lastIndexOf(".txt");
        return (idx > 0) ? name.substring(0, idx) : name;
    }

    private static void writeFile(File target, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(target);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    // ================= CONTEXTO DE ERRO (opcional) =================

    // Carrega o ErrorReporter e a localização atual (arquivo/movie/layer/anim/fr)
    // através da cadeia de parsing, sem precisar de 5 parâmetros extras em cada
    // método — os índices são atualizados pelos próprios laços que os percorrem.
    // Quando reporter == null (uso padrão, sem coleta de erros), report(...) é
    // no-op e o custo extra é irrelevante.
    private static final class ErrCtx {
        final ErrorReporter reporter;
        final String fileName;
        Integer movieIndex;
        Integer layerIndex;
        Integer animIndex;
        Integer frIndex;

        ErrCtx(ErrorReporter reporter, String fileName) {
            this.reporter = reporter;
            this.fileName = fileName;
        }

        void report(String fieldTag, String message) {
            if (reporter == null) return;
            String path = "$MOVIE_" + movieIndex
                    + (layerIndex != null ? "/$LAYER_" + layerIndex : "")
                    + (animIndex != null ? "/$ANIM_" + animIndex : "")
                    + (frIndex != null ? "/$FR_" + frIndex : "");
            reporter.report(CompileError.builder()
                    .fileKind("spr")
                    .fileName(fileName)
                    .fieldTag(fieldTag)
                    .message(path + " — " + message));
        }
    }

    // ================= LEITURA DO TEXTO INTEIRO + SPLIT EM $MOVIE_N =================

    private static byte[] buildBinFromText(File txtFile, File sprDir, ErrorReporter reporter) throws Exception {
        String fullText = readFileUtf8(txtFile);
        ErrCtx ctx = new ErrCtx(reporter, txtFile.getName());

        // Trabalha em blocos de texto inteiros (não linha a linha),
        // porque o formato tem chaves aninhadas em múltiplas linhas.
        List<String> movieBlocks = splitTopLevelBlocks(fullText, "$MOVIE_");

        ByteArrayOutputStream moviesData = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<Integer>();
        List<Integer> sizes = new ArrayList<Integer>();

        // Cache de dimensões de imagem por índice — evita reabrir o
        // mesmo PNG/WEBP repetidamente ao resolver vários $FR que usam
        // o mesmo $IMG.
        Map<Integer, int[]> imageDimsCache = new HashMap<Integer, int[]>();

        int movieIdx = 0;
        for (String movieBlock : movieBlocks) {
            ctx.movieIndex = movieIdx;
            ByteArrayOutputStream movieOut = new ByteArrayOutputStream();

            String infoBlock = extractBraceBlock(movieBlock, "$INFO");
            int psz = extractIntChecked(infoBlock, PSZ_P, "$PSZ", ctx, 0, 65535);
            int pco = extractIntChecked(infoBlock, PCO_P, "$PCO", ctx, 0, 255);
            int fco = extractIntChecked(infoBlock, FCO_P, "$FCO", ctx, 0, 65535);

            writeShortLE(movieOut, psz);
            writeShortLE(movieOut, pco);
            writeShortLE(movieOut, extractIntChecked(infoBlock, SSZ_P, "$SSZ", ctx, 0, 65535));
            writeShortLE(movieOut, extractIntChecked(infoBlock, SCO_P, "$SCO", ctx, 0, 65535));
            writeShortLE(movieOut, fco);
            writeShortLE(movieOut, extractIntChecked(infoBlock, LP_P, "$LP", ctx, 0, 65535));
            writeIntLE(movieOut, extractIntChecked(infoBlock, TP_P, "$TP", ctx, Integer.MIN_VALUE, Integer.MAX_VALUE));

            // Uma $LAYER por objeto (pco layers esperadas) — se o texto
            // tiver menos $LAYER_N que pco, as faltantes ficam totalmente
            // vazias (img=-1 em todos os ticks), igual um objeto que
            // nunca aparece.
            List<String> layerBlocks = splitTopLevelBlocks(movieBlock, "$LAYER_");

            if (layerBlocks.size() > pco) {
                ctx.report("$LAYER_N", "há " + layerBlocks.size() + " bloco(s) $LAYER_N no texto, mas $PCO declara "
                        + pco + " — os blocos excedentes serão ignorados");
            }

            // [layer][tick] — um conjunto de arrays por campo, já que
            // Java não tem "struct array" nativo sem alocar um objeto por
            // célula (o que seria bem mais lento/pesado para fco*pco
            // potencialmente grande).
            int[][] imgG = new int[pco][fco];
            double[][] midXG = new double[pco][fco];
            double[][] midYG = new double[pco][fco];
            double[][] midWG = new double[pco][fco];
            double[][] midHG = new double[pco][fco];
            double[][] rotG = new double[pco][fco]; // graus, 0 = sem rotação
            boolean[][] flipXG = new boolean[pco][fco]; // NOVO
            boolean[][] flipYG = new boolean[pco][fco]; // NOVO
            double[][] cropXG = new double[pco][fco];
            double[][] cropYG = new double[pco][fco];
            double[][] cropWG = new double[pco][fco];
            double[][] cropHG = new double[pco][fco];
            int[][] alphaG = new int[pco][fco];
            int[][] modeG = new int[pco][fco];

            for (int[] row : imgG) Arrays.fill(row, -1); // default: gap (sem objeto)

            for (int layerIdx = 0; layerIdx < layerBlocks.size() && layerIdx < pco; layerIdx++) {
                ctx.layerIndex = layerIdx;
                LayerBlock layer = parseLayerBlock(layerBlocks.get(layerIdx), ctx);
                expandLayerIntoTicks(layer, fco, sprDir, imageDimsCache,
                        imgG[layerIdx], midXG[layerIdx], midYG[layerIdx], midWG[layerIdx], midHG[layerIdx],
                        rotG[layerIdx], flipXG[layerIdx], flipYG[layerIdx],
                        cropXG[layerIdx], cropYG[layerIdx], cropWG[layerIdx], cropHG[layerIdx],
                        alphaG[layerIdx], modeG[layerIdx], ctx);
            }
            ctx.layerIndex = null;

            // Escreve na ordem que o binário sempre exigiu: por TICK,
            // dentro de cada tick por OBJETO (pco) — ver
            // SpriteData.Init/DrawSprite, que leem "psz por objeto,
            // repetido pco vezes, repetido fco vezes".
            for (int tick = 0; tick < fco; tick++) {
                for (int layerIdx = 0; layerIdx < pco; layerIdx++) {
                    int start = movieOut.size();
                    int img = imgG[layerIdx][tick];
                    writeShortLE(movieOut, img);

                    if (img != -1) {
                        // $MID aplica $FLIP (espelhamento) e depois $ROT
                        // (rotação em volta do centro) — se flipX=flipY=
                        // false e rot=0, produz exatamente o mesmo quad
                        // alinhado aos eixos de sempre (ver
                        // writeQuadFlippedAndRotated). $CROP NUNCA
                        // espelha/rotaciona (é sempre um recorte reto de
                        // textura).
                        writeQuadFlippedAndRotated(movieOut, midXG[layerIdx][tick], midYG[layerIdx][tick],
                                midWG[layerIdx][tick], midHG[layerIdx][tick], rotG[layerIdx][tick],
                                flipXG[layerIdx][tick], flipYG[layerIdx][tick]);
                        writeQuadFromRect(movieOut, cropXG[layerIdx][tick], cropYG[layerIdx][tick],
                                cropWG[layerIdx][tick], cropHG[layerIdx][tick]);
                        movieOut.write(alphaG[layerIdx][tick] & 0xFF);
                        movieOut.write(modeG[layerIdx][tick] & 0xFF);
                    }

                    while (movieOut.size() - start < psz) {
                        movieOut.write(0);
                    }
                }
            }

            byte[] movieBytes = movieOut.toByteArray();
            offsets.add(moviesData.size());
            sizes.add(movieBytes.length);
            moviesData.write(movieBytes);
            movieIdx++;
        }

        return finalizeBin(movieBlocks.size(), offsets, sizes, moviesData);
    }

    // ================= PARSE: $LAYER -> lista de $ANIM -> lista de $FR (cru) =================

    private static LayerBlock parseLayerBlock(String layerText, ErrCtx ctx) {
        LayerBlock layer = new LayerBlock();
        List<String> animBlocks = splitTopLevelBlocks(layerText, "$ANIM_");

        int animIdx = 0;
        for (String animText : animBlocks) {
            ctx.animIndex = animIdx;
            AnimBlock anim = new AnimBlock();
            anim.sourceIndex = animIdx;
            anim.start = extractIntChecked(animText, START_P, "$START", ctx, 0, Integer.MAX_VALUE);

            List<String> frBlocks = splitTopLevelBlocks(animText, "$FR_");
            int frIdx = 0;
            for (String frText : frBlocks) {
                ctx.frIndex = frIdx;
                RawFr rawFr = parseRawFr(frText, ctx);
                rawFr.sourceIndex = frIdx;
                anim.frames.add(rawFr);
                frIdx++;
            }
            ctx.frIndex = null;
            layer.anims.add(anim);
            animIdx++;
        }
        ctx.animIndex = null;

        // Ordena por $START crescente — robustez contra reordenação
        // manual do autor no texto fonte.
        Collections.sort(layer.anims, new Comparator<AnimBlock>() {
            @Override public int compare(AnimBlock a, AnimBlock b) {
                return Integer.compare(a.start, b.start);
            }
        });

        return layer;
    }

    private static RawFr parseRawFr(String frText, ErrCtx ctx) {
        RawFr fr = new RawFr();
        int rawTime = extractIntChecked(frText, TIME_P, "$TIME", ctx, 0, Integer.MAX_VALUE);
        fr.time = Math.max(1, rawTime);
        fr.img = extractIntChecked(frText, IMG_P, "$IMG", ctx, Integer.MIN_VALUE, Integer.MAX_VALUE);

        double[] crop = extractRect(frText, CROP_P, "$CROP", ctx);
        fr.cropX = crop[0]; fr.cropY = crop[1]; fr.cropW = crop[2]; fr.cropH = crop[3];

        double[] mid = extractRect(frText, MID_P, "$MID", ctx);
        fr.midX = mid[0]; fr.midY = mid[1]; fr.midW = mid[2]; fr.midH = mid[3];

        fr.easeParam = parseTransition(frText, ctx);
        fr.opacity = extractIntChecked(frText, OPACITY_P, "$OPACITY", ctx, 0, 255); // dentro de $RAZ:{...} — extractInt varre frText inteiro, então funciona sem isolar o bloco
        fr.mode = extractIntChecked(frText, MODE_P, "$MODE", ctx, 0, 255);
        fr.rot = extractDouble(frText, ROT_P); // ausente = 0 (sem rotação); decimal é o formato esperado, sem checagem de "não deveria ser decimal"

        // $FLIP:{$X:.., $Y:..} — ausente = not/not (sem espelhamento),
        // mesmo padrão de campo opcional dos demais.
        Matcher flipMatch = FLIP_P.matcher(frText);
        if (flipMatch.find()) {
            String flipInner = flipMatch.group(1);
            fr.flipX = parseYesNo(flipInner, FIELD_FLIP_X_P);
            fr.flipY = parseYesNo(flipInner, FIELD_FLIP_Y_P);
        }
        return fr;
    }

    private static boolean parseYesNo(String text, Pattern p) {
        Matcher m = p.matcher(text);
        if (!m.find()) return false;
        String v = m.group(1).toLowerCase();
        return v.equals("yes") || v.equals("1");
    }

    // yes/not são tratados como PALAVRAS-CHAVE do formato antigo (nunca
    // números — o writer anterior sempre escrevia por extenso, nunca
    // "1"/"0" como sinônimo, então não há arquivo real ambíguo).
    //   "yes"          -> easeParam 0.0 (interpola o intervalo inteiro,
    //                     mesmo efeito visual do antigo "yes")
    //   "not"          -> EASE_PARAM_NOT (sentinela: "sem transição de
    //                     verdade" — nunca produzido pelo formato novo,
    //                     que só usa 0.0-1.0; ver expandLayerIntoTicks)
    //   qualquer número -> easeParam direto, SEM reinterpretação (evita
    //                     a ambiguidade de "1 seria yes, ou 1.0 mesmo?")
    private static final double EASE_PARAM_NOT = -1.0;

    private static double parseTransition(String text, ErrCtx ctx) {
        Matcher m = TRANSITION_P.matcher(text);
        if (!m.find()) return EASE_PARAM_NOT; // ausente = sem transição, mesmo comportamento cauteloso de sempre
        String v = m.group(1).toLowerCase();
        if (v.equals("yes")) return 0.0;
        if (v.equals("not")) return EASE_PARAM_NOT;
        double val = Double.parseDouble(v);
        if (ctx != null && (val < 0.0 || val > 1.0)) {
            ctx.report("$TRANSITION", "$TRANSITION = " + val + " está fora do range esperado [0.0..1.0] "
                    + "— valores fora desse intervalo produzem interpolação incorreta (ver effectiveProgress)");
        }
        return val;
    }

    // ================= RESOLUÇÃO: RawFr (com auto) -> ResolvedFr (valores reais) =================

    // Resolve $CROP automático (W<0 e H<0 -> imagem inteira) e depois
    // $MID automático (W<0 e H<0 -> tamanho do $CROP já resolvido) — a
    // ORDEM importa: $MID pode depender do $CROP resolvido, nunca o
    // contrário.
    private static ResolvedFr resolveFrKeyframe(RawFr raw, File sprDir, Map<Integer, int[]> imageDimsCache, ErrCtx ctx)
            throws IOException {
        ResolvedFr out = new ResolvedFr();
        out.img = raw.img;
        out.opacity = raw.opacity;
        out.mode = raw.mode;
        out.rot = raw.rot;
        out.flipX = raw.flipX;
        out.flipY = raw.flipY;

        int[] dims = getImageDims(raw.img, sprDir, imageDimsCache);

        boolean cropIsAuto = raw.cropW < 0 && raw.cropH < 0;
        if (cropIsAuto) {
            out.cropX = 0;
            out.cropY = 0;
            out.cropW = (dims != null) ? dims[0] : 0;
            out.cropH = (dims != null) ? dims[1] : 0;
            if (dims == null && raw.img >= 0 && ctx != null) {
                ctx.report("$IMG", "$IMG:" + raw.img + " referenciado com $CROP automático, mas nenhuma imagem "
                        + idx3(raw.img) + ".png/.webp foi encontrada na pasta — $CROP ficará 0x0");
            }
        } else {
            out.cropX = raw.cropX;
            out.cropY = raw.cropY;
            out.cropW = raw.cropW;
            out.cropH = raw.cropH;
        }

        boolean midIsAuto = raw.midW < 0 && raw.midH < 0;
        out.midX = raw.midX; // X/Y nunca são auto
        out.midY = raw.midY;
        if (midIsAuto) {
            out.midW = out.cropW;
            out.midH = out.cropH;
        } else {
            out.midW = raw.midW;
            out.midH = raw.midH;
        }

        return out;
    }

    // ================= EXPANSÃO: $ANIM/$FR (com $TIME/$TRANSITION) -> 1 registro por tick =================

    // Preenche os arrays [fco] desta layer com os valores já resolvidos
    // de cada tick — ticks fora de qualquer $ANIM permanecem com img=-1
    // (default já aplicado pelo chamador antes desta chamada).
    private static void expandLayerIntoTicks(LayerBlock layer, int fco, File sprDir,
            Map<Integer, int[]> imageDimsCache,
            int[] imgOut, double[] midXOut, double[] midYOut, double[] midWOut, double[] midHOut,
            double[] rotOut, boolean[] flipXOut, boolean[] flipYOut,
            double[] cropXOut, double[] cropYOut, double[] cropWOut, double[] cropHOut,
            int[] alphaOut, int[] modeOut, ErrCtx ctx) throws IOException {

        for (AnimBlock anim : layer.anims) {
            ctx.animIndex = anim.sourceIndex;
            int tick = anim.start;
            ResolvedFr prev = null;

            for (RawFr rawFr : anim.frames) {
                ctx.frIndex = rawFr.sourceIndex;
                ResolvedFr fr = resolveFrKeyframe(rawFr, sprDir, imageDimsCache, ctx);

                if (prev == null || rawFr.easeParam == EASE_PARAM_NOT) {
                    // Sem transição (ou primeiro $FR do $ANIM, que não
                    // tem "de onde vir"): mantém o valor deste $FR
                    // constante por rawFr.time ticks — salto seco.
                    for (int t = 0; t < rawFr.time && tick < fco; t++, tick++) {
                        writeTick(tick, fr.img, fr.midX, fr.midY, fr.midW, fr.midH, fr.rot, fr.flipX, fr.flipY,
                                fr.cropX, fr.cropY, fr.cropW, fr.cropH, fr.opacity, fr.mode,
                                imgOut, midXOut, midYOut, midWOut, midHOut, rotOut, flipXOut, flipYOut,
                                cropXOut, cropYOut, cropWOut, cropHOut, alphaOut, modeOut);
                    }
                } else {
                    // Transição com PONTO DE CORTE (easeParam 0.0-1.0):
                    // $TIME deste $FR = quantos ticks até CHEGAR nele
                    // (mesma semântica do DAC). Antes do ponto de corte
                    // (rawProgress <= easeParam), o valor fica PARADO no
                    // keyframe anterior; a partir do corte, interpola
                    // linearmente até este, chegando exatamente nele no
                    // último tick — ver effectiveProgress(). A
                    // VELOCIDADE nunca é escolhida diretamente: quanto
                    // mais tarde o corte, menos tempo sobra para a mesma
                    // distância, logo mais rápida a transição.
                    //
                    // O QUE INTERPOLA: $MID/$ROT/$OPACITY (propriedades
                    // de exibição contínuas). O QUE NÃO INTERPOLA: img,
                    // mode, flipX, flipY, $CROP — todos usam o valor de
                    // DESTINO (fr) desde o primeiro tick da transição
                    // (ver nota completa mantida abaixo).
                    for (int t = 0; t < rawFr.time && tick < fco; t++, tick++) {
                        double rawProgress = (double) (t + 1) / (double) rawFr.time;
                        double progress = effectiveProgress(rawProgress, rawFr.easeParam);
                        double midX = lerp(prev.midX, fr.midX, progress);
                        double midY = lerp(prev.midY, fr.midY, progress);
                        double midW = lerp(prev.midW, fr.midW, progress);
                        double midH = lerp(prev.midH, fr.midH, progress);
                        double rot = lerp(prev.rot, fr.rot, progress);
                        int alpha = (int) Math.round(lerp(prev.opacity, fr.opacity, progress));

                        writeTick(tick, fr.img, midX, midY, midW, midH, rot, fr.flipX, fr.flipY,
                                fr.cropX, fr.cropY, fr.cropW, fr.cropH, alpha, fr.mode,
                                imgOut, midXOut, midYOut, midWOut, midHOut, rotOut, flipXOut, flipYOut,
                                cropXOut, cropYOut, cropWOut, cropHOut, alphaOut, modeOut);
                    }
                }

                prev = fr;
            }
        }
        ctx.animIndex = null;
        ctx.frIndex = null;
    }

    // Converte o progresso "bruto" (t/time, 0..1) no progresso EFETIVO
    // de interpolação, dado o ponto de corte easeParam (0..1): antes do
    // corte, devolve 0 (valor parado no keyframe anterior); a partir
    // dele, interpola linearmente até 1 no fim do intervalo. Ver
    // Keyframe.easeParam para a explicação completa da semântica.
    private static double effectiveProgress(double rawProgress, double easeParam) {
        if (rawProgress <= easeParam) return 0.0;
        if (easeParam >= 1.0) return 0.0; // corte no fim ou além — nunca chega a interpolar de fato
        return (rawProgress - easeParam) / (1.0 - easeParam);
    }

    private static void writeTick(int tick, int img,
            double midX, double midY, double midW, double midH, double rot, boolean flipX, boolean flipY,
            double cropX, double cropY, double cropW, double cropH,
            int alpha, int mode,
            int[] imgOut, double[] midXOut, double[] midYOut, double[] midWOut, double[] midHOut, double[] rotOut,
            boolean[] flipXOut, boolean[] flipYOut,
            double[] cropXOut, double[] cropYOut, double[] cropWOut, double[] cropHOut,
            int[] alphaOut, int[] modeOut) {
        imgOut[tick] = img;
        midXOut[tick] = midX; midYOut[tick] = midY; midWOut[tick] = midW; midHOut[tick] = midH;
        rotOut[tick] = rot;
        flipXOut[tick] = flipX; flipYOut[tick] = flipY;
        cropXOut[tick] = cropX; cropYOut[tick] = cropY; cropWOut[tick] = cropW; cropHOut[tick] = cropH;
        alphaOut[tick] = alpha; modeOut[tick] = mode;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // NOTA sobre $ROT: interpola com lerp() comum (não circular) porque o
    // valor lido do texto fonte já vem DESDOBRADO (unwrapped) pelo
    // SprExtractor — pode ser negativo ou maior que 360°, preservando a
    // direção real e a quantidade de voltas do giro original. Uma
    // interpolação circular (escolhendo sempre "o caminho mais curto"
    // entre dois ângulos normalizados) causava um bug real: quando a
    // diferença entre dois keyframes consecutivos ficava perto de 180°,
    // o caminho mais curto podia discordar da direção real do movimento
    // por poucos graus, fazendo a rotação girar ao contrário por um
    // trecho em vez de completar a volta no sentido certo. A conversão
    // de volta para [0,360) (necessária para gerar os vértices de fato)
    // acontece só dentro de writeQuadFlippedAndRotated, via
    // Math.toRadians — que já lida naturalmente com ângulos fora de
    // [0,360) sem precisar de normalização explícita antes.

    // ================= LEITURA DE DIMENSÕES (PNG cru + WEBP via libwebp) =================

    // Devolve [width, height] do arquivo NNN.png ou NNN.webp
    // correspondente ao índice de imagem, ou null se nenhum dos dois
    // existir/for legível.
    private static int[] getImageDims(int imgIndex, File sprDir, Map<Integer, int[]> cache) throws IOException {
        if (imgIndex < 0) return null;
        if (cache.containsKey(imgIndex)) return cache.get(imgIndex);

        String idx = idx3(imgIndex);
        int[] dims = null;

        File png = new File(sprDir, idx + ".png");
        if (png.exists()) {
            dims = readPngDims(png);
        } else {
            File webp = new File(sprDir, idx + ".webp");
            if (webp.exists()) {
                dims = readWebpDims(webp);
            }
        }

        cache.put(imgIndex, dims);
        return dims;
    }

    // Lê width/height do cabeçalho PNG sem decodificar a imagem inteira:
    // assinatura de 8 bytes + chunk IHDR (4 bytes length + "IHDR" + 4
    // bytes width BE + 4 bytes height BE, a partir do offset 16).
    private static int[] readPngDims(File f) throws IOException {
        byte[] header = new byte[24];
        try (FileInputStream in = new FileInputStream(f)) {
            int read = in.read(header);
            if (read < 24) return null;
        }
        int width = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16)
                | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
        int height = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16)
                | ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
        return new int[]{width, height};
    }

    // Lê width/height de um .webp usando BitmapFactory nativo do
    // Android (inJustDecodeBounds = true decodifica só o cabeçalho, sem
    // alocar o bitmap inteiro) — o Android já suporta WebP nativamente
    // desde a API 14, incluindo os formatos VP8/VP8L/VP8X, então não é
    // necessário nenhum decoder externo aqui.
    //
    // NOTA: este é o SprBuilder do GAME COMPILER (dentro do próprio
    // jogo, usado para builds de teste em tempo real — ver
    // Compiler.java) — WebpAnimNative/libwebp (via JNI) já está
    // disponível neste pacote, mas BitmapFactory é preferido aqui mesmo
    // assim por simplicidade e consistência com a versão do app
    // editor/compilador (appCompiler): só precisamos das DIMENSÕES (para
    // resolver $CROP/$MID automáticos), não da decodificação de todos os
    // frames da animação, que é para o que WebpAnimNative existe de
    // verdade (reprodução da animação em tempo real no jogo).
    private static int[] readWebpDims(File f) {
        android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null;
        return new int[]{opts.outWidth, opts.outHeight};
    }

    // ================= PARSE DE BLOCOS COM CHAVES ANINHADAS =================

    // Encontra TODAS as ocorrências de "prefix" (ex.: "$LAYER_") no nível
    // em que aparecem no texto, e devolve o CONTEÚDO de cada bloco
    // "prefixN: { ... }" correspondente (o texto entre as chaves
    // balanceadas, sem as chaves externas) — respeitando aninhamento
    // (chaves internas de $ANIM/$FR não confundem o parser).
    private static List<String> splitTopLevelBlocks(String text, String prefix) {
        List<String> result = new ArrayList<String>();
        int searchFrom = 0;
        while (true) {
            int prefixPos = text.indexOf(prefix, searchFrom);
            if (prefixPos < 0) break;

            int bracePos = text.indexOf('{', prefixPos);
            if (bracePos < 0) break;

            int endPos = findMatchingBrace(text, bracePos);
            if (endPos < 0) break;

            result.add(text.substring(bracePos + 1, endPos));
            searchFrom = endPos + 1;
        }
        return result;
    }

    // Extrai o conteúdo de um bloco nomeado ÚNICO (ex.: "$INFO") dentro
    // de um texto maior — usado só para $INFO (um por $MOVIE, diferente
    // de $LAYER_N/$ANIM_N/$FR_N, que se repetem com sufixo numérico,
    // tratados por splitTopLevelBlocks).
    private static String extractBraceBlock(String text, String name) {
        int pos = text.indexOf(name);
        if (pos < 0) return "";
        int bracePos = text.indexOf('{', pos);
        if (bracePos < 0) return "";
        int endPos = findMatchingBrace(text, bracePos);
        if (endPos < 0) return "";
        return text.substring(bracePos + 1, endPos);
    }

    // Dado o índice de uma '{' de abertura, encontra o índice da '}' que
    // fecha ela (respeitando aninhamento de outras chaves no meio).
    private static int findMatchingBrace(String text, int openPos) {
        int depth = 0;
        for (int i = openPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    // ================= HELPERS =================

    private static double[] extractRect(String text, Pattern outerPattern, String fieldTag, ErrCtx ctx) {
        Matcher m = outerPattern.matcher(text);
        if (!m.find()) return new double[]{0, 0, 0, 0};
        String inner = m.group(1);

        if (inner.contains("$X") || inner.contains("$Y") || inner.contains("$W") || inner.contains("$H")) {
            double x = extractIntChecked(inner, FIELD_X_P, fieldTag + ".$X", ctx, Integer.MIN_VALUE, Integer.MAX_VALUE);
            double y = extractIntChecked(inner, FIELD_Y_P, fieldTag + ".$Y", ctx, Integer.MIN_VALUE, Integer.MAX_VALUE);
            double w = extractIntChecked(inner, FIELD_W_P, fieldTag + ".$W", ctx, Integer.MIN_VALUE, Integer.MAX_VALUE);
            double h = extractIntChecked(inner, FIELD_H_P, fieldTag + ".$H", ctx, Integer.MIN_VALUE, Integer.MAX_VALUE);
            return new double[]{x, y, w, h};
        }

        String[] parts = inner.split(",");
        if (parts.length == 4) {
            return new double[]{i(parts[0]), i(parts[1]), i(parts[2]), i(parts[3])};
        }
        if (ctx != null) {
            ctx.report(fieldTag, fieldTag + " não tem 4 valores no formato esperado ($X:/$Y:/$W:/$H: ou 4 números separados por vírgula) — usando 0,0,0,0");
        }
        return new double[]{0, 0, 0, 0};
    }

    // Gera os 4 vértices de um retângulo x,y,w,h (canto superior-esquerdo
    // + tamanho, mesma convenção de $MID/writeQuadFromRect) rotacionado
    // por rotDegrees graus em volta do PRÓPRIO CENTRO, e então espelhado
    // conforme flipX/flipY — inverso exato de
    // SprExtractor.extractQuadGeometry (que faz o caminho oposto: desfaz
    // o flip reordenando os pontos, DEPOIS mede a rotação).
    //
    // ORDEM: gera os 4 pontos rotacionados normalmente (mesmo algoritmo
    // de sempre), e só DEPOIS reordena os vértices para aplicar o flip —
    // flipX troca p0<->p3 e p1<->p2 (mesma reordenação usada em
    // extractQuadGeometry para "desfazer" um flipX); flipY troca p0<->p1
    // e p2<->p3. Se flipX=flipY=false e rotDegrees=0, produz EXATAMENTE
    // os mesmos 4 pontos que writeQuadFromRect já produzia.
    private static void writeQuadFlippedAndRotated(ByteArrayOutputStream out, double x, double y, double w, double h,
            double rotDegrees, boolean flipX, boolean flipY) throws IOException {

        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        double halfW = w / 2.0;
        double halfH = h / 2.0;

        double[][] localCorners = {
                {-halfW, -halfH}, // p0
                {-halfW, halfH},  // p1
                {halfW, halfH},   // p2
                {halfW, -halfH},  // p3
        };

        double rad = Math.toRadians(rotDegrees);
        double cosR = Math.cos(rad);
        double sinR = Math.sin(rad);

        int[] xs = new int[4];
        int[] ys = new int[4];
        for (int i = 0; i < 4; i++) {
            double localX = localCorners[i][0];
            double localY = localCorners[i][1];
            double rotatedX = localX * cosR - localY * sinR;
            double rotatedY = localX * sinR + localY * cosR;
            xs[i] = (int) Math.round(cx + rotatedX);
            ys[i] = (int) Math.round(cy + rotatedY);
        }

        // Reordena os índices conforme o flip (mesma reordenação que
        // extractQuadGeometry usa para DESFAZER — aqui aplicamos, então é
        // a operação direta, não inversa, mas a permutação de índices é
        // idêntica porque flip é sua própria operação inversa).
        int[] order = {0, 1, 2, 3};
        if (flipX) {
            order = new int[]{order[3], order[2], order[1], order[0]};
        }
        if (flipY) {
            order = new int[]{order[1], order[0], order[3], order[2]};
        }

        for (int idx : order) {
            writeShortLE(out, xs[idx]);
            writeShortLE(out, ys[idx]);
        }
    }

    private static void writeQuadFromRect(ByteArrayOutputStream out, double x, double y, double w, double h)
            throws IOException {
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        int iw = (int) Math.round(w);
        int ih = (int) Math.round(h);
        writeShortLE(out, ix); writeShortLE(out, iy);
        writeShortLE(out, ix); writeShortLE(out, iy + ih);
        writeShortLE(out, ix + iw); writeShortLE(out, iy + ih);
        writeShortLE(out, ix + iw); writeShortLE(out, iy);
    }

    private static int i(String s) { return Integer.parseInt(s.trim()); }

    private static byte[] finalizeBin(int count, List<Integer> offs, List<Integer> szs,
                                      ByteArrayOutputStream data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeShortLE(out, count);
        for (int i = 0; i < count; i++) {
            writeIntLE(out, offs.get(i));
            writeIntLE(out, szs.get(i));
        }
        out.write(data.toByteArray());
        return out.toByteArray();
    }

    private static double extractDouble(String src, Pattern p) {
        Matcher m = p.matcher(src);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    /**
     * Extrai um inteiro e valida contra [min, max], reportando (sem abortar)
     * quando o valor está fora do range esperado — o range em si já é
     * garantido pela regex do Pattern (todas as *_P deste arquivo usam
     * \d+/-?\d+, nunca aceitam ponto decimal), então aqui só falta checar
     * limites de negócio (ex: $PCO não pode ser negativo).
     *
     * @return o valor extraído (mesmo fora de range, para manter o binário
     *         "gerável"), ou 0 se a tag estiver ausente (comportamento
     *         idêntico ao extractInt original).
     */
    private static int extractIntChecked(String src, Pattern p, String fieldTag, ErrCtx ctx, int min, int max) {
        Matcher m = p.matcher(src);
        if (!m.find()) return 0;

        int value;
        try {
            value = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            if (ctx != null) ctx.report(fieldTag, fieldTag + " espera um inteiro, mas recebeu \"" + m.group(1) + "\"");
            return 0;
        }

        if (ctx != null && (value < min || value > max)) {
            ctx.report(fieldTag, fieldTag + " = " + value + " está fora do range esperado [" + min + ".." + max + "]");
        }

        return value;
    }

    private static String readFileUtf8(File f) throws IOException {
        byte[] bytes = readFullFileBytes(f);
        return new String(bytes, "UTF-8");
    }

    private static byte[] readFullFileBytes(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        try (FileInputStream in = new FileInputStream(f)) {
            in.read(b);
        }
        return b;
    }

    private static String idx3(int i) {
        return (i < 10 ? "00" : i < 100 ? "0" : "") + i;
    }

    private static void writeShortLE(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF); os.write((v >>> 8) & 0xFF);
    }

    private static void writeIntLE(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >>> 8) & 0xFF);
        os.write((v >>> 16) & 0xFF);
        os.write((v >>> 24) & 0xFF);
    }
}
