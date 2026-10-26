package com.zue.gd.util;

import java.io.*;
import java.util.*;

// ══════════════════════════════════════════════════════════════════════
// SprExtractor — decompila NNN.bin (binário lido por SpriteData.Init/
// TCBManajer.DrawSprite) em NNN.bin (formato fonte $LAYER/$ANIM/$FR,
// companheiro de SprBuilder.java — que faz o caminho inverso).
//
// O binário é sempre "1 registro por tick, repetido pco vezes por tick,
// repetido fco vezes" (nunca guarda $TIME/$TRANSITION — isso é só do
// texto fonte). Este extractor faz o caminho de COMPRESSÃO: por
// trilha (índice de objeto), agrupa ticks consecutivos em $ANIM (quando
// não há gap de img=-1) e dentro de cada $ANIM comprime a progressão de
// mid/crop/alpha em $FR (keyframes), aceitando desvios da reta ideal até
// TOLERANCE_PX (mid/crop) / TOLERANCE_ALPHA (opacidade) — quanto maior a
// tolerância, mais uma animação "quase linear mas irregular" (ex.:
// exportada por outra engine com pequenos arredondamentos) ainda vira UM
// $FR interpolado em vez de vários $FR secos.
// ══════════════════════════════════════════════════════════════════════
public final class SprExtractor {

    // ── Tolerância de compressão — ajustável em runtime ─────────────────
    // Quanto maior, mais discrepância entre o movimento real (tick a
    // tick) e a reta interpolada ideal o extractor aceita antes de
    // "quebrar" o segmento em um novo $FR. Zero = só comprime trechos
    // PERFEITAMENTE lineares (qualquer desvio de 1 unidade já quebra).
    private static double TOLERANCE_PX = 1.5;      // desvio máximo aceito em $MID/$CROP (pixels)
    private static double TOLERANCE_ALPHA = 2.0;   // desvio máximo aceito em $OPACITY (0-255)
    private static double TOLERANCE_ROT_DEGREES = 1.0; // NOVO — desvio máximo aceito em $ROT (graus)

    public static void setTolerancePx(double v) { TOLERANCE_PX = v; }
    public static void setToleranceAlpha(double v) { TOLERANCE_ALPHA = v; }
    public static void setToleranceRotDegrees(double v) { TOLERANCE_ROT_DEGREES = v; }

    private SprExtractor() {}

    public static void process(String pacName, String index, byte[] src, int dataPos, int dataSize, File outDir) throws Exception {
        File sprDir = new File(outDir, index + ".spr");
        if (!sprDir.exists()) sprDir.mkdirs();

        byte[] seg = new byte[dataSize];
        System.arraycopy(src, dataPos, seg, 0, dataSize);
        extractMiniPac(seg, sprDir);
    }

    private static void extractMiniPac(byte[] seg, File sprDir) throws Exception {
        if (seg == null || seg.length < 2) return;

        int count = readShortLE(seg, 0);
        int headerSize = 2 + (count * 16);

        int internalPngCount = 0;
        int pos = 2;
        for (int i = 0; i < count; i++) {
            int type = readIntBE(seg, pos + 8);
            if (type == 0x706E6700) internalPngCount++;
            pos += 16;
        }

        int pngPtr = 0;
        int wavPtr = internalPngCount;
        int webpPtr = 0;
        pos = 2;

        // Guarda os segmentos de "bin" (movie binário) para processar
        // DEPOIS de todos os .png/.webp já terem sido salvos em disco —
        // sprBinExtractor precisa ler as dimensões dessas imagens para
        // detectar $CROP/$MID automáticos, então a ordem de extração
        // importa (imagens primeiro, bin por último).
        List<byte[]> pendingBinSegments = new ArrayList<byte[]>();
        List<String> pendingBinNames = new ArrayList<String>();

        for (int i = 0; i < count; i++) {
            int off = readIntLE(seg, pos);
            int size = readIntLE(seg, pos + 4);
            int type = readIntBE(seg, pos + 8);
            pos += 16;

            int dataOff = headerSize + off;

            String name;
            if (type == 0x706E6700) name = idx3(pngPtr++);
            else if (type == 0x77617600) name = idx3(wavPtr++);
            else if (type == 0x77656200) name = idx3(webpPtr++);
            else name = "000";

            if (type == 0x706E6700) {
                saveBytes(seg, dataOff, size, new File(sprDir, name + ".png"));
            } else if (type == 0x62696E00) {
                pendingBinSegments.add(slice(seg, dataOff, size));
                pendingBinNames.add(name);
            } else if (type == 0x77656200) {
                saveBytes(seg, dataOff, size, new File(sprDir, name + ".webp"));
            }
        }

        for (int i = 0; i < pendingBinSegments.size(); i++) {
            byte[] textBytes = sprBinExtractor(pendingBinSegments.get(i), sprDir);
            File outFile = new File(sprDir, pendingBinNames.get(i) + ".bin.txt");
            saveBytes(textBytes, 0, textBytes.length, outFile);
        }
    }

