package com.zue.gd.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Formato genérico "linha × coluna" para HDATs que o jogo lê como tabela simples:
//
//   byte:  piGameData[Pos + linha * XSize + coluna]
//   short: piGameData[Pos + linha * 2 * XSize + coluna * 2]   (LE, com sinal)
//
// Uma linha por $NOME: [v0, v1, ...] — todas as colunas, então é sempre sem
// perda. Usado por hdatGD/hdatChar para as tabelas que não precisam de um
// formato próprio.
final class HdatTabela {

    private HdatTabela() {}

    /** Rótulo da linha: nome conhecido ou LINHA_N. */
    static String rotulo(String[] nomes, int linha) {
        return (nomes != null && linha < nomes.length && nomes[linha] != null) ? nomes[linha] : ("LINHA_" + linha);
    }

    /**
     * @return false se o tamanho do bloco não for XSize × YSize (× 2 em tabela
     *         de short) — quem chama cai no $RAW.
     */
    static boolean escrever(BufferedWriter w, String tag, String[] doc, byte[] data, int xsize, int ysize,
                            boolean shorts, String[] nomes) throws IOException {
        int cel = shorts ? 2 : 1;
        if (xsize <= 0 || ysize <= 0 || data == null || data.length != xsize * ysize * cel) return false;

        w.write("      " + tag + ":{\n");
        if (doc != null) {
            for (String d : doc) w.write("         // " + d + "\n");
            w.write("\n");
        }
        for (int l = 0; l < ysize; l++) {
            w.write("         $" + rotulo(nomes, l) + ": [");
            for (int c = 0; c < xsize; c++) {
                int p = (l * xsize + c) * cel;
                int v = shorts ? (short) ((data[p] & 0xFF) | ((data[p + 1] & 0xFF) << 8)) : (data[p] & 0xFF);
                w.write(String.valueOf(v));
                if (c < xsize - 1) w.write(", ");
            }
            w.write("],\n");
        }
        w.write("      },\n");
        return true;
    }

    static byte[] compilar(String block, int xsize, int ysize, boolean shorts, String[] nomes,
                           int hdatIndex, ErrorReporter reporter) {
        int cel = shorts ? 2 : 1;
        if (xsize <= 0 || ysize <= 0) return new byte[0];
        byte[] data = new byte[xsize * ysize * cel];
        int min = shorts ? -32768 : 0, max = shorts ? 65535 : 255;

        for (int l = 0; l < ysize; l++) {
            String nome = rotulo(nomes, l);
            Matcher m = Pattern.compile("\\$" + Pattern.quote(nome) + ":\\s*\\[([^\\]]*)\\]").matcher(block);
            if (!m.find()) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .severity(CompileError.Severity.WARNING)
                        .fieldTag("$" + nome).hdatIndex(hdatIndex)
                        .message("$" + nome + " não encontrado — linha " + l + " fica zerada"));
                continue;
            }
            String[] partes = m.group(1).split(",");
            for (int c = 0; c < xsize && c < partes.length; c++) {
                String raw = partes[c].trim();
                if (raw.length() == 0) continue;
                int v;
                try {
                    v = (int) Double.parseDouble(raw);
                } catch (NumberFormatException e) {
                    ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                            .fieldTag("$" + nome + "[" + c + "]").hdatIndex(hdatIndex).rawLine(m.group())
                            .message("$" + nome + "[" + c + "] espera um número, mas recebeu \"" + raw + "\""));
                    continue;
                }
                if (v < min || v > max) {
                    ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                            .fieldTag("$" + nome + "[" + c + "]").hdatIndex(hdatIndex).rawLine(m.group())
                            .message("$" + nome + "[" + c + "] = " + v + " está fora do range [" + min + ".." + max + "]"));
                }
                int p = (l * xsize + c) * cel;
                data[p] = (byte) (v & 0xFF);
                if (shorts) data[p + 1] = (byte) ((v >> 8) & 0xFF);
            }
            if (partes.length < xsize) {
                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .severity(CompileError.Severity.WARNING)
                        .fieldTag("$" + nome).hdatIndex(hdatIndex).rawLine(m.group())
                        .message("$" + nome + " tem " + partes.length + " valor(es), $COL pede " + xsize + " — o resto fica 0"));
            }
        }
        return data;
    }
}
