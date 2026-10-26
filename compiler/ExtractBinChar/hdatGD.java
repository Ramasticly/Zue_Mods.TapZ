package com.zue.gd.util;

import java.io.*;
import java.util.regex.*;

// Dados de gamedata.pac. Antes desta consolidação, cada HDAT vivia numa classe própria
// (hdatgd3.java, hdatgd43.java, ..., hdatgd167.java, hdatDanoGD.java, hdatCartas.java,
// hdatgd51_134.java) — todas com a mesma lógica de extração (regex + parseInt/readShort)
// duplicada N vezes. Agora todos os HDATs de gamedata vivem aqui, com os utilitários de
// extração compartilhados de fato.
//
// Convenção de nomes:
//   exHDAT{N}  — extração: bytes → texto (era "writehdat")
//   bdHDAT{N}  — build:     texto → bytes (era "buildhdat")
//
// Integração:
//   BinExtractor: case 0 (pasta gamedata) → hdatGD.processExtraction(...)
//   BinBuilder:   hdatGD.processBuild(...) — despachado por tag no texto do bloco.
public final class hdatGD {

    private hdatGD() {}

    /* ══════════════════════════════════════════════════════════════════
       FACHADA — pontos de entrada únicos chamados por BinExtractor/BinBuilder
    ══════════════════════════════════════════════════════════════════ */

    /**
     * Extração (bytes → texto).
     * @return true se escreveu algo; false se este HDAT não é conhecido por esta
     *         classe — quem chamou deve aplicar o fallback (texto genérico ou RAW).
     */
    public static boolean processExtraction(BufferedWriter w, byte[] data, int hdat, int xsize, int ysize) throws IOException {
        switch (hdat) {
            case 3:   exHDAT3(w, data, xsize, ysize);   return true;
            case 43:  exHDAT43(w, data, xsize, ysize);  return true;
            case 50:  exHDAT50(w, data, xsize, ysize);  return true;
            case 136: exHDAT136(w, data, xsize, ysize); return true;
            case 145: exHDAT145(w, data, xsize, ysize); return true;
            case 146: exHDAT146(w, data, xsize, ysize); return true;
            case 147: exHDAT147(w, data, xsize, ysize); return true;
            case 148: exHDAT148(w, data, xsize, ysize); return true;
            case 149: exHDAT149(w, data, xsize, ysize); return true;
            case 150: exHDAT150(w, data, xsize, ysize); return true;
            case 153: exHDAT153(w, data, xsize, ysize); return true;
            case 155: exHDAT155(w, data, xsize, ysize); return true;
            case 156: exHDAT156(w, data, xsize, ysize); return true;
            case 157: exHDAT157(w, data, xsize, ysize); return true;
            case 158: exHDAT158(w, data, xsize, ysize); return true;
            case 160: exHDAT160(w, data, xsize, ysize); return true;
            case 161: exHDAT161(w, data, xsize, ysize); return true;
            case 162: exHDAT162(w, data, xsize, ysize); return true;
            case 163: exHDAT163(w, data, xsize, ysize); return true;
            case 164: exHDAT164(w, data, xsize, ysize); return true;
            case 165: exHDAT165(w, data, xsize, ysize); return true;
            case 166: exHDAT166(w, data, xsize, ysize); return true;
            case 167: exHDAT167(w, data, xsize, ysize); return true;
            default:
                if (hdat >= 10 && hdat <= 39)   { exHDATDanoGD(w, data, xsize, ysize, hdat - 10); return true; }
                if (hdat >= 51 && hdat <= 134)  { exHDAT51_134(w, data, xsize, ysize, hdat - 51); return true; }
                if (hdat >= 170 && hdat <= 250) { exHDATCartas(w, data, xsize, ysize, hdat - 170); return true; }
                return false;
        }
    }