    // ================= BIN -> TEXTO ($LAYER/$ANIM/$FR) =================

    // Estado de UM tick de UMA trilha, já decodificado do binário.
    private static final class TickState {
        int img; // -1 = gap
        double midX, midY, midW, midH;
        double rot; // graus, 0 = sem rotação
        boolean flipX, flipY; // NOVO — espelhamento nos eixos, ver extractQuadGeometry
        double cropX, cropY, cropW, cropH;
        int alpha;
        int mode;
    }

    // Devolve os bytes UTF-8 do texto $LAYER/$ANIM/$FR gerado — NÃO
    // escreve em arquivo diretamente (contrato do appCompiler: quem
    // chama decide onde persistir, igual ao restante do extractor).
    public static byte[] sprBinExtractor(byte[] seg, File sprDir) throws IOException {
        int movieCount = readShortLE(seg, 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(baos, "UTF-8"));
        SprWriter w = new SprWriter(out);

        int p = 2;
        int[] offs = new int[movieCount];
        for (int i = 0; i < movieCount; i++) {
            offs[i] = readIntLE(seg, p) + (movieCount * 8) + 2;
            p += 8;
        }

        Map<Integer, int[]> imageDimsCache = new HashMap<Integer, int[]>();

        for (int m = 0; m < movieCount; m++) {
            int cur = offs[m];

            int psz = readShortLE(seg, cur); cur += 2;
            int pco = readShortLE(seg, cur); cur += 2;
            int ssz = readShortLE(seg, cur); cur += 2;
            int sco = readShortLE(seg, cur); cur += 2;
            int fco = readShortLE(seg, cur); cur += 2;
            int lp  = readShortLE(seg, cur); cur += 2;
            int tp  = readIntLE(seg, cur);   cur += 4;

            w.writeLine("$MOVIE_" + m + ": {$INFO:{"
                    + "$PSZ: " + psz + ", $PCO: " + pco + ", $SSZ: " + ssz
                    + ", $SCO: " + sco + ", $FCO: " + fco + ", $LP: " + lp
                    + ", $TP: " + tp + "},");

            // ── Passo 1: ler TODOS os ticks, organizados por [layer][tick] ──
            TickState[][] ticks = new TickState[pco][fco];
            double[] prevRotByLayer = new double[pco];
            Arrays.fill(prevRotByLayer, Double.NaN); // NaN = ainda não há tick anterior nesta trilha (ou trilha ainda não começou um $ANIM)
            // NOTA: prevRotByLayer[layer] NÃO é resetado quando a trilha
            // passa por um gap (img=-1) — se ela reaparecer depois, ainda
            // faz sentido comparar contra o último rot conhecido antes do
            // gap (mais contexto é melhor que nenhum, e um novo $ANIM
            // após o gap normalmente reinicia visualmente do zero de
            // qualquer forma, então o "salto" de continuidade tende a
            // ser irrelevante na prática).

            for (int tick = 0; tick < fco; tick++) {
                for (int layer = 0; layer < pco; layer++) {
                    int objBase = cur;
                    TickState ts = new TickState();
                    short imgId = readShortLE(seg, cur);
                    ts.img = imgId;

                    if (imgId != -1) {
                        short[] vt = new short[8];
                        for (int k = 0; k < 8; k++) vt[k] = readShortLE(seg, cur + 2 + (k * 2));
                        short[] uv = new short[8];
                        for (int k = 0; k < 8; k++) uv[k] = readShortLE(seg, cur + 18 + (k * 2));

                        // $VT (mid): pode estar ROTACIONADO e/ou ESPELHADO
                        // — extração via extractQuadGeometry, que testa as
                        // 4 combinações de flip e escolhe a mais consistente
                        // com o rot do tick anterior desta mesma trilha
                        // (continuidade). Ver comentário completo da função.
                        double[] midGeom = extractQuadGeometry(vt, prevRotByLayer[layer]);
                        ts.midX = midGeom[0]; ts.midY = midGeom[1];
                        ts.midW = midGeom[2]; ts.midH = midGeom[3];
                        double normalizedRot = midGeom[4]; // sempre em [0,360)
                        ts.flipX = midGeom[5] != 0;
                        ts.flipY = midGeom[6] != 0;

                        // rot DESDOBRADO (unwrapped): ao contrário de
                        // normalizedRot (sempre [0,360)), este campo pode
                        // crescer indefinidamente (ou ficar negativo) —
                        // resolve uma ambiguidade real: quando a diferença
                        // entre dois keyframes fica perto de 180°, o
                        // "caminho mais curto" entre os dois ângulos
                        // normalizados pode discordar da DIREÇÃO REAL do
                        // giro (ex.: 112°->293° tem diferença direta de
                        // 181° mas caminho contrário de só 179° — por 2°
                        // de diferença, escolher "o caminho mais curto"
                        // inverteria o sentido do giro, fazendo a
                        // animação girar ao contrário por um instante em
                        // vez de completar a volta). Guardando o ângulo
                        // já desdobrado tick-a-tick (que sabe a direção
                        // real, pois nunca pula mais que a rotação real
                        // entre ticks consecutivos), a interpolação entre
                        // $FR consecutivos vira uma reta comum (lerp, não
                        // lerpAngle) — sem ambiguidade de "qual caminho".
                        double rotDelta = normalizedRot - (prevRotByLayer[layer] % 360 + 360) % 360;
                        while (rotDelta > 180) rotDelta -= 360;
                        while (rotDelta < -180) rotDelta += 360;
                        double unwrappedRot = Double.isNaN(prevRotByLayer[layer])
                                ? normalizedRot
                                : prevRotByLayer[layer] + rotDelta;

                        ts.rot = unwrappedRot;
                        prevRotByLayer[layer] = unwrappedRot;

                        // $UV (crop): NUNCA rotaciona/espelha no jogo
                        // (recorte de textura sempre alinhado aos eixos) —
                        // mantém a extração simples de sempre.
                        ts.cropX = uv[0]; ts.cropY = uv[1];
                        ts.cropW = uv[4] - uv[0]; ts.cropH = uv[3] - uv[1];

                        ts.alpha = seg[cur + 34] & 0xFF;
                        ts.mode = seg[cur + 35] & 0xFF;
                    }

                    ticks[layer][tick] = ts;
                    cur = objBase + psz;
                }
            }

            // ── Passo 2..4: por trilha, segmenta em $ANIM e comprime em $FR ──
            for (int layer = 0; layer < pco; layer++) {
                w.writeLine("   $LAYER_" + layer + ": {");
                writeCompressedLayer(w, ticks[layer], fco, sprDir, imageDimsCache);
                w.writeLine("   },");
            }

            w.writeLine("},");
        }
        w.close();
        return baos.toByteArray();
    }

