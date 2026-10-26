package com.zue.gd.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public class BinExtractor {

    /**
     * HDATs que NÃO são extraídos no formato detalhado: vão direto em $RAW,
     * com os bytes originais, sem passar por hdatGD/hdatChar/hdatBack nem
     * pelo $TXT. Serve para desligar o extractor de um HDAT (ou de uma faixa)
     * e ver se ele é a causa de um comportamento inesperado.
     *
     * Cada item é "pack:hdat". O pack é o nome sem ".pac" (vale só para ele)
     * ou o tipo sem o número (vale para todos daquele tipo); o hdat é um
     * número, uma faixa "a-b" ou "*" (todos):
     *   "gamedata:175"    HDAT 175 do gamedata.pac
     *   "gamedata:50-60"  HDATs 50 a 60 do gamedata.pac
     *   "char:13"         HDAT 13 de todos os charNN.pac
     *   "char05:13"       HDAT 13 só do char05.pac
     *   "back:*"          todos os HDATs dos backNN.pac
     * Item escrito errado é ignorado; os HDATs pulados ficam marcados com um
     * comentário no .txt, então dá para conferir se pegou.
     *
     * Só vale na extração: um pack que já tem pasta precisa ser extraído de
     * novo (no jogo, apagando a pasta dele em Tap Battle/assets/).
     */
    public static String[] SKIP_HDATS = {
    };

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
                    if (isSkipped(name, i)) {
                        w.write("      // Desligado em BinExtractor.SKIP_HDATS — bytes originais, sem o extractor detalhado.\n");
                        writeRaw(w, block, cols[i]);
                    } else {
                        writeVerifiedBlock(w, block, pastaFixa, i, cols[i], rows[i], size);
                    }
                } else {
                    w.write("      $RAW:null\n");
                }
                w.write("   },\n");
            }
            w.write("}");
            w.flush();
        }
    }

    /** true se o HDAT {@code hdat} deste pack (nome em minúsculas, com ".pac") está em SKIP_HDATS. */
    static boolean isSkipped(String pacName, int hdat) {
        String[] list = SKIP_HDATS;
        if (list == null || list.length == 0) return false;
        String pack = pacName.endsWith(".pac") ? pacName.substring(0, pacName.length() - 4) : pacName;
        String kind = pack.replaceAll("\\d+$", ""); // char05 → char, card001 → card
        for (String item : list) {
            if (item == null) continue;
            String s = item.trim().toLowerCase(Locale.US);
            int c = s.indexOf(':');
            if (c < 0) continue;
            String p = s.substring(0, c).trim();
            if (p.endsWith(".pac")) p = p.substring(0, p.length() - 4);
            if (!p.equals(pack) && !p.equals(kind)) continue;
            String h = s.substring(c + 1).trim();
            if (h.equals("*")) return true;
            try {
                int d = h.indexOf('-');
                int a = Integer.parseInt((d < 0 ? h : h.substring(0, d)).trim());
                int b = d < 0 ? a : Integer.parseInt(h.substring(d + 1).trim());
                if (hdat >= a && hdat <= b) return true;
            } catch (NumberFormatException e) {
                // item escrito errado: ignora
            }
        }
        return false;
    }

    /**
     * Escreve o HDAT no formato detalhado (hdatGD/hdatChar/hdatBack/texto) SÓ
     * se esse texto, compilado de volta pelo BinBuilder, reproduz exatamente os
     * mesmos bytes. Caso contrário grava $RAW, que é sempre exato.
     *
     * Motivo: vários formatos detalhados assumem um tamanho fixo de tabela
     * (ex.: carta = 616 bytes, formação = 22 bytes, $PONTUACAO_POR_GOLPE = 80
     * bytes) ou convertem com perda (ex.: $LARGURA = byte/2, $REGENERACAO_KI
     * em %). Num .pac cujo HDAT não bate com essa suposição, extrair e
     * recompilar SEM editar nada mudava o binário. Com a verificação,
     * "extrair → compilar" sem edição é sempre identidade, e o formato
     * detalhado continua sendo usado em todos os HDATs onde ele é exato.
     */
    private static void writeVerifiedBlock(BufferedWriter w, byte[] data, int pasta, int hdat,
                                           int xsize, int ysize, int size) throws IOException {
        String formatted = null;
        try {
            StringWriter sw = new StringWriter();
            BufferedWriter tmp = new BufferedWriter(sw);
            boolean wrote = writeFormatted(tmp, data, pasta, hdat, xsize, ysize);
            tmp.flush();
            if (wrote) formatted = sw.toString();
        } catch (RuntimeException e) {
            formatted = null; // formato detalhado não suporta este tamanho (ex.: índice fora do array)
        }

        if (formatted != null) {
            String asBlock = "   $HDAT_" + hdat + ":{$info:{$COL: " + xsize + ", $ROW: " + ysize
                    + ", $SZ: &" + String.format("%08X", size) + "},\n" + formatted + "   },\n";
            byte[] rebuilt;
            try {
                rebuilt = BinBuilder.buildHdatBlock(asBlock, xsize, ysize);
            } catch (Exception e) {
                rebuilt = null;
            }
            if (Arrays.equals(rebuilt, data)) {
                w.write(formatted);
                return;
            }
            w.write("      // Formato detalhado deste HDAT não reproduz os bytes originais\n");
            w.write("      // (tamanho/colunas diferentes do esperado) — gravado em $RAW para não perder dado.\n");
        }
        writeRaw(w, data, xsize);
    }

    /** @return true se escreveu um formato detalhado; false se este HDAT só tem $RAW. */
    private static boolean writeFormatted(BufferedWriter w, byte[] data, int pasta, int hdat, int xsize, int ysize)
            throws IOException {
        switch (pasta) {
            case 0: // gamedata
                switch (hdat) {
                    // 7 = texto do CREATE_STRING; 137/138/154 = GameData_TextPage do idioma 0
                    // (nome/descrição de formação e textos de estágio); 271-279 = as mesmas
                    // páginas nos idiomas 1-3 (TCBManajer.GameData_TextPage[iLanguage * 3 + n]).
                    case 7: case 44: case 137: case 138: case 154:
                    case 271: case 272: case 273: case 274: case 275: case 276: case 277: case 278: case 279:
                        writeText(w, data, ysize); return true;
                }
                return hdatGD.processExtraction(w, data, hdat, xsize, ysize);
            case 3: // text00
            case 5: // card
                writeText(w, data, ysize);
                return true;
            case 4: // char
                return hdatChar.processExtraction(w, data, hdat, xsize, ysize);
            case 6: // back (cenário/stage): backNN.pac, chunk "bin" → ChrGameData[1].piGameData[]
                // UMA classe só cuida de todos os HDATs desta pasta — ela decide por
                // dentro o que já foi confirmado (HDAT 0, HDAT 2); o resto vai em $RAW.
                // Ver hdatBack.java pro detalhe de cada campo.
                return hdatBack.processExtraction(w, data, hdat, xsize, ysize);
        }
        return false;
    }

    private static void writeRaw(BufferedWriter w, byte[] data, int xsize) throws IOException {
        int ptr = 0;
        int len = data.length;
        // xsize só define a largura da linha; com $COL: 0 o laço abaixo nunca
        // avançava ptr e travava a extração num loop infinito.
        int perLine = (xsize > 0) ? xsize : 16;
        w.write("      $RAW:B[\n");
        while (ptr < len) {
            w.write("         ");
            for (int c = 0; c < perLine && ptr < len; c++) {
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
            w.write("         $T_" + i + ":" + quoteText(chunk) + ",\n");
        }
        w.write("      },\n");
    }

    /**
     * Texto de um $T_N entre aspas, num formato que o BinBuilder volta
     * EXATAMENTE aos mesmos bytes:
     *   "..."      UTF-8 — o que o jogo lê (TCBManajer.gub() é sempre utf-8)
     *   SJIS"..."  Shift_JIS, como nos textos japoneses originais do .pac.
     *              Antes eles eram lidos em Shift_JIS e gravados de volta em
     *              UTF-8, e o HDAT inteiro mudava. Tirando o SJIS da frente,
     *              o texto passa a ser gravado em UTF-8.
     *   \xHH       um byte cru: caracteres de controle e, se a string não é
     *              nem UTF-8 nem Shift_JIS válido, a string inteira.
     */
    static String quoteText(byte[] chunk) {
        String s = decodeExact(chunk, StandardCharsets.UTF_8);
        if (s != null) return "\"" + escapeText(s) + "\"";
        Charset sjis = shiftJis();
        if (sjis != null && (s = decodeExact(chunk, sjis)) != null) return "SJIS\"" + escapeText(s) + "\"";
        StringBuilder sb = new StringBuilder("\"");
        for (byte b : chunk) sb.append("\\x").append(HEX_TABLE[b & 0xFF]);
        return sb.append('"').toString();
    }

    // O "SJIS" do texto é o Shift_JIS do Windows (windows-31j/MS932), que também
    // tem os caracteres NEC dos textos originais (ex.: ㈱ = 87 8A). No Android o
    // "Shift_JIS" já é essa variante; no Java de PC não, por isso vem primeiro.
    static Charset shiftJis() {
        for (String name : new String[]{"windows-31j", "Shift_JIS"}) {
            try {
                return Charset.forName(name);
            } catch (RuntimeException e) {
                // tenta o próximo
            }
        }
        return null; // sem Shift_JIS na plataforma: a string sai em \xHH
    }

    /** Decodifica só se a volta (encode) dá os mesmos bytes; senão null. */
    private static String decodeExact(byte[] chunk, Charset cs) {
        try {
            CharsetDecoder dec = cs.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            String s = dec.decode(ByteBuffer.wrap(chunk)).toString();
            return Arrays.equals(s.getBytes(cs), chunk) ? s : null;
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    // Par do BinBuilder.encodeText: \\ \" \n \r, e \xHH para os outros controles.
    private static String escapeText(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '"') sb.append("\\\"");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c < 0x20 || c == 0x7F) sb.append("\\x").append(HEX_TABLE[c]);
            else sb.append(c);
        }
        return sb.toString();
    }
}
