package com.zue.gd.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public class BinExtractor {

    private static final String[] HEX_TABLE = new String[256];

    static {
        for (int i = 0; i < 256; i++) HEX_TABLE[i] = String.format("%02X", i);
    }

    public static void process(String pacName, String fileName, byte
                    [] src, int dataPos, int dataSize, int cnvType, File outDir)
            throws Exception {

        // Identificação da pasta conforme o original
        int pastaFixa = -1;
        String name = pacName.toLowerCase();
        if (name.equals("gamedata.pac")) pastaFixa = 0;
        else if (name.equals("text00.pac")) pastaFixa = 3;
        else if (name.matches("char\\d{2}\\.pac")) pastaFixa = 4;
        else if (name.matches("card\\d{3}\\.pac")) pastaFixa = 5;
        else if (name.matches("back\\d{2}\\.pac")) pastaFixa = 6; // NOVO — cenário/stage, ver backBin.java

        byte[] data = new byte[dataSize];
        System.arraycopy(src, dataPos, data, 0, dataSize);

        if (data.length < 2) return;

        int count = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
        if ((count * 8) + 2 > data.length) {
            count = (data.length - 2) / 8;
        }

        int[] offsets = new int[count + 1];
        int[] cols = new int[count];
        int[] rows = new int[count];

        for (int i = 0; i < count; i++) {
            int p = 2 + (i * 8);
            offsets[i] = (data[p] & 0xFF) | ((data[p + 1] & 0xFF) << 8) |
                    ((data[p + 2] & 0xFF) << 16) | ((data[p + 3] & 0xFF) << 24);
            cols[i] = (data[p + 4] & 0xFF) | ((data[p + 5] & 0xFF) << 8);
            rows[i] = (data[p + 6] & 0xFF) | ((data[p + 7] & 0xFF) << 8);
        }
        offsets[count] = data.length;

        // A extensão aqui segue a lógica do seletor que você postou
        String ext = (pastaFixa == 0 || pastaFixa == 3) ? ".dac.txt" : ".bin.txt";
        File outFile = new File(outDir, fileName + ext);

        // Usando BufferedWriter puro para controlar exatamente cada quebra de linha
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            w.write("BIN_CNV_ {$COUNT: " + count + ",\n");

            for (int i = 0; i < count; i++) {
                int start = Math.max(0, Math.min(offsets[i], data.length));
                int end = Math.max(start, Math.min(offsets[i + 1], data.length));
                int size = end - start;

                w.write("   $HDAT_" + i + ":{$info:{$COL: " + cols[i] + ", $ROW: " + rows[
                        i] + ", $SZ: &" + String.format("%08X", size) + "},\n");

                if (size > 0) {
                    byte[] block = new byte[size];
                    System.arraycopy(data, start, block, 0, size);
                    writeInnerBlock(w, block, pastaFixa, i, cols[i], rows[i]);
                } else {
                    w.write("      $RAW:null\n");
                }
                w.write("   },\n");
            }
            w.write("}");
            w.flush();
        }
    }

    private static void writeInnerBlock(BufferedWriter w, byte
                    [] data, int pasta, int hdat, int xsize, int ysize)
            throws IOException {
        int ptr = 0;
        int len = data.length;

        switch (pasta) {
            case 0: // gamedata
                switch (hdat) {
                    case 7: case 44: case 137: case 138: case 154:
                        writeText(w, data, ysize); return;
                }
                if (hdatGD.processExtraction(w, data, hdat, xsize, ysize)) return;
                break;
            case 3: // text00
            case 5: // card
                writeText(w, data, ysize);
                return;
            case 4: // char
                if (hdatChar.processExtraction(w, data, hdat, xsize, ysize)) return;
                break;
            /*case 6: // NOVO — back (cenário/stage): backNN.pac, chunk "bin" → ChrGameData[1].piGameData[]
                // Diferente de "gamedata"/"char" acima, aqui é UMA classe só cuidando de
                // todos os HDATs desta pasta — ela decide por dentro o que já foi
                // confirmado (HDAT 0, HDAT 2) e o que ainda não foi (fallback hex).
                // Ver hdatBack.java pro detalhe de cada campo.
                if (hdatBack.processExtraction(w, data, hdat, xsize, ysize)) return;
                break;*/
        }
        w.write("      $RAW:B[\n");
        while (ptr < len) {
            w.write("         ");
            for (int c = 0; c < xsize && ptr < len; c++) {
                w.write("&" + HEX_TABLE[data[ptr++] & 0xFF] + ", ");
            }
            w.write("\n");
        }
        w.write("      ],\n");
    }

    private static void writeText(BufferedWriter w, byte[] data, int ysize) throws IOException {
        w.write("      $TXT:{\n");
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
            String escapedTxt = txt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            w.write("         $T_" + i + ":\"" + escapedTxt + "\",\n");
        }
        w.write("      },\n");
    }

    private static String decodeWithFallback(byte[] chunk) {
        if (chunk == null || chunk.length == 0) return "";
        try {
            CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return utf8Decoder.decode(java.nio.ByteBuffer.wrap(chunk)).toString();
        } catch (Exception e) {
            return new String(chunk, Charset.forName("Shift_JIS"));
        }
    }
}