    // ================= GEOMETRIA DE QUAD (detecção de rotação + flip) =================

    // Dado um quad de 4 pontos, devolve [x, y, w, h, rotDegrees, flipX, flipY]:
    //
    //   - x,y,w,h = retângulo NÃO rotacionado/NÃO espelhado equivalente
    //     (o $MID que, após aplicar flipX/flipY e depois rotDegrees,
    //     reproduz este quad)
    //   - rotDegrees = ângulo de rotação, 0-360
    //   - flipX/flipY = 0 ou 1 (mapeados para "not"/"yes" por quem chama)
    //
    // POR QUE TESTAR FLIP: um flip (reflexão) e uma rotação de 180°
    // produzem o MESMO quad em casos simétricos (ex.: quadrado sem
    // recorte assimétrico) — mas são operações semanticamente diferentes
    // (flip inverte a orientação/chirality do polígono, rotação
    // preserva). A shoelace formula (área com sinal) detecta ISSO: um
    // flip sozinho inverte o sinal da área; uma rotação, sozinha, nunca
    // inverte. Mas flipX e flipY sozinhos dão o MESMO sinal invertido
    // (ambíguos entre si só pela área), e flipX+flipY juntos CANCELAM o
    // sinal de volta ao original (equivalente, em termos de área, a uma
    // rotação de 180° sem flip nenhum) — por isso não basta olhar o
    // sinal da área: é preciso testar as 4 combinações (nenhum flip,
    // flipX, flipY, flipX+flipY), "desfazer" cada uma reordenando os
    // pontos, medir o rot resultante, e escolher a combinação cujo rot
    // fica mais PRÓXIMO do rot do frame anterior desta mesma trilha
    // (continuidade) — já que flip é um evento discreto (troca ou não
    // troca) e rotação é contínua ao longo do tempo, a combinação certa
    // é a que não causa um salto abrupto no ângulo medido.
    //
    // prevRot: rotação (graus) do tick anterior desta trilha, ou
    // Double.NaN se este for o primeiro tick com img != -1 do $ANIM
    // (nesse caso não há "anterior" para comparar — usa-se a combinação
    // que dá o MENOR rot em valor absoluto, por ser a leitura mais
    // simples/direta na ausência de contexto).
    private static double[] extractQuadGeometry(short[] vt, double prevRot) {
        double p0x = vt[0], p0y = vt[1];
        double p1x = vt[2], p1y = vt[3];
        double p2x = vt[4], p2y = vt[5];
        double p3x = vt[6], p3y = vt[7];

        double[] bestResult = null;
        double bestScore = Double.MAX_VALUE;

        // As 4 combinações: para "desfazer" um flipX, a ordem de leitura
        // dos 4 pontos ao redor do quad se inverte no eixo horizontal —
        // isto equivale a trocar p0<->p3 e p1<->p2 (mesma lógica de
        // writeQuadRotated/writeQuadFromRect, que sempre gera p0=sup-esq,
        // p1=inf-esq, p2=inf-dir, p3=sup-dir; um flipX troca "esquerda"
        // por "direita", ou seja, sup-esq vira sup-dir e vice-versa).
        // Um flipY troca p0<->p1 e p3<->p2 (topo vira baixo).
        for (int flipX = 0; flipX <= 1; flipX++) {
            for (int flipY = 0; flipY <= 1; flipY++) {
                double[] q = {p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y};
                if (flipX == 1) {
                    q = new double[]{q[6], q[7], q[4], q[5], q[2], q[3], q[0], q[1]}; // p0<->p3, p1<->p2
                }
                if (flipY == 1) {
                    q = new double[]{q[2], q[3], q[0], q[1], q[6], q[7], q[4], q[5]}; // p0<->p1, p2<->p3
                }

                double cx = (q[0] + q[2] + q[4] + q[6]) / 4.0;
                double cy = (q[1] + q[3] + q[5] + q[7]) / 4.0;
                double h = Math.hypot(q[2] - q[0], q[3] - q[1]);
                double w = Math.hypot(q[4] - q[2], q[5] - q[3]);
                double baseAngle = Math.toDegrees(Math.atan2(-h / 2.0, -w / 2.0));
                double measuredAngle = Math.toDegrees(Math.atan2(q[1] - cy, q[0] - cx));
                double rot = measuredAngle - baseAngle;
                rot = ((rot % 360) + 360) % 360;

                double score;
                if (Double.isNaN(prevRot)) {
                    score = angleDiff(rot, 0); // sem contexto: prefere o rot mais "limpo" (perto de 0)
                } else {
                    score = angleDiff(rot, prevRot); // com contexto: prefere continuidade
                }

                if (score < bestScore) {
                    bestScore = score;
                    double x = cx - w / 2.0;
                    double y = cy - h / 2.0;
                    bestResult = new double[]{x, y, w, h, rot, flipX, flipY};
                }
            }
        }

        return bestResult;
    }

