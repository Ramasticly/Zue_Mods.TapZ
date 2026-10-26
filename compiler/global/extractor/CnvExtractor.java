package com.zue.gd.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class CnvExtractor {

    private CnvExtractor() {}

    public static void process(String pacName,
                               String index,
                               byte[] src,
                               int dataPos,
                               int dataSize,
                               int cnvType,
                               File outDir) throws Exception {

        boolean isChar = pacName.matches("char\\d{2}\\.pac");

        File outFile = new File(outDir, index + ".cnv.txt");

        byte[] segment = new byte[dataSize];
        System.arraycopy(src, dataPos, segment, 0, dataSize);

        extract(segment, outFile, isChar);
    }

    private static void extract(byte[] segment,
                                File outFile,
                                boolean isChar) throws IOException {

        int total = segment.length / 9;

        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(outFile),
                        StandardCharsets.UTF_8
                )
        );

        try {
            writer.write("CNV_TXT_ {\n");

            for (int idx = 0; idx < total; idx++) {
                int base = idx * 9;

                int src = segment[base] & 0xFF;
                int x = readShortBE(segment, base + 1);
                int y = readShortBE(segment, base + 3);
                int w = readShortBE(segment, base + 5);
                int h = readShortBE(segment, base + 7);

                if (isChar) {
                    x <<= 1;
                    y <<= 1;
                    w <<= 1;
                    h <<= 1;
                }

                writer.write(
                        "   $SPR_" + idx + ":{"
                                + "$src:" + src
                                + ", $x:" + x
                                + ", $y:" + y
                                + ", $w:" + w
                                + ", $h:" + h
                                + "},\n"
                );
            }

            writer.write("}\n");
        } finally {
            writer.close();
        }
    }

    private static int readShortBE(byte[] b, int pos) {
        return (short) (((b[pos] & 0xFF) << 8) | (b[pos + 1] & 0xFF));
    }
}