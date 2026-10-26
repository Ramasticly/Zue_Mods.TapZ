package com.zue.gd.util;

import java.io.*;
import java.util.regex.*;

// BIN de backNN.pac — dados do cenário (stage). Consumido em runtime por
// TCBManajer.Game17 (e, em menor grau, Game10) via ChrGameData[1].piGameData[]/
// piGameDataPos[]/piGameDataXSize[]/piGameDataYSize[] (ver GameData.binCnv()).
//
// Uma classe só cuida de TODOS os HDATs da pasta "back" — quem decide o formato
// de cada campo é o número do HDAT recebido em processExtraction/processBuild.
//
// O que está confirmado por engenharia reversa de TCBManajer.java (não é chute):
//
//   HDAT 0 — Game17, case 2 (setup do cenário de luta) → $LOOPS_CENARIO:
//     XSIZE = número de colunas do mapa de paralaxe (usado direto pelo jogo, com
//       teto de 6: "min(XSIZE, 6)" — colunas além da 6ª nunca viram objeto).
//     Linha 0 (a ÚNICA lida pelo jogo, campo $PATTERN_POR_COLUNA): 1 byte por
//       coluna = índice de pattern base do spr pra aquela coluna. O jogo
//       multiplica esse valor por 3 pra montar o pattern de cada uma das 3
//       camadas de paralaxe: base*3+0 → camada 0 (mais perto), base*3+1 →
//       camada 2, base*3+2 → camada 1 (mais longe). Ver fluxo_parallax_stage.md,
//       seção 2. É o loop de cenário "de verdade" — o que se repete pelo mapa.
//     Linhas >0 (se YSIZE>1, campo $LINHAS_EXTRAS_RAW): TCBManajer nunca lê —
//       layout não confirmado.
//
//   HDAT 2 — Game10, case 456 (cutscene de movimento especial: câmera recua,
//     iEventBackScroll=true, NÃO é o cenário base da luta) → $SCROLL_CENARIO_CUTSCENE:
//     Linha 0, colunas 0 e 1 ($SCROLL_RAPIDO / $SCROLL_LENTO): dois índices de
//       pattern do spr, usados DIRETO (sem o *3 do HDAT 0) — cada um vira
//       SpriteSet(obj, valor) de um grupo de 3 cópias (case 451/452), espaçadas
//       512px (o scroll faz "% 512", por isso o espaçamento), cada grupo com sua
//       própria velocidade de scroll hardcoded no case 456 ($SCROLL_RAPIDO = 24
//       pixels/tick, prio 201; $SCROLL_LENTO = 12 pixels/tick, prio 200) — é um
//       scroll à parte, independente do loop do HDAT 0, feito à mão (scroll
//       manual em objExec.x) em vez de usar o laço de tiling do DrawSprite.
//
//   Qualquer outro HDAT: layout não confirmado nesta investigação — quem chama
//   (BinExtractor/BinBuilder) aplica o fallback RAW hex genérico.
//
// Integração:
//   BinExtractor: case 6 (pasta back) → hdatBack.processExtraction(...)
//   BinBuilder:   hdatBack.processBuild(...) — despachado por tag no texto do
//     bloco (igual hdatChar/hdatGD), não por pastaFixa.
public final class hdatBack {

    private hdatBack() {}

    private static final String[] HEX_TABLE = new String[256];
    static {
        for (int i = 0; i < 256; i++) HEX_TABLE[i] = String.format("%02X", i);
    }

    /* ══════════════════════════════════════════════════════════════════
       FACHADA — pontos de entrada únicos chamados por BinExtractor/BinBuilder
    ══════════════════════════════════════════════════════════════════ */

    /**
     * Extração (bytes → texto). Escreve no writer se reconhecer o HDAT.
     * @return true se escreveu algo; false se este HDAT não é conhecido por esta
     *         classe — quem chamou deve aplicar o fallback RAW.
     */
    public static boolean processExtraction(BufferedWriter w, byte[] data, int hdat, int xsize, int ysize) throws IOException {
        switch (hdat) {
            case 0: exHDAT0(w, data, xsize, ysize); return true;
            case 2: exHDAT2(w, data, xsize, ysize); return true;
            default: return false;
        }
    }