    // ================= SEGMENTAÇÃO EM $ANIM (por gap de img=-1) =================

    private static void writeCompressedLayer(SprWriter w, TickState[] track, int fco,
            File sprDir, Map<Integer, int[]> imageDimsCache) throws IOException {

        int animIndex = 0;
        int tick = 0;
        while (tick < fco) {
            if (track[tick].img == -1) {
                tick++; // gap — nenhum $ANIM aqui, avança
                continue;
            }

            int animStart = tick;
            int animEnd = tick; // exclusivo
            while (animEnd < fco && track[animEnd].img != -1) {
                animEnd++;
            }

            w.writeLine("      $ANIM_" + (animIndex++) + ": {$START: " + animStart + ",");
            writeCompressedAnim(w, track, animStart, animEnd, sprDir, imageDimsCache);
            w.writeLine("      },");

            tick = animEnd;
        }
    }

    // ================= COMPRESSÃO EM $FR (por tolerância) =================

    // Comprime o intervalo [animStart, animEnd) de UMA trilha em uma
    // sequência de $FR. Regra: um novo segmento SEMPRE começa quando
    // $IMG ou $MODE mudam (isto nunca depende de tolerância — é exato,
    // já que nenhum dos dois interpola). Dentro de um trecho de
    // $IMG/$MODE constantes, estende o segmento greedy enquanto TODOS os
    // ticks intermediários couberem dentro da tolerância de uma reta
    // entre o primeiro e o candidato a último tick do segmento.
    //
    // O $FR escrito para cada segmento representa o DESTINO dele (mesma
    // semântica de "chegando no keyframe" que SprBuilder usa): valores do
    // ÚLTIMO tick do segmento, com $TIME = comprimento do segmento
    // inteiro (quantos ticks para chegar neste estado a partir do
    // anterior).
    private static void writeCompressedAnim(SprWriter w, TickState[] track, int animStart, int animEnd,
            File sprDir, Map<Integer, int[]> imageDimsCache) throws IOException {

        int frIndex = 0;
        int segStart = animStart;
        boolean isFirstSegmentOfAnim = true;

        while (segStart < animEnd) {
            int img = track[segStart].img;
            int mode = track[segStart].mode;
            boolean flipX = track[segStart].flipX;
            boolean flipY = track[segStart].flipY;
            double cropX = track[segStart].cropX;
            double cropY = track[segStart].cropY;
            double cropW = track[segStart].cropW;
            double cropH = track[segStart].cropH;

            int bestEnd = segStart + 1; // exclusivo
            int candidateEnd = segStart + 1;

            while (candidateEnd < animEnd
                    && track[candidateEnd].img == img
                    && track[candidateEnd].mode == mode
                    && track[candidateEnd].flipX == flipX
                    && track[candidateEnd].flipY == flipY
                    // $CROP é tratado como DISCRETO aqui, igual img/mode —
                    // NÃO passa pela checagem de tolerância de posição.
                    // Um recorte de textura representa qual PEDAÇO da
                    // imagem é mostrado (ex.: troca de pose/sprite sheet)
                    // — mesmo uma pequena mudança de $CROP pode ser uma
                    // troca de conteúdo semanticamente diferente, não um
                    // "quase mesmo lugar" que valha a pena suavizar.
                    // Interpolar entre dois $CROP diferentes faria o
                    // recorte varrer continuamente por uma região da
                    // textura que nunca deveria aparecer ali, cortando
                    // partes da imagem de forma incorreta durante a
                    // transição (bug observado: sprite aparecendo cortado
                    // na borda em determinados frames).
                    && track[candidateEnd].cropX == cropX
                    && track[candidateEnd].cropY == cropY
                    && track[candidateEnd].cropW == cropW
                    && track[candidateEnd].cropH == cropH
                    && fitsWithinTolerance(track, segStart, candidateEnd)) {
                bestEnd = candidateEnd + 1;
                candidateEnd++;
            }

            int segLen = bestEnd - segStart;
            TickState first = track[segStart];
            TickState dest = track[bestEnd - 1]; // valores de destino escritos no $FR

            boolean varies = segLen > 1 && (
                    Math.abs(first.midX - dest.midX) > 0.01 || Math.abs(first.midY - dest.midY) > 0.01 ||
                    Math.abs(first.midW - dest.midW) > 0.01 || Math.abs(first.midH - dest.midH) > 0.01 ||
                    Math.abs(first.rot - dest.rot) > 0.01 ||
                    Math.abs(first.cropX - dest.cropX) > 0.01 || Math.abs(first.cropY - dest.cropY) > 0.01 ||
                    Math.abs(first.cropW - dest.cropW) > 0.01 || Math.abs(first.cropH - dest.cropH) > 0.01 ||
                    first.alpha != dest.alpha);

            // CASO ESPECIAL: se este é o PRIMEIRO segmento do $ANIM E ele
            // tem movimento real (varies=true), um único $FR com
            // $TRANSITION:yes não bastaria — o builder SEMPRE trata o
            // primeiro $FR de um $ANIM como seco (não há "$FR anterior"
            // de onde interpolar; ver SprBuilder.expandLayerIntoTicks,
            // "prev == null"). Sem este caso especial, todo o movimento
            // do primeiro segmento seria perdido: a trilha ficaria
            // parada no valor de DESTINO (dest) do início ao fim, em vez
            // de se mover de first até dest (bug observado: nuvens que
            // deveriam atravessar a tela ficavam completamente paradas).
            //
            // Correção: escreve um $FR_0 extra, fixando o valor INICIAL
            // (first) com $TIME:1 e $TRANSITION:not (é só o ponto de
            // partida, nada para interpolar ainda) — e SÓ ENTÃO o $FR
            // com o valor final (dest), $TIME:(segLen-1) já que 1 tick
            // foi "gasto" pelo novo $FR_0, $TRANSITION:yes. O total de
            // ticks cobertos continua sendo exatamente segLen.
            if (isFirstSegmentOfAnim && varies) {
                int[] firstDims = getImageDims(first.img, sprDir, imageDimsCache);
                String firstCropOut = formatCropOrAuto(first.cropX, first.cropY, first.cropW, first.cropH, firstDims);
                String firstMidOut = formatMidOrAuto(first.midX, first.midY, first.midW, first.midH, first.cropW, first.cropH);

                w.writeLine("         $FR_" + (frIndex++) + ":{"
                        + "$TIME: 1, $IMG: " + first.img + ","
                        + "$CROP:[" + firstCropOut + "],"
                        + "$MID:[" + firstMidOut + "],"
                        + "$TRANSITION: not,"
                        + "$RAZ:{$OPACITY: " + first.alpha + ", $MODE: " + first.mode
                        + ", $ROT: " + roundRot(first.rot)
                        + ", $FLIP:{$X: " + (first.flipX ? "yes" : "not") + ", $Y: " + (first.flipY ? "yes" : "not") + "}},"
                        + "},");

                int[] dims = getImageDims(dest.img, sprDir, imageDimsCache);
                String cropOut = formatCropOrAuto(dest.cropX, dest.cropY, dest.cropW, dest.cropH, dims);
                String midOut = formatMidOrAuto(dest.midX, dest.midY, dest.midW, dest.midH, dest.cropW, dest.cropH);

                w.writeLine("         $FR_" + (frIndex++) + ":{"
                        + "$TIME: " + (segLen - 1) + ", $IMG: " + dest.img + ","
                        + "$CROP:[" + cropOut + "],"
                        + "$MID:[" + midOut + "],"
                        + "$TRANSITION: yes,"
                        + "$RAZ:{$OPACITY: " + dest.alpha + ", $MODE: " + dest.mode
                        + ", $ROT: " + roundRot(dest.rot)
                        + ", $FLIP:{$X: " + (dest.flipX ? "yes" : "not") + ", $Y: " + (dest.flipY ? "yes" : "not") + "}},"
                        + "},");
            } else {
                int[] dims = getImageDims(dest.img, sprDir, imageDimsCache);
                String cropOut = formatCropOrAuto(dest.cropX, dest.cropY, dest.cropW, dest.cropH, dims);
                String midOut = formatMidOrAuto(dest.midX, dest.midY, dest.midW, dest.midH, dest.cropW, dest.cropH);

                w.writeLine("         $FR_" + (frIndex++) + ":{"
                        + "$TIME: " + segLen + ", $IMG: " + dest.img + ","
                        + "$CROP:[" + cropOut + "],"
                        + "$MID:[" + midOut + "],"
                        + "$TRANSITION: " + (varies ? "yes" : "not") + ","
                        + "$RAZ:{$OPACITY: " + dest.alpha + ", $MODE: " + dest.mode
                        + ", $ROT: " + roundRot(dest.rot)
                        + ", $FLIP:{$X: " + (dest.flipX ? "yes" : "not") + ", $Y: " + (dest.flipY ? "yes" : "not") + "}},"
                        + "},");
            }

            isFirstSegmentOfAnim = false;
            segStart = bestEnd;
        }
    }

