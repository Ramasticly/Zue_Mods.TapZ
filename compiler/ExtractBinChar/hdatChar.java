package com.zue.gd.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

// Dados de personagem (char??.pac). Antes desta consolidação, cada HDAT vivia numa
// classe própria (hdat0.java, hdat1.java, ..., hdat41.java, hdatDano.java) — todas com
// a mesma lógica de extração (regex + parseInt) duplicada N vezes. Agora todos os
// HDATs de personagem vivem aqui, com os utilitários de extração compartilhados de
// fato (um único método por família, não uma cópia por classe).
//
// Convenção de nomes:
//   exHDAT{N}  — extração: bytes → texto (era "writehdat")
//   bdHDAT{N}  — build:     texto → bytes (era "buildhdat")
//
// Integração:
//   BinExtractor: case 4 (pasta char) → hdatChar.processExtraction(...)
//   BinBuilder:   hdatChar.processBuild(...) — despachado por tag no texto do bloco.
public final class hdatChar {

    private hdatChar() {}

    /* ══════════════════════════════════════════════════════════════════
       FACHADA — pontos de entrada únicos chamados por BinExtractor/BinBuilder
    ══════════════════════════════════════════════════════════════════ */

    /**
     * Extração (bytes → texto).
     * @return true se escreveu algo; false se este HDAT não é conhecido por esta
     *         classe — quem chamou deve aplicar o fallback RAW.
     */
    public static boolean processExtraction(BufferedWriter w, byte[] data, int hdat, int xsize, int ysize) throws IOException {
        switch (hdat) {
            // case 0:  exHDAT0(w, data, xsize, ysize); return true; // desativado no BinExtractor original — mantido comentado de propósito
            case 1:  exHDAT1(w, data, xsize, ysize); return true;
            case 2:  exHDAT2(w, data, xsize, ysize); return true;
            case 3:  exHDAT3(w, data, xsize, ysize); return true;
            case 4:  exHDAT4(w, data, xsize, ysize); return true;
            case 5:  exHDAT5(w, data, xsize, ysize); return true;
            // case 6:  exHDAT6(w, data, xsize, ysize); return true; // desativado no BinExtractor original — mantido comentado de propósito
            case 13: exHDAT13(w, data, xsize, ysize); return true;
            case 34: exHDAT34(w, data, xsize, ysize); return true;
            case 35: exHDAT35(w, data, xsize, ysize); return true;
            case 36: exHDAT36(w, data, xsize, ysize); return true;
            case 37: exHDAT37(w, data, xsize, ysize); return true;
            case 38: exHDAT38(w, data, xsize, ysize); return true;
            case 39: exHDAT39(w, data, xsize, ysize); return true;
            case 40: exHDAT40(w, data, xsize, ysize); return true;
            case 41: exHDAT41(w, data, xsize, ysize); return true;
            default:
                if (hdat >= 14 && hdat <= 33) { exHDATDano(w, data, xsize, ysize, hdat - 14); return true; }
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
        if (block.contains("$COMPORTAMENTO_IA"))        return bdHDAT0(block, reporter);
        if (block.contains("$REGENERACAO_KI"))           return bdHDAT1(block, reporter);
        if (block.contains("$TEXTOS_DO_PERSONAGEM"))     return bdHDAT2(block);
        if (block.contains("$STATUS_BASE_E_FISICA"))     return bdHDAT3(block, reporter);
        if (block.contains("$ATRIBUTOS_DE_GOLPE"))       return bdHDAT4(block, reporter);
        if (block.contains("$PONTUACAO_POR_GOLPE"))      return bdHDAT5(block, xsize, ysize, reporter);
        if (block.contains("$SEQUENCIAS_IA"))            return bdHDAT6(block, xsize, reporter);
        if (block.contains("$ROTEIRO_CUTSCENE"))         return bdHDAT13(block, xsize, ysize, reporter);
        // O extractor (exHDATDano) escreve "$REACAO_DANO_N"; aqui só se
        // procurava "$REACAO_DE_DANO_", então os HDAT 14-33 caíam no fallback
        // e voltavam com 0 bytes. "DE_" continua aceito (arquivos antigos); o
        // dígito logo após "DANO_" deixa de fora o "$REACAO_DANO_GD_" do gamedata.
        if (DANO_TAG.matcher(block).find())             return bdHDATDano(block, reporter);
        if (block.contains("$DISPUTA_DE_PODER"))         return bdHDAT34(block, xsize, ysize, reporter);
        if (block.contains("$DANO_DE_EVENTOS_CUTSCENE"))  return bdHDAT35(block, xsize, ysize, reporter);
        if (block.contains("$SISTEMA_DE_VOZES"))         return bdHDAT36(block, xsize, ysize, reporter);
        if (block.contains("$DANO_NO_ESTADO_DE_FURIA"))  return bdHDAT37(block, xsize, ysize, reporter);
        if (block.contains("$CARTA_RECOMPENSA_ARCADE"))  return bdHDAT38(block, xsize, ysize, reporter);
        if (block.contains("$CENARIO_PADRAO"))           return bdHDAT39(block, reporter);
        if (block.contains("$PROGRESSAO_CAMPANHA"))      return bdHDAT40(block, xsize, ysize, reporter);
        if (block.contains("$PERSONAGENS_DESBLOQUEAVEIS")) return bdHDAT41(block, xsize, ysize, reporter);
        return null;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 0 — $COMPORTAMENTO_IA
    ══════════════════════════════════════════════════════════════════ */

    private static final String[] SITUACOES = {
        "PROXIMO_CHAO", "MEDIO_CHAO", "LONGE_CHAO", "PROXIMO_AR_BAIXO",
        "MEDIO_AR_BAIXO", "LONGE_AR", "PROJETIL_VINDO", "ATAQUE_RECEBIDO",
        "PROXIMO_AR_ALTO", "MEDIO_AR_ALTO", "LONGE_AR_ALTO"
    };

    private static void exHDAT0(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $COMPORTAMENTO_IA:{\n");

        w.write("      // ─────────────────────────────────────────────────────────────────────────\n");
        w.write("      //\n");
        w.write("      //  Tabela de decisão da CPU (HDAT_0). Lida em TCBManajer.Game7, modo 952.\n");
        w.write("      //\n");
        w.write("      //  A cada frame, a CPU avalia a situação (distância + posição vertical\n");
        w.write("      //  do oponente) e escolhe um CPUSeqNo via:\n");
        w.write("      //\n");
        w.write("      //      row = situação × 4 + (CPURand % 4)   → variação sorteada\n");
        w.write("      //      col = CPULevel  (0–7 se CPU no chão, 8–15 se CPU no ar)\n");
        w.write("      //      valor lido = CPUSeqNo → índice em $SEQUENCIAS_IA (HDAT 6)\n");
        w.write("      //\n");
        w.write("      //  Cada $SITUACAO tem 4 variações ($VAR_0 a $VAR_3) sorteadas via CPURand.\n");
        w.write("      //  Cada variação define um CPUSeqNo por nível de dificuldade:\n");
        w.write("      //      N0 = mais fácil, N7 = mais difícil.\n");
        w.write("      //  Separado em $CHAO (CPU no chão, colunas 0–7) e\n");
        w.write("      //              $AR   (CPU no ar,   colunas 8–15).\n");
        w.write("      //  Valor 0 = sem ação (CPU fica parada/anda — SeqNo 0).\n");
        w.write("      //\n");
        w.write("      //  $POOL_DE_SEQUENCIAS (linha 44, L23200 de TCBManajer):\n");
        w.write("      //  Limita o sorteio aleatório de CPUSeqNo por nível. A CPU só usa\n");
        w.write("      //  SeqNos menores que esse valor (via CPURand % POOL).\n");
        w.write("      //  N0=30 → amplo repertório (nível mais fácil).\n");
        w.write("      //  N7=3  → CPU usa sempre as mesmas 3 sequências (mais difícil,\n");
        w.write("      //           pois só as 3 melhores ficam disponíveis).\n");
        w.write("      //\n");
        w.write("      // ─────────────────────────────────────────────────────────────────────────\n");
        w.write("\n\n");

        for (int sit = 0; sit < 11; sit++) {
            String nome = sit < SITUACOES.length ? SITUACOES[sit] : "SITUACAO_" + sit;
            w.write("         $" + nome + ":{\n");
            for (int var = 0; var < 4; var++) {
                int row = sit * 4 + var;
                w.write("            $VAR_" + var + ":{ ");
                w.write("$CHAO:[");
                for (int col = 0; col < 8; col++) {
                    int idx = row * xsize + col;
                    int val = (idx < data.length) ? data[idx] & 0xFF : 0;
                    w.write("N" + col + ":" + val + (col < 7 ? ", " : ""));
                }
                w.write("], $AR:[");
                for (int col = 0; col < 8; col++) {
                    int idx = row * xsize + 8 + col;
                    int val = (idx < data.length) ? data[idx] & 0xFF : 0;
                    w.write("N" + col + ":" + val + (col < 7 ? ", " : ""));
                }
                w.write("] },\n");
            }
            w.write("         },\n");
        }

        if (ysize > 44) {
            w.write("\n         $POOL_DE_SEQUENCIAS:{\n");
            w.write("            $CHAO:[ ");
            for (int col = 0; col < 8; col++) {
                int idx = 44 * xsize + col;
                int val = (idx < data.length) ? data[idx] & 0xFF : 0;
                w.write("N" + col + ":" + val + (col < 7 ? ", " : ""));
            }
            w.write(" ],\n");
            w.write("            $AR:[   ");
            for (int col = 0; col < 8; col++) {
                int idx = 44 * xsize + 8 + col;
                int val = (idx < data.length) ? data[idx] & 0xFF : 0;
                w.write("N" + col + ":" + val + (col < 7 ? ", " : ""));
            }
            w.write(" ]\n");
            w.write("         },\n");
        }

        w.write("      },\n");
    }

    private static byte[] bdHDAT0(String block, ErrorReporter reporter) throws IOException {
        int xsize = 16;
        int ysize = 45;
        byte[] data = new byte[xsize * ysize];

        for (int sit = 0; sit < 11; sit++) {
            String nome = sit < SITUACOES.length ? SITUACOES[sit] : "SITUACAO_" + sit;
            String sitBlock = extractBlock(block, "\\$" + nome);
            if (sitBlock == null) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .fieldTag("$" + nome)
                        .hdatIndex(0)
                        .message("$COMPORTAMENTO_IA está sem o bloco $" + nome + " — situação ficará zerada"));
                continue;
            }
            for (int var = 0; var < 4; var++) {
                int row = sit * 4 + var;
                String varBlock = extractBlock(sitBlock, "\\$VAR_" + var);
                if (varBlock == null) continue;
                String chaoBlock = extractBracket(varBlock, "\\$CHAO");
                String arBlock   = extractBracket(varBlock, "\\$AR");
                for (int col = 0; col < 8; col++) {
                    data[row * xsize + col]     = (byte) extractNivelChecked(chaoBlock, col, "$" + nome + "/$VAR_" + var + "/$CHAO", 0, reporter);
                    data[row * xsize + 8 + col] = (byte) extractNivelChecked(arBlock,   col, "$" + nome + "/$VAR_" + var + "/$AR",   0, reporter);
                }
            }
        }

        String poolBlock = extractBlock(block, "\\$POOL_DE_SEQUENCIAS");
        if (poolBlock != null) {
            String chaoBlock = extractBracket(poolBlock, "\\$CHAO");
            String arBlock   = extractBracket(poolBlock, "\\$AR");
            for (int col = 0; col < 8; col++) {
                data[44 * xsize + col]     = (byte) extractNivelChecked(chaoBlock, col, "$POOL_DE_SEQUENCIAS/$CHAO", 0, reporter);
                data[44 * xsize + 8 + col] = (byte) extractNivelChecked(arBlock,   col, "$POOL_DE_SEQUENCIAS/$AR",   0, reporter);
            }
        }

        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 1 — $REGENERACAO_KI
    ══════════════════════════════════════════════════════════════════ */

    private static final String[] REGEN_LABELS = { "AO_ATACAR", "AO_RECEBER_DANO", "AO_DEFENDER", "AO_BATER_NA_GUARDA" };

    private static void exHDAT1(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $REGENERACAO_KI:{\n");
        for (int i = 0; i < 4; i++) {
            int base = i * 2;
            int div = (base < data.length) ? data[base] & 0xFF : 1;
            int bon = (base + 1 < data.length) ? data[base + 1] & 0xFF : 0;
            // O jogo faz dano * bônus / divisor. Divisor 100 → sai como % (formato
            // de sempre); qualquer outro sai como "bônus/divisor" para não perder
            // precisão (antes era sempre convertido para % e gravado com divisor 100).
            String value = (div == 100) ? String.valueOf(bon) : (bon + "/" + div);
            w.write("         " + REGEN_LABELS[i] + ": " + value + ",\n");
        }
        w.write("         // Nota: Valores em % do dano convertido em Ki. 300 de dano, 30% do dano convertido em Ki = 100 de ki (uma barra)\n");
        w.write("         // \"B/D\" = bônus/divisor exatos do binário (o jogo usa dano * B / D).\n");
        w.write("      },\n");
    }

    private static byte[] bdHDAT1(String block, ErrorReporter reporter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String label : REGEN_LABELS) {
            Matcher frac = Pattern.compile(label + ":\\s*(\\d+)\\s*/\\s*(\\d+)").matcher(block);
            if (frac.find()) {
                Integer bon = checkedRawGroup(frac.group(1), "$" + label, 1, 0, 255, reporter, frac.group());
                Integer div = checkedRawGroup(frac.group(2), "$" + label, 1, 0, 255, reporter, frac.group());
                baos.write((div != null ? div : 100) & 0xFF);
                baos.write((bon != null ? bon : 0) & 0xFF);
                continue;
            }
            Integer percent = extractOptionalChecked(block, label + ":\\s*(-?[\\d.]+)", "$" + label, 1, 0, 255, reporter);
            int p = (percent != null) ? percent : 0;
            baos.write(100 & 0xFF);
            baos.write(p & 0xFF);
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 2 — $TEXTOS_DO_PERSONAGEM
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT2(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $TEXTOS_DO_PERSONAGEM:{\n");
        int ptr = 0;
        int len = data.length;
        for (int i = 0; i < ysize; i++) {
            String txt = "";
            if (ptr + 2 <= len) {
                int strLen = ((data[ptr] & 0xFF) << 8) | (data[ptr + 1] & 0xFF);
                ptr += 2;
                if (strLen > 0) {
                    int available = Math.min(strLen, len - ptr);
                    byte[] chunk = new byte[available];
                    System.arraycopy(data, ptr, chunk, 0, available);
                    txt = decodeWithFallback(chunk);
                    ptr += available;
                }
            }
            String escaped = txt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            w.write("         $LINHA_" + i + ": \"" + escaped + "\",\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT2(String block) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Matcher m = Pattern.compile("\\$LINHA_\\d+:\\s*" + BinBuilder.QUOTED_TEXT).matcher(block);
        while (m.find()) {
            byte[] b = BinBuilder.unescapeText(m.group(1)).getBytes(StandardCharsets.UTF_8);
            baos.write((b.length >> 8) & 0xFF);
            baos.write(b.length & 0xFF);
            baos.write(b);
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 3 — $STATUS_BASE_E_FISICA
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT3(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $STATUS_BASE_E_FISICA:{\n");

        w.write("      // ─────────────────────────────────────────────────────────────────────────\n");
        w.write("      //  CORPO:      $LARGURA, $ALTURA\n");
        w.write("      //  MOVIMENTO:  $VELOCIDADE_MOVIMENTO  — em direção ao oponente e vertical aéreo\n");
        w.write("      //              $VELOCIDADE_RECUO       — se afastando do oponente\n");
        w.write("      //              $VELOCIDADE_DASH_VERTICAL — cima/baixo durante o dash\n");
        w.write("      //              $DISTANCIA_AGARRE       — teleporte dos ATK_FLAGS 8 e 128\n");
        w.write("      //              $PODE_VOAR              — 0=não, 1=sim (flutua livremente)\n");
        w.write("      //  STATS:      $VIDA_INICIAL, $ATK_BASE\n");
        w.write("      //              $KI_MAXIMO_BASE, $VELOCIDADE_CARGA_KI\n");
        w.write("      //              $DEFESA_BASE\n");
        w.write("      //              $TIPO_COUNTER — throw do counter (ATK_FLAGS bit 2):\n");
        w.write("      //                              0–29=agarre chão, 30–59=agarre aéreo\n");
        w.write("      //  IDENTIDADE: $ID_ATRIBUTO  — ícone na seleção e afinidade com cartas\n");
        w.write("      //              $ANIM_ENTRADA, $PONTOS_EQUIPAR_CARTAS\n");
        w.write("      // ─────────────────────────────────────────────────────────────────────────\n");
        w.write("\n\n");

        int[] vals = new int[data.length];
        for (int i = 0; i < data.length; i++) vals[i] = data[i] & 0xFF;

        w.write("         // --- CORPO ---\n");
        // byte 3 = largura cheia; o texto mostra a metade. Byte ímpar sai como
        // "N.5" (antes o bit baixo era descartado no rebuild).
        w.write("         $LARGURA: "                + (vals[3] / 2) + (vals[3] % 2 != 0 ? ".5" : "") + ",\n");
        w.write("         $ALTURA: "                 + vals[4] + ",\n");
        w.write("\n");

        w.write("         // --- MOVIMENTO ---\n");
        w.write("         $VELOCIDADE_MOVIMENTO: "   + vals[0] + ",\n");
        w.write("         $VELOCIDADE_RECUO: "       + vals[1] + ",\n");
        w.write("         $VELOCIDADE_DASH_VERTICAL: " + vals[7] + ",\n");
        w.write("         $DISTANCIA_AGARRE: "       + vals[5] + ",\n");
        w.write("         $PODE_VOAR: "              + vals[6] + ",\n");
        w.write("\n");

        w.write("         // --- STATS ---\n");
        w.write("         $VIDA_INICIAL: "           + vals[11] + ",\n");
        w.write("         $ATK_BASE: "               + vals[12] * 10 + ",\n");
        w.write("         $KI_MAXIMO_BASE: "         + vals[8] * 100  + ",\n");
        w.write("         //O jogo tem duas barras por padrão, então 1 + 2 = 3.\n");
        w.write("         $VELOCIDADE_CARGA_KI: "    + vals[10] + ",\n");
        w.write("         $DEFESA_BASE: "            + vals[14] * 10 + ",\n");
        w.write("         $TIPO_COUNTER: "           + vals[13] + ",\n");
        w.write("\n");

        w.write("         // --- IDENTIDADE ---\n");
        w.write("         $ID_ATRIBUTO: "            + vals[9] + ",\n");
        w.write("         $ANIM_ENTRADA: "           + vals[16] + ",\n");
        w.write("         $PONTOS_EQUIPAR_CARTAS: "  + vals[17] + ",\n");
        w.write("\n");

        w.write("         // Campos sem função conhecida. Mantenha os valores originais\n");
        w.write("         // ou experimente alterá-los para descobrir novos comportamentos.\n");
        w.write("         $DESCONHECIDO_2: "         + vals[2] + ",\n");
        w.write("         $DESCONHECIDO_15: "        + vals[15] + "\n");

        w.write("      },\n");
    }

    private static byte[] bdHDAT3(String block, ErrorReporter reporter) {
        byte[] data = new byte[18];
        data[0]  = (byte) checked(block, "\\$VELOCIDADE_MOVIMENTO:\\s*(-?[\\d.]+)",       "$VELOCIDADE_MOVIMENTO",       3, 0, 255, reporter);
        data[1]  = (byte) checked(block, "\\$VELOCIDADE_RECUO:\\s*(-?[\\d.]+)",           "$VELOCIDADE_RECUO",           3, 0, 255, reporter);
        data[2]  = (byte) checked(block, "\\$DESCONHECIDO_2:\\s*(-?[\\d.]+)",             "$DESCONHECIDO_2",             3, 0, 255, reporter);
        data[3]  = (byte) larguraToByte(block, reporter);
        data[4]  = (byte) checked(block, "\\$ALTURA:\\s*(-?[\\d.]+)",                     "$ALTURA",                     3, 0, 255, reporter);
        data[5]  = (byte) checked(block, "\\$DISTANCIA_AGARRE:\\s*(-?[\\d.]+)",           "$DISTANCIA_AGARRE",           3, 0, 255, reporter);
        data[6]  = (byte) checked(block, "\\$PODE_VOAR:\\s*(-?[\\d.]+)",                  "$PODE_VOAR",                  3, 0, 1,   reporter);
        data[7]  = (byte) checked(block, "\\$VELOCIDADE_DASH_VERTICAL:\\s*(-?[\\d.]+)",   "$VELOCIDADE_DASH_VERTICAL",  3, 0, 255, reporter);
        data[8]  = (byte) (checked(block, "\\$KI_MAXIMO_BASE:\\s*(-?[\\d.]+)",             "$KI_MAXIMO_BASE",             3, 0, 25500, reporter) / 100);
        data[9]  = (byte) checked(block, "\\$ID_ATRIBUTO:\\s*(-?[\\d.]+)",                "$ID_ATRIBUTO",                3, 0, 255, reporter);
        data[10] = (byte) checked(block, "\\$VELOCIDADE_CARGA_KI:\\s*(-?[\\d.]+)",        "$VELOCIDADE_CARGA_KI",        3, 0, 255, reporter);
        data[11] = (byte) checked(block, "\\$VIDA_INICIAL:\\s*(-?[\\d.]+)",               "$VIDA_INICIAL",               3, 0, 255, reporter);
        data[12] = (byte) (checked(block, "\\$ATK_BASE:\\s*(-?[\\d.]+)",                   "$ATK_BASE",                   3, 0, 2550, reporter) / 10);
        data[13] = (byte) checked(block, "\\$TIPO_COUNTER:\\s*(-?[\\d.]+)",               "$TIPO_COUNTER",               3, 0, 255, reporter);
        data[14] = (byte) (checked(block, "\\$DEFESA_BASE:\\s*(-?[\\d.]+)",                "$DEFESA_BASE",                3, 0, 2550, reporter) / 10);
        data[15] = (byte) checked(block, "\\$DESCONHECIDO_15:\\s*(-?[\\d.]+)",            "$DESCONHECIDO_15",            3, 0, 255, reporter);
        data[16] = (byte) checked(block, "\\$ANIM_ENTRADA:\\s*(-?[\\d.]+)",               "$ANIM_ENTRADA",               3, 0, 255, reporter);
        data[17] = (byte) checked(block, "\\$PONTOS_EQUIPAR_CARTAS:\\s*(-?[\\d.]+)",      "$PONTOS_EQUIPAR_CARTAS",      3, 0, 255, reporter);
        return data;
    }

    // $LARGURA aceita meio pixel ("12.5" → byte 25), espelhando exHDAT3.
    private static int larguraToByte(String block, ErrorReporter reporter) {
        Matcher m = Pattern.compile("\\$LARGURA:\\s*(-?[\\d.]+)").matcher(block);
        if (!m.find()) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$LARGURA").hdatIndex(3).message("$LARGURA não encontrado no bloco"));
            return 0;
        }
        int v;
        try {
            v = (int) Math.round(Double.parseDouble(m.group(1)) * 2);
        } catch (NumberFormatException e) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$LARGURA").hdatIndex(3).rawLine(m.group())
                    .message("$LARGURA espera um número, mas recebeu \"" + m.group(1) + "\""));
            return 0;
        }
        if (v < 0 || v > 255) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$LARGURA").hdatIndex(3).rawLine(m.group())
                    .message("$LARGURA = " + m.group(1) + " está fora do range esperado [0..127.5]"));
        }
        return v;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 4 — $ATRIBUTOS_DE_GOLPE
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT4(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $ATRIBUTOS_DE_GOLPE:{\n");
        int stride = 48;
        int count = data.length / stride;

        w.write("      // (documentação de campos disponível no histórico do projeto)\n\n");

        for (int i = 0; i < count; i++) {
            int base = i * stride;

            w.write("         $GOLPE_" + i + ":{\n");
            w.write("            // --- GOLPE ---\n");
            w.write("            $ATK_FLAGS: "                        + readS16(data, base +  0) + ",\n");
            w.write("            $ANIM_INICIAL: "                     + readS16(data, base +  2) + ",\n");
            w.write("            // --- PROJÉTIL ---\n");
            w.write("            $PROJETIL_ANIM_NORMAL: "             + readS16(data, base +  4) + ",\n");
            w.write("            $PROJETIL_ANIM_ABAIXO: "             + readS16(data, base + 34) + ",\n");
            w.write("            $PROJETIL_ANIM_ACIMA: "              + readS16(data, base + 36) + ",\n");
            w.write("            $PROJETIL_ANIM_ACERTAR_GLOBAL: "     + readS16(data, base + 38) + ",\n");
            w.write("            $PROJETIL_ANIM_AO_ACERTAR: "         + readS16(data, base +  6) + ",\n");
            w.write("            $PROJETIL_COLISAO_CHAO: "            + readS16(data, base + 40) + ",\n");
            w.write("            $HOMING: "                           + readS16(data, base + 26) + ",\n");
            w.write("            $HOMING_VELOCIDADE: "                + readS16(data, base + 28) + ",\n");
            w.write("            $MAX_REPETICOES: "                   + readS16(data, base + 30) + ",\n");
            w.write("            $SUBPROJETIL_ANIM_ID: "              + readS16(data, base + 18) + ",\n");
            w.write("            $SUBPROJETIL_ANIM_AO_ACERTAR: "      + readS16(data, base + 20) + ",\n");
            w.write("            $SUBPROJETIL_COLISAO_CHAO: "         + readS16(data, base + 42) + ",\n");
            w.write("            // --- ATACANTE ---\n");
            w.write("            $ANIM_AO_ACERTAR: "                  + readS16(data, base + 32) + ",\n");
            w.write("            $ANIM_SEQ_CHAO: "                    + readS16(data, base + 10) + ",\n");
            w.write("            $ANIM_END_CHAO: "                    + readS16(data, base + 12) + ",\n");
            w.write("            $ANIM_SEQ_AR: "                      + readS16(data, base + 14) + ",\n");
            w.write("            $ANIM_END_AR: "                      + readS16(data, base + 16) + ",\n");
            w.write("            $POUSO_AO_ERRAR: "                   + readS16(data, base +  8) + ",\n");
            w.write("            $POUSO_AO_ACERTAR: "                 + readS16(data, base + 22) + ",\n");
            w.write("            // --- OPONENTE ---\n");
            w.write("            $OPONENTE_ANIM_SENDO_AGARRADO: "     + readS16(data, base + 24) + ",\n");
            w.write("            // Campos sem função conhecida.\n");
            w.write("            $DESCONHECIDO_44: "                   + readS16(data, base + 44) + ",\n");
            w.write("            $DESCONHECIDO_46: "                   + readS16(data, base + 46) + "\n");
            w.write("         },\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT4(String block, ErrorReporter reporter) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Descobre quantos $GOLPE_N existem e em que ordem, depois isola cada bloco
        // por contagem de profundidade de chaves (extractBlock) — mais robusto que
        // regex lazy/lookahead quando o conteúdo interno tem vírgulas/quebras livres.
        List<Integer> golpeIndices = new ArrayList<Integer>();
        Matcher mIdx = Pattern.compile("\\$GOLPE_(\\d+)\\s*:\\s*\\{").matcher(block);
        while (mIdx.find()) golpeIndices.add(Integer.parseInt(mIdx.group(1)));

        int found = 0;
        for (int golpeNum : golpeIndices) {
            String golpeLabel = "$GOLPE_" + golpeNum;
            String c = extractBlock(block, "\\$GOLPE_" + golpeNum);
            if (c == null) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .fieldTag(golpeLabel)
                        .hdatIndex(4)
                        .message(golpeLabel + " tem chaves desbalanceadas — bloco ignorado"));
                continue;
            }
            found++;
            writeShortLE(baos, checkedS16(c, "\\$ATK_FLAGS:\\s*(-?[\\d.]+)",                    golpeLabel + "/$ATK_FLAGS", reporter));
            writeShortLE(baos, checkedS16(c, "\\$ANIM_INICIAL:\\s*(-?[\\d.]+)",                 golpeLabel + "/$ANIM_INICIAL", reporter));
            writeShortLE(baos, checkedS16(c, "\\$PROJETIL_ANIM_NORMAL:\\s*(-?[\\d.]+)",         golpeLabel + "/$PROJETIL_ANIM_NORMAL", reporter));
            writeShortLE(baos, checkedS16(c, "\\$PROJETIL_ANIM_AO_ACERTAR:\\s*(-?[\\d.]+)",     golpeLabel + "/$PROJETIL_ANIM_AO_ACERTAR", reporter));
            writeShortLE(baos, checkedS16(c, "\\$POUSO_AO_ERRAR:\\s*(-?[\\d.]+)",               golpeLabel + "/$POUSO_AO_ERRAR", reporter));
            writeShortLE(baos, checkedS16(c, "\\$ANIM_SEQ_CHAO:\\s*(-?[\\d.]+)",                golpeLabel + "/$ANIM_SEQ_CHAO", reporter));
            writeShortLE(baos, checkedS16(c, "\\$ANIM_END_CHAO:\\s*(-?[\\d.]+)",                golpeLabel + "/$ANIM_END_CHAO", reporter));
            writeShortLE(baos, checkedS16(c, "\\$ANIM_SEQ_AR:\\s*(-?[\\d.]+)",                  golpeLabel + "/$ANIM_SEQ_AR", reporter));
            writeShortLE(baos, checkedS16(c, "\\$ANIM_END_AR:\\s*(-?[\\d.]+)",                  golpeLabel + "/$ANIM_END_AR", reporter));
            writeShortLE(baos, checkedS16(c, "\\$SUBPROJETIL_ANIM_ID:\\s*(-?[\\d.]+)",          golpeLabel + "/$SUBPROJETIL_ANIM_ID", reporter));
            writeShortLE(baos, checkedS16(c, "\\$SUBPROJETIL_ANIM_AO_ACERTAR:\\s*(-?[\\d.]+)",  golpeLabel + "/$SUBPROJETIL_ANIM_AO_ACERTAR", reporter));
            writeShortLE(baos, checkedS16(c, "\\$POUSO_AO_ACERTAR:\\s*(-?[\\d.]+)",             golpeLabel + "/$POUSO_AO_ACERTAR", reporter));
            writeShortLE(baos, checkedS16(c, "\\$OPONENTE_ANIM_SENDO_AGARRADO:\\s*(-?[\\d.]+)", golpeLabel + "/$OPONENTE_ANIM_SENDO_AGARRADO", reporter));
            writeShortLE(baos, checkedS16(c, "\\$HOMING:\\s*(-?[\\d.]+)",                       golpeLabel + "/$HOMING", reporter));
            writeShortLE(baos, checkedS16(c, "\\$HOMING_VELOCIDADE:\\s*(-?[\\d.]+)",            golpeLabel + "/$HOMING_VELOCIDADE", reporter));
            writeShortLE(baos, checkedS16(c, "\\$MAX_REPETICOES:\\s*(-?[\\d.]+)",               golpeLabel + "/$MAX_REPETICOES", reporter));
            writeShortLE(baos, checkedS16(c, "\\$ANIM_AO_ACERTAR:\\s*(-?[\\d.]+)",              golpeLabel + "/$ANIM_AO_ACERTAR", reporter));
            writeShortLE(baos, checkedS16(c, "\\$PROJETIL_ANIM_ABAIXO:\\s*(-?[\\d.]+)",         golpeLabel + "/$PROJETIL_ANIM_ABAIXO", reporter));
            writeShortLE(baos, checkedS16(c, "\\$PROJETIL_ANIM_ACIMA:\\s*(-?[\\d.]+)",          golpeLabel + "/$PROJETIL_ANIM_ACIMA", reporter));
            writeShortLE(baos, checkedS16(c, "\\$PROJETIL_ANIM_ACERTAR_GLOBAL:\\s*(-?[\\d.]+)", golpeLabel + "/$PROJETIL_ANIM_ACERTAR_GLOBAL", reporter));
            writeShortLE(baos, checkedS16(c, "\\$PROJETIL_COLISAO_CHAO:\\s*(-?[\\d.]+)",        golpeLabel + "/$PROJETIL_COLISAO_CHAO", reporter));
            writeShortLE(baos, checkedS16(c, "\\$SUBPROJETIL_COLISAO_CHAO:\\s*(-?[\\d.]+)",     golpeLabel + "/$SUBPROJETIL_COLISAO_CHAO", reporter));
            writeShortLE(baos, checkedS16(c, "\\$DESCONHECIDO_44:\\s*(-?[\\d.]+)",              golpeLabel + "/$DESCONHECIDO_44", reporter));
            writeShortLE(baos, checkedS16(c, "\\$DESCONHECIDO_46:\\s*(-?[\\d.]+)",              golpeLabel + "/$DESCONHECIDO_46", reporter));
        }
        if (found == 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .severity(CompileError.Severity.WARNING)
                    .fieldTag("$GOLPE_N")
                    .hdatIndex(4)
                    .message("$ATRIBUTOS_DE_GOLPE não contém nenhum $GOLPE_N reconhecível — verifique chaves balanceadas"));
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 5 — $PONTUACAO_POR_GOLPE
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT5(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $PONTUACAO_POR_GOLPE:{\n");
        w.write("      // Pontos adicionados ao placar ao acertar com cada animação especial.\n");
        w.write("      // Indexado por animação: $ANO_27 = índice 0, ..., $ANO_66 = índice 39.\n");
        w.write("      // 0 = sem pontuação para esse golpe.\n\n");

        for (int i = 0; i < 40; i++) {
            int ptr = i * 2;
            int value = (ptr + 1 < data.length)
                ? (data[ptr] & 0xFF) | ((data[ptr + 1] & 0xFF) << 8)
                : 0;
            w.write(String.format("         $ANO_%d: %d,\n", i + 27, value));
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT5(String block, int xsize, int ysize, ErrorReporter reporter) {
        byte[] data = new byte[80]; // sempre 40 shorts, independente dos parâmetros

        Pattern p = Pattern.compile("\\$ANO_(\\d+):\\s*(-?[\\d.]+)");
        Matcher m = p.matcher(block);
        while (m.find()) {
            int ano = Integer.parseInt(m.group(1));
            Integer value = checkedRawGroup(m.group(2), "$ANO_" + ano, 5, 0, 65535, reporter, m.group());
            int i = ano - 27;
            if (i < 0 || i >= 40) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .severity(CompileError.Severity.WARNING)
                        .fieldTag("$ANO_" + ano)
                        .hdatIndex(5)
                        .message("$ANO_" + ano + " está fora do range válido (27-66) para $PONTUACAO_POR_GOLPE — ignorado"));
                continue;
            }
            int v = (value != null) ? value : 0;
            int ptr = i * 2;
            data[ptr]     = (byte) (v & 0xFF);
            data[ptr + 1] = (byte) ((v >> 8) & 0xFF);
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 6 — $SEQUENCIAS_IA
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT6(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $SEQUENCIAS_IA:{\n");
        w.write("      // Tabela de sequências de input da CPU (HDAT_6).\n\n");

        for (int i = 0; i < ysize; i++) {
            int rowStart = i * xsize;
            if (rowStart >= data.length) break;

            w.write("         $SEQ_" + i + ":{\n");
            for (int step = 0; step < 4; step++) {
                int pAct  = rowStart + step * 2;
                int pTime = rowStart + step * 2 + 1;
                int act   = (pAct  < data.length) ? data[pAct]  & 0xFF : 0;
                int time  = (pTime < data.length) ? data[pTime] & 0xFF : 0;
                w.write("            {$ACT:" + act + ", $ACT_TIME:" + time + "}");
                if (step < 3) w.write(",");
                w.write("\n");
            }

            if (xsize >= 10) {
                int continua = (rowStart + 8 < data.length) ? data[rowStart + 8] & 0xFF : 0;
                int exigeCan = (rowStart + 9 < data.length) ? data[rowStart + 9] & 0xFF : 0;
                w.write("            $CONTINUA:" + continua + ", $EXIGE_CANCEL:" + exigeCan + "\n");
            }

            w.write("         },\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT6(String block, int xsize, ErrorReporter reporter) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int maxSeq = 0;
        Matcher mCount = Pattern.compile("\\$SEQ_(\\d+)").matcher(block);
        while (mCount.find()) {
            maxSeq = Math.max(maxSeq, Integer.parseInt(mCount.group(1)));
        }

        for (int i = 0; i <= maxSeq; i++) {
            byte[] row = new byte[xsize];

            String content = extractBlock(block, "\\$SEQ_" + i);
            if (content != null) {
                Pattern pStep = Pattern.compile(
                    "\\{\\s*\\$ACT\\s*:\\s*(-?[\\d.]+)\\s*,\\s*\\$ACT_TIME\\s*:\\s*(-?[\\d.]+)\\s*\\}");
                Matcher mStep = pStep.matcher(content);

                List<int[]> steps = new ArrayList<int[]>();
                while (mStep.find() && steps.size() < 4) {
                    Integer act  = checkedRawGroup(mStep.group(1), "$SEQ_" + i + "/$ACT",      6, 0, 255, reporter, mStep.group());
                    Integer time = checkedRawGroup(mStep.group(2), "$SEQ_" + i + "/$ACT_TIME", 6, 0, 255, reporter, mStep.group());
                    steps.add(new int[]{ act != null ? act : 0, time != null ? time : 0 });
                }

                for (int step = 0; step < steps.size(); step++) {
                    row[step * 2]     = (byte) (steps.get(step)[0] & 0xFF);
                    row[step * 2 + 1] = (byte) (steps.get(step)[1] & 0xFF);
                }

                if (xsize >= 10) {
                    Integer cont = extractOptionalChecked(content, "\\$CONTINUA\\s*:\\s*(-?[\\d.]+)", "$SEQ_" + i + "/$CONTINUA", 6, 0, 255, reporter);
                    row[8] = (byte) ((cont != null ? cont : 0) & 0xFF);

                    Integer canc = extractOptionalChecked(content, "\\$EXIGE_CANCEL\\s*:\\s*(-?[\\d.]+)", "$SEQ_" + i + "/$EXIGE_CANCEL", 6, 0, 1, reporter);
                    row[9] = (byte) ((canc != null ? canc : 0) & 0xFF);
                }
            } else {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .severity(CompileError.Severity.WARNING)
                        .fieldTag("$SEQ_" + i)
                        .hdatIndex(6)
                        .message("$SEQ_" + i + " foi referenciado, mas o bloco correspondente não foi encontrado (chaves desbalanceadas?)"));
            }
            baos.write(row);
        }
        return baos.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 13 — $ROTEIRO_CUTSCENE
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT13(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        // Comentários e leitura COM SINAL vindos do hdat13.java mais novo do
        // gameCompiler: o TCBManajer (modo 996) lê todos os campos como short
        // ((short) ...), então -1 aparece como -1 e não 65535.
        w.write("      $ROTEIRO_CUTSCENE:{\n");
        w.write("         // Ativado por hitbox flag=20. O 3º valor da hitbox (COORDS) é o índice do evento.\n");
        w.write("         // $ALVO: 0=effect.pac | 1=char.pac do player | outro valor=ranura 2\n");
        w.write("         // $DURACAO: 0 ou valor negativo = até mudar a animação; N positivo = limite de frames\n");
        w.write("         // $Z_LAYER: 0=prioridade 200 (eventos 8/9: 10); 1=17 (evento 8: 19; evento 9: 21); outro=400\n");
        w.write("         // $ANIM_ID: animação virado para a direita (0=sem animação); $ANIM_FLIP: virado para a esquerda\n");
        w.write("         // $CAM_MODE: 0=câmera normal | diferente de 0=origem MID em 0,0; 1 também ativa tela cheia\n");
        w.write("         //            Eventos 8 e 9 são posicionados em MID 0,0 independentemente de $CAM_MODE.\n");
        w.write("         // $TRAVAR_VIT: diferente de 0 congela o oponente no estado 925\n");
        w.write("         // $FLAG: 0=nada | 1=PlayerState do atacante | 2=PlayerState do oponente se Life<=0\n");
        w.write("         //        3=scroll até o atacante | 4=desativa seguimento de câmera | 5=foca o oponente | 6=parede na borda\n");
        w.write("         //        7=oponente atravessa bordas | 8=atacante atravessa bordas | 9=ambos atravessam bordas\n");
        w.write("         //        10-12=teleporte do oponente | 13-15=teleporte do atacante\n\n");

        for (int i = 0; i < ysize; i++) {
            int ptr = i * 2 * xsize;
            if (ptr + 15 >= data.length) break;

            int alvo      = readS16(data, ptr +  0);
            int duracao   = readS16(data, ptr +  2);
            int zLayer    = readS16(data, ptr +  4);
            int animId    = readS16(data, ptr +  6);
            int animFlip  = readS16(data, ptr +  8);
            int camMode   = readS16(data, ptr + 10);
            int travarVit = readS16(data, ptr + 12);
            int mapEvt    = readS16(data, ptr + 14);

            w.write(String.format(
                "         $EVT_%02d: { $ALVO:%d, $DURACAO:%d, $Z_LAYER:%d, $ANIM_ID:%d, $ANIM_FLIP:%d, $CAM_MODE:%d, $TRAVAR_VIT:%d, $FLAG:%d },\n",
                i, alvo, duracao, zLayer, animId, animFlip, camMode, travarVit, mapEvt));
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT13(String block, int xsize, int ysize, ErrorReporter reporter) {
        byte[] data = new byte[ysize * 2 * xsize];

        Pattern p = Pattern.compile(
            "\\$EVT_(\\d+):\\s*\\{\\s*\\$ALVO:(-?[\\d.]+),\\s*\\$DURACAO:(-?[\\d.]+),\\s*\\$Z_LAYER:(-?[\\d.]+)," +
            "\\s*\\$ANIM_ID:(-?[\\d.]+),\\s*\\$ANIM_FLIP:(-?[\\d.]+),\\s*\\$CAM_MODE:(-?[\\d.]+)," +
            "\\s*\\$TRAVAR_VIT:(-?[\\d.]+),\\s*\\$FLAG:(-?[\\d.]+)\\s*\\}");
        Matcher m = p.matcher(block);

        int matched = 0;
        while (m.find()) {
            matched++;
            int i = Integer.parseInt(m.group(1));
            if (i >= ysize) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .severity(CompileError.Severity.WARNING)
                        .fieldTag("$EVT_" + i)
                        .hdatIndex(13)
                        .message("$EVT_" + i + " está além do YSIZE (" + ysize + ") deste bloco — ignorado"));
                continue;
            }
            int ptr = i * 2 * xsize;
            if (ptr + 15 >= data.length) continue;

            String label = "$EVT_" + i;
            writeShortAt(data, ptr +  0, checkedRawGroup(m.group(2), label + "/$ALVO",       13, -32768, 65535, reporter, m.group()));
            writeShortAt(data, ptr +  2, checkedRawGroup(m.group(3), label + "/$DURACAO",    13, -32768, 65535, reporter, m.group()));
            writeShortAt(data, ptr +  4, checkedRawGroup(m.group(4), label + "/$Z_LAYER",    13, -32768, 65535, reporter, m.group()));
            writeShortAt(data, ptr +  6, checkedRawGroup(m.group(5), label + "/$ANIM_ID",    13, -32768, 65535, reporter, m.group()));
            writeShortAt(data, ptr +  8, checkedRawGroup(m.group(6), label + "/$ANIM_FLIP",  13, -32768, 65535, reporter, m.group()));
            writeShortAt(data, ptr + 10, checkedRawGroup(m.group(7), label + "/$CAM_MODE",   13, -32768, 65535, reporter, m.group()));
            writeShortAt(data, ptr + 12, checkedRawGroup(m.group(8), label + "/$TRAVAR_VIT", 13, -32768, 65535, reporter, m.group()));
            writeShortAt(data, ptr + 14, checkedRawGroup(m.group(9), label + "/$FLAG",       13, -32768, 65535, reporter, m.group()));
        }

        // Deteta $EVT_ presentes no texto que não bateram no regex inteiro (ex: campo faltando,
        // vírgula extra) — esses somem silenciosamente do while(m.find()) acima sem isto.
        int declaredCount = countOccurrences(block, "$EVT_");
        if (declaredCount > matched) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$EVT_N")
                    .hdatIndex(13)
                    .message("$ROTEIRO_CUTSCENE declara " + declaredCount + " ocorrência(s) de $EVT_, mas apenas "
                            + matched + " puderam ser interpretadas — verifique campos faltando ou sintaxe do bloco"));
        }

        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 14-33 — $REACAO_DE_DANO_{N} (aéreo/throw, MODO 877, char.pac)
    ══════════════════════════════════════════════════════════════════ */

    private static final int DANO_STRIDE = 14;
    private static final Pattern DANO_TAG = Pattern.compile("\\$REACAO_(?:DE_)?DANO_\\d");

    private static void exHDATDano(BufferedWriter w, byte[] data, int xsize, int ysize, int damageType) throws IOException {
        w.write("      $REACAO_DANO_" + damageType + ":{\n");

        if (ysize == 0 || data == null || data.length == 0) {
            w.write("      // nula — este personagem não define a reação " + damageType + "\n");
            w.write("      },\n");
            return;
        }

        if (damageType == 0) {
            w.write("      // $FR_N passo | $DUR_C/$DUR_A duração chão/ar (255=indefinido) | $ANO animação\n");
            w.write("      // $XSPEED/$YSPEED/$XACCEL/$YACCEL física no chão\n");
            w.write("      // $XSPEED_A/$YSPEED_A/$XACCEL_A/$YACCEL_A física no ar (ou posição relativa em agarre)\n");
            w.write("      // $COMP próxima reação ao tocar chão/parede | $WALL borda | $VOZ som\n\n");
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

    private static byte[] bdHDATDano(String block, ErrorReporter reporter) {
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
       HDAT 34 — $DISPUTA_DE_PODER
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT34(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $DISPUTA_DE_PODER:{\n");
        w.write("         // A disputa inicia quando ambos estão na animação gatilho com ki >= 100%.\n\n");

        int anim = (data.length > 0) ? data[0] & 0xFF : 0;
        int cena = (data.length > 1) ? data[1] & 0xFF : 0;

        w.write(String.format("         $ANIM_GATILHO: %d,\n", anim));
        w.write("         // Animação que ativa a disputa (ex: pose de carregamento do especial).\n\n");
        w.write(String.format("         $TEM_CUTSCENE: %d,\n", cena));
        w.write("         // 0 = resultado simples. 1+ = sequência cinemática ao terminar.\n");
        w.write("      },\n");
    }

    private static byte[] bdHDAT34(String block, int xsize, int ysize, ErrorReporter reporter) {
        byte[] data = new byte[xsize * ysize];

        Integer anim = extractOptionalChecked(block, "\\$ANIM_GATILHO:\\s*(-?[\\d.]+)", "$ANIM_GATILHO", 34, 0, 255, reporter);
        if (anim != null && data.length > 0) data[0] = (byte) (anim & 0xFF);

        Integer cena = extractOptionalChecked(block, "\\$TEM_CUTSCENE:\\s*(-?[\\d.]+)", "$TEM_CUTSCENE", 34, 0, 255, reporter);
        if (cena != null && data.length > 1) data[1] = (byte) (cena & 0xFF);

        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 35 — $DANO_DE_EVENTOS_CUTSCENE
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT35(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $DANO_DE_EVENTOS_CUTSCENE:{\n");
        w.write("         // $DANO: valor base antes dos multiplicadores de ATK/DEF. $ANIM: animação do evento (0=nenhuma).\n\n");

        int stride = xsize * 2;
        for (int i = 0; i < ysize; i++) {
            int ptr = i * stride;
            if (ptr + 3 >= data.length) break;

            int dano = readShortU(data, ptr + 0);
            int anim = readShortU(data, ptr + 2);

            w.write(String.format("         $LINHA_%d: { $DANO:%d, $ANIM:%d },\n", i, dano, anim));
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT35(String block, int xsize, int ysize, ErrorReporter reporter) {
        int stride = xsize * 2;
        byte[] data = new byte[stride * ysize];

        Pattern p = Pattern.compile("\\$LINHA_(\\d+):\\s*\\{\\s*\\$DANO:(-?[\\d.]+),\\s*\\$ANIM:(-?[\\d.]+)\\s*\\}");
        Matcher m = p.matcher(block);

        int matched = 0;
        while (m.find()) {
            matched++;
            int i = Integer.parseInt(m.group(1));
            if (i >= ysize) continue;

            String label = "$LINHA_" + i;
            Integer dano = checkedRawGroup(m.group(2), label + "/$DANO", 35, -32768, 65535, reporter, m.group());
            Integer anim = checkedRawGroup(m.group(3), label + "/$ANIM", 35, -32768, 65535, reporter, m.group());

            int ptr = i * stride;
            writeShortAt(data, ptr + 0, dano);
            writeShortAt(data, ptr + 2, anim);
        }

        int declared = countOccurrences(block, "$LINHA_");
        if (declared > matched) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag("$LINHA_N")
                    .hdatIndex(35)
                    .message("$DANO_DE_EVENTOS_CUTSCENE declara " + declared + " linha(s), mas apenas "
                            + matched + " foram interpretadas corretamente"));
        }

        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 36 — $SISTEMA_DE_VOZES
    ══════════════════════════════════════════════════════════════════ */

    private static final String[] VOZ_CAMPOS = {
        "$DESCONHECIDO_0", "$DESCONHECIDO_1", "$CUTSCENE_INICIO", "$CUTSCENE_HIT_1",
        "$CUTSCENE_HIT_2", "$CUTSCENE_HIT_3", "$CUTSCENE_HIT_4", "$CUTSCENE_POS_EVENTO",
        "$SELECAO_DE_CARTAS", "$DISPUTA_DE_KI", "$EVENTO_10",
        "$DESCONHECIDO_11", "$DESCONHECIDO_12", "$DESCONHECIDO_13", "$DESCONHECIDO_14", "$DESCONHECIDO_15",
    };

    private static void exHDAT36(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $SISTEMA_DE_VOZES:{\n");
        w.write("         // ID do som tocado em cada situação.\n");
        w.write("         // 0 = sem voz. 1-79 = som global (o jogo subtrai 1 antes de tocar). 80+ = voz do personagem.\n\n");

        for (int i = 0; i < ysize && i < VOZ_CAMPOS.length; i++) {
            int ptr = i * xsize;
            int val = (ptr < data.length) ? data[ptr] & 0xFF : 0;
            w.write(String.format("         %-26s %d,\n", VOZ_CAMPOS[i] + ":", val));
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT36(String block, int xsize, int ysize, ErrorReporter reporter) {
        byte[] data = new byte[xsize * ysize];

        for (int i = 0; i < VOZ_CAMPOS.length && i < ysize; i++) {
            String key = Pattern.quote(VOZ_CAMPOS[i]);
            Integer v = extractOptionalChecked(block, key + ":\\s*(-?[\\d.]+)", VOZ_CAMPOS[i], 36, 0, 255, reporter);
            if (v != null) {
                int ptr = i * xsize;
                if (ptr < data.length) data[ptr] = (byte) (v & 0xFF);
            }
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 37 — $DANO_NO_ESTADO_DE_FURIA
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT37(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        int value = (data != null && data.length > 0) ? data[0] & 0xFF : 0;
        w.write("      $DANO_NO_ESTADO_DE_FURIA: " + value + ",\n");
        w.write("      // Multiplicador de dano durante a fúria. 100 = dano normal, 150 = +50%.\n");
    }

    private static byte[] bdHDAT37(String block, int xsize, int ysize, ErrorReporter reporter) {
        byte[] data = new byte[xsize * ysize];
        Integer v = extractOptionalChecked(block, "\\$DANO_NO_ESTADO_DE_FURIA:\\s*(-?[\\d.]+)", "$DANO_NO_ESTADO_DE_FURIA", 37, 0, 255, reporter);
        if (v != null && data.length > 0) data[0] = (byte) (v & 0xFF);
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 38 — $CARTA_RECOMPENSA_ARCADE
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT38(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        int value = (data != null && data.length >= 2)
            ? (data[0] & 0xFF) | ((data[1] & 0xFF) << 8)
            : (data != null && data.length > 0 ? data[0] & 0xFF : 0);
        w.write("      $CARTA_RECOMPENSA_ARCADE: " + value + ",\n");
        w.write("      // Índice da carta recebida ao vencer o modo arcade com este personagem.\n");
    }

    private static byte[] bdHDAT38(String block, int xsize, int ysize, ErrorReporter reporter) {
        // O jogo lê um short (2 bytes) aqui; com $COL:1/$ROW:1 (tabela de short)
        // xsize*ysize dava 1 byte e o byte alto da carta se perdia.
        byte[] data = new byte[Math.max(2, xsize * ysize)];
        Integer v = extractOptionalChecked(block, "\\$CARTA_RECOMPENSA_ARCADE:\\s*(-?[\\d.]+)", "$CARTA_RECOMPENSA_ARCADE", 38, 0, 65535, reporter);
        if (v != null) {
            if (data.length >= 2) {
                data[0] = (byte) (v & 0xFF);
                data[1] = (byte) ((v >> 8) & 0xFF);
            } else if (data.length == 1) {
                data[0] = (byte) (v & 0xFF);
            }
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 39 — $CENARIO_PADRAO
    ══════════════════════════════════════════════════════════════════ */

    private static void exHDAT39(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        int value = (data != null && data.length > 0) ? data[0] & 0xFF : 0;
        w.write("      $CENARIO_PADRAO: " + value + ",\n");
        w.write("      // Arena carregada quando este personagem é o oponente.\n");
    }

    private static byte[] bdHDAT39(String block, ErrorReporter reporter) {
        byte[] data = new byte[1];
        Integer v = extractOptionalChecked(block, "\\$CENARIO_PADRAO:\\s*(-?[\\d.]+)", "$CENARIO_PADRAO", 39, 0, 255, reporter);
        if (v != null) data[0] = (byte) (v & 0xFF);
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 40 — $PROGRESSAO_CAMPANHA
    ══════════════════════════════════════════════════════════════════ */

    private static final String[] CAMPANHA_NOMES = { "ARCADE", "BOSS_CANDIDATOS", "DESCONHECIDO", "HISTORIA" };

    private static void exHDAT40(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $PROGRESSAO_CAMPANHA:{\n");
        w.write("      // Índices de personagem (valor - 1 = ID real). 0 = slot vazio.\n\n");

        for (int row = 0; row < ysize; row++) {
            String label = (row < CAMPANHA_NOMES.length) ? CAMPANHA_NOMES[row] : "LINHA_" + row;
            w.write("         $" + label + ": [");
            for (int col = 0; col < xsize; col++) {
                int idx = row * xsize + col;
                int v = (idx < data.length) ? data[idx] & 0xFF : 0;
                w.write(String.valueOf(v));
                if (col < xsize - 1) w.write(", ");
            }
            w.write("],\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT40(String block, int xsize, int ysize, ErrorReporter reporter) {
        return buildIndexedRows(block, xsize, ysize, CAMPANHA_NOMES, 40, reporter);
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 41 — $PERSONAGENS_DESBLOQUEAVEIS
    ══════════════════════════════════════════════════════════════════ */

    private static final String[] DESBLOQUEIO_NOMES = { "EASY", "NORMAL", "HARD" };

    private static void exHDAT41(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        w.write("      $PERSONAGENS_DESBLOQUEAVEIS:{\n");
        w.write("      // Personagens desbloqueados ao vencer o arcade. Valor = ID + 1. 0 = fim.\n\n");

        for (int diff = 0; diff < ysize; diff++) {
            String name = (diff < DESBLOQUEIO_NOMES.length) ? DESBLOQUEIO_NOMES[diff] : "DIFICULDADE_" + diff;
            int ptr = diff * xsize;
            w.write("         $" + name + ": [");
            for (int col = 0; col < xsize; col++) {
                int v = (ptr + col < data.length) ? data[ptr + col] & 0xFF : 0;
                w.write(String.valueOf(v));
                if (col < xsize - 1) w.write(", ");
            }
            w.write("],\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT41(String block, int xsize, int ysize, ErrorReporter reporter) {
        return buildIndexedRows(block, xsize, ysize, DESBLOQUEIO_NOMES, 41, reporter);
    }

    /** Compartilhado por HDAT 40 e 41 — mesma estrutura de "N linhas nomeadas, cada uma um array de bytes". */
    private static byte[] buildIndexedRows(String block, int xsize, int ysize, String[] nomes, int hdatIndex, ErrorReporter reporter) {
        byte[] data = new byte[xsize * ysize];

        for (int row = 0; row < ysize; row++) {
            String label = (row < nomes.length) ? nomes[row] : "LINHA_" + row;
            Pattern p = Pattern.compile("\\$" + label + ":\\s*\\[([^\\]]+)\\]");
            Matcher m = p.matcher(block);
            if (!m.find()) continue;
            String[] vals = m.group(1).split(",");
            if (vals.length > xsize) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .severity(CompileError.Severity.WARNING)
                        .fieldTag("$" + label)
                        .hdatIndex(hdatIndex)
                        .message("$" + label + " tem " + vals.length + " valores, mas XSIZE é " + xsize
                                + " — valores excedentes serão descartados"));
            }
            for (int col = 0; col < Math.min(vals.length, xsize); col++) {
                Integer v = checkedRawGroup(vals[col].trim(), "$" + label + "[" + col + "]", hdatIndex, 0, 255, reporter, m.group());
                if (v != null) data[row * xsize + col] = (byte) (v & 0xFF);
            }
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       Utilitários compartilhados por todos os exHDAT/bdHDAT desta classe
    ══════════════════════════════════════════════════════════════════ */

    private static short readS16(byte[] data, int ptr) {
        if (ptr + 1 >= data.length) return 0;
        return (short) ((data[ptr] & 0xFF) | ((data[ptr + 1] & 0xFF) << 8));
    }

    private static int readShortU(byte[] data, int ptr) {
        if (ptr + 1 >= data.length) return 0;
        return (data[ptr] & 0xFF) | ((data[ptr + 1] & 0xFF) << 8);
    }

    private static void writeShortAt(byte[] data, int ptr, Integer value) {
        if (value == null || ptr + 1 >= data.length) return;
        data[ptr]     = (byte) (value & 0xFF);
        data[ptr + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private static int readUByte(byte[] data, int ptr) {
        if (ptr >= data.length) return 0;
        return data[ptr] & 0xFF;
    }

    private static int readSByte(byte[] data, int ptr) {
        if (ptr >= data.length) return 0;
        return (byte) (data[ptr] & 0xFF);
    }

    private static void writeShortLE(OutputStream os, Integer v) throws IOException {
        int value = (v != null) ? v : 0;
        os.write(value & 0xFF);
        os.write((value >> 8) & 0xFF);
    }

    private static String decodeWithFallback(byte[] chunk) {
        if (chunk == null || chunk.length == 0) return "";
        try {
            CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return utf8.decode(ByteBuffer.wrap(chunk)).toString();
        } catch (Exception e) {
            return new String(chunk, Charset.forName("Shift_JIS"));
        }
    }

    /**
     * Extrai o conteúdo entre { } de uma chave, respeitando aninhamento de chaves.
     * Conta profundidade caractere por caractere — necessário porque um regex lazy
     * (.+?) pararia na primeira "}" interna (ex: fecho de um sub-bloco), não na do
     * bloco pedido.
     */
    private static String extractBlock(String src, String key) {
        Pattern p = Pattern.compile(key + "\\s*:\\s*\\{");
        Matcher m = p.matcher(src);
        if (!m.find()) return null;
        int start = m.end();
        int depth = 1;
        for (int i = start; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return src.substring(start, i);
        }
        return null; // chaves desbalanceadas no arquivo
    }

    /** Extrai conteúdo entre [ ] de uma chave (sem colchetes aninhados, regex lazy é seguro). */
    private static String extractBracket(String src, String key) {
        Pattern p = Pattern.compile(key + "\\s*:\\s*\\[(.+?)\\]");
        Matcher m = p.matcher(src);
        return m.find() ? m.group(1) : null;
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /* ── Família de validação: parse + range check, reportando erro sem abortar ── */

    /** Variante "Nx" usada por $CHAO/$AR (hdat0) — mantém 0 como fallback em vez de null, já era o comportamento original. */
    private static int extractNivelChecked(String src, int n, String fieldPath, int hdatIndex, ErrorReporter reporter) {
        if (src == null) return 0;
        Pattern p = Pattern.compile("N" + n + "\\s*:\\s*(-?[\\d.]+)");
        Matcher m = p.matcher(src);
        if (!m.find()) return 0;
        Integer v = checkedRawGroup(m.group(1), fieldPath + "/N" + n, hdatIndex, 0, 255, reporter, m.group());
        return (v != null) ? v : 0;
    }

    /** Busca uma tag opcional; se ausente, retorna null (não reporta erro — ausência é normal). Se presente mas inválida, reporta. */
    private static Integer extractOptionalChecked(String src, String regex, String fieldTag, int hdatIndex, int min, int max, ErrorReporter reporter) {
        Matcher m = Pattern.compile(regex).matcher(src);
        if (!m.find()) return null;
        return checkedRawGroup(m.group(1), fieldTag, hdatIndex, min, max, reporter, m.group());
    }

    /** Busca uma tag obrigatória num contexto que já reconheceu o campo (regex já casou); valida e retorna 0 se ausente/inválida. */
    private static int checked(String src, String regex, String fieldTag, int hdatIndex, int min, int max, ErrorReporter reporter) {
        Matcher m = Pattern.compile(regex).matcher(src);
        if (!m.find()) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag(fieldTag)
                    .hdatIndex(hdatIndex)
                    .message(fieldTag + " não encontrado no bloco"));
            return 0;
        }
        Integer v = checkedRawGroup(m.group(1), fieldTag, hdatIndex, min, max, reporter, m.group());
        return (v != null) ? v : 0;
    }

    private static Integer checkedS16(String src, String regex, String fieldTag, ErrorReporter reporter) {
        return extractOptionalChecked(src, regex, fieldTag, 4, -32768, 65535, reporter);
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
     * @return o valor interpretado (mesmo fora de range — quem chama decide se usa
     *         truncado), ou null se nem foi possível interpretar como número.
     */
    private static Integer checkedRawGroup(String raw, String fieldTag, int hdatIndex, int min, int max, ErrorReporter reporter, String rawLine) {
        String trimmed = raw.trim();

        if (trimmed.contains(".")) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag(fieldTag)
                    .hdatIndex(hdatIndex)
                    .rawLine(rawLine)
                    .message(fieldTag + " espera um inteiro, mas recebeu \"" + trimmed + "\" (valor decimal)"));
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

        if (value < min || value > max) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .fieldTag(fieldTag)
                    .hdatIndex(hdatIndex)
                    .rawLine(rawLine)
                    .message(fieldTag + " = " + value + " está fora do range esperado [" + min + ".." + max
                            + "] — será truncado ao gravar no binário"));
        }

        return value;
    }
}
