package com.zue.gd.util;

import java.io.*;
import java.util.*;

// Empacotador ÚNICO de pasta → .pac (contagem + tabela de 16 bytes por bloco
// + dados), usado pelo PacBuilder no modo app, pelo PacBuilderView e pelo
// ModApkBuilder do jogo.
//
// Só entram no .pac os tipos que o jogo carrega (.png .act .bin .cnv .dac .wav
// .mp4 .webp e pastas .spr), na MESMA ordem do Compiler.loadFromProjectFolder
// (compareToIgnoreCase). Qualquer outro arquivo da pasta (.txt, .json, backups
// do editor, .nomedia...) fica de fora: antes ele entrava como bloco BIN e o
// GameData.decodeData o carregava por cima do bin real do personagem.
public class RealPacBuilder {

    public interface ProgressCallback {
        void onBuildStart(String pacName, int index, int total);
        void onSegmentPacked(String pacName, int segmentIndex, String fileName, int size);
        void onBuildDone(String pacName);
        void onError(String error);
    }

    private static final int TYPE_PNG = 0x706E6700;
    private static final int TYPE_ACT = 0x61637400;
    private static final int TYPE_BIN = 0x62696E00;
    private static final int TYPE_CNV = 0x636E7600;
    private static final int TYPE_DAC = 0x64616300;
    private static final int TYPE_SPR = 0x73707200;
    private static final int TYPE_WAV = 0x77617600;
    private static final int TYPE_VID = 0x76696400; // mesma tag "vid\0" do GameData.decodeData
    private static final int TYPE_WEBP = 0x77656200; // mesma tag "web\0" do GameData.decodeData

    private static final Comparator<File> BY_NAME = new Comparator<File>() {
        @Override public int compare(File a, File b) { return a.getName().compareToIgnoreCase(b.getName()); }
    };

    public static void buildSingle(File packDir, File outRoot, ProgressCallback cb) {
        if (HelpFolder.isHelp(packDir)) return; // documentação, não vira .pac
        try {
            String packName = packDir.getName();
            String pacFileName = packName.toLowerCase().endsWith(".pac") ? packName : packName + ".pac";

            if (cb != null) cb.onBuildStart(pacFileName, 0, 1);

            byte[] pacData = packFolderToBytes(packDir, pacFileName, cb);

            saveBytes(new File(outRoot, pacFileName), pacData);

            if (cb != null) cb.onBuildDone(pacFileName);
        } catch (Exception e) {
            if (cb != null) cb.onError(e.getMessage());
        }
    }

    /** Monta o .pac de uma pasta de pack (sem compilar os .txt — rode PacBuilder.build antes). */
    public static byte[] packFolder(File folder) throws IOException {
        return packFolderToBytes(folder, folder.getName(), null);
    }

    /** Tipo do bloco pela extensão (nome em minúsculas), ou -1 se o jogo não carrega esse arquivo. */
    public static int detectType(String lowerName) {
        if (lowerName.endsWith(".png")) return TYPE_PNG;
        if (lowerName.endsWith(".wav")) return TYPE_WAV;
        if (lowerName.endsWith(".act")) return TYPE_ACT;
        if (lowerName.endsWith(".bin")) return TYPE_BIN;
        if (lowerName.endsWith(".cnv")) return TYPE_CNV;
        if (lowerName.endsWith(".dac")) return TYPE_DAC;
        if (lowerName.endsWith(".mp4")) return TYPE_VID;
        if (lowerName.endsWith(".webp")) return TYPE_WEBP;
        return -1;
    }

    private static byte[] packFolderToBytes(File folder, String currentPacName, ProgressCallback cb) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return new byte[]{0, 0};

        Arrays.sort(files, BY_NAME);

        List<byte[]> segments = new ArrayList<byte[]>();
        List<Integer> types = new ArrayList<Integer>();

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String name = f.getName().toLowerCase();

            byte[] seg;
            int type;
            if (f.isDirectory()) {
                if (!name.endsWith(".spr")) continue;
                seg = packSpr(f);
                type = TYPE_SPR;
            } else {
                type = detectType(name);
                if (type == -1) continue;
                seg = readBytes(f);
            }
            segments.add(seg);
            types.add(type);

            if (cb != null) cb.onSegmentPacked(currentPacName, i, f.getName(), seg.length);
        }

        return assemble(segments, types);
    }

    // Mini-pac de um sprite: só o que GameData.SpriteData / CompilerSpriteData
    // leem (o .bin compilado, os .png e os .webp).
    private static byte[] packSpr(File dir) throws IOException {
        File[] files = dir.listFiles();
        List<byte[]> segments = new ArrayList<byte[]>();
        List<Integer> types = new ArrayList<Integer>();
        if (files != null) {
            Arrays.sort(files, BY_NAME);
            for (File f : files) {
                if (!f.isFile()) continue;
                String n = f.getName().toLowerCase();
                int t = n.endsWith(".bin") ? TYPE_BIN : n.endsWith(".png") ? TYPE_PNG : n.endsWith(".webp") ? TYPE_WEBP : -1;
                if (t == -1) continue;
                segments.add(readBytes(f));
                types.add(t);
            }
        }
        return assemble(segments, types);
    }

    private static byte[] assemble(List<byte[]> segments, List<Integer> types) throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        int count = segments.size();
        header.write(count & 0xFF);
        header.write((count >> 8) & 0xFF);

        int currentOffset = 0;
        for (int i = 0; i < count; i++) {
            byte[] segData = segments.get(i);
            int type = types.get(i);

            writeUint32LE(header, currentOffset);
            writeUint32LE(header, segData.length);

            header.write((type >> 24) & 0xFF);
            header.write((type >> 16) & 0xFF);
            header.write((type >> 8) & 0xFF);
            header.write(type & 0xFF);

            writeUint32LE(header, 0);

            data.write(segData);
            currentOffset += segData.length;
        }

        header.write(data.toByteArray());
        return header.toByteArray();
    }

    private static void writeUint32LE(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF); os.write((v >> 8) & 0xFF);
        os.write((v >> 16) & 0xFF); os.write((v >> 24) & 0xFF);
    }

    // Lê até o fim: um único in.read(b) pode devolver menos bytes que o
    // tamanho do arquivo, e o resto do bloco saía zerado no .pac.
    private static byte[] readBytes(File f) throws IOException {
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
        } finally { in.close(); }
    }

    private static void saveBytes(File f, byte[] data) throws IOException {
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(f);
        try { fos.write(data); } finally { fos.close(); }
    }
}