    // $CROP é "auto" (escrito como $W:-1,$H:-1) se X=0,Y=0 e W/H batem
    // EXATAMENTE com as dimensões reais da imagem inteira — nesse caso é
    // indistinguível de "o autor pediu a imagem toda", então escrevemos
    // no formato mais legível/compacto em vez do valor explícito.
    private static String formatCropOrAuto(double x, double y, double w, double h, int[] fullDims) {
        boolean isAuto = fullDims != null
                && Math.round(x) == 0 && Math.round(y) == 0
                && Math.round(w) == fullDims[0] && Math.round(h) == fullDims[1];
        if (isAuto) {
            return "$X:0, $Y:0, $W:-1, $H:-1";
        }
        return "$X:" + Math.round(x) + ", $Y:" + Math.round(y) + ", $W:" + Math.round(w) + ", $H:" + Math.round(h);
    }

    // $MID é "auto" (escrito como $W:-1,$H:-1) se W/H batem EXATAMENTE
    // com o $CROP já resolvido daquele mesmo $FR — X/Y são sempre
    // escritos como valores reais (nunca auto, mesma regra do builder).
    private static String formatMidOrAuto(double x, double y, double w, double h,
            double resolvedCropW, double resolvedCropH) {
        boolean isAuto = Math.round(w) == Math.round(resolvedCropW) && Math.round(h) == Math.round(resolvedCropH);
        if (isAuto) {
            return "$X:" + Math.round(x) + ", $Y:" + Math.round(y) + ", $W:-1, $H:-1";
        }
        return "$X:" + Math.round(x) + ", $Y:" + Math.round(y) + ", $W:" + Math.round(w) + ", $H:" + Math.round(h);
    }