    /**
     * Build (texto → bytes). Despacha pela tag presente em {@code block}.
     * @param reporter opcional (pode ser null) — coleta erros de validação sem
     *                 nunca abortar a geração do binário.
     * @return os bytes construídos, ou null se nenhuma tag conhecida desta classe
     *         foi encontrada no bloco — quem chamou deve tentar outro domínio ou
     *         aplicar o fallback.
     */
    public static byte[] processBuild(String block, int xsize, int ysize, ErrorReporter reporter) {
        if (block.contains("$LOOPS_CENARIO"))           return bdHDAT0(block, xsize, ysize, reporter);
        if (block.contains("$SCROLL_CENARIO_CUTSCENE"))  return bdHDAT2(block, xsize, ysize, reporter);
        return null;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 0 — $LOOPS_CENARIO (mapa de colunas de paralaxe)
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT0(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $LOOPS_CENARIO:{\n");
        w.write("         $PATTERN_POR_COLUNA:[");
        for (int col = 0; col < xsize && col < data.length; col++) {
            w.write((data[col] & 0xFF) + ", ");
        }
        w.write("],\n");

        if (ysize > 1) {
            // linhas além da 0 — TCBManajer nunca lê; preserva em hex cru pra não perder dado
            w.write("         $LINHAS_EXTRAS_RAW:B[\n");
            for (int row = 1; row < ysize; row++) {
                w.write("            ");
                for (int col = 0; col < xsize; col++) {
                    int idx = row * xsize + col;
                    if (idx >= data.length) break;
                    w.write("&" + HEX_TABLE[data[idx] & 0xFF] + ", ");
                }
                w.write("\n");
            }
            w.write("         ],\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT0(String block, int xsize, int ysize, ErrorReporter reporter) {
        int w = Math.max(xsize, 0);
        int h = Math.max(ysize, 1);
        byte[] out = new byte[w * h];

        Matcher mBase = Pattern.compile("\\$PATTERN_POR_COLUNA:\\[([^\\]]*)\\]").matcher(block);
        if (mBase.find()) {
            String[] vals = mBase.group(1).trim().split(",");
            if (vals.length > w) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .severity(CompileError.Severity.WARNING)
                        .fieldTag("$PATTERN_POR_COLUNA")
                        .hdatIndex(0)
                        .rawLine(mBase.group())
                        .message("$PATTERN_POR_COLUNA tem " + vals.length + " valores, mas XSIZE é " + w
                                + " — valores excedentes serão descartados"));
            }
            for (int col = 0; col < vals.length && col < w; col++) {
                String v = vals[col].trim();
                if (v.isEmpty()) continue;
                Integer parsed = parseByteValue(v, "$PATTERN_POR_COLUNA", 0, reporter, mBase.group());
                if (parsed != null) out[col] = (byte) (parsed & 0xFF);
            }
        }

        Matcher mExtra = Pattern.compile("\\$LINHAS_EXTRAS_RAW:B\\[(.*?)\\]", Pattern.DOTALL).matcher(block);
        if (mExtra.find()) {
            Matcher hex = Pattern.compile("&([0-9A-Fa-f]{2})").matcher(mExtra.group(1));
            int idx = w; // linha 1 começa logo depois da linha 0
            while (hex.find() && idx < out.length) {
                out[idx++] = (byte) Integer.parseInt(hex.group(1), 16);
            }
        }
        return out;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 2 — $SCROLL_CENARIO_CUTSCENE
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT2(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $SCROLL_CENARIO_CUTSCENE:{\n");
        w.write("         $SCROLL_RAPIDO: " + (data.length > 0 ? (data[0] & 0xFF) : 0) + ",  // prio 201, 24/tick\n");
        w.write("         $SCROLL_LENTO: " + (data.length > 1 ? (data[1] & 0xFF) : 0) + ",  // prio 200, 12/tick\n");
        w.write("      },\n");

        int used = 2; // só os 2 primeiros bytes da linha 0 estão confirmados
        int total = Math.max(xsize, 0) * Math.max(ysize, 1);
        if (total > used && data.length > used) {
            w.write("      $EXTRA_RAW:B[\n");
            int ptr = used;
            while (ptr < data.length) {
                w.write("         ");
                for (int c = 0; c < xsize && ptr < data.length; c++) {
                    w.write("&" + HEX_TABLE[data[ptr++] & 0xFF] + ", ");
                }
                w.write("\n");
            }
            w.write("      ],\n");
        }
    }

    private static byte[] bdHDAT2(String block, int xsize, int ysize, ErrorReporter reporter) {
        int w = Math.max(xsize, 2);
        int h = Math.max(ysize, 1);
        byte[] out = new byte[w * h];

        Matcher mA = Pattern.compile("\\$SCROLL_RAPIDO:\\s*(-?[\\d.]+)").matcher(block);
        if (mA.find()) {
            Integer v = parseByteValue(mA.group(1), "$SCROLL_RAPIDO", 2, reporter, mA.group());
            if (v != null) out[0] = (byte) (v & 0xFF);
        } else {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$SCROLL_RAPIDO")
                    .hdatIndex(2)
                    .message("$SCROLL_CENARIO_CUTSCENE presente, mas $SCROLL_RAPIDO não foi encontrado"));
        }

        Matcher mB = Pattern.compile("\\$SCROLL_LENTO:\\s*(-?[\\d.]+)").matcher(block);
        if (mB.find() && out.length > 1) {
            Integer v = parseByteValue(mB.group(1), "$SCROLL_LENTO", 2, reporter, mB.group());
            if (v != null) out[1] = (byte) (v & 0xFF);
        } else if (!mB.find()) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$SCROLL_LENTO")
                    .hdatIndex(2)
                    .message("$SCROLL_CENARIO_CUTSCENE presente, mas $SCROLL_LENTO não foi encontrado"));
        }

        Matcher mExtra = Pattern.compile("\\$EXTRA_RAW:B\\[(.*?)\\]", Pattern.DOTALL).matcher(block);
        if (mExtra.find()) {
            Matcher hex = Pattern.compile("&([0-9A-Fa-f]{2})").matcher(mExtra.group(1));
            int idx = 2;
            while (hex.find() && idx < out.length) {
                out[idx++] = (byte) Integer.parseInt(hex.group(1), 16);
            }
        }
        return out;
    }

    /* ══════════════════════════════════════════════════════════════════
       Utilitários compartilhados por todos os exHDAT/bdHDAT desta classe
    ══════════════════════════════════════════════════════════════════ */

    /**
     * Interpreta um valor textual como inteiro de byte sem sinal (0-255).
     * Reporta erro (não aborta) quando:
     *  - o texto não é um inteiro válido (ex: "23.7", "abc")
     *  - o valor está fora do range 0-255 (o byte final seria truncado silenciosamente)
     *
     * @return o valor parseado (mesmo que fora de range — quem chama decide se usa
     *         truncado ou ignora), ou null se nem foi possível interpretar como número.
     */
    private static Integer parseByteValue(String raw, String fieldTag, int hdatIndex, ErrorReporter reporter, String rawLine) {
        String trimmed = raw.trim();

        if (trimmed.contains(".")) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag(fieldTag)
                    .hdatIndex(hdatIndex)
                    .rawLine(rawLine)
                    .message(fieldTag + " espera um inteiro, mas recebeu \"" + trimmed + "\" (valor decimal)"));
            // ainda tenta truncar pra manter o binário "gerável", mas o erro já foi reportado
        }

        int value;
        try {
            value = (int) Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag(fieldTag)
                    .hdatIndex(hdatIndex)
                    .rawLine(rawLine)
                    .message(fieldTag + " espera um valor numérico, mas recebeu \"" + trimmed + "\""));
            return null;
        }

        if (value < 0 || value > 255) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag(fieldTag)
                    .hdatIndex(hdatIndex)
                    .rawLine(rawLine)
                    .message(fieldTag + " = " + value + " está fora do range de byte (0-255) — será truncado para "
                            + (value & 0xFF)));
        }

        return value;
    }
}