    /**
     * Build (texto → bytes). Despacha pela tag presente em {@code block}.
     * @param reporter opcional (pode ser null) — coleta erros de validação sem
     *                 nunca abortar a geração do binário.
     * @return os bytes construídos, ou null se nenhuma tag conhecida desta classe
     *         foi encontrada no bloco.
     */
    public static byte[] processBuild(String block, int xsize, int ysize, ErrorReporter reporter) throws IOException {
        if (block.contains("$IA_COMPORTAMENTO"))        return bdHDAT3(block, reporter);
        if (block.contains("$EXP_NEXT_LEVEL"))          return bdHDAT43(block, reporter);
        if (block.contains("$FORMACAO"))                return bdHDAT50(block, reporter);
        if (block.contains("$LOJA_CARTAS"))             return bdHDAT136(block, xsize, reporter);
        if (block.contains("$PERSONAGENS_EXCLUIDOS_CPU")) return bdHDAT145(block, reporter);
        if (block.contains("$RESULTADO_DISPUTA_KI"))    return bdHDAT146(block, reporter);
        if (block.contains("$DIFICULDADE_DISPUTA"))     return bdHDAT147(block, reporter);
        if (block.contains("$PADROES_BONUS_PUSH"))      return bdHDAT148(block, reporter);
        if (block.contains("$TIERS_PODER_DECK"))        return bdHDAT149(block, reporter);
        if (block.contains("$SCORE_EVENTOS_CUTSCENE"))  return bdHDAT150(block, reporter);
        if (block.contains("$ESFERAS_DRAGAO"))          return bdHDAT153(block, reporter);
        if (block.contains("$BONUS_AFINIDADE"))         return bdHDAT155(block, reporter);
        if (block.contains("$SEQUENCIA_PUSH"))          return bdHDAT156(block, reporter);
        if (block.contains("$DECKS_CPU"))               return bdHDAT157(block, reporter);
        if (block.contains("$DEF_CHARS"))               return bdHDAT158(block, ysize, reporter);
        if (block.contains("$CARTAS_INICIAIS"))         return bdHDAT160(block, reporter);
        if (block.contains("$SOUND"))                   return bdHDAT161(block, reporter);
        if (block.contains("$TICKETS_INICIAIS"))        return bdHDAT162(block, reporter);
        if (block.contains("$INIT_CHAR_VALUES"))        return bdHDAT163(block, reporter);
        if (block.contains("$ESFERAS_INICIAIS"))        return bdHDAT164(block, reporter);
        if (block.contains("$TICKETS_POR_PARTIDA"))     return bdHDAT165(block, reporter);
        if (block.contains("$FLAGS_VIDA_ESPECIAL"))     return bdHDAT166(block, reporter);
        if (block.contains("$MODO_DEBUG"))              return bdHDAT167(block, reporter);
        if (block.contains("$REACAO_DANO_GD_"))         return bdHDATDanoGD(block, reporter);
        if (block.contains("$EFEITO_FORMACAO_"))        return bdHDAT51_134(block, reporter);
        if (block.contains("$CARTA_"))                  return bdHDATCartas(block, reporter);
        return null;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 3 — $IA_COMPORTAMENTO
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT3_COLS = 8;
    private static final String[] HDAT3_NOMES = new String[20];
    static {
        HDAT3_NOMES[1]  = "L01_CANCEL_GUARD_CONTRA_ATAQUE";
        HDAT3_NOMES[2]  = "L02_CANCEL_COMBO";
        HDAT3_NOMES[3]  = "L03_GUARD";
        HDAT3_NOMES[4]  = "L04_BONUS_TEMPO_VITORIA";
        HDAT3_NOMES[18] = "L18_INICIAR_PUSH";
        HDAT3_NOMES[19] = "L19_CHANCE_RUSH";
    }

    private static void exHDAT3(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $IA_COMPORTAMENTO:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            String nome = HDAT3_NOMES[linha] != null ? HDAT3_NOMES[linha] : ("L" + String.format("%02d", linha) + "_DESCONHECIDO");
            w.write("         $" + nome + ": [");
            for (int col = 0; col < HDAT3_COLS; col++) {
                int idx = linha * HDAT3_COLS + col;
                int val = (idx < data.length) ? (data[idx] & 0xFF) : 0;
                w.write(val + (col < HDAT3_COLS - 1 ? ", " : ""));
            }
            w.write("], // por nível de CPU 0-7\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT3(String block, ErrorReporter reporter) {
        byte[] data = new byte[20 * HDAT3_COLS];
        for (int linha = 0; linha < 20; linha++) {
            String nome = HDAT3_NOMES[linha] != null ? HDAT3_NOMES[linha] : ("L" + String.format("%02d", linha) + "_DESCONHECIDO");
            fillByteRow(block, "$" + nome, data, linha * HDAT3_COLS, HDAT3_COLS, 3, reporter);
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 43 — $EXP_NEXT_LEVEL
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT43(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $EXP_NEXT_LEVEL:{\n");
        for (int i = 0; i < ysize && i < data.length; i++)
            w.write("         $LVL_" + (i * 5) + ": " + (data[i] & 0xFF) + ",\n");
        w.write("      },\n");
    }

    private static byte[] bdHDAT43(String block, ErrorReporter reporter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Matcher m = Pattern.compile("\\$LVL_(\\d+):\\s*(-?[\\d.]+)").matcher(block);
        while (m.find()) {
            Integer v = checkedRawGroup(m.group(2), "$LVL_" + m.group(1), 43, 0, 255, reporter, m.group());
            baos.write((v != null ? v : 0) & 0xFF);
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 50 — $FORMACAO
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT50_COLS = 15;

    private static void exHDAT50(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $FORMACAO:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            int base = linha * HDAT50_COLS;
            if (base + HDAT50_COLS > data.length) break;

            int cardId = data[base] & 0xFF;
            int tipoRequisito = data[base + 1] & 0xFF;
            int reqAfin0 = data[base + 2] & 0xFF;
            int reqAfin1 = data[base + 3] & 0xFF;
            int reqAfin2 = data[base + 4] & 0xFF;

            w.write("         $LINHA_" + linha + ": {$CARD_ID: " + cardId
                    + ", $TIPO_REQUISITO: " + tipoRequisito
                    + ", $REQ_AFINIDADE: [" + reqAfin0 + ", " + reqAfin1 + ", " + reqAfin2 + "]"
                    + ", $CARTAS_EXIGIDAS: [");
            for (int i = 0; i < 8; i++) {
                w.write(String.valueOf(data[base + 5 + i] & 0xFF));
                if (i < 7) w.write(", ");
            }
            w.write("], $DESCONHECIDO: [" + (data[base + 13] & 0xFF) + ", " + (data[base + 14] & 0xFF) + "]},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT50(String block, ErrorReporter reporter) {
        Pattern pLinha = Pattern.compile(
            "\\$LINHA_(\\d+):\\s*\\{\\$CARD_ID:\\s*(-?[\\d.]+)" +
            ",\\s*\\$TIPO_REQUISITO:\\s*(-?[\\d.]+)" +
            ",\\s*\\$REQ_AFINIDADE:\\s*\\[(-?[\\d.]+),\\s*(-?[\\d.]+),\\s*(-?[\\d.]+)\\]" +
            ",\\s*\\$CARTAS_EXIGIDAS:\\s*\\[([^\\]]*)\\]" +
            ",\\s*\\$DESCONHECIDO:\\s*\\[(-?[\\d.]+),\\s*(-?[\\d.]+)\\]\\}");

        int maxLinha = -1;
        Matcher mCount = pLinha.matcher(block);
        while (mCount.find()) {
            int idx = Integer.parseInt(mCount.group(1));
            if (idx > maxLinha) maxLinha = idx;
        }
        if (maxLinha < 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$LINHA_N").hdatIndex(50)
                    .message("$FORMACAO não contém nenhum $LINHA_N reconhecível"));
            return new byte[0];
        }

        byte[] data = new byte[(maxLinha + 1) * HDAT50_COLS];

        Matcher m = pLinha.matcher(block);
        while (m.find()) {
            int linha = Integer.parseInt(m.group(1));
            int base = linha * HDAT50_COLS;
            String label = "$LINHA_" + linha;

            data[base]     = byteOf(checkedRawGroup(m.group(2), label + "/$CARD_ID",        50, 0, 255, reporter, m.group()));
            data[base + 1] = byteOf(checkedRawGroup(m.group(3), label + "/$TIPO_REQUISITO",  50, 0, 255, reporter, m.group()));
            data[base + 2] = byteOf(checkedRawGroup(m.group(4), label + "/$REQ_AFINIDADE[0]", 50, 0, 255, reporter, m.group()));
            data[base + 3] = byteOf(checkedRawGroup(m.group(5), label + "/$REQ_AFINIDADE[1]", 50, 0, 255, reporter, m.group()));
            data[base + 4] = byteOf(checkedRawGroup(m.group(6), label + "/$REQ_AFINIDADE[2]", 50, 0, 255, reporter, m.group()));

            String[] cartas = m.group(7).split(",");
            for (int i = 0; i < 8; i++) {
                if (i < cartas.length) {
                    Integer v = checkedRawGroup(cartas[i].trim(), label + "/$CARTAS_EXIGIDAS[" + i + "]", 50, 0, 255, reporter, m.group());
                    data[base + 5 + i] = byteOf(v);
                }
            }

            data[base + 13] = byteOf(checkedRawGroup(m.group(8), label + "/$DESCONHECIDO[0]", 50, 0, 255, reporter, m.group()));
            data[base + 14] = byteOf(checkedRawGroup(m.group(9), label + "/$DESCONHECIDO[1]", 50, 0, 255, reporter, m.group()));
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 136 — $LOJA_CARTAS
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT136(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $LOJA_CARTAS:{\n");
        for (int cardId = 0; cardId < ysize; cardId++) {
            int base = cardId * xsize;
            if (base + 3 > data.length) break;

            boolean visivelA = (data[base] & 0xFF) != 0;
            boolean visivelB = (data[base + 1] & 0xFF) != 0;
            int preco = data[base + 2] & 0xFF;

            w.write("         $BUY_CARTA_" + cardId + ": {$VISIVEL_MODO_A: " + (visivelA ? "YES" : "NO")
                    + ", $VISIVEL_MODO_B: " + (visivelB ? "YES" : "NO")
                    + ", $PRECO_TICKETS: " + preco + "},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT136(String block, int xsize, ErrorReporter reporter) {
        Pattern p = Pattern.compile(
            "\\$BUY_CARTA_(\\d+):\\s*\\{\\$VISIVEL_MODO_A:\\s*(\\w+),\\s*\\$VISIVEL_MODO_B:\\s*(\\w+),\\s*\\$PRECO_TICKETS:\\s*(-?[\\d.]+)\\}");
        Matcher mCount = p.matcher(block);

        int maxId = -1;
        while (mCount.find()) {
            int idx = Integer.parseInt(mCount.group(1));
            if (idx > maxId) maxId = idx;
        }
        if (maxId < 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$BUY_CARTA_N").hdatIndex(136)
                    .message("$LOJA_CARTAS não contém nenhum $BUY_CARTA_N reconhecível"));
            return new byte[0];
        }

        byte[] data = new byte[(maxId + 1) * xsize];

        Matcher m = p.matcher(block);
        while (m.find()) {
            int cardId = Integer.parseInt(m.group(1));
            int base = cardId * xsize;
            if (base + 3 > data.length) continue;
            String label = "$BUY_CARTA_" + cardId;

            data[base]     = (byte) (checkedYesNo(m.group(2), label + "/$VISIVEL_MODO_A", 136, reporter) ? 1 : 0);
            data[base + 1] = (byte) (checkedYesNo(m.group(3), label + "/$VISIVEL_MODO_B", 136, reporter) ? 1 : 0);
            Integer preco = checkedRawGroup(m.group(4), label + "/$PRECO_TICKETS", 136, 0, 255, reporter, m.group());
            data[base + 2] = byteOf(preco);
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 145 — $PERSONAGENS_EXCLUIDOS_CPU
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT145(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        int id1 = data.length > 0 ? (data[0] & 0xFF) : 0;
        int id2 = data.length > 1 ? (data[1] & 0xFF) : 0;
        w.write("      $PERSONAGENS_EXCLUIDOS_CPU: {$ID_1: " + id1 + ", $ID_2: " + id2 + "},\n");
    }

    private static byte[] bdHDAT145(String block, ErrorReporter reporter) {
        Matcher m = Pattern.compile(
            "\\$PERSONAGENS_EXCLUIDOS_CPU:\\s*\\{\\$ID_1:\\s*(-?[\\d.]+),\\s*\\$ID_2:\\s*(-?[\\d.]+)\\}").matcher(block);
        if (m.find()) {
            Integer id1 = checkedRawGroup(m.group(1), "$ID_1", 145, 0, 255, reporter, m.group());
            Integer id2 = checkedRawGroup(m.group(2), "$ID_2", 145, 0, 255, reporter, m.group());
            return new byte[]{ byteOf(id1), byteOf(id2) };
        }
        ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                .fieldTag("$PERSONAGENS_EXCLUIDOS_CPU").hdatIndex(145)
                .message("$PERSONAGENS_EXCLUIDOS_CPU presente, mas não foi possível interpretar $ID_1/$ID_2"));
        return new byte[]{0, 0};
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 146 — $RESULTADO_DISPUTA_KI
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT146_SHORTS_POR_ENTRADA = 10;

    private static void exHDAT146(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $RESULTADO_DISPUTA_KI:{\n");
        int totalShorts = data.length / 2;
        int entradas = totalShorts / HDAT146_SHORTS_POR_ENTRADA;

        for (int entrada = 0; entrada < entradas; entrada++) {
            int base = entrada * HDAT146_SHORTS_POR_ENTRADA * 2;

            int desconhecido = readShortAt(data, base, 0);
            int numIcones = readShortAt(data, base, 1);

            w.write("         $ENTRADA_" + entrada + ": {$DESCONHECIDO: " + desconhecido
                    + ", $NUM_ICONES: " + numIcones + ", $POSICOES: [");
            for (int i = 0; i < 4; i++) {
                int x = readShortAt(data, base, 2 + i * 2);
                int y = readShortAt(data, base, 3 + i * 2);
                w.write("{$X: " + x + ", $Y: " + y + "}");
                if (i < 3) w.write(", ");
            }
            w.write("]},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT146(String block, ErrorReporter reporter) {
        Pattern pEntrada = Pattern.compile(
            "\\$ENTRADA_(\\d+):\\s*\\{\\$DESCONHECIDO:\\s*(-?[\\d.]+),\\s*\\$NUM_ICONES:\\s*(-?[\\d.]+),\\s*\\$POSICOES:\\s*\\[(.*?)\\]\\}",
            Pattern.DOTALL);
        Pattern pPos = Pattern.compile("\\{\\$X:\\s*(-?[\\d.]+),\\s*\\$Y:\\s*(-?[\\d.]+)\\}");

        int maxEntrada = -1;
        Matcher mCount = pEntrada.matcher(block);
        while (mCount.find()) {
            int idx = Integer.parseInt(mCount.group(1));
            if (idx > maxEntrada) maxEntrada = idx;
        }
        if (maxEntrada < 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$ENTRADA_N").hdatIndex(146)
                    .message("$RESULTADO_DISPUTA_KI não contém nenhum $ENTRADA_N reconhecível"));
            return new byte[0];
        }

        byte[] data = new byte[(maxEntrada + 1) * HDAT146_SHORTS_POR_ENTRADA * 2];

        Matcher m = pEntrada.matcher(block);
        while (m.find()) {
            int entrada = Integer.parseInt(m.group(1));
            String label = "$ENTRADA_" + entrada;
            Integer desconhecido = checkedRawGroup(m.group(2), label + "/$DESCONHECIDO", 146, 0, 65535, reporter, m.group());
            Integer numIcones = checkedRawGroup(m.group(3), label + "/$NUM_ICONES", 146, 0, 65535, reporter, m.group());
            String posBlock = m.group(4);

            int base = entrada * HDAT146_SHORTS_POR_ENTRADA * 2;
            writeShortAtIdx(data, base, 0, desconhecido);
            writeShortAtIdx(data, base, 1, numIcones);

            Matcher mP = pPos.matcher(posBlock);
            int i = 0;
            while (mP.find() && i < 4) {
                Integer x = checkedRawGroup(mP.group(1), label + "/$POSICOES[" + i + "]/$X", 146, 0, 65535, reporter, mP.group());
                Integer y = checkedRawGroup(mP.group(2), label + "/$POSICOES[" + i + "]/$Y", 146, 0, 65535, reporter, mP.group());
                writeShortAtIdx(data, base, 2 + i * 2, x);
                writeShortAtIdx(data, base, 3 + i * 2, y);
                i++;
            }
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 147 — $DIFICULDADE_DISPUTA
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT147_COLS = 10;
    private static final int HDAT147_ROWS = 7;
    private static final String[] HDAT147_NOMES = new String[HDAT147_ROWS];
    static {
        HDAT147_NOMES[0] = "L0_ESPERA_PROXIMO_BOTAO";
        HDAT147_NOMES[3] = "L3_INTERVALO_INPUT_CPU";
        HDAT147_NOMES[4] = "L4_FORCA_MIN_CPU";
        HDAT147_NOMES[5] = "L5_FORCA_MAX_CPU";
    }

    private static void exHDAT147(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $DIFICULDADE_DISPUTA:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            String nome = HDAT147_NOMES[linha] != null ? HDAT147_NOMES[linha] : ("L" + linha + "_DESCONHECIDO");
            int base = linha * HDAT147_COLS;
            w.write("         $" + nome + ": [");
            for (int col = 0; col < HDAT147_COLS; col++) {
                int val = (base + col < data.length) ? (data[base + col] & 0xFF) : 0;
                w.write(String.valueOf(val));
                if (col < HDAT147_COLS - 1) w.write(", ");
            }
            w.write("], // por nível de CPU 0-9\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT147(String block, ErrorReporter reporter) {
        byte[] data = new byte[HDAT147_ROWS * HDAT147_COLS];
        for (int linha = 0; linha < HDAT147_ROWS; linha++) {
            String nome = HDAT147_NOMES[linha] != null ? HDAT147_NOMES[linha] : ("L" + linha + "_DESCONHECIDO");
            fillByteRow(block, "$" + nome, data, linha * HDAT147_COLS, HDAT147_COLS, 147, reporter);
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 148 — $PADROES_BONUS_PUSH
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT148_COLS = 8;

    private static void exHDAT148(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $PADROES_BONUS_PUSH:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            int base = linha * HDAT148_COLS;
            if (base + HDAT148_COLS > data.length) break;

            w.write("         $PADRAO_" + linha + ": [");
            for (int col = 0; col < HDAT148_COLS; col++) {
                w.write(String.valueOf(data[base + col] & 0xFF));
                if (col < HDAT148_COLS - 1) w.write(", ");
            }
            w.write("], // bônus por nível de CPU 0-7\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT148(String block, ErrorReporter reporter) {
        Pattern pLinha = Pattern.compile("\\$PADRAO_(\\d+):\\s*\\[([^\\]]*)\\]");
        Matcher mCount = pLinha.matcher(block);

        int maxLinha = -1;
        while (mCount.find()) {
            int idx = Integer.parseInt(mCount.group(1));
            if (idx > maxLinha) maxLinha = idx;
        }
        if (maxLinha < 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$PADRAO_N").hdatIndex(148)
                    .message("$PADROES_BONUS_PUSH não contém nenhum $PADRAO_N reconhecível"));
            return new byte[0];
        }

        byte[] data = new byte[(maxLinha + 1) * HDAT148_COLS];

        Matcher m = pLinha.matcher(block);
        while (m.find()) {
            int linha = Integer.parseInt(m.group(1));
            fillByteRowFromCsv(m.group(2), "$PADRAO_" + linha, data, linha * HDAT148_COLS, HDAT148_COLS, 148, reporter);
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 149 — $TIERS_PODER_DECK
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT149(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $TIERS_PODER_DECK:{\n");
        for (int tier = 0; tier < ysize && tier < data.length; tier++) {
            int limiarBruto = data[tier] & 0xFF;
            w.write("         $TIER_" + tier + ": " + limiarBruto
                    + ", // limiar real de poder de ataque: " + (limiarBruto * 1000) + "\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT149(String block, ErrorReporter reporter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Matcher m = Pattern.compile("\\$TIER_(\\d+):\\s*(-?[\\d.]+)").matcher(block);
        while (m.find()) {
            Integer v = checkedRawGroup(m.group(2), "$TIER_" + m.group(1), 149, 0, 255, reporter, m.group());
            baos.write((v != null ? v : 0) & 0xFF);
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 150 — $SCORE_EVENTOS_CUTSCENE
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT150(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $SCORE_EVENTOS_CUTSCENE:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            int base = linha * 2;
            int val = (base + 1 < data.length) ? readShortAt(data, base, 0) : 0;
            w.write("         $LINHA_" + linha + ": " + val + ",\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT150(String block, ErrorReporter reporter) {
        Pattern pLinha = Pattern.compile("\\$LINHA_(\\d+):\\s*(-?[\\d.]+)");
        Matcher mCount = pLinha.matcher(block);

        int maxLinha = -1;
        while (mCount.find()) {
            int idx = Integer.parseInt(mCount.group(1));
            if (idx > maxLinha) maxLinha = idx;
        }
        if (maxLinha < 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$LINHA_N").hdatIndex(150)
                    .message("$SCORE_EVENTOS_CUTSCENE não contém nenhum $LINHA_N reconhecível"));
            return new byte[0];
        }

        byte[] data = new byte[(maxLinha + 1) * 2];

        Matcher m = pLinha.matcher(block);
        while (m.find()) {
            int linha = Integer.parseInt(m.group(1));
            Integer val = checkedRawGroup(m.group(2), "$LINHA_" + linha, 150, 0, 65535, reporter, m.group());
            writeShortAtIdx(data, linha * 2, 0, val);
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 153 — $ESFERAS_DRAGAO
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT153_COLS = 11;
    private static final int HDAT153_ESFERAS_POR_ROUND = 7;

    private static void exHDAT153(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $ESFERAS_DRAGAO:{\n");
        int rounds = ysize / HDAT153_ESFERAS_POR_ROUND;

        for (int round = 0; round < rounds; round++) {
            w.write("         $ROUND_" + round + ":{\n");
            for (int esfera = 0; esfera < HDAT153_ESFERAS_POR_ROUND; esfera++) {
                int linha = round * HDAT153_ESFERAS_POR_ROUND + esfera;
                int base = linha * HDAT153_COLS * 2;
                if (base + HDAT153_COLS * 2 > data.length) break;

                w.write("            $ESFERA_" + esfera + ": {"
                        + "$PLAYER_TYPE: " + readShortAt(data, base, 0)
                        + ", $DIFICULDADE: " + readShortAt(data, base, 1)
                        + ", $SCORE_MIN: " + readShortAt(data, base, 2)
                        + ", $COMBO_MIN: " + readShortAt(data, base, 3)
                        + ", $SEM_DERROTA: " + readShortAt(data, base, 4)
                        + ", $TEMPO_LIMITE: " + readShortAt(data, base, 5)
                        + ", $DESCONHECIDO_6: " + readShortAt(data, base, 6)
                        + ", $DESCONHECIDO_7: " + readShortAt(data, base, 7)
                        + ", $MODO_FLAG: " + readShortAt(data, base, 8)
                        + ", $DESCONHECIDO_9: " + readShortAt(data, base, 9)
                        + ", $NUM_ESFERA: " + readShortAt(data, base, 10)
                        + "},\n");
            }
            w.write("         },\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT153(String block, ErrorReporter reporter) {
        Pattern pRound = Pattern.compile("\\$ROUND_(\\d+):\\{(.*?)\\n\\s*\\},", Pattern.DOTALL);
        Pattern pEsfera = Pattern.compile(
            "\\$ESFERA_(\\d+):\\s*\\{" +
            "\\$PLAYER_TYPE:\\s*(-?[\\d.]+)" +
            ",\\s*\\$DIFICULDADE:\\s*(-?[\\d.]+)" +
            ",\\s*\\$SCORE_MIN:\\s*(-?[\\d.]+)" +
            ",\\s*\\$COMBO_MIN:\\s*(-?[\\d.]+)" +
            ",\\s*\\$SEM_DERROTA:\\s*(-?[\\d.]+)" +
            ",\\s*\\$TEMPO_LIMITE:\\s*(-?[\\d.]+)" +
            ",\\s*\\$DESCONHECIDO_6:\\s*(-?[\\d.]+)" +
            ",\\s*\\$DESCONHECIDO_7:\\s*(-?[\\d.]+)" +
            ",\\s*\\$MODO_FLAG:\\s*(-?[\\d.]+)" +
            ",\\s*\\$DESCONHECIDO_9:\\s*(-?[\\d.]+)" +
            ",\\s*\\$NUM_ESFERA:\\s*(-?[\\d.]+)\\}");

        int maxRound = -1;
        Matcher mR = pRound.matcher(block);
        while (mR.find()) {
            int r = Integer.parseInt(mR.group(1));
            if (r > maxRound) maxRound = r;
        }
        if (maxRound < 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$ROUND_N").hdatIndex(153)
                    .message("$ESFERAS_DRAGAO não contém nenhum $ROUND_N reconhecível"));
            return new byte[0];
        }

        int totalLinhas = (maxRound + 1) * HDAT153_ESFERAS_POR_ROUND;
        byte[] data = new byte[totalLinhas * HDAT153_COLS * 2];

        mR = pRound.matcher(block);
        while (mR.find()) {
            int round = Integer.parseInt(mR.group(1));
            String roundBlock = mR.group(2);

            Matcher mE = pEsfera.matcher(roundBlock);
            while (mE.find()) {
                int esfera = Integer.parseInt(mE.group(1));
                int linha = round * HDAT153_ESFERAS_POR_ROUND + esfera;
                int base = linha * HDAT153_COLS * 2;
                String label = "$ROUND_" + round + "/$ESFERA_" + esfera;

                String[] names = { "$PLAYER_TYPE", "$DIFICULDADE", "$SCORE_MIN", "$COMBO_MIN", "$SEM_DERROTA",
                                    "$TEMPO_LIMITE", "$DESCONHECIDO_6", "$DESCONHECIDO_7", "$MODO_FLAG", "$DESCONHECIDO_9", "$NUM_ESFERA" };
                for (int i = 0; i < 11; i++) {
                    Integer v = checkedRawGroup(mE.group(i + 2), label + "/" + names[i], 153, 0, 65535, reporter, mE.group());
                    writeShortAtIdx(data, base, i, v);
                }
            }
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 155 — $BONUS_AFINIDADE
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT155(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        int valor = readShortLoose(data);
        w.write("      $BONUS_AFINIDADE: " + valor + ",\n");
    }

    private static byte[] bdHDAT155(String block, ErrorReporter reporter) {
        Integer valor = extractOptionalChecked(block, "\\$BONUS_AFINIDADE:\\s*(-?[\\d.]+)", "$BONUS_AFINIDADE", 155, -32768, 65535, reporter);
        int v = (valor != null) ? valor : 0;
        return new byte[]{(byte) (v & 0xFF), (byte) ((v >> 8) & 0xFF)};
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 156 — $SEQUENCIA_PUSH
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT156_COLS = 29;
    private static final int HDAT156_BOTOES = 7;

    private static void exHDAT156(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $SEQUENCIA_PUSH:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            int base = linha * HDAT156_COLS * 2;
            if (base + HDAT156_COLS * 2 > data.length) break;

            int numBotoes = readShortAt(data, base, 0);
            w.write("         $LINHA_" + linha + ": {$NUM_BOTOES: " + numBotoes + ", $BOTOES: [\n");
            for (int i = 0; i < HDAT156_BOTOES; i++) {
                int idx = 1 + i * 4;
                w.write("            {$POS_X: " + readShortAt(data, base, idx)
                        + ", $POS_Y: " + readShortAt(data, base, idx + 1)
                        + ", $DELAY: " + readShortAt(data, base, idx + 2)
                        + ", $JANELA: " + readShortAt(data, base, idx + 3) + "},\n");
            }
            w.write("         ]},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT156(String block, ErrorReporter reporter) {
        Pattern pLinha = Pattern.compile(
            "\\$LINHA_(\\d+):\\s*\\{\\$NUM_BOTOES:\\s*(-?[\\d.]+),\\s*\\$BOTOES:\\s*\\[(.*?)\\]\\}",
            Pattern.DOTALL);
        Pattern pBotao = Pattern.compile(
            "\\{\\$POS_X:\\s*(-?[\\d.]+),\\s*\\$POS_Y:\\s*(-?[\\d.]+),\\s*\\$DELAY:\\s*(-?[\\d.]+),\\s*\\$JANELA:\\s*(-?[\\d.]+)\\}");

        int maxLinha = -1;
        Matcher mCount = pLinha.matcher(block);
        while (mCount.find()) {
            int idx = Integer.parseInt(mCount.group(1));
            if (idx > maxLinha) maxLinha = idx;
        }
        if (maxLinha < 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$LINHA_N").hdatIndex(156)
                    .message("$SEQUENCIA_PUSH não contém nenhum $LINHA_N reconhecível"));
            return new byte[0];
        }

        byte[] data = new byte[(maxLinha + 1) * HDAT156_COLS * 2];

        Matcher mL = pLinha.matcher(block);
        while (mL.find()) {
            int linha = Integer.parseInt(mL.group(1));
            String label = "$LINHA_" + linha;
            Integer numBotoes = checkedRawGroup(mL.group(2), label + "/$NUM_BOTOES", 156, 0, 65535, reporter, mL.group());
            String botoesBlock = mL.group(3);
            int base = linha * HDAT156_COLS * 2;

            writeShortAtIdx(data, base, 0, numBotoes);

            Matcher mB = pBotao.matcher(botoesBlock);
            int i = 0;
            while (mB.find() && i < HDAT156_BOTOES) {
                int idx = 1 + i * 4;
                String botaoLabel = label + "/$BOTOES[" + i + "]";
                writeShortAtIdx(data, base, idx,     checkedRawGroup(mB.group(1), botaoLabel + "/$POS_X", 156, 0, 65535, reporter, mB.group()));
                writeShortAtIdx(data, base, idx + 1, checkedRawGroup(mB.group(2), botaoLabel + "/$POS_Y", 156, 0, 65535, reporter, mB.group()));
                writeShortAtIdx(data, base, idx + 2, checkedRawGroup(mB.group(3), botaoLabel + "/$DELAY", 156, 0, 65535, reporter, mB.group()));
                writeShortAtIdx(data, base, idx + 3, checkedRawGroup(mB.group(4), botaoLabel + "/$JANELA", 156, 0, 65535, reporter, mB.group()));
                i++;
            }
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 157 — $DECKS_CPU
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT157_CARTAS_POR_DECK = 8;

    private static void exHDAT157(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $DECKS_CPU:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            int base = linha * (HDAT157_CARTAS_POR_DECK * 2) * 2;
            if (base + HDAT157_CARTAS_POR_DECK * 4 > data.length) break;

            w.write("         $DECK_" + linha + ": {$CARTAS: [");
            for (int i = 0; i < HDAT157_CARTAS_POR_DECK; i++) {
                w.write(String.valueOf(readShortAt(data, base, i)));
                if (i < HDAT157_CARTAS_POR_DECK - 1) w.write(", ");
            }
            w.write("], $NIVEIS: [");
            for (int i = 0; i < HDAT157_CARTAS_POR_DECK; i++) {
                w.write(String.valueOf(readShortAt(data, base, HDAT157_CARTAS_POR_DECK + i)));
                if (i < HDAT157_CARTAS_POR_DECK - 1) w.write(", ");
            }
            w.write("]},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT157(String block, ErrorReporter reporter) {
        Pattern pLinha = Pattern.compile(
            "\\$DECK_(\\d+):\\s*\\{\\$CARTAS:\\s*\\[([^\\]]*)\\],\\s*\\$NIVEIS:\\s*\\[([^\\]]*)\\]\\}");
        Matcher mCount = pLinha.matcher(block);

        int maxLinha = -1;
        while (mCount.find()) {
            int idx = Integer.parseInt(mCount.group(1));
            if (idx > maxLinha) maxLinha = idx;
        }
        if (maxLinha < 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$DECK_N").hdatIndex(157)
                    .message("$DECKS_CPU não contém nenhum $DECK_N reconhecível"));
            return new byte[0];
        }

        byte[] data = new byte[(maxLinha + 1) * HDAT157_CARTAS_POR_DECK * 2 * 2];

        Matcher m = pLinha.matcher(block);
        while (m.find()) {
            int linha = Integer.parseInt(m.group(1));
            String label = "$DECK_" + linha;
            String[] cartas = m.group(2).split(",");
            String[] niveis = m.group(3).split(",");
            int base = linha * HDAT157_CARTAS_POR_DECK * 2 * 2;

            for (int i = 0; i < HDAT157_CARTAS_POR_DECK && i < cartas.length; i++) {
                Integer v = checkedRawGroup(cartas[i].trim(), label + "/$CARTAS[" + i + "]", 157, 0, 65535, reporter, m.group());
                writeShortAtIdx(data, base, i, v);
            }
            for (int i = 0; i < HDAT157_CARTAS_POR_DECK && i < niveis.length; i++) {
                Integer v = checkedRawGroup(niveis[i].trim(), label + "/$NIVEIS[" + i + "]", 157, 0, 65535, reporter, m.group());
                writeShortAtIdx(data, base, HDAT157_CARTAS_POR_DECK + i, v);
            }
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 158 — $DEF_CHARS
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT158(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $DEF_CHARS:{\n");
        int count = ysize - 1; // última linha é terminador, não emitida
        for (int i = 0; i < count; i++) {
            int base = i * 2;
            if (base + 1 >= data.length) break;
            w.write("         $CHAR:{$index: " + (data[base]     & 0xFF)
                         + ", $isDef: "         + (data[base + 1] & 0xFF) + "},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT158(String block, int ysize, ErrorReporter reporter) {
        // Confirmado por comparação byte a byte com gamedata.pac original: o binário
        // real NÃO reserva uma linha extra de terminador — tem exatamente
        // (nº de $CHAR no texto) * 2 bytes. exHDAT158 mostra ysize-1 entradas porque
        // ysize-1 É o número real de linhas úteis (a extração está certa em quantas
        // linhas exibir); o bug era presumir uma linha adicional fixa no BUILD, que
        // não existe no dado real do jogo.
        Pattern p = Pattern.compile("\\$CHAR:\\{\\$index:\\s*(-?[\\d.]+),\\s*\\$isDef:\\s*(-?[\\d.]+)\\}");
        Matcher m = p.matcher(block);

        int count = 0;
        Matcher mCount = p.matcher(block);
        while (mCount.find()) count++;

        byte[] data = new byte[count * 2];
        int i = 0;
        while (m.find()) {
            Integer idx = checkedRawGroup(m.group(1), "$CHAR/$index", 158, 0, 255, reporter, m.group());
            Integer isDef = checkedRawGroup(m.group(2), "$CHAR/$isDef", 158, 0, 255, reporter, m.group());
            int base = i * 2;
            data[base]     = byteOf(idx);
            data[base + 1] = byteOf(isDef);
            i++;
        }

        if (ysize > 0 && count != ysize - 1) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .severity(CompileError.Severity.WARNING)
                    .fieldTag("$CHAR").hdatIndex(158)
                    .message("$DEF_CHARS tem " + count + " entrada(s) $CHAR, mas $ROW do cabeçalho ("
                            + ysize + ") sugere " + (ysize - 1) + " — confira se o número de linhas está correto"));
        }

        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 160 — $CARTAS_INICIAIS
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT160(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $CARTAS_INICIAIS:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            int base = linha * 2;
            if (base + 2 > data.length) break;
            w.write("         $CARTA_" + linha + ": {$ID: " + (data[base] & 0xFF) + ", $NIVEL: " + (data[base + 1] & 0xFF) + "},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT160(String block, ErrorReporter reporter) {
        Pattern p = Pattern.compile("\\$CARTA_(\\d+):\\s*\\{\\$ID:\\s*(-?[\\d.]+),\\s*\\$NIVEL:\\s*(-?[\\d.]+)\\}");
        Matcher m = p.matcher(block);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (m.find()) {
            String label = "$CARTA_" + m.group(1);
            Integer id = checkedRawGroup(m.group(2), label + "/$ID", 160, 0, 255, reporter, m.group());
            Integer nivel = checkedRawGroup(m.group(3), label + "/$NIVEL", 160, 0, 255, reporter, m.group());
            baos.write((id != null ? id : 0) & 0xFF);
            baos.write((nivel != null ? nivel : 0) & 0xFF);
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 161 — $SOUND
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT161(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $SOUND:{\n");
        for (int i = 0; i < ysize; i++) {
            int base = i * 2;
            if (base + 1 >= data.length) break;
            int id   = (data[base] & 0xFF) - 1; // armazenado como id+1
            int loop = (data[base + 1] != 0) ? 1 : 0;
            w.write("         $SND_" + i + ": {$id: " + id + ", $loop: " + loop + "},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT161(String block, ErrorReporter reporter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Pattern p = Pattern.compile("\\$SND_(\\d+):\\s*\\{\\$id:\\s*(-?[\\d.]+),\\s*\\$loop:\\s*(-?[\\d.]+)\\}");
        Matcher m = p.matcher(block);
        while (m.find()) {
            String label = "$SND_" + m.group(1);
            Integer id = checkedRawGroup(m.group(2), label + "/$id", 161, -1, 254, reporter, m.group());
            Integer loop = checkedRawGroup(m.group(3), label + "/$loop", 161, 0, 1, reporter, m.group());
            int idVal = (id != null ? id : -1) + 1; // restaura id+1
            baos.write(idVal & 0xFF);
            baos.write((loop != null ? loop : 0) & 0xFF);
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 162 — $TICKETS_INICIAIS
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT162(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $TICKETS_INICIAIS: " + readShortLoose(data) + ",\n");
    }

    private static byte[] bdHDAT162(String block, ErrorReporter reporter) {
        Integer v = extractOptionalChecked(block, "\\$TICKETS_INICIAIS:\\s*(-?[\\d.]+)", "$TICKETS_INICIAIS", 162, -32768, 65535, reporter);
        int valor = (v != null) ? v : 0;
        return new byte[]{(byte) (valor & 0xFF), (byte) ((valor >> 8) & 0xFF)};
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 163 — $INIT_CHAR_VALUES
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT163_STRIDE = 10;

    private static void exHDAT163(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $INIT_CHAR_VALUES:{\n");
        for (int i = 0; i < ysize; i++) {
            int base = i * HDAT163_STRIDE;
            if (base + HDAT163_STRIDE > data.length) break;
            w.write("         $CHAR:{"
                + "$level: " + (data[base + 0] & 0xFF)
                + ", $exp: " + (data[base + 1] & 0xFF)
                + ", $card1: " + (data[base + 2] & 0xFF)
                + ", $card2: " + (data[base + 3] & 0xFF)
                + ", $card3: " + (data[base + 4] & 0xFF)
                + ", $card4: " + (data[base + 5] & 0xFF)
                + ", $card5: " + (data[base + 6] & 0xFF)
                + ", $card6: " + (data[base + 7] & 0xFF)
                + ", $card7: " + (data[base + 8] & 0xFF)
                + ", $card8: " + (data[base + 9] & 0xFF)
                + "},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT163(String block, ErrorReporter reporter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Pattern p = Pattern.compile(
            "\\$CHAR:\\{\\$level:\\s*(-?[\\d.]+),\\s*\\$exp:\\s*(-?[\\d.]+)" +
            ",\\s*\\$card1:\\s*(-?[\\d.]+),\\s*\\$card2:\\s*(-?[\\d.]+)" +
            ",\\s*\\$card3:\\s*(-?[\\d.]+),\\s*\\$card4:\\s*(-?[\\d.]+)" +
            ",\\s*\\$card5:\\s*(-?[\\d.]+),\\s*\\$card6:\\s*(-?[\\d.]+)" +
            ",\\s*\\$card7:\\s*(-?[\\d.]+),\\s*\\$card8:\\s*(-?[\\d.]+)");
        Matcher m = p.matcher(block);
        String[] names = { "$level", "$exp", "$card1", "$card2", "$card3", "$card4", "$card5", "$card6", "$card7", "$card8" };
        while (m.find()) {
            for (int g = 1; g <= 10; g++) {
                Integer v = checkedRawGroup(m.group(g), "$CHAR/" + names[g - 1], 163, 0, 255, reporter, m.group());
                baos.write((v != null ? v : 0) & 0xFF);
            }
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 164 — $ESFERAS_INICIAIS
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT164_COLS = 7;

    private static void exHDAT164(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $ESFERAS_INICIAIS:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            int base = linha * HDAT164_COLS;
            if (base + HDAT164_COLS > data.length) break;

            w.write("         $ROUND_" + linha + ": [");
            for (int col = 0; col < HDAT164_COLS; col++) {
                w.write(String.valueOf(data[base + col] & 0xFF));
                if (col < HDAT164_COLS - 1) w.write(", ");
            }
            w.write("], // esferas 0-6 concedidas de fábrica neste round\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT164(String block, ErrorReporter reporter) {
        Pattern pLinha = Pattern.compile("\\$ROUND_(\\d+):\\s*\\[([^\\]]*)\\]");
        Matcher mCount = pLinha.matcher(block);

        int maxLinha = -1;
        while (mCount.find()) {
            int idx = Integer.parseInt(mCount.group(1));
            if (idx > maxLinha) maxLinha = idx;
        }
        if (maxLinha < 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$ROUND_N").hdatIndex(164)
                    .message("$ESFERAS_INICIAIS não contém nenhum $ROUND_N reconhecível"));
            return new byte[0];
        }

        byte[] data = new byte[(maxLinha + 1) * HDAT164_COLS];

        Matcher m = pLinha.matcher(block);
        while (m.find()) {
            int linha = Integer.parseInt(m.group(1));
            fillByteRowFromCsv(m.group(2), "$ROUND_" + linha, data, linha * HDAT164_COLS, HDAT164_COLS, 164, reporter);
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 165 — $TICKETS_POR_PARTIDA
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT165(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $TICKETS_POR_PARTIDA:{\n");
        for (int linha = 0; linha < ysize; linha++) {
            int base = linha * 2;
            if (base + 2 > data.length) break;

            int derrota = data[base] & 0xFF;
            int vitoria = data[base + 1] & 0xFF;
            String nome = (linha == 0) ? "LIVRE" : ("MISSAO_" + (linha - 1));
            w.write("         $" + nome + ": {$DERROTA: " + derrota + ", $VITORIA: " + vitoria + "},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT165(String block, ErrorReporter reporter) {
        Pattern pLivre = Pattern.compile("\\$LIVRE:\\s*\\{\\$DERROTA:\\s*(-?[\\d.]+),\\s*\\$VITORIA:\\s*(-?[\\d.]+)\\}");
        Pattern pMissao = Pattern.compile("\\$MISSAO_(\\d+):\\s*\\{\\$DERROTA:\\s*(-?[\\d.]+),\\s*\\$VITORIA:\\s*(-?[\\d.]+)\\}");

        int maxMissao = -1;
        Matcher mCount = pMissao.matcher(block);
        while (mCount.find()) {
            int idx = Integer.parseInt(mCount.group(1));
            if (idx > maxMissao) maxMissao = idx;
        }

        byte[] data = new byte[(maxMissao + 2) * 2];

        Matcher mL = pLivre.matcher(block);
        if (mL.find()) {
            Integer derrota = checkedRawGroup(mL.group(1), "$LIVRE/$DERROTA", 165, 0, 255, reporter, mL.group());
            Integer vitoria = checkedRawGroup(mL.group(2), "$LIVRE/$VITORIA", 165, 0, 255, reporter, mL.group());
            data[0] = byteOf(derrota);
            data[1] = byteOf(vitoria);
        } else {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .severity(CompileError.Severity.WARNING)
                    .fieldTag("$LIVRE").hdatIndex(165)
                    .message("$TICKETS_POR_PARTIDA está sem o bloco $LIVRE — linha 0 ficará zerada"));
        }

        Matcher mM = pMissao.matcher(block);
        while (mM.find()) {
            int missao = Integer.parseInt(mM.group(1));
            int base = (missao + 1) * 2;
            String label = "$MISSAO_" + missao;
            data[base]     = byteOf(checkedRawGroup(mM.group(2), label + "/$DERROTA", 165, 0, 255, reporter, mM.group()));
            data[base + 1] = byteOf(checkedRawGroup(mM.group(3), label + "/$VITORIA", 165, 0, 255, reporter, mM.group()));
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 166 — $FLAGS_VIDA_ESPECIAL
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT166(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        int imunidadeJogador = data.length > 0 ? (data[0] & 0xFF) : 0;
        int oponente1HP = data.length > 1 ? (data[1] & 0xFF) : 0;

        w.write("      $FLAGS_VIDA_ESPECIAL:{\n");
        w.write("         $IMUNIDADE_JOGADOR: " + imunidadeJogador + ",\n");
        w.write("         $OPONENTE_1HP: " + oponente1HP + ",\n");
        w.write("      },\n");
    }

    private static byte[] bdHDAT166(String block, ErrorReporter reporter) {
        Integer imunidade = extractOptionalChecked(block, "\\$IMUNIDADE_JOGADOR:\\s*(-?[\\d.]+)", "$IMUNIDADE_JOGADOR", 166, 0, 255, reporter);
        Integer oponente1hp = extractOptionalChecked(block, "\\$OPONENTE_1HP:\\s*(-?[\\d.]+)", "$OPONENTE_1HP", 166, 0, 255, reporter);
        return new byte[]{ byteOf(imunidade), byteOf(oponente1hp) };
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 167 — $MODO_DEBUG
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT167(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        int valor = data.length > 0 ? (data[0] & 0xFF) : 0;
        w.write("      $MODO_DEBUG: " + valor + ",\n");
    }

    private static byte[] bdHDAT167(String block, ErrorReporter reporter) {
        Integer valor = extractOptionalChecked(block, "\\$MODO_DEBUG:\\s*(-?[\\d.]+)", "$MODO_DEBUG", 167, 0, 255, reporter);
        return new byte[]{ byteOf(valor) };
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 10-39 — $REACAO_DANO_GD_{N} (chão, MODO 876, gamedata.pac)
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDATDanoGD(BufferedWriter w, byte[] data, int xsize, int ysize, int damageType) throws IOException {
        w.write("      $REACAO_DANO_GD_" + damageType + ":{\n");

        if (damageType == 0) {
            w.write("      // $FR_N passo | $DUR_C/$DUR_A duração chão/ar | $ANO animação\n");
            w.write("      // $XSPEED/$YSPEED/$XACCEL/$YACCEL física no chão\n");
            w.write("      // $XSPEED_A/$YSPEED_A/$XACCEL_A/$YACCEL_A física no ar (ou posição relativa em agarre)\n");
            w.write("      // $COMP próxima reação ao tocar chão/parede (modo 876) | $WALL borda | $VOZ som\n\n");
        }

        int count = (ysize > 0) ? ysize : (data.length / DANO_STRIDE);
        for (int i = 0; i < count; i++) {
            int b = i * DANO_STRIDE;
            w.write("         $FR_" + i + ": {"
                + "$DUR_C: "      + readUByte(data, b +  0)
                + ", $DUR_A: "    + readUByte(data, b +  1)
                + ", $ANO: "      + readSByte(data, b +  2)
                + ", $XSPEED: "   + readSByte(data, b +  3)
                + ", $YSPEED: "   + readSByte(data, b +  4)
                + ", $XACCEL: "   + readSByte(data, b +  5)
                + ", $YACCEL: "   + readSByte(data, b +  6)
                + ", $XSPEED_A: " + readSByte(data, b +  7)
                + ", $YSPEED_A: " + readSByte(data, b +  8)
                + ", $XACCEL_A: " + readSByte(data, b +  9)
                + ", $YACCEL_A: " + readSByte(data, b + 10)
                + ", $COMP: "     + readUByte(data, b + 11)
                + ", $WALL: "     + readUByte(data, b + 12)
                + ", $VOZ: "      + readUByte(data, b + 13)
                + "},\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDATDanoGD(String block, ErrorReporter reporter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Pattern pFr = Pattern.compile("\\$FR_(\\d+):\\s*\\{([^}]+)\\}");
        Matcher mFr = pFr.matcher(block);
        while (mFr.find()) {
            String label = "$FR_" + mFr.group(1);
            String f = mFr.group(2);
            baos.write(checkedU(f, "\\$DUR_C:\\s*(-?[\\d.]+)",  label + "/$DUR_C",  reporter));
            baos.write(checkedU(f, "\\$DUR_A:\\s*(-?[\\d.]+)",  label + "/$DUR_A",  reporter));
            baos.write(checkedS(f, "\\$ANO:\\s*(-?[\\d.]+)",    label + "/$ANO",    reporter));
            baos.write(checkedS(f, "\\$XSPEED:\\s*(-?[\\d.]+)", label + "/$XSPEED", reporter));
            baos.write(checkedS(f, "\\$YSPEED:\\s*(-?[\\d.]+)", label + "/$YSPEED", reporter));
            baos.write(checkedS(f, "\\$XACCEL:\\s*(-?[\\d.]+)", label + "/$XACCEL", reporter));
            baos.write(checkedS(f, "\\$YACCEL:\\s*(-?[\\d.]+)", label + "/$YACCEL", reporter));
            baos.write(checkedS(f, "\\$XSPEED_A:\\s*(-?[\\d.]+)", label + "/$XSPEED_A", reporter));
            baos.write(checkedS(f, "\\$YSPEED_A:\\s*(-?[\\d.]+)", label + "/$YSPEED_A", reporter));
            baos.write(checkedS(f, "\\$XACCEL_A:\\s*(-?[\\d.]+)", label + "/$XACCEL_A", reporter));
            baos.write(checkedS(f, "\\$YACCEL_A:\\s*(-?[\\d.]+)", label + "/$YACCEL_A", reporter));
            baos.write(checkedU(f, "\\$COMP:\\s*(-?[\\d.]+)", label + "/$COMP", reporter));
            baos.write(checkedU(f, "\\$WALL:\\s*(-?[\\d.]+)", label + "/$WALL", reporter));
            baos.write(checkedU(f, "\\$VOZ:\\s*(-?[\\d.]+)",  label + "/$VOZ",  reporter));
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 51-134 — $EFEITO_FORMACAO_{N}
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDAT51_134_TAMANHO = 22;

    private static void exHDAT51_134(BufferedWriter w, byte[] data, int xsize, int ysize, int formacaoId) throws IOException {
        int ataque   = data.length > 3  ? (data[3]  & 0xFF) : 0;
        int defesa   = data.length > 6  ? (data[6]  & 0xFF) : 0;
        int bonusKi  = data.length > 7  ? (data[7]  & 0xFF) * 10 : 0;
        int barraKi  = data.length > 8  ? (data[8]  & 0xFF) : 0;
        boolean furia = data.length > 16 && (data[16] & 0xFF) != 0;

        w.write("      $EFEITO_FORMACAO_" + formacaoId + ": {\n");
        w.write("         $ATAQUE: " + ataque + ",\n");
        w.write("         $DEFESA: " + defesa + ",\n");
        w.write("         $BONUS_KI: " + bonusKi + ",\n");
        w.write("         $BONUS_BARRA_KI: " + barraKi + ",\n");
        w.write("         $FURIA_PERMANENTE: " + (furia ? "YES" : "NO") + ",\n");
        w.write("      },\n");
    }

    private static byte[] bdHDAT51_134(String block, ErrorReporter reporter) {
        byte[] data = new byte[HDAT51_134_TAMANHO];

        Integer ataque  = extractOptionalChecked(block, "\\$ATAQUE:\\s*(-?[\\d.]+)", "$ATAQUE", -1, 0, 255, reporter);
        Integer defesa  = extractOptionalChecked(block, "\\$DEFESA:\\s*(-?[\\d.]+)", "$DEFESA", -1, 0, 255, reporter);
        Integer bonusKi = extractOptionalChecked(block, "\\$BONUS_KI:\\s*(-?[\\d.]+)", "$BONUS_KI", -1, 0, 2550, reporter);
        Integer barraKi = extractOptionalChecked(block, "\\$BONUS_BARRA_KI:\\s*(-?[\\d.]+)", "$BONUS_BARRA_KI", -1, 0, 255, reporter);
        boolean furia = extractYesNo(block, "\\$FURIA_PERMANENTE:\\s*(\\w+)");

        data[3]  = byteOf(ataque);
        data[6]  = byteOf(defesa);
        data[7]  = (byte) (((bonusKi != null ? bonusKi : 0) / 10) & 0xFF); // desfaz a multiplicação por 10 da exibição
        data[8]  = byteOf(barraKi);
        data[16] = (byte) (furia ? 1 : 0);

        // Preserva campos brutos $CAMPO_NN adicionados manualmente ao texto — o que
        // não for encontrado fica 0, mesmo comportamento de sempre.
        Matcher m = Pattern.compile("\\$CAMPO_(\\d+):\\s*(-?[\\d.]+)").matcher(block);
        while (m.find()) {
            int pos = Integer.parseInt(m.group(1));
            if (pos >= 0 && pos < HDAT51_134_TAMANHO) {
                Integer v = checkedRawGroup(m.group(2), "$CAMPO_" + pos, -1, 0, 255, reporter, m.group());
                data[pos] = byteOf(v);
            }
        }

        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 170-250 — $CARTA_{N}
    ══════════════════════════════════════════════════════════════════ */

    private static final int HDATCARTAS_NIVEIS = 11;
    private static final int HDATCARTAS_STRIDE = 11;
    private static final int HDATCARTAS_TAMANHO_BLOCO = 616;
    private static final int HDATCARTAS_MAX_CONSTANTE = 56;

    private static final String[] HDATCARTAS_NOMES = new String[HDATCARTAS_MAX_CONSTANTE];
    static {
        HDATCARTAS_NOMES[6]  = "ATK";
        HDATCARTAS_NOMES[12] = "DEF";
        HDATCARTAS_NOMES[32] = "ANGRY";
        HDATCARTAS_NOMES[44] = "AFINIDADE";
        HDATCARTAS_NOMES[48] = "CUSTO";
        HDATCARTAS_NOMES[50] = "ATQ";
        HDATCARTAS_NOMES[52] = "DEF_EXIBICAO";
    }

    private static void exHDATCartas(BufferedWriter w, byte[] data, int xsize, int ysize, int cardId) throws IOException {
        w.write("      $CARTA_" + cardId + ":{\n");
        for (int constante = 0; constante < HDATCARTAS_MAX_CONSTANTE; constante++) {
            String nome = HDATCARTAS_NOMES[constante] != null ? HDATCARTAS_NOMES[constante] : ("ATR_" + String.format("%02d", constante));
            w.write("         $" + nome + ": [");
            for (int nivel = 0; nivel < HDATCARTAS_NIVEIS; nivel++) {
                int byteOffset = HDATCARTAS_STRIDE * constante + nivel * 2;
                int val = readShortAtByteOffset(data, byteOffset);
                w.write(String.valueOf(val));
                if (nivel < HDATCARTAS_NIVEIS - 1) w.write(", ");
            }
            w.write("], // por nível 0-10\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDATCartas(String block, ErrorReporter reporter) {
        byte[] data = new byte[HDATCARTAS_TAMANHO_BLOCO];

        for (int constante = 0; constante < HDATCARTAS_MAX_CONSTANTE; constante++) {
            String nome = HDATCARTAS_NOMES[constante] != null ? HDATCARTAS_NOMES[constante] : ("ATR_" + String.format("%02d", constante));
            Pattern p = Pattern.compile("\\$" + nome + ":\\s*\\[([^\\]]*)\\]");
            Matcher m = p.matcher(block);
            if (!m.find()) continue;
            String[] partes = m.group(1).split(",");
            for (int nivel = 0; nivel < HDATCARTAS_NIVEIS && nivel < partes.length; nivel++) {
                int byteOffset = HDATCARTAS_STRIDE * constante + nivel * 2;
                if (byteOffset + 1 >= data.length) continue;
                Integer v = checkedRawGroup(partes[nivel].trim(), "$" + nome + "[" + nivel + "]", -1, 0, 65535, reporter, m.group());
                writeShortAtByteOffset(data, byteOffset, v);
            }
        }
        return data;
    }

    /** Extrai o ID da carta (0-80) do texto de um bloco $CARTA_N — mantido por
     *  compatibilidade; o dispatch atual (processBuild) não depende dele. */
    public static int extractCardId(String block) {
        Matcher m = Pattern.compile("\\$CARTA_(\\d+):").matcher(block);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /* ══════════════════════════════════════════════════════════════════
       Utilitários compartilhados por todos os exHDAT/bdHDAT desta classe
    ══════════════════════════════════════════════════════════════════ */

    private static final int DANO_STRIDE = 14;

    private static int readUByte(byte[] data, int ptr) {
        if (ptr < 0 || ptr >= data.length) return 0;
        return data[ptr] & 0xFF;
    }

    private static int readSByte(byte[] data, int ptr) {
        if (ptr < 0 || ptr >= data.length) return 0;
        return (byte) (data[ptr] & 0xFF);
    }

    /** Lê um short little-endian no offset base + idx*2 (idx em unidades de short), sem sinal —
     *  mesmo comportamento do readShort() original de hdatgd146/153/156/157. */
    private static int readShortAt(byte[] data, int base, int idx) {
        int off = base + idx * 2;
        if (off < 0 || off + 1 >= data.length) return 0;
        int lo = data[off] & 0xFF;
        int hi = data[off + 1] & 0xFF;
        return (hi << 8) | lo;
    }

    /** Escreve um short little-endian no offset base + idx*2. null vira 0 (campo ausente). */
    private static void writeShortAtIdx(byte[] data, int base, int idx, Integer val) {
        int off = base + idx * 2;
        if (off < 0 || off + 1 >= data.length) return;
        int v = (val != null) ? val : 0;
        data[off]     = (byte) (v & 0xFF);
        data[off + 1] = (byte) ((v >> 8) & 0xFF);
    }

    /** Lê um valor de 1-2 bytes conforme o tamanho disponível (padrão usado por vários HDAT globais). */
    private static int readShortLoose(byte[] data) {
        if (data.length >= 2) {
            int lo = data[0] & 0xFF;
            int hi = data[1] & 0xFF;
            return (hi << 8) | lo;
        } else if (data.length == 1) {
            return data[0] & 0xFF;
        }
        return 0;
    }

    /** Lê um short little-endian num offset absoluto em bytes (sem sinal) — usado por HDATCartas,
     *  cujo STRIDE (11) é ímpar e por isso não se encaixa no índice "idx em unidades de short". */
    private static int readShortAtByteOffset(byte[] data, int byteOffset) {
        if (byteOffset < 0 || byteOffset + 1 >= data.length) return 0;
        int lo = data[byteOffset] & 0xFF;
        int hi = data[byteOffset + 1] & 0xFF;
        return (hi << 8) | lo;
    }

    /** Escreve um short little-endian num offset absoluto em bytes (sem sinal). null vira 0. */
    private static void writeShortAtByteOffset(byte[] data, int byteOffset, Integer val) {
        if (byteOffset < 0 || byteOffset + 1 >= data.length) return;
        int v = (val != null) ? val : 0;
        data[byteOffset]     = (byte) (v & 0xFF);
        data[byteOffset + 1] = (byte) ((v >> 8) & 0xFF);
    }

    private static byte byteOf(Integer v) {
        return (byte) ((v != null ? v : 0) & 0xFF);
    }

    /** Preenche `count` bytes de `data` a partir de `offset`, lendo o array "$tag: [v0, v1, ...]". */
    private static void fillByteRow(String block, String tag, byte[] data, int offset, int count, int hdatIndex, ErrorReporter reporter) {
        Pattern p = Pattern.compile(Pattern.quote(tag) + ":\\s*\\[([^\\]]*)\\]");
        Matcher m = p.matcher(block);
        if (!m.find()) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .severity(CompileError.Severity.WARNING)
                    .fieldTag(tag).hdatIndex(hdatIndex)
                    .message(tag + " não encontrado — linha ficará zerada"));
            return;
        }
        fillByteRowFromCsv(m.group(1), tag, data, offset, count, hdatIndex, reporter);
    }

    /** Mesma lógica de fillByteRow, mas já recebendo o conteúdo interno do "[...]" (usado quando o
     *  chamador já isolou o bloco via outro regex, ex: $PADRAO_N). */
    private static void fillByteRowFromCsv(String csv, String tag, byte[] data, int offset, int count, int hdatIndex, ErrorReporter reporter) {
        String[] partes = csv.split(",");
        if (partes.length > count) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .severity(CompileError.Severity.WARNING)
                    .fieldTag(tag).hdatIndex(hdatIndex)
                    .message(tag + " tem " + partes.length + " valores, mas eram esperados " + count + " — valores excedentes descartados"));
        }
        for (int col = 0; col < count && col < partes.length; col++) {
            String raw = partes[col].trim();
            if (raw.isEmpty()) continue;
            Integer v = checkedRawGroup(raw, tag + "[" + col + "]", hdatIndex, 0, 255, reporter, csv);
            if (v != null) data[offset + col] = (byte) (v & 0xFF);
        }
    }

    private static boolean checkedYesNo(String raw, String fieldTag, int hdatIndex, ErrorReporter reporter) {
        String v = raw.trim();
        if (v.equalsIgnoreCase("YES") || v.equalsIgnoreCase("NO")) {
            return v.equalsIgnoreCase("YES");
        }
        ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                .fieldTag(fieldTag).hdatIndex(hdatIndex)
                .message(fieldTag + " espera YES ou NO, mas recebeu \"" + v + "\" — tratado como NO"));
        return false;
    }

    private static boolean extractYesNo(String src, String regex) {
        Matcher m = Pattern.compile(regex).matcher(src);
        return m.find() && m.group(1).equalsIgnoreCase("YES");
    }

    /** Busca uma tag opcional; se ausente, retorna null (ausência é normal). Se presente mas inválida, reporta. */
    private static Integer extractOptionalChecked(String src, String regex, String fieldTag, int hdatIndex, int min, int max, ErrorReporter reporter) {
        Matcher m = Pattern.compile(regex).matcher(src);
        if (!m.find()) return null;
        return checkedRawGroup(m.group(1), fieldTag, hdatIndex, min, max, reporter, m.group());
    }

    private static int checkedU(String src, String regex, String fieldTag, ErrorReporter reporter) {
        Integer v = extractOptionalChecked(src, regex, fieldTag, -1, 0, 255, reporter);
        return (v != null) ? (v & 0xFF) : 0;
    }

    private static int checkedS(String src, String regex, String fieldTag, ErrorReporter reporter) {
        Integer v = extractOptionalChecked(src, regex, fieldTag, -1, -128, 127, reporter);
        return (v != null) ? (v & 0xFF) : 0;
    }

    /**
     * Núcleo de validação: interpreta um valor já extraído (raw, como string) como
     * inteiro, reportando (sem abortar):
     *  - decimal onde se espera inteiro (ex: "23.7")
     *  - texto não numérico
     *  - valor fora do range [min, max] esperado para este campo
     *
     * @return o valor interpretado (mesmo fora de range), ou null se nem foi possível
     *         interpretar como número.
     */
    private static Integer checkedRawGroup(String raw, String fieldTag, int hdatIndex, int min, int max, ErrorReporter reporter, String rawLine) {
        String trimmed = raw.trim();

        if (trimmed.contains(".")) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag(fieldTag)
                    .hdatIndex(hdatIndex >= 0 ? hdatIndex : null)
                    .rawLine(rawLine)
                    .message(fieldTag + " espera um inteiro, mas recebeu \"" + trimmed + "\" (valor decimal)"));
        }

        int value;
        try {
            value = (int) Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag(fieldTag)
                    .hdatIndex(hdatIndex >= 0 ? hdatIndex : null)
                    .rawLine(rawLine)
                    .message(fieldTag + " espera um valor numérico, mas recebeu \"" + trimmed + "\""));
            return null;
        }

        if (value < min || value > max) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag(fieldTag)
                    .hdatIndex(hdatIndex >= 0 ? hdatIndex : null)
                    .rawLine(rawLine)
                    .message(fieldTag + " = " + value + " está fora do range esperado [" + min + ".." + max
                            + "] — será truncado ao gravar no binário"));
        }

        return value;
    }
}
