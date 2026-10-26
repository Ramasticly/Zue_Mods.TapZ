package com.zue.gd.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public final class Text00Extractor {

    // ----------------------------------------
    // Utilidades LE
    // ----------------------------------------
    private static int readU16LE(byte[] buf, int pos) {
        return (buf[pos] & 0xFF) | ((buf[pos + 1] & 0xFF) << 8);
    }

    private static int readU32LE(byte[] buf, int pos) {
        return  (buf[pos] & 0xFF) |
                ((buf[pos + 1] & 0xFF) << 8) |
                ((buf[pos + 2] & 0xFF) << 16) |
                ((buf[pos + 3] & 0xFF) << 24);
    }

    private static void saveText(File file, String text) throws Exception {
        try (OutputStreamWriter w =
                     new OutputStreamWriter(
                             new FileOutputStream(file),
                             StandardCharsets.UTF_8)) {
            w.write(text);
        }
    }

    // ===============================================================
    // MÉTODO PRINCIPAL
    // ===============================================================
    public static void process(String packName,
                               String index,
                               byte[] src,
                               int dataPos,
                               int dataSize,
                               File outDir) throws Exception {

        byte[] dac = new byte[dataSize];
        System.arraycopy(src, dataPos, dac, 0, dataSize);

        String textDump = extractBin(dac);

        File out = new File(outDir, index + ".dac.txt");
        saveText(out, textDump);
    }

    // ===============================================================
    // extractBin
    // ===============================================================
    private static String extractBin(byte[] data) {

        StringBuilder out = new StringBuilder();

        int count = readU16LE(data, 0);

        int[] offsets = new int[count + 1];
        int[] cols    = new int[count];
        int[] rows    = new int[count];

        for (int i = 0; i < count; i++) {
            int p = 2 + (i * 8);
            offsets[i] = readU32LE(data, p);
            cols[i]    = readU16LE(data, p + 4);
            rows[i]    = readU16LE(data, p + 6);
        }
        offsets[count] = data.length;

        out.append("BIN_CNV_ {$COUNT: 1,\n");

        int col = cols[0];
        int row = rows[0];

        int start = offsets[0];
        int end   = offsets[1];

        if (start < 0) start = 0;
        if (end < start) end = start;
        if (end > data.length) end = data.length;

        out.append("   $HDAT_0:{$info:{")
           .append("$COL: ").append(col).append(", ")
           .append("$ROW: ").append(row).append(", ")
           .append("$SZ: &").append(String.format("%08X", end - start))
           .append("},\n");

        extractText(out, data, start, end, row);

        out.append("   },\n");
        out.append("}\n");

        return out.toString();
    }

    // ===============================================================
    // extractText
    // ===============================================================
    private static void extractText(StringBuilder out,
                                    byte[] data,
                                    int start,
                                    int end,
                                    int row) {

        int size = end - start;
        int pos = 0;

        out.append("      $TXT:{\n");

        for (int i = 0; i < row; i++) {

            if (pos + 2 > size) {
                out.append("         $T_").append(i).append(":\"\",\n");
                continue;
            }

            int len = ((data[start + pos] & 0xFF) << 8) |
                      (data[start + pos + 1] & 0xFF);

            pos += 2;

            if (len <= 0 || pos + len > size) {
                out.append("         $T_").append(i).append(":\"\",\n");
                continue;
            }

            byte[] chunk = new byte[len];
            System.arraycopy(data, start + pos, chunk, 0, len);
            pos += len;

            String txt;
            try {
                txt = new String(chunk, StandardCharsets.UTF_8);
            } catch (Exception e) {
                txt = new String(chunk);
            }

            txt = txt.replace("\"", "\\\"");

            out.append("         $T_").append(i)
               .append(":\"").append(txt).append("\",\n");
        }

        out.append("      },\n");
    }
}