    // ================= SEGMENTAÇÃO POR TOLERÂNCIA =================

    // Verifica se TODOS os ticks entre segStart e segEnd (inclusive)
    // ficam dentro da tolerância de uma reta de MELHOR AJUSTE
    // (regressão linear por mínimos quadrados) sobre TODOS os pontos do
    // segmento — não mais uma reta ancorada rigidamente nos 2 pontos
    // extremos (segStart, segEnd).
    //
    // POR QUE MUDAR: um movimento linear real, gerado por outra engine,
    // quase sempre tem um pequeno "jitter" de arredondamento (±1px por
    // frame, de converter posição float para short inteiro) — isso é
    // imperceptível visualmente, mas faz a reta ANCORADA NOS EXTREMOS
    // (que dependia de EXATAMENTE onde o segmento termina) ter uma
    // inclinação ligeiramente diferente da velocidade média real,
    // dependendo de qual tick calhou de ser escolhido como fim do
    // segmento. Isso fazia o algoritmo quebrar prematuramente em vários
    // segmentos com velocidades LIGEIRAMENTE diferentes entre si — cada
    // um individualmente parecia "reto o bastante", mas a MUDANÇA de
    // velocidade na junção entre dois segmentos consecutivos é
    // perceptível como um solavanco/travada, mesmo a posição nunca
    // "pulando" de forma visível. A regressão por mínimos quadrados usa
    // TODOS os pontos do segmento para achar a MELHOR reta geral,
    // absorvendo o jitter local em vez de ser enviesada por ele — na
    // prática, permite comprimir trechos de movimento linear real MUITO
    // mais longos num único $FR (validado: 200 frames de scroll de nuvem
    // cabem inteiros em 1 $FR com resíduo máximo de ~1px, contra apenas
    // ~39 frames pelo método antigo).
    //
    // O $FR escrito ainda usa os valores REAIS do último tick do
    // segmento (não os previstos pela regressão) — a regressão serve só
    // como CRITÉRIO DE ACEITAÇÃO, garantindo que a transição para o
    // PRÓXIMO segmento sempre comece exatamente de onde a anterior
    // terminou (continuidade de posição entre segmentos consecutivos).
    private static boolean fitsWithinTolerance(TickState[] track, int segStart, int segEnd) {
        int n = segEnd - segStart + 1;
        if (n <= 2) return true; // 2 pontos sempre formam uma reta perfeita — nada a checar

        if (!linearRegressionFits(track, segStart, segEnd, TOLERANCE_PX, Field.MID_X)) return false;
        if (!linearRegressionFits(track, segStart, segEnd, TOLERANCE_PX, Field.MID_Y)) return false;
        if (!linearRegressionFits(track, segStart, segEnd, TOLERANCE_PX, Field.MID_W)) return false;
        if (!linearRegressionFits(track, segStart, segEnd, TOLERANCE_PX, Field.MID_H)) return false;
        // $CROP não é checado aqui: o chamador (writeCompressedAnim) já
        // exige cropX/Y/W/H IDÊNTICOS em todo o segmento antes mesmo de
        // chamar esta função — ver comentário na condição de extensão do
        // segmento greedy.
        if (!linearRegressionFits(track, segStart, segEnd, TOLERANCE_ALPHA, Field.ALPHA)) return false;

        // Rotação: agora que rot é DESDOBRADO (unwrapped, pode crescer
        // indefinidamente ou ficar negativo — ver comentário em TickState/
        // no laço de extração principal), a regressão linear comum já
        // serve, sem precisar de tratamento circular (lerpAngle/
        // angleDiff). Isso elimina a ambiguidade que existia antes: com
        // rot sempre normalizado em [0,360), uma diferença entre
        // keyframes perto de 180° podia fazer o "caminho mais curto"
        // discordar da direção REAL do giro (bug observado: rotação
        // completando a volta no sentido errado por um trecho).
        if (!linearRegressionFits(track, segStart, segEnd, TOLERANCE_ROT_DEGREES, Field.ROT)) return false;
        return true;
    }

