package com.zue.gd.util;

import java.io.*;
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

    private static byte[] buildInnerBlock(String block, int xsize, int ysize, ErrorReporter reporter) throws IOException {
        byte[] result;

        if ((result = hdatChar.processBuild(block, xsize, ysize, reporter)) != null) return result;
        if ((result = hdatGD.processBuild(block, xsize, ysize, reporter)) != null) return result;
        // if ((result = hdatBack.processBuild(block, xsize, ysize, reporter)) != null) return result; // back (cenário) — despacho por tag, ver hdatBack.java

        // --- BLOCOS DE SISTEMA / GLOBAIS (não pertencem a nenhum domínio específico) ---
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (block.contains("$TXT:")) {
            // Captura apenas o conteúdo de $T_0: "texto"
            Pattern p = Pattern.compile("\\$T_\\d+:\\s*\"(.*?)\",", Pattern.DOTALL);
            Matcher m = p.matcher(block);
            while (m.find()) {
                byte[] b = unescape(m.group(1)).getBytes(StandardCharsets.UTF_8);
                writeShortBE(baos, b.length); // Tamanho Big Endian como no Extractor
                baos.write(b);
            }
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

    private static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String getBalancedBlock(String text, String startMarker) {
        int startIndex = text.indexOf(startMarker);
        if (startIndex == -1) return null;
        int braceStart = text.indexOf("{", startIndex);
        if (braceStart == -1) return null;
        int depth = 0;
        for (int i = braceStart; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
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
