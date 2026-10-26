package com.zue.gd.util;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CnvBuilder {

    private static final int IO_BUFFER = 2 * 1024 * 1024; // 2 MB

    // Registro bem formado: todos os 5 campos são inteiros (nunca decimal —
    // ver CnvExtractor, que só produz byte/short, jamais ponto flutuante).
    private static final Pattern CNV_PATTERN = Pattern.compile(
            "\\$src:(-?\\d+),\\s*\\$x:(-?\\d+),\\s*\\$y:(-?\\d+),\\s*\\$w:(-?\\d+),\\s*\\$h:(-?\\d+)"
    );

    // Usado só para CONTAR quantos registros $SPR_N existem no texto (mesmo
    // que malformados), para detectar quando um registro foi descartado
    // pelo CNV_PATTERN — o que causaria desalinhamento silencioso de todos
    // os índices seguintes no binário, já que o build não usa o número do
    // $SPR_N, só a ordem de match.
    private static final Pattern SPR_HEADER_PATTERN = Pattern.compile("\\$SPR_(\\d+):");

    private CnvBuilder() {}

    /** Compatibilidade: comportamento idêntico ao de sempre, sem coleta de erros. */
    public static byte[] build(String packName, File cnvFile) throws Exception {
        return build(packName, cnvFile, null);
    }

    /**
     * Mesmo comportamento de build(String, File), com um ErrorReporter opcional
     * (pode ser null — nesse caso custo extra é zero) para detectar registros
     * malformados e o desalinhamento de índice que eles causam no binário.
     */
    public static byte[] build(String packName, File cnvFile, ErrorReporter reporter) throws Exception {
        boolean isChar = packName.matches("char\\d{2}");

        String content = readFastText(cnvFile);

        if (reporter != null) {
            checkForMalformedRecords(packName, cnvFile.getName(), content, reporter);
        }

        Matcher m = CNV_PATTERN.matcher(content);

        // 9 bytes por registro → evita realocação
        ByteArrayOutputStream out =
                new ByteArrayOutputStream(content.length() / 10);

        while (m.find()) {
            int src = Integer.parseInt(m.group(1));
            int x   = Integer.parseInt(m.group(2));
            int y   = Integer.parseInt(m.group(3));
            int w   = Integer.parseInt(m.group(4));
            int h   = Integer.parseInt(m.group(5));

            if (isChar) {
                x >>= 1;
                y >>= 1;
                w >>= 1;
                h >>= 1;
            }

            out.write(src & 0xFF);
            writeShortBE(out, x);
            writeShortBE(out, y);
            writeShortBE(out, w);
            writeShortBE(out, h);
        }

        return out.toByteArray();
    }

    /**
     * Conta quantos cabeçalhos "$SPR_N:" existem no texto e compara com quantos
     * registros o CNV_PATTERN conseguiu casar por completo. Se houver diferença,
     * localiza e reporta cada registro que não bateu — porque o build usa apenas
     * a ORDEM de match (não o índice N do texto), então cada registro perdido
     * desalinha silenciosamente todos os índices seguintes no binário final.
     */
    private static void checkForMalformedRecords(String packName, String fileName, String content, ErrorReporter reporter) {
        Matcher headers = SPR_HEADER_PATTERN.matcher(content);
        Matcher wellFormed = CNV_PATTERN.matcher(content);

        // Posições onde um registro bem formado começa (início do match completo),
        // para saber, por cabeçalho, se ele tem um registro correspondente logo em seguida.
        java.util.List<Integer> matchStarts = new java.util.ArrayList<Integer>();
        while (wellFormed.find()) matchStarts.add(wellFormed.start());

        int matchIdx = 0;
        int lost = 0;
        while (headers.find()) {
            int sprIndex = Integer.parseInt(headers.group(1));
            int headerEnd = headers.end();

            // Um registro bem formado é válido para este cabeçalho se o match
            // começa a partir de onde o cabeçalho terminou (mesmo registro),
            // permitindo até a chave/abre-chaves "{" entre o header e "$src:".
            boolean matched = false;
            while (matchIdx < matchStarts.size() && matchStarts.get(matchIdx) < headerEnd) {
                matchIdx++; // já consumido por um cabeçalho anterior, ignora
            }
            if (matchIdx < matchStarts.size()) {
                int gap = matchStarts.get(matchIdx) - headerEnd;
                // pequena folga (poucos caracteres) para "{" e espaços entre o header e "$src:"
                if (gap >= 0 && gap <= 10) {
                    matched = true;
                    matchIdx++;
                }
            }

            if (!matched) {
                lost++;
                int lineNumber = countLines(content, headers.start());
                String rawLine = extractLineAt(content, headers.start());

                ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                        .pacFolder(packName)
                        .fileName(fileName)
                        .fileKind("cnv")
                        .lineNumber(lineNumber)
                        .rawLine(rawLine)
                        .fieldTag("$SPR_" + sprIndex)
                        .message("$SPR_" + sprIndex + " está malformado (campo não-inteiro, faltando, ou fora do padrão $src/$x/$y/$w/$h) "
                                + "e será DESCARTADO do binário — isso desalinha o índice de TODOS os $SPR_N seguintes, "
                                + "pois o compilador identifica sprites pela ordem, não pelo número do rótulo"));
            }
        }

        if (lost > 0) {
            ErrorReporter.reportIfPresent(reporter, CompileError.builder()
                    .severity(CompileError.Severity.WARNING)
                    .pacFolder(packName)
                    .fileName(fileName)
                    .fileKind("cnv")
                    .message(lost + " registro(s) malformado(s) detectado(s) neste arquivo — "
                            + "a partir do primeiro registro perdido, todo $SPR_N no jogo pode estar apontando para o sprite errado"));
        }
    }

    private static int countLines(String content, int uptoIndex) {
        int line = 1;
        for (int i = 0; i < uptoIndex && i < content.length(); i++) {
            if (content.charAt(i) == '\n') line++;
        }
        return line;
    }

    private static String extractLineAt(String content, int index) {
        int start = content.lastIndexOf('\n', index);
        start = (start < 0) ? 0 : start + 1;
        int end = content.indexOf('\n', index);
        if (end < 0) end = content.length();
        return content.substring(start, end).trim();
    }

    private static void writeShortBE(OutputStream os, int v) throws IOException {
        os.write((v >>> 8) & 0xFF);
        os.write(v & 0xFF);
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
}