    // Campos numéricos de TickState que podem ser testados por regressão
    // linear (todos exceto rot, que precisa de tratamento circular).
    private enum Field { MID_X, MID_Y, MID_W, MID_H, CROP_X, CROP_Y, CROP_W, CROP_H, ALPHA, ROT }

    private static double fieldValue(TickState ts, Field f) {
        switch (f) {
            case MID_X: return ts.midX;
            case MID_Y: return ts.midY;
            case MID_W: return ts.midW;
            case MID_H: return ts.midH;
            case CROP_X: return ts.cropX;
            case CROP_Y: return ts.cropY;
            case CROP_W: return ts.cropW;
            case CROP_H: return ts.cropH;
            case ALPHA: return ts.alpha;
            case ROT: return ts.rot;
            default: return 0;
        }
    }

    // Calcula a regressão linear (mínimos quadrados) de um campo ao
    // longo de [segStart, segEnd] e verifica se TODOS os pontos reais
    // ficam dentro de "tolerance" do valor previsto pela reta de melhor
    // ajuste. Isto substitui a checagem ponto-a-ponto contra uma reta
    // ancorada nos extremos.
    private static boolean linearRegressionFits(TickState[] track, int segStart, int segEnd,
            double tolerance, Field field) {
        int n = segEnd - segStart + 1;
        double meanT = (n - 1) / 2.0; // t normalizado: 0, 1, 2, ..., n-1
        double sumTY = 0, sumT2 = 0, meanY = 0;

        for (int i = 0; i < n; i++) {
            double y = fieldValue(track[segStart + i], field);
            meanY += y;
        }
        meanY /= n;

        for (int i = 0; i < n; i++) {
            double t = i - meanT;
            double y = fieldValue(track[segStart + i], field) - meanY;
            sumTY += t * y;
            sumT2 += t * t;
        }

        double slope = (sumT2 == 0) ? 0 : sumTY / sumT2;
        double intercept = meanY - slope * meanT;

        for (int i = 0; i < n; i++) {
            double predicted = slope * i + intercept;
            double real = fieldValue(track[segStart + i], field);
            if (Math.abs(real - predicted) > tolerance) {
                return false;
            }
        }
        return true;
    }

