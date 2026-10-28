package com.zue.gd.util;

import java.io.*;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.Arrays;
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
            case 0:  return exHDAT0(w, data, xsize, ysize);
            case 1:  exHDAT1(w, data, xsize, ysize); return true;
            case 2:  exHDAT2(w, data, xsize, ysize); return true;
            case 3:  exHDAT3(w, data, xsize, ysize); return true;
            case 4:  exHDAT4(w, data, xsize, ysize); return true;
            case 5:  exHDAT5(w, data, xsize, ysize); return true;
            case 6:  return exHDAT6(w, data, xsize, ysize);
            case 7:  return exHDAT7(w, data, xsize, ysize);
            case 13: exHDAT13(w, data, xsize, ysize); return true;
            case 34: exHDAT34(w, data, xsize, ysize); return true;
            case 35: exHDAT35(w, data, xsize, ysize); return true;
            case 36: exHDAT36(w, data, xsize, ysize); return true;
            case 37: exHDAT37(w, data, xsize, ysize); return true;
            case 38: exHDAT38(w, data, xsize, ysize); return true;
            case 39: exHDAT39(w, data, xsize, ysize); return true;
            case 40: exHDAT40(w, data, xsize, ysize); return true;
            case 41: exHDAT41(w, data, xsize, ysize); return true;
            case 42: return exHDAT42(w, data, xsize, ysize);
            // 43-45: mesma estrutura do HDAT 2, nos outros idiomas
            // (TCBManajer.CharData_TextPage = {2, 43, 44, 45}, índice = iLanguage).
            case 43: case 44: case 45: exHDAT2(w, data, xsize, ysize); return true;
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
        if (block.contains("$COMPORTAMENTO_IA"))        return bdHDAT0(block, xsize, ysize, reporter);
        if (block.contains("$REGENERACAO_KI"))           return bdHDAT1(block, reporter);
        if (block.contains("$TEXTOS_DO_PERSONAGEM"))     return bdHDAT2(block);
        if (block.contains("$STATUS_BASE_E_FISICA"))     return bdHDAT3(block, reporter);
        if (block.contains("$ATRIBUTOS_DE_GOLPE"))       return bdHDAT4(block, xsize, reporter);
        if (block.contains("$PONTUACAO_POR_GOLPE"))      return bdHDAT5(block, xsize, ysize, reporter);
        if (block.contains("$SEQUENCIAS_IA"))            return bdHDAT6(block, xsize, ysize, reporter);
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
        if (block.contains("$CRESCIMENTO_POR_NIVEL"))    return bdHDAT7(block, xsize, ysize, reporter);
        if (block.contains("$COMANDOS_TOUCH"))           return bdHDAT42(block, xsize, ysize, reporter);
        return null;
    }

    /* ══════════════════════════════════════════════════════════════════
       Tabelas simples (HdatTabela) — só para HDAT 7/42 com tamanho fora do padrão
    ══════════════════════════════════════════════════════════════════ */

    private static final String[] DOC_HDAT7 = {
        "Crescimento por nível: linha = nível (0-98). Stat no nível N = base do HDAT 3 + soma das linhas 0..N.",
        "Col 0 = HP (GetHP, base [11])   col 1 = ATK (GetAttack, base [12])   col 2 = KI/Synchro (GetSynchro, base [8] x 100)",
        "Col 3 = DEF (GetDeffence, base [14])   col 21 = custo de cartas (GetCost, base [17]). Demais colunas não são lidas.",
    };
    private static final String[] DOC_HDAT42 = {
        "Comandos de toque (Game8, modo 38). Tabela de short; linha = situação (distância/estado)",
        "+ 20 quando Synchro >= 100. Col 0 = golpe disparado (0 = nenhum), col 1 = custo de Synchro (x 100).",
        "A linha seguinte (N + 1) é a do comando de continuação.",
    };

    /* ══════════════════════════════════════════════════════════════════
       HDAT 0 — $COMPORTAMENTO_IA   (byte, 16 × 45; TCBManajer.Game7 modo 952)

       linha  = situação × 4 + CPURand % 4   (11 situações × 4 variações = 0-43)
       coluna = CPULevel (0-7), + 8 quando a CPU está no ar
       valor  = CPUSeqNo, a sequência do HDAT 6 que a CPU executa
       linha 44 = tempo de reação: a CPU espera CPURand % valor frames antes
       de começar a sequência (0 = sem espera).

       A situação olha para o OPONENTE: projétil chegando (iRangeBulletGuard)
       e "atacando" só para a CPU do jogador 2; depois a distância (iDistNear/
       iDistMiddle e as versões Air, HDAT 2 do gamedata) e a altura (no chão,
       no ar a menos de 80 px do chão, ou a 80 px ou mais: y <= 160).
    ══════════════════════════════════════════════════════════════════ */

    private static final String[] SITUACOES = {
        "OPONENTE_CHAO_PERTO", "OPONENTE_CHAO_MEDIO", "OPONENTE_CHAO_LONGE",
        "OPONENTE_AR_BAIXO_PERTO", "OPONENTE_AR_BAIXO_MEDIO", "OPONENTE_AR_BAIXO_LONGE",
        "PROJETIL_CHEGANDO", "OPONENTE_ATACANDO",
        "OPONENTE_AR_ALTO_PERTO", "OPONENTE_AR_ALTO_MEDIO", "OPONENTE_AR_ALTO_LONGE"
    };
    /** Nomes do extractor antigo (que ficava desligado) — o build ainda aceita. */
    private static final String[] SITUACOES_ANTIGAS = {
        "PROXIMO_CHAO", "MEDIO_CHAO", "LONGE_CHAO", "PROXIMO_AR_BAIXO",
        "MEDIO_AR_BAIXO", "LONGE_AR", "PROJETIL_VINDO", "ATAQUE_RECEBIDO",
        "PROXIMO_AR_ALTO", "MEDIO_AR_ALTO", "LONGE_AR_ALTO"
    };
    private static final String[] SITUACOES_DOC = {
        "linhas 0-3: oponente no chão, perto",
        "linhas 4-7: oponente no chão, distância média",
        "linhas 8-11: oponente no chão, longe",
        "linhas 12-15: oponente no ar a menos de 80 px do chão, perto",
        "linhas 16-19: oponente no ar a menos de 80 px do chão, distância média",
        "linhas 20-23: oponente no ar a menos de 80 px do chão, longe",
        "linhas 24-27: um projétil do oponente está chegando (só a CPU do jogador 2)",
        "linhas 28-31: o oponente está atacando (só a CPU do jogador 2)",
        "linhas 32-35: oponente a 80 px ou mais do chão, perto",
        "linhas 36-39: oponente a 80 px ou mais do chão, distância média",
        "linhas 40-43: oponente a 80 px ou mais do chão, longe",
    };

    private static boolean exHDAT0(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        if (xsize != 16 || ysize != 45 || data.length != 16 * 45) return false; // layout diferente → $RAW

        w.write("      $COMPORTAMENTO_IA:{\n");
        w.write("         // Decisões da CPU. Ver #HELP/binChar.md (HDAT 0).\n");
        w.write("         // A cada decisão a CPU vê em que situação está o OPONENTE, sorteia uma das 4\n");
        w.write("         // variações (VAR_0 a VAR_3) e executa a sequência do HDAT 6 com esse número.\n");
        w.write("         // Cada lista tem um valor por nível da CPU (0 = mais fraca, 7 = mais forte):\n");
        w.write("         // CPU_NO_CHAO vale com a CPU no chão; CPU_NO_AR, com a CPU no ar.\n");
        w.write("         // Perto / médio / longe: distâncias do HDAT 2 do gamedata.\n");
        w.write("         // O jogo troca a sequência sozinho: barra de fúria cheia = 55; 7 e 20 sem a fúria\n");
        w.write("         // ativa = 8; número maior que o último SEQ do HDAT 6 = 8.\n\n");

        char[] h = new char[131];
        Arrays.fill(h, ' ');
        poe(h, 12, "// CPU no chão, nível:");
        poe(h, 77, "no ar, nível:");
        for (int i = 0; i < 8; i++) {
            poe(h, 37 + 5 * i, String.format("%3d", i));
            poe(h, 91 + 5 * i, String.format("%3d", i));
        }
        String cab = new String(h).replaceAll("\\s+$", "");

        for (int sit = 0; sit < 11; sit++) {
            w.write("         // " + SITUACOES_DOC[sit] + "\n");
            w.write("         $" + SITUACOES[sit] + ":{\n");
            w.write(cab + "\n");
            for (int var = 0; var < 4; var++) {
                int row = sit * 4 + var;
                w.write("            $VAR_" + var + ": { $CPU_NO_CHAO: " + listaBytes(data, row * 16, 8)
                        + ", $CPU_NO_AR: " + listaBytes(data, row * 16 + 8, 8) + " },\n");
            }
            w.write("         },\n");
        }
        w.write("\n         // Tempo de reação (linha 44): antes de cada sequência a CPU espera um número\n");
        w.write("         // sorteado de 0 até (valor - 1) frames. 0 = sem espera. Maior = CPU mais lenta.\n");
        w.write("         $TEMPO_DE_REACAO: { $CPU_NO_CHAO: " + listaBytes(data, 44 * 16, 8)
                + ", $CPU_NO_AR: " + listaBytes(data, 44 * 16 + 8, 8) + " },\n");
        w.write("      },\n");
        return true;
    }

    private static byte[] bdHDAT0(String block, int xsize, int ysize, ErrorReporter reporter) {
        if (xsize <= 0 || ysize <= 0) { xsize = 16; ysize = 45; }
        byte[] data = new byte[xsize * ysize];

        for (int sit = 0; sit < 11; sit++) {
            String nome = SITUACOES[sit];
            String sitBlock = extractBlock(block, "\\$" + nome);
            if (sitBlock == null) {
                nome = SITUACOES_ANTIGAS[sit];
                sitBlock = extractBlock(block, "\\$" + nome);
            }
            if (sitBlock == null) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .fieldTag("$" + SITUACOES[sit])
                        .hdatIndex(0)
                        .message("$COMPORTAMENTO_IA está sem o bloco $" + SITUACOES[sit] + " — situação ficará zerada"));
                continue;
            }
            for (int var = 0; var < 4; var++) {
                String varBlock = extractBlock(sitBlock, "\\$VAR_" + var);
                if (varBlock == null) continue;
                int row = sit * 4 + var;
                String path = "$" + nome + "/$VAR_" + var;
                gravarNiveis(data, row * xsize, varBlock, "CPU_NO_CHAO", "CHAO", path, reporter);
                gravarNiveis(data, row * xsize + 8, varBlock, "CPU_NO_AR", "AR", path, reporter);
            }
        }

        String reacao = extractBlock(block, "\\$TEMPO_DE_REACAO");
        if (reacao == null) reacao = extractBlock(block, "\\$POOL_DE_SEQUENCIAS"); // nome antigo
        if (reacao != null && 44 * xsize + 16 <= data.length) {
            gravarNiveis(data, 44 * xsize, reacao, "CPU_NO_CHAO", "CHAO", "$TEMPO_DE_REACAO", reporter);
            gravarNiveis(data, 44 * xsize + 8, reacao, "CPU_NO_AR", "AR", "$TEMPO_DE_REACAO", reporter);
        }
        return data;
    }

    /** Grava "$NOME: [v0, ..., v7]" (ou o antigo "$NOME_ANTIGO:[N0:v, ...]") em data[off..off+7]. */
    private static void gravarNiveis(byte[] data, int off, String src, String nome, String nomeAntigo,
                                     String path, ErrorReporter reporter) {
        String lst = extractBracket(src, "\\$" + nome);
        String usado = nome;
        if (lst == null) {
            lst = extractBracket(src, "\\$" + nomeAntigo);
            usado = nomeAntigo;
        }
        if (lst == null) return;
        String[] partes = lst.split(",");
        for (int i = 0; i < 8 && i < partes.length; i++) {
            String p = partes[i].trim();
            int c = p.indexOf(':');
            if (c >= 0) p = p.substring(c + 1).trim(); // formato antigo "N3: 5"
            if (p.length() == 0 || off + i >= data.length) continue;
            Integer v = checkedRawGroup(p, path + "/$" + usado + "[" + i + "]", 0, 0, 255, reporter, lst);
            if (v != null) data[off + i] = (byte) (v & 0xFF);
        }
    }

    /** "[  1,   2, …]": n bytes sem sinal a partir de off, com 3 casas cada. */
    private static String listaBytes(byte[] data, int off, int n) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%3d", data[off + i] & 0xFF));
        }
        return sb.append(']').toString();
    }

    private static void poe(char[] linha, int col, String s) {
        for (int i = 0; i < s.length() && col + i < linha.length; i++) linha[col + i] = s.charAt(i);
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 7 — $CRESCIMENTO_POR_NIVEL   (byte, 22 × 99)

       GetHP / GetAttack / GetSynchro / GetDeffence / GetCost: stat = base do
       HDAT 3 + soma das linhas 0..Level (Level limitado a 98). O jogo mostra
       Level + 1, então a linha N é o ganho ao chegar no nível N + 1 (a linha
       0 já vale no nível 1). A tela de "subiu de nível" (modo 233) mostra a
       linha do nível novo: ATK e DEF × 1000, ki e custo como estão.
       Colunas: 0 HP, 1 ATK, 2 ki máximo, 3 DEF, 21 custo de cartas; 4-20 não
       são lidas.
    ══════════════════════════════════════════════════════════════════ */

    private static final int[] H7_COLS = { 0, 1, 2, 3, 21 };
    private static final String[] H7_NOMES = { "HP", "ATK", "KI", "DEF", "CUSTO" };

    private static boolean exHDAT7(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        if (xsize != 22 || ysize <= 0 || data.length != xsize * ysize)
            return HdatTabela.escrever(w, "$CRESCIMENTO_POR_NIVEL", DOC_HDAT7, data, xsize, ysize, false, null);

        w.write("      $CRESCIMENTO_POR_NIVEL:{\n");
        w.write("         // Quanto cada stat sobe ao chegar em cada nível (o nível que o jogo mostra).\n");
        w.write("         // Stat no nível N = base do HDAT 3 + a soma de NIVEL_1 até NIVEL_N.\n");
        w.write("         // O comentário de cada linha mostra essa soma (sem a base do HDAT 3).\n");
        w.write("         // HP: soma à vida.  ATK / DEF: cada ponto = +1% de dano / de redução de dano\n");
        w.write("         // (a tela mostra × 1000).  KI: soma ao ki máximo (100 = 1 barra).\n");
        w.write("         // CUSTO: soma ao custo máximo das cartas.  NAO_LIDOS: colunas 4-20, o jogo não lê.\n");
        w.write("         // Valores de 0 a 255. O nível máximo é 99 (linhas além dele não são usadas).\n\n");

        int[] soma = new int[H7_COLS.length];
        for (int l = 0; l < ysize; l++) {
            int p = l * xsize;
            StringBuilder sb = new StringBuilder("         ");
            sb.append(String.format("%-11s", "$NIVEL_" + (l + 1) + ":")).append("{ ");
            for (int k = 0; k < H7_COLS.length; k++) {
                int v = data[p + H7_COLS[k]] & 0xFF;
                soma[k] += v;
                sb.append('$').append(H7_NOMES[k]).append(": ").append(String.format("%3d", v)).append(", ");
            }
            sb.append("$NAO_LIDOS: [");
            for (int c = 4; c <= 20; c++) {
                if (c > 4) sb.append(", ");
                sb.append(data[p + c] & 0xFF);
            }
            sb.append("] },");
            if (l < 99) {
                sb.append("  // soma: HP +").append(soma[0]).append(", ATK +").append(soma[1])
                  .append(", KI +").append(soma[2]).append(", DEF +").append(soma[3])
                  .append(", CUSTO +").append(soma[4]);
            } else {
                sb.append("  // não usado: o nível máximo é 99");
            }
            w.write(sb.append('\n').toString());
        }
        w.write("      },\n");
        return true;
    }

    private static byte[] bdHDAT7(String block, int xsize, int ysize, ErrorReporter reporter) {
        if (!block.contains("$NIVEL_"))  // formato de tabela ($LINHA_N: [...])
            return HdatTabela.compilar(block, xsize, ysize, false, null, 7, reporter);
        if (xsize <= 0 || ysize <= 0) { xsize = 22; ysize = 99; }
        byte[] data = new byte[xsize * ysize];

        for (int l = 0; l < ysize; l++) {
            String tag = "$NIVEL_" + (l + 1);
            Matcher m = Pattern.compile("\\$NIVEL_" + (l + 1) + "\\s*:\\s*\\{([^}]*)\\}").matcher(block);
            if (!m.find()) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .severity(CompileError.Severity.WARNING)
                        .fieldTag(tag).hdatIndex(7)
                        .message(tag + " não encontrado — o nível fica sem crescimento (linha zerada)"));
                continue;
            }
            String linha = m.group(1);
            int p = l * xsize;
            for (int k = 0; k < H7_COLS.length; k++) {
                if (H7_COLS[k] >= xsize) continue;
                Integer v = extractOptionalChecked(linha, "\\$" + H7_NOMES[k] + "\\s*:\\s*(-?[\\d.]+)",
                        tag + "/$" + H7_NOMES[k], 7, 0, 255, reporter);
                if (v != null) data[p + H7_COLS[k]] = (byte) (v & 0xFF);
            }
            String resto = extractBracket(linha, "\\$NAO_LIDOS");
            if (resto != null) {
                String[] partes = resto.split(",");
                for (int c = 4, i = 0; c <= 20 && c < xsize && i < partes.length; c++, i++) {
                    String raw = partes[i].trim();
                    if (raw.length() == 0) continue;
                    Integer v = checkedRawGroup(raw, tag + "/$NAO_LIDOS[" + i + "]", 7, 0, 255, reporter, resto);
                    if (v != null) data[p + c] = (byte) (v & 0xFF);
                }
            }
        }
        return data;
    }

    /* ══════════════════════════════════════════════════════════════════
       HDAT 42 — $COMANDOS_TOUCH   (short, 3 × 120; TCBManajer.Game8 modo 38)

       Toque no oponente (jogador humano). Linha = estado + distância + ki:
         estado:    0 = no chão, 40 = pulando (ANO 10-12), 80 = voando (PlayerAir)
         distância: |X| até o oponente: 0 = < 80 px, 5 = < 160, 10 = < 320, 15 = mais
         + 20 com ki (Synchro) >= 100
       Col 0 = sequência do HDAT 6 (0 = o toque não faz nada). As 4 linhas
       seguintes são as continuações do combo (_commandButtonTmp2): col 0 =
       sequência, col 1 = ki exigido × 100. Col 2 não é lida.
       Antes da tabela: barra de fúria cheia → ativa a fúria (código 200, seq.
       55); fúria ativa e ki >= 200 → código 201 (seq. 7).
    ══════════════════════════════════════════════════════════════════ */

    private static final String[] TOUCH_ESTADOS = { "CHAO", "PULO", "VOANDO" };
    private static final String[] TOUCH_ESTADOS_DOC = { "no chão", "pulando", "voando" };
    private static final String[] TOUCH_DIST = { "PERTO", "MEDIO", "LONGE", "MUITO_LONGE" };
    private static final String[] TOUCH_DIST_DOC = { "menos de 80 px", "80 a 159 px", "160 a 319 px", "320 px ou mais" };
    private static final String[] TOUCH_LINHAS = { "TOQUE", "COMBO_1", "COMBO_2", "COMBO_3", "COMBO_4" };

    /** Grupo g (0-23) = linhas g*5 .. g*5+4. */
    private static String grupoTouch(int g) {
        int base = g * 5, resto = base % 40;
        return TOUCH_ESTADOS[base / 40] + "_" + TOUCH_DIST[(resto % 20) / 5] + (resto >= 20 ? "_KI" : "");
    }

    private static boolean exHDAT42(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        if (xsize != 3 || ysize != 120 || data.length != 3 * 120 * 2)
            return HdatTabela.escrever(w, "$COMANDOS_TOUCH", DOC_HDAT42, data, xsize, ysize, true, null);

        w.write("      $COMANDOS_TOUCH:{\n");
        w.write("         // O que acontece quando o jogador TOCA no oponente (a até 48 px dele).\n");
        w.write("         // Cada grupo é uma situação: estado do personagem, distância em X até o\n");
        w.write("         // oponente e ki (_KI = com 100 de ki ou mais).\n");
        w.write("         // TOQUE: [sequência do HDAT 6, -, -]. 0 = o toque não faz nada.\n");
        w.write("         // COMBO_1 a COMBO_4: [sequência, ki exigido × 100, -]. Saem tocando de novo\n");
        w.write("         // (a até 88 px do oponente) durante a janela da hitbox tipo 2 do DAC, com o\n");
        w.write("         // oponente apanhando. Sequência 0 encerra o combo.\n");
        w.write("         // As colunas marcadas \"-\" não são lidas pelo jogo.\n");
        w.write("         // Antes da tabela: barra de fúria cheia = ativa a fúria (sequência 55);\n");
        w.write("         // fúria ativa e 200 de ki = sequência 7.\n\n");

        for (int g = 0; g < 24; g++) {
            int base = g * 5, resto = base % 40;
            w.write("         // linhas " + base + "-" + (base + 4) + ": " + TOUCH_ESTADOS_DOC[base / 40] + ", "
                    + TOUCH_DIST_DOC[(resto % 20) / 5] + ", ki " + (resto >= 20 ? ">= 100" : "< 100") + "\n");
            w.write("         $" + grupoTouch(g) + ":{\n");
            for (int k = 0; k < 5; k++) {
                int p = (base + k) * 6;
                w.write("            " + String.format("%-10s", "$" + TOUCH_LINHAS[k] + ":") + "["
                        + readS16(data, p) + ", " + readS16(data, p + 2) + ", " + readS16(data, p + 4) + "],\n");
            }
            w.write("         },\n");
        }
        w.write("      },\n");
        return true;
    }

    private static byte[] bdHDAT42(String block, int xsize, int ysize, ErrorReporter reporter) {
        if (!block.contains("$TOQUE"))  // formato de tabela ($LINHA_N: [...])
            return HdatTabela.compilar(block, xsize, ysize, true, null, 42, reporter);
        if (xsize <= 0 || ysize <= 0) { xsize = 3; ysize = 120; }
        byte[] data = new byte[xsize * ysize * 2];

        for (int g = 0; g < 24; g++) {
            String nome = grupoTouch(g);
            String grp = extractBlock(block, "\\$" + nome);
            if (grp == null) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .severity(CompileError.Severity.WARNING)
                        .fieldTag("$" + nome).hdatIndex(42)
                        .message("$" + nome + " não encontrado — o toque nessa situação não faz nada"));
                continue;
            }
            for (int k = 0; k < 5; k++) {
                int linha = g * 5 + k;
                if (linha >= ysize) break;
                String lst = extractBracket(grp, "\\$" + TOUCH_LINHAS[k]);
                if (lst == null) continue;
                String[] partes = lst.split(",");
                for (int c = 0; c < xsize && c < partes.length; c++) {
                    String raw = partes[c].trim();
                    if (raw.length() == 0) continue;
                    Integer v = checkedRawGroup(raw, "$" + nome + "/$" + TOUCH_LINHAS[k] + "[" + c + "]",
                            42, -32768, 65535, reporter, lst);
                    writeShortAt(data, (linha * xsize + c) * 2, v);
                }
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
            byte[] chunk = new byte[0];
            if (ptr + 2 <= len) {
                int strLen = ((data[ptr] & 0xFF) << 8) | (data[ptr + 1] & 0xFF);
                ptr += 2;
                if (strLen > 0) {
                    int available = Math.min(strLen, len - ptr);
                    chunk = new byte[available];
                    System.arraycopy(data, ptr, chunk, 0, available);
                    ptr += available;
                }
            }
            // mesmo formato do $T_N: "..." UTF-8, SJIS"..." Shift_JIS, \xHH byte cru
            w.write("         $LINHA_" + i + ": " + BinExtractor.quoteText(chunk) + ",\n");
        }
        w.write("      },\n");
    }

    private static byte[] bdHDAT2(String block) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Matcher m = Pattern.compile("\\$LINHA_\\d+:\\s*(SJIS)?" + BinBuilder.QUOTED_TEXT).matcher(block);
        Charset sjis = null;
        while (m.find()) {
            Charset cs = StandardCharsets.UTF_8;
            if (m.group(1) != null) {
                if (sjis == null) sjis = BinExtractor.shiftJis();
                if (sjis == null) throw new IOException("Shift_JIS não existe nesta plataforma ($LINHA_ com SJIS\"\")");
                cs = sjis;
            }
            byte[] b = BinBuilder.encodeText(m.group(2), cs);
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
        int stride = hdat4Stride(xsize);
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
            if (stride > 44) w.write("            // Campos sem função conhecida.\n");
            for (int off = 44; off + 1 < stride; off += 2) {
                w.write("            $DESCONHECIDO_" + off + ": " + readS16(data, base + off) + (off + 3 < stride ? ",\n" : "\n"));
            }
            w.write("         },\n");
        }
        w.write("      },\n");
    }

    /**
     * Bytes por golpe no HDAT 4 = 2 * $COL (TCBManajer lê golpe * 2 *
     * piGameDataXSize[4] + offset). Não é sempre 24 colunas: o char01 original
     * tem 23, e com 48 bytes fixos os golpes saíam desalinhados e os bytes do
     * fim se perdiam. Depois do offset 42 (último que o jogo lê) vêm
     * $DESCONHECIDO_44, _46... até o fim da linha.
     */
    private static int hdat4Stride(int xsize) {
        return xsize > 0 ? xsize * 2 : 48;
    }

    private static byte[] bdHDAT4(String block, int xsize, ErrorReporter reporter) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int stride = hdat4Stride(xsize);

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
            ByteArrayOutputStream baos = new ByteArrayOutputStream(stride);
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
            for (int off = 44; off + 1 < stride; off += 2) {
                writeShortLE(baos, checkedS16(c, "\\$DESCONHECIDO_" + off + ":\\s*(-?[\\d.]+)", golpeLabel + "/$DESCONHECIDO_" + off, reporter));
            }
            out.write(Arrays.copyOf(baos.toByteArray(), stride)); // $COL < 22: o que não cabe na linha fica de fora
        }
        if (found == 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .severity(CompileError.Severity.WARNING)
                    .fieldTag("$GOLPE_N")
                    .hdatIndex(4)
                    .message("$ATRIBUTOS_DE_GOLPE não contém nenhum $GOLPE_N reconhecível — verifique chaves balanceadas"));
        }
        return out.toByteArray();
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
       HDAT 6 — $SEQUENCIAS_IA   (byte, 10 colunas; TCBManajer modos 953-960)

       Uma linha por sequência (CPUSeqNo). Executada pela CPU (HDAT 0) e pelos
       toques do jogador (HDAT 42).
         col 0/2/4/6 = passo 1-4: comando + 1 (0 = fim da sequência)
         col 1/3/5/7 = frames com o comando apertado (0 vira 1, menos no "nada")
         col 8 = sequência seguinte (só a CPU do jogador 2, fora do modo 8)
         col 9 = 1: depois de um passo de golpe (tecla 16384/32768/65536/524288)
                 sem janela de cancelamento (CancelTime == 0), a sequência acaba
       Comando = índice em TCBManajer.SeqKey; o efeito de cada um saiu do
       CmdCheck (AICtrl) e dos modos 955/959/960/980/981. Comando >= 30 = nada.
    ══════════════════════════════════════════════════════════════════ */

    /** Nome curto de cada valor de passo (índice = valor gravado = comando + 1). */
    private static final String[] PASSO_NOMES = {
        "fim",
        "nada",
        "defender",
        "ataque",
        "especial 1",
        "andar p/ frente",
        "andar p/ trás",
        "especial 2",
        "super",
        "pular p/ frente",
        "pular",
        "pular p/ trás",
        "dash p/ frente",
        "dash p/ trás",
        "ataque + frente",
        "ataque + baixo",
        "especial 3",
        "ativar fúria",
        "ataque + trás",
        "golpe 16/17",
        "carregar ki",
        "ki + trás",
        "ki + frente",
        "investida p/ frente",
        "investida p/ trás",
        "levantar voo",
        "subida rápida",
        "descida rápida",
        "especial máximo",
        "investida p/ cima",
        "investida p/ baixo",
    };

    /** Explicação de cada valor, para o cabeçalho do bloco. */
    private static final String[] PASSO_DOC = {
        "fim da sequência",
        "nada: não aperta nada e a CPU escolhe de novo pelo HDAT 0 (use só na CPU)",
        "defender (enquanto o oponente ataca ou um projétil chega)",
        "ataque (ANO 30; pulando, ANO 34)",
        "especial 1 (golpe 0/3/6)",
        "andar / mover para frente",
        "andar / mover para trás",
        "especial 2 (golpe 1/4/7)",
        "super especial (golpe 9/10, 1 barra de ki)",
        "pular para frente (ANO 11)",
        "pular para cima (ANO 10)",
        "pular para trás (ANO 12)",
        "dash para frente (ANO 6; no ar, 8)",
        "dash para trás (ANO 7; no ar, 9)",
        "ataque + frente (ANO 31; pulando, 35)",
        "ataque + baixo (ANO 36; pulando, 38)",
        "especial 3 (golpe 2/5/8)",
        "ativar a fúria (golpe 14/15, barra de fúria cheia)",
        "ataque + trás (ANO 37; pulando, 39)",
        "golpe 16/17",
        "carregar ki (para sozinho com o ki cheio)",
        "ki + trás: golpe 18 virado para a direita, 19 para a esquerda",
        "ki + frente: golpe 19 virado para a direita, 18 para a esquerda",
        "investida para frente (ANO 2)",
        "investida para trás (ANO 3)",
        "levantar voo (só quem voa e está no chão)",
        "subida rápida (ANO 82, só quem voa; quem não voa faz o especial 1)",
        "descida rápida (ANO 83, só quem voa e está no ar; quem não voa faz o especial 1)",
        "especial máximo (golpe 11/12, 2 barras de ki)",
        "investida para cima (ANO 2)",
        "investida para baixo (ANO 2)",
    };

    private static String nomePasso(int v) {
        return v < PASSO_NOMES.length ? PASSO_NOMES[v] : "nada";
    }

    private static boolean exHDAT6(BufferedWriter w, byte[] data, int xsize, int ysize) throws IOException {
        if (xsize != 10 || ysize <= 0 || data.length != 10 * ysize) return false; // layout diferente → $RAW

        w.write("      $SEQUENCIAS_IA:{\n");
        w.write("         // Sequências de comandos da CPU (HDAT 0) e dos toques (HDAT 42).\n");
        w.write("         // Ver #HELP/binChar.md (HDAT 6).\n");
        w.write("         // P1 a P4 = os 4 passos: [comando, frames]. O passo fica apertado esses\n");
        w.write("         // frames (0 = 1 frame). Comando 0 = fim: os passos depois dele não rodam.\n");
        w.write("         // PROXIMA: sequência que começa quando esta acaba (0 = nenhuma). Só vale\n");
        w.write("         // para a CPU do jogador 2 fora do modo 8; nos toques é ignorada.\n");
        w.write("         // SO_COM_CANCEL: 1 = se um passo de golpe acabar sem janela de cancelamento\n");
        w.write("         // (o golpe não acertou), a sequência para ali.\n");
        w.write("         // Sequências especiais: 55 = ativar a fúria; 7 e 20 = só com a fúria ativa\n");
        w.write("         // (senão vira a 8); 8 = a reserva; 36 = usada no modo especial com 100 de ki.\n");
        w.write("         // Golpe N = linha GOLPE_N do HDAT 4. Comandos:\n");
        for (int v = 0; v < PASSO_DOC.length; v++) {
            w.write("         //   " + String.format("%2d", v) + " = " + PASSO_DOC[v] + "\n");
        }
        w.write("         //   31 ou mais = nada\n\n");

        for (int i = 0; i < ysize; i++) {
            int p = i * 10;
            StringBuilder sb = new StringBuilder("         ");
            sb.append(String.format("%-9s", "$SEQ_" + i + ":")).append("{ ");
            for (int s = 0; s < 4; s++) {
                sb.append("$P").append(s + 1).append(": [")
                  .append(String.format("%2d", data[p + s * 2] & 0xFF)).append(", ")
                  .append(String.format("%3d", data[p + s * 2 + 1] & 0xFF)).append("], ");
            }
            int prox = data[p + 8] & 0xFF, canc = data[p + 9] & 0xFF;
            sb.append("$PROXIMA: ").append(String.format("%3d", prox))
              .append(", $SO_COM_CANCEL: ").append(canc).append(" },");

            StringBuilder desc = new StringBuilder();
            for (int s = 0; s < 4; s++) {
                int cmd = data[p + s * 2] & 0xFF;
                if (cmd == 0) break;
                if (desc.length() > 0) desc.append(" > ");
                desc.append(nomePasso(cmd)).append(' ').append(data[p + s * 2 + 1] & 0xFF).append('f');
            }
            if (desc.length() == 0) desc.append("(vazia)");
            if (prox != 0) desc.append(" > seq ").append(prox);
            if (i == 55) desc.append("   [ativar a fúria]");
            else if (i == 7 || i == 20) desc.append("   [só com a fúria ativa]");
            else if (i == 8) desc.append("   [reserva]");
            else if (i == 36) desc.append("   [modo especial]");
            sb.append("  // ").append(desc);
            w.write(sb.append('\n').toString());
        }
        w.write("      },\n");
        return true;
    }

    private static byte[] bdHDAT6(String block, int xsize, int ysize, ErrorReporter reporter) throws IOException {
        if (xsize <= 0) xsize = 10;

        int maxSeq = -1;
        Matcher mCount = Pattern.compile("\\$SEQ_(\\d+)\\s*:").matcher(block);
        while (mCount.find()) {
            maxSeq = Math.max(maxSeq, Integer.parseInt(mCount.group(1)));
        }
        if (ysize > 0 && maxSeq + 1 > ysize) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .severity(CompileError.Severity.WARNING)
                    .fieldTag("$SEQ_" + maxSeq).hdatIndex(6)
                    .message("$SEQ_" + maxSeq + " passa do $ROW (" + ysize + "): aumente o $ROW para " + (maxSeq + 1)
                            + ", senão o jogo troca essas sequências pela 8"));
        }
        int linhas = Math.max(ysize, maxSeq + 1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Pattern pStep = Pattern.compile(
            "\\{\\s*\\$ACT\\s*:\\s*(-?[\\d.]+)\\s*,\\s*\\$ACT_TIME\\s*:\\s*(-?[\\d.]+)\\s*\\}");

        for (int i = 0; i < linhas; i++) {
            byte[] row = new byte[xsize];
            String content = extractBlock(block, "\\$SEQ_" + i);
            if (content == null) {
                if (i <= maxSeq) {
                    ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                            .severity(CompileError.Severity.WARNING)
                            .fieldTag("$SEQ_" + i)
                            .hdatIndex(6)
                            .message("$SEQ_" + i + " não encontrado (ou com chaves desbalanceadas) — a sequência fica vazia"));
                }
                baos.write(row);
                continue;
            }

            String tag = "$SEQ_" + i;
            if (content.contains("$P1")) {
                for (int s = 0; s < 4 && s * 2 + 1 < xsize; s++) {
                    String par = extractBracket(content, "\\$P" + (s + 1));
                    if (par == null) continue;
                    String[] v = par.split(",");
                    Integer cmd = v.length > 0 && v[0].trim().length() > 0
                            ? checkedRawGroup(v[0].trim(), tag + "/$P" + (s + 1) + "[0]", 6, 0, 255, reporter, par) : null;
                    Integer fr  = v.length > 1 && v[1].trim().length() > 0
                            ? checkedRawGroup(v[1].trim(), tag + "/$P" + (s + 1) + "[1]", 6, 0, 255, reporter, par) : null;
                    if (cmd != null) row[s * 2]     = (byte) (cmd & 0xFF);
                    if (fr  != null) row[s * 2 + 1] = (byte) (fr & 0xFF);
                }
                if (xsize >= 10) {
                    Integer prox = extractOptionalChecked(content, "\\$PROXIMA\\s*:\\s*(-?[\\d.]+)", tag + "/$PROXIMA", 6, 0, 255, reporter);
                    if (prox != null) row[8] = (byte) (prox & 0xFF);
                    Integer canc = extractOptionalChecked(content, "\\$SO_COM_CANCEL\\s*:\\s*(-?[\\d.]+)", tag + "/$SO_COM_CANCEL", 6, 0, 255, reporter);
                    if (canc != null) row[9] = (byte) (canc & 0xFF);
                }
            } else {
                // Formato antigo: {$ACT:a, $ACT_TIME:t} × 4, $CONTINUA, $EXIGE_CANCEL.
                Matcher mStep = pStep.matcher(content);
                int s = 0;
                while (mStep.find() && s < 4 && s * 2 + 1 < xsize) {
                    Integer act  = checkedRawGroup(mStep.group(1), tag + "/$ACT",      6, 0, 255, reporter, mStep.group());
                    Integer time = checkedRawGroup(mStep.group(2), tag + "/$ACT_TIME", 6, 0, 255, reporter, mStep.group());
                    row[s * 2]     = (byte) ((act != null ? act : 0) & 0xFF);
                    row[s * 2 + 1] = (byte) ((time != null ? time : 0) & 0xFF);
                    s++;
                }
                if (xsize >= 10) {
                    Integer cont = extractOptionalChecked(content, "\\$CONTINUA\\s*:\\s*(-?[\\d.]+)", tag + "/$CONTINUA", 6, 0, 255, reporter);
                    row[8] = (byte) ((cont != null ? cont : 0) & 0xFF);
                    Integer canc = extractOptionalChecked(content, "\\$EXIGE_CANCEL\\s*:\\s*(-?[\\d.]+)", tag + "/$EXIGE_CANCEL", 6, 0, 255, reporter);
                    row[9] = (byte) ((canc != null ? canc : 0) & 0xFF);
                }
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
        w.write("         // A disputa começa quando este personagem está na animação gatilho e o oponente está\n");
        w.write("         // a 240 px ou mais, parado/andando (ANO 0-12 ou 81) e com ki >= 100. Ver #HELP/binChar.md.\n\n");

        int anim = (data.length > 0) ? data[0] & 0xFF : 0;
        int cena = (data.length > 1) ? data[1] & 0xFF : 0;

        w.write(String.format("         $ANIM_GATILHO: %d,\n", anim));
        w.write("         // Animação que ativa a disputa (ex: pose de carregamento do especial).\n\n");
        w.write(String.format("         $TEM_CUTSCENE: %d,\n", cena));
        w.write("         // 0 = mostra a imagem final (ANO 280 do vencedor) por 90 frames; diferente de 0 = pula a imagem.\n");
        w.write("         // Vale também para o fim da cena do golpe Extreme.\n");
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
