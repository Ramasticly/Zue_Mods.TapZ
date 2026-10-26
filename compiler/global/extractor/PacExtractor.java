package com.zue.gd.util;

import java.io.*;

public final class PacExtractor {

    // Modo app: quando PacBuilder.isApp == true, extract() organiza a saída
    // em subpastas por categoria (BACKS/CARDS/CHARS/SYSTEM) dentro de
    // pacPath, em vez de gravar direto em pacPath/nomeDoPac/ como o jogo
    // espera.
    //
    // Não existe flag própria aqui: a decisão "estamos rodando como app,
    // não como jogo em runtime" é UMA só para o pacote inteiro e vive em
    // PacBuilder.isApp (o jogo nunca liga; o app liga uma vez, antes de
    // extrair/compilar em lote).

    // Callback opcional de progresso, por segmento dentro do PAC.
    // Aditivo: quem já chama extract(pacPath, pacFileName) — como o
    // Compiler.java do jogo, em runtime — continua funcionando sem
    // nenhuma mudança, sempre com cb == null internamente.
    public interface ProgressCallback {
        void onSegmentStart(String pacName, String fileName, int index, int total);

        void onSegmentExtracted(String pacName, String fileName, int index, int total);
    }

    private static final int TYPE_PNG = 0x706E6700;
    private static final int TYPE_ACT = 0x61637400;
    private static final int TYPE_BIN = 0x62696E00;
    private static final int TYPE_CNV = 0x636E7600;
    private static final int TYPE_DAC = 0x64616300;
    private static final int TYPE_SPR = 0x73707200;
    private static final int TYPE_WAV = 0x77617600;
    private static final int TYPE_VID = 0x76696400; // NOVO — mesma tag "vid\0" de
                                                    // RealPacBuilder/GameData.decodeData
    private static final int TYPE_WEBP = 0x77656200; // NOVO — mesma tag "web\0" de
                                                     // GameData.decodeData

    private PacExtractor() {}

    /**
     * @param pacPath Caminho da pasta (ex: "/sdcard/Download")
     * @param pacFileName Nome do arquivo (ex: "personagens.pac")
     */
    public static void extract(String pacPath, String pacFileName) throws Exception {
        extract(pacPath, pacFileName, null);
    }

    /**
     * Mesmo comportamento de extract(pacPath, pacFileName), com um callback opcional de progresso
     * por segmento. Passe cb == null para o comportamento síncrono padrão (sem overhead de
     * notificação).
     *
     * @param pacPath Caminho da pasta (ex: "/sdcard/Download")
     * @param pacFileName Nome do arquivo (ex: "personagens.pac")
     * @param cb Callback de progresso por segmento, ou null.
     */
    public static void extract(String pacPath, String pacFileName, ProgressCallback cb)
            throws Exception {
        File pacFile = new File(pacPath, pacFileName);

        // 1. Se não existe e NÃO é o font00, dá erro fatal.
        if ((!pacFile.exists() || !pacFile.isFile()) && !pacFileName.equalsIgnoreCase("font00.pac")) {
            throw new FileNotFoundException("PAC não encontrado: " + pacFileName);
        }

        // 2. Se não existe (neste ponto, só pode ser o font00), encerra silenciosamente.
        if (!pacFile.exists()) {
            return;
        }

        String folderName = pacFileName.toLowerCase().endsWith(".pac")
                ? pacFileName.substring(0, pacFileName.length() - 4)
                : pacFileName + "_extracted";

        File outDir;
        if (PacBuilder.isApp) {
            String category = decideCategory(pacFileName);
            outDir = new File(new File(pacPath, category), folderName);
        } else {
            outDir = new File(pacPath, folderName);
        }
        if (!outDir.exists()) outDir.mkdirs();
        HelpFolder.garantir(new File(pacPath)); // #HELP com a documentação dos HDATs

        byte[] src = readAllBytes(pacFile);
        if (src.length < 2) return;

        int count = (src[0] & 0xFF) | ((src[1] & 0xFF) << 8);
        int cnvType = decideCnvType(pacFileName);

        // --- PRIMEIRA PASSAGEM: Contar PNGs (WAV precisa saber quantos
        // PNGs existem para continuar a numeração a partir daí — mesmo
        // comportamento de sempre). VID e WEBP NÃO precisam desta
        // passagem: cada um tem seu próprio contador independente,
        // começando em 0, sem relação com PNG/WAV/entre si. ---
        int totalPngs = 0;
        for (int i = 0; i < count; i++) {
            int typePos = (i << 4) + 2 + 8;
            int type = ((src[typePos] & 0xFF) << 24) | ((src[typePos + 1] & 0xFF) << 16) |
                    ((src[typePos + 2] & 0xFF) << 8) | (src[typePos + 3] & 0xFF);
            if (type == TYPE_PNG) totalPngs++;
        }

        // --- SEGUNDA PASSAGEM: Extração com índices recalculados ---
        int pngCounter = 0;
        int wavCounter = totalPngs; // O primeiro WAV será "total de pngs + 0"
        int vidCounter = 0; // NOVO — contador próprio, independente de PNG/WAV
        int webpCounter = 0; // NOVO — contador próprio, independente de PNG/WAV/VID

        for (int i = 0; i < count; i++) {
            int tablePos = (i << 4) + 2;
            int relOffset = (src[tablePos] & 0xFF) | ((src[tablePos + 1] & 0xFF) << 8) |
                    ((src[tablePos + 2] & 0xFF) << 16) | ((src[tablePos + 3] & 0xFF) << 24);
            int size = (src[tablePos + 4] & 0xFF) | ((src[tablePos + 5] & 0xFF) << 8) |
                    ((src[tablePos + 6] & 0xFF) << 16) | ((src[tablePos + 7] & 0xFF) << 24);
            int type = ((src[tablePos + 8] & 0xFF) << 24) | ((src[tablePos + 9] & 0xFF) << 16) |
                    ((src[tablePos + 10] & 0xFF) << 8) | (src[tablePos + 11] & 255);
            int dataPos = (count << 4) + 2 + relOffset;

            String finalIdx;
            if (type == TYPE_PNG) {
                finalIdx = String.format("%03d", pngCounter++);
            } else if (type == TYPE_WAV) {
                finalIdx = String.format("%03d", wavCounter++);
            } else if (type == TYPE_VID) {
                finalIdx = String.format("%03d", vidCounter++);
            } else if (type == TYPE_WEBP) {
                finalIdx = String.format("%03d", webpCounter++);
            } else {
                finalIdx = "000"; // BIN, ACT, CNV, DAC, SPR sempre 000
            }

            String segmentName = finalIdx + guessExt(type);
            if (cb != null) cb.onSegmentStart(pacFileName, segmentName, i, count);

            // Chama o processamento original com o novo finalIdx
            processEntryWithIndex(pacFileName, finalIdx, src, dataPos, size, type, cnvType, outDir);

            if (cb != null) cb.onSegmentExtracted(pacFileName, segmentName, i, count);
        }
    }

