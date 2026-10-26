package com.zue.gd.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BinBuilder {

    /**
     * Compatibilidade: comportamento idêntico ao de sempre, sem coleta de erros.
     */
    public static byte[] build(File txtFile) throws Exception {
        return build(txtFile, null);
    }

    /**
     * Mesmo comportamento de build(File), com um ErrorReporter opcional para quem
     * quiser inspecionar problemas de validação (valores fora de range, tags
     * malformadas, etc.) sem que isso afete a geração do binário — reporter pode
     * ser null, e nesse caso o custo extra é zero (nenhuma coleta acontece).
     */
    public static byte[] build(File txtFile, ErrorReporter reporter) throws Exception {
        String content = readFileToString(txtFile.getAbsolutePath());

        Matcher mCount = Pattern.compile("\\$COUNT:\\s*(\\d+)").matcher(content);
        if (!mCount.find()) throw new Exception("Tag $COUNT não encontrada.");
        int count = Integer.parseInt(mCount.group(1));

        ByteArrayOutputStream headerTable = new ByteArrayOutputStream();
        ByteArrayOutputStream dataBody = new ByteArrayOutputStream();

        int currentDataOffset = 2 + (count * 8);

        for (int i = 0; i < count; i++) {
            String hdatKey = "$HDAT_" + i + ":";
            String hdatBlock = getBalancedBlock(content, hdatKey);

            int col = 0, row = 0;
            byte[] blockData = new byte[0];

            if (hdatBlock != null) {
                col = (int) extractVal(hdatBlock, "\\$COL:\\s*(\\d+)");
                row = (int) extractVal(hdatBlock, "\\$ROW:\\s*(\\d+)");
                blockData = buildInnerBlock(hdatBlock, col, row, reporter);
            }

            writeIntLE(headerTable, currentDataOffset);
            writeShortLE(headerTable, col);
            writeShortLE(headerTable, row);

            dataBody.write(blockData);
            currentDataOffset += blockData.length;
        }

        ByteArrayOutputStream finalBin = new ByteArrayOutputStream();
        writeShortLE(finalBin, count);
        finalBin.write(headerTable.toByteArray());
        finalBin.write(dataBody.toByteArray());

        return finalBin.toByteArray();
    }
    
    public static String readFileToString(String path) throws IOException {
        return readText(path);
    }

    public static String readText(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Compila UM bloco $HDAT_N (o texto completo, de "$HDAT_N:{" até a "}"
     * final) exatamente como build() faria. Usado pelo BinExtractor para
     * conferir, já na extração, se o texto gerado volta aos mesmos bytes.
     */
    static byte[] buildHdatBlock(String hdatBlock, int xsize, int ysize) throws IOException {
        return buildInnerBlock(hdatBlock, xsize, ysize, null);
    }

    private static byte[] buildInnerBlock(String block, int xsize, int ysize, ErrorReporter reporter) throws IOException {
        byte[] result;

        if ((result = hdatChar.processBuild(block, xsize, ysize, reporter)) != null) return result;
        if ((result = hdatGD.processBuild(block, xsize, ysize, reporter)) != null) return result;
        if ((result = hdatBack.processBuild(block, xsize, ysize, reporter)) != null) return result; // back (cenário) — despacho por tag, ver hdatBack.java

        // --- BLOCOS DE SISTEMA / GLOBAIS (não pertencem a nenhum domínio específico) ---
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (block.contains("$TXT:")) {
            // $T_N: "texto" (UTF-8) ou $T_N: SJIS"texto" (Shift_JIS) — ver BinExtractor.quoteText
            Matcher m = Pattern.compile("\\$T_\\d+:\\s*(SJIS)?" + QUOTED_TEXT).matcher(block);
            int linhas = 0;
            Charset sjis = null;
            while (m.find()) {
                Charset cs = StandardCharsets.UTF_8;
                if (m.group(1) != null) {
                    if (sjis == null) sjis = BinExtractor.shiftJis();
                    if (sjis == null) throw new IOException("Shift_JIS não existe nesta plataforma ($T_ com SJIS\"\")");
                    cs = sjis;
                }
                byte[] b = encodeText(m.group(2), cs);
                writeShortBE(baos, b.length); // Tamanho Big Endian como no Extractor
                baos.write(b);
                linhas++;
            }
            // Completa até $ROW com textos vazios (como o Text00Builder fazia): o
            // jogo acha a linha N somando os tamanhos das anteriores, então uma
            // linha a menos faria as últimas lerem o HDAT seguinte.
            for (; linhas < ysize; linhas++) writeShortBE(baos, 0);
        return baos.toByteArray();
        }

        // Definição de Background global
        if (block.contains("$DEF_BACKGROUND")) {
            Matcher m = Pattern.compile("\\$DEF_BACKGROUND:(\\d+)").matcher(block);
            if (m.find()) baos.write(Integer.parseInt(m.group(1)) & 0xFF);
            return baos.toByteArray();
        }

        // --- FALLBACK RAW ---
        if (block.contains("$RAW:B[") && block.contains("&")) {
            int start = block.indexOf("[");
            int end = block.lastIndexOf("]");
            if (start != -1 && end > start) {
                Matcher m = Pattern.compile("&([0-9A-Fa-f]{2})").matcher(block.substring(start, end));
                while (m.find()) baos.write(Integer.parseInt(m.group(1), 16));
            }
            return baos.toByteArray();
        }

        return new byte[0];
    }

    // --- Auxiliares de escrita (Little Endian) ---
    private static void writeShortLE(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
    }

    private static void writeIntLE(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
        os.write((v >> 16) & 0xFF);
        os.write((v >> 24) & 0xFF);
    }
    
    private static void writeShortBE(OutputStream os, int v) throws IOException {
        os.write((v >> 8) & 0xFF);
        os.write(v & 0xFF);
    }

    /**
     * Texto entre aspas no formato que os extractors escrevem (\\ \" \n \r
     * escapados), seguido da vírgula. O antigo "(.*?)\"," parava no primeiro
     * \"," DE DENTRO do texto (ex.: "Olá", disse) e cortava a string.
     */
    // Forma "unrolled" possessiva: (?:[^"\\]|\\.)* recursava uma vez POR
    // CARACTERE no java.util.regex e estourava a pilha (StackOverflowError) em
    // textos longos; assim só há repetição de grupo por sequência de escape.
    static final String QUOTED_TEXT = "\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\"\\s*,";

    /**
     * Desfaz o escape em UMA passada. O replace encadeado anterior
     * (\\n → quebra antes de \\\\ → \\) transformava um "\\" seguido de "n" do
     * texto original (ex.: C:\\novo) em quebra de linha.
     */
    static String unescapeText(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'n') { sb.append('\n'); i++; continue; }
                if (n == 'r') { sb.append('\r'); i++; continue; }
                if (n == '"' || n == '\\') { sb.append(n); i++; continue; }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Desfaz o escape de um $T_N (como unescapeText) e codifica em cs; \xHH
     * vira o byte HH cru, sem passar pelo charset (ver BinExtractor.quoteText).
     */
    static byte[] encodeText(String s, Charset cs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length() * 2);
        StringBuilder pending = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'x' && i + 3 < s.length() && isHex(s.charAt(i + 2)) && isHex(s.charAt(i + 3))) {
                    byte[] e = pending.toString().getBytes(cs);
                    out.write(e, 0, e.length);
                    pending.setLength(0);
                    out.write(Integer.parseInt(s.substring(i + 2, i + 4), 16));
                    i += 3;
                    continue;
                }
                if (n == 'n') { pending.append('\n'); i++; continue; }
                if (n == 'r') { pending.append('\r'); i++; continue; }
                if (n == '"' || n == '\\') { pending.append(n); i++; continue; }
            }
            pending.append(c);
        }
        byte[] e = pending.toString().getBytes(cs);
        out.write(e, 0, e.length);
        return out.toByteArray();
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static String getBalancedBlock(String text, String startMarker) {
        int startIndex = text.indexOf(startMarker);
        if (startIndex == -1) return null;
        int braceStart = text.indexOf("{", startIndex);
        if (braceStart == -1) return null;
        int depth = 0;
        boolean inQuote = false;
        for (int i = braceStart; i < text.length(); i++) {
            char c = text.charAt(i);
            // Chaves dentro de textos ($T_N:"...{...}") não contam — antes um
            // "{" ou "}" num texto desalinhava o fim do $HDAT_N.
            if (inQuote) {
                if (c == '\\') i++;
                else if (c == '"') inQuote = false;
                continue;
            }
            if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                int eol = text.indexOf('\n', i);
                if (eol < 0) break;
                i = eol; // comentário "//" até o fim da linha: aspas/chaves nele não contam
                continue;
            }
            if (c == '"') inQuote = true;
            else if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return text.substring(startIndex, i + 1);
            }
        }
        return null;
    }

    private static long extractVal(String src, String regex) {
        Matcher m = Pattern.compile(regex).matcher(src);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }
}
