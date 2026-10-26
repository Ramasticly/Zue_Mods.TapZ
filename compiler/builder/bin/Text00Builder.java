package com.zue.gd.util;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text00Builder {

    private static final int IO_BUFFER = 2 * 1024 * 1024; // 2 MB

    private static final Pattern COL_PATTERN =
            Pattern.compile("\\$COL:\\s*(\\d+)");
    private static final Pattern ROW_PATTERN =
            Pattern.compile("\\$ROW:\\s*(\\d+)");
    private static final Pattern TEXT_PATTERN =
            Pattern.compile("\\$T_\\d+:\\\"(.*?)\\\",", Pattern.DOTALL);

    private Text00Builder() {}

    public static byte[] build(File txtFile) throws Exception {
        String content = readFastText(txtFile);

        int col = extractInt(content, COL_PATTERN);
        int row = extractInt(content, ROW_PATTERN);

        ByteArrayOutputStream textData =
                new ByteArrayOutputStream(content.length());

        Matcher m = TEXT_PATTERN.matcher(content);

        int count = 0;
        while (m.find() && count < row) {
            String raw = m.group(1);
            String str = unescapeQuotes(raw);

            byte[] bytes = str.getBytes("UTF-8");

            // tamanho (U16 BE)
            textData.write((bytes.length >>> 8) & 0xFF);
            textData.write(bytes.length & 0xFF);
            textData.write(bytes);

            count++;
        }

        // completa ROW com strings vazias
        while (count < row) {
            textData.write(0);
            textData.write(0);
            count++;
        }

        byte[] payload = textData.toByteArray();

        ByteArrayOutputStream out =
                new ByteArrayOutputStream(16 + payload.length);

        // count de blocos = 1
        writeShortLE(out, 1);

        // offset do bloco (2 + 8 = 10)
        writeIntLE(out, 10);
        writeShortLE(out, col);
        writeShortLE(out, row);

        out.write(payload);
        return out.toByteArray();
    }

    private static int extractInt(String src, Pattern p) {
        Matcher m = p.matcher(src);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static String unescapeQuotes(String s) {
        // mais rápido que regex
        if (s.indexOf("\\\"") < 0) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length() && s.charAt(i + 1) == '"') {
                sb.append('"');
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String readFastText(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream((int) f.length());

            byte[] buf = new byte[IO_BUFFER];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    private static void writeShortLE(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >>> 8) & 0xFF);
    }

    private static void writeIntLE(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >>> 8) & 0xFF);
        os.write((v >>> 16) & 0xFF);
        os.write((v >>> 24) & 0xFF);
    }
}