    // Extensão "provável" de um segmento, só para reportar no callback de
    // progresso antes/depois do processamento real — não influencia em
    // nada a extração em si (processEntryWithIndex decide o nome de fato).
    private static String guessExt(int type) {
        if (type == TYPE_PNG) return ".png";
        if (type == TYPE_WAV) return ".wav";
        if (type == TYPE_VID) return ".mp4";
        if (type == TYPE_WEBP) return ".webp";
        if (type == TYPE_ACT) return ".act";
        if (type == TYPE_BIN) return ".bin.txt";
        if (type == TYPE_CNV) return ".cnv.txt";
        if (type == TYPE_DAC) return ".dac.txt";
        if (type == TYPE_SPR) return ".spr";
        return "";
    }

// Método auxiliar para manter o switch original limpo
    private static void processEntryWithIndex(String packName, String idx, byte[] src, int dataPos, int size, int type, int cnvType, File outDir)
            throws Exception {
        switch (type) {
            case TYPE_PNG:
                save(outDir, idx + ".png", src, dataPos, size);
                break;
            case TYPE_WAV:
                save(outDir, idx + ".wav", src, dataPos, size);
                break;
            case TYPE_VID:
                // Bytes crus do vídeo (mp4), sem nenhum processamento —
                // mesmo tratamento simples de PNG/WAV. Extensão .mp4
                // espelha RealPacBuilder.detectType, que reconhece ".mp4"
                // de volta para TYPE_VID na hora de remontar o .pac.
                save(outDir, idx + ".mp4", src, dataPos, size);
                break;
            case TYPE_WEBP:
                // Bytes crus do webp animado, sem nenhum processamento —
                // mesmo tratamento simples de PNG/WAV/VID. Extensão .webp
                // espelha RealPacBuilder.detectType.
                save(outDir, idx + ".webp", src, dataPos, size);
                break;
            case TYPE_ACT:
                save(outDir, idx + ".act", src, dataPos, size);
                break;
            case TYPE_BIN:
                BinExtractor.process(packName, idx, src, dataPos, size, cnvType, outDir);
                break;
            case TYPE_CNV:
                CnvExtractor.process(packName, idx, src, dataPos, size, cnvType, outDir);
                break;
            case TYPE_DAC:
                if (cnvType == 3 && packName.equalsIgnoreCase("gamedata.pac"))
                    BinExtractor.process(packName, idx, src, dataPos, size, cnvType, outDir);
                else if (cnvType == 3 && packName.equalsIgnoreCase("text00.pac"))
                    Text00Extractor.process(packName, idx, src, dataPos, size, outDir);
                else DacExtractor.process(packName, idx, src, dataPos, size, cnvType, outDir);
                break;
            case TYPE_SPR:
                SprExtractor.process(packName, idx, src, dataPos, size, outDir);
                break;
        }
    }

    private static int decideCnvType(String name) {
        String n = name.toLowerCase();
        if (n.equals("gamedata.pac")) return 3;
        if (n.equals("text00.pac")) return 3;
        return 2;
    }

    // Portado do PacExtractor original do app — organização em subpastas
    // usada apenas quando PacBuilder.isApp == true.
    private static String decideCategory(String packName) {
        String n = packName.toLowerCase();
        if (n.startsWith("back") || n.startsWith("bobj")) return "BACKS";
        if (n.startsWith("card")) return "CARDS";
        if (n.startsWith("char")) return "CHARS";
        return "SYSTEM";
    }

    private static void save(File dir, String name,
            byte[] src, int pos, int size)
            throws IOException {
        FileOutputStream out = new FileOutputStream(new File(dir, name));
        try {
            out.write(src, pos, size);
        } finally {
            out.close();
        }
    }

    private static byte[] readAllBytes(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            byte[] b = new byte[(int) f.length()];
            int off = 0;
            while (off < b.length) {
                int r = in.read(b, off, b.length - off);
                if (r <= 0) break;
                off += r;
            }
            return b;
        } finally {
            in.close();
        }
    }
}