    // Menor distância angular entre dois ângulos NORMALIZADOS (para
    // decidir flip em extractQuadGeometry, onde os candidatos testados
    // são sempre [0,360)) — sempre >= 0 e <= 180.
    private static double angleDiff(double a, double b) {
        double diff = Math.abs(a - b) % 360;
        return diff > 180 ? 360 - diff : diff;
    }

    // Arredonda um ângulo (graus) para inteiro — SEM normalizar para
    // [0,360). $ROT agora é escrito no texto fonte já DESDOBRADO
    // (unwrapped): pode ser negativo ou maior que 360, e isso é
    // INTENCIONAL — preserva a direção real e a quantidade de voltas do
    // giro original, que se perderia se normalizássemos de volta para
    // [0,360) aqui (ver comentário completo no laço principal de
    // extração, campo TickState.rot, sobre a ambiguidade de direção que
    // a normalização causava).
    private static long roundRot(double rotDegrees) {
        return Math.round(rotDegrees);
    }

    // ================= LEITURA DE DIMENSÕES (mesma lógica de SprBuilder) =================

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

    // Lê width/height de um .webp usando BitmapFactory nativo do Android
    // (só o cabeçalho, via inJustDecodeBounds) — ver nota completa na
    // versão gêmea em SprBuilder.readWebpDims: aqui é o game compiler
    // (WebpAnimNative estaria disponível), mas BitmapFactory é preferido
    // por simplicidade/consistência com o app editor, já que só
    // precisamos das dimensões, não da decodificação da animação.
    private static int[] readWebpDims(File f) {
        android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null;
        return new int[]{opts.outWidth, opts.outHeight};
    }

    private static byte[] readFullFileBytes(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        try (FileInputStream in = new FileInputStream(f)) {
            in.read(b);
        }
        return b;
    }

    // ================= Helpers =================

    private static short readShortLE(byte[] a, int p) {
        return (short) ((a[p] & 0xFF) | ((a[p + 1] & 0xFF) << 8));
    }

    private static int readIntLE(byte[] a, int p) {
        return (a[p] & 0xFF)
                | ((a[p + 1] & 0xFF) << 8)
                | ((a[p + 2] & 0xFF) << 16)
                | ((a[p + 3] & 0xFF) << 24);
    }

    private static int readIntBE(byte[] a, int p) {
        return ((a[p] & 0xFF) << 24)
                | ((a[p + 1] & 0xFF) << 16)
                | ((a[p + 2] & 0xFF) << 8)
                | (a[p + 3] & 0xFF);
    }

    private static byte[] slice(byte[] src, int pos, int len) {
        byte[] out = new byte[len];
        System.arraycopy(src, pos, out, 0, len);
        return out;
    }

    private static void saveBytes(byte[] src, int pos, int size, File out) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out))) {
            bos.write(src, pos, size);
        }
    }

    private static String idx3(int i) {
        return (i < 10 ? "00" : i < 100 ? "0" : "") + i;
    }

    static final class SprWriter {
        private final BufferedWriter out;

        SprWriter(Writer w) {
            this.out = (w instanceof BufferedWriter)
                    ? (BufferedWriter) w
                    : new BufferedWriter(w);
        }

        void writeLine(String s) throws IOException {
            out.write(s);
            out.newLine();
        }

        void close() throws IOException {
            out.close();
        }
    }
}
