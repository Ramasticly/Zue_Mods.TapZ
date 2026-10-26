package com.zue.gd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Monta a pasta "assets" do Mod APK a partir de Tap Battle/assets (mesmo resultado do export do app Tap Ult):
 *   - cada pasta de pack: compila os .txt com o PacBuilder DO JOGO (igual ao Compiler em runtime: limpa os
 *     binarios antigos e chama PacBuilder.build) e empacota num <pack>.pac;
 *   - .pac soltos sem pasta (pack nunca extraido): copiados como estao;
 *   - bgm_*.ogg e se_*.ogg soltos na pasta: copiados.
 * O empacotamento e o mesmo formato do RealPacBuilder, mas so com os tipos que o jogo carrega
 * (.png .act .bin .cnv .dac .wav .spr .mp4 .webp) e na mesma ordem do Compiler.loadFromProjectFolder
 * (compareToIgnoreCase): arquivos extras na pasta (ex.: os backups .antes_editor do editor) nao entram
 * no .pac e nao mudam a posicao dos blocos.
 * Rodar com o jogo parado (o PacBuilder e estatico e o jogo tambem o usa no SetLoad).
 */
public final class ModApkBuilder {
    private ModApkBuilder() { }

    public interface Progress {
        void step(String message, int percent);
    }

    private static final int TYPE_PNG = 0x706E6700;
    private static final int TYPE_ACT = 0x61637400;
    private static final int TYPE_BIN = 0x62696E00;
    private static final int TYPE_CNV = 0x636E7600;
    private static final int TYPE_DAC = 0x64616300;
    private static final int TYPE_SPR = 0x73707200;
    private static final int TYPE_WAV = 0x77617600;
    private static final int TYPE_VID = 0x76696400;
    private static final int TYPE_WEBP = 0x77656200;

    private static final Comparator<File> BY_NAME = new Comparator<File>() {
        @Override
        public int compare(File a, File b) {
            return a.getName().compareToIgnoreCase(b.getName());
        }
    };

    /** Resultado: quantos packs compilados, copiados e sons. */
    public static final class Result {
        public int compiled, copied, sounds;
        public final List<String> failed = new ArrayList<String>();
    }

    /**
     * @param assetsRoot Tap Battle/assets
     * @param out        pasta de saida (apagada e recriada)
     */
    public static Result buildAssets(File assetsRoot, File out, Progress p) throws IOException {
        Result r = new Result();
        deleteTree(out);
        if (!out.mkdirs() && !out.isDirectory()) {
            throw new IOException("nao consegui criar " + out.getPath());
        }
        File[] all = assetsRoot.listFiles();
        if (all == null) {
            throw new IOException("pasta nao encontrada: " + assetsRoot.getPath());
        }
        Arrays.sort(all, BY_NAME);

        List<File> packs = new ArrayList<File>();
        for (File f : all) {
            if (f.isDirectory() && isPackFolder(f)) {
                packs.add(f);
            }
        }

        // 1. pastas de pack: compila e empacota
        for (int i = 0; i < packs.size(); ++i) {
            File dir = packs.get(i);
            String name = dir.getName();
            if (p != null) {
                p.step("Compilando " + name + " (" + (i + 1) + "/" + packs.size() + ")", packs.isEmpty() ? 0 : i * 85 / packs.size());
            }
            try {
                cleanProjectFolder(assetsRoot, name);
                PacBuilder.build(dir.getAbsolutePath());
                writeFile(new File(out, name + ".pac"), packFolder(dir));
                r.compiled++;
            } catch (Throwable e) {
                r.failed.add(name + ": " + e.getMessage());
            }
        }

        // 2. .pac sem pasta e sons soltos
        if (p != null) {
            p.step("Copiando .pac e sons...", 88);
        }
        for (File f : all) {
            if (!f.isFile()) {
                continue;
            }
            String n = f.getName().toLowerCase();
            if (n.endsWith(".pac")) {
                File dir = new File(assetsRoot, f.getName().substring(0, f.getName().length() - 4));
                if (!dir.isDirectory() || !isPackFolder(dir)) {
                    copyFile(f, new File(out, f.getName()));
                    r.copied++;
                }
            } else if (n.endsWith(".ogg") && (n.startsWith("bgm_") || n.startsWith("se_"))) {
                copyFile(f, new File(out, f.getName()));
                r.sounds++;
            }
        }
        return r;
    }

    /** Pasta de pack: tem algum arquivo direto que o compilador ou o jogo usa. */
    static boolean isPackFolder(File dir) {
        if (dir.getName().startsWith(".")) {
            return false;
        }
        File[] fs = dir.listFiles();
        if (fs == null) {
            return false;
        }
        for (File f : fs) {
            String n = f.getName().toLowerCase();
            if (f.isFile() && (n.endsWith(".txt") || typeOf(n) != -1)) {
                return true;
            }
            if (f.isDirectory() && n.endsWith(".spr")) {
                return true;
            }
        }
        return false;
    }

    static int typeOf(String lowerName) {
        if (lowerName.endsWith(".png")) return TYPE_PNG;
        if (lowerName.endsWith(".act")) return TYPE_ACT;
        if (lowerName.endsWith(".bin")) return TYPE_BIN;
        if (lowerName.endsWith(".cnv")) return TYPE_CNV;
        if (lowerName.endsWith(".dac")) return TYPE_DAC;
        if (lowerName.endsWith(".wav")) return TYPE_WAV;
        if (lowerName.endsWith(".mp4")) return TYPE_VID;
        if (lowerName.endsWith(".webp")) return TYPE_WEBP;
        return -1;
    }

    /** Mesmo formato do RealPacBuilder (contagem + tabela de 16 bytes por bloco + dados), filtrado e ordenado. */
    static byte[] packFolder(File folder) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) {
            return new byte[]{0, 0};
        }
        Arrays.sort(files, BY_NAME);
        List<byte[]> segs = new ArrayList<byte[]>();
        List<Integer> types = new ArrayList<Integer>();
        for (File f : files) {
            String n = f.getName().toLowerCase();
            if (f.isDirectory()) {
                if (n.endsWith(".spr")) {
                    segs.add(packSpr(f));
                    types.add(Integer.valueOf(TYPE_SPR));
                }
                continue;
            }
            int t = typeOf(n);
            if (t == -1) {
                continue;                        // .txt, backups, .nomedia...: fora do .pac
            }
            segs.add(readFile(f));
            types.add(Integer.valueOf(t));
        }
        return assemble(segs, types);
    }

    /** Sprite (.spr): mini-pac com o 000.bin, os .png e os .webp (o que o CompilerSpriteData le). */
    static byte[] packSpr(File dir) throws IOException {
        File[] files = dir.listFiles();
        List<byte[]> segs = new ArrayList<byte[]>();
        List<Integer> types = new ArrayList<Integer>();
        if (files != null) {
            Arrays.sort(files, BY_NAME);
            for (File f : files) {
                String n = f.getName().toLowerCase();
                if (!f.isFile()) {
                    continue;
                }
                int t = n.endsWith(".bin") ? TYPE_BIN : n.endsWith(".png") ? TYPE_PNG : n.endsWith(".webp") ? TYPE_WEBP : -1;
                if (t == -1) {
                    continue;
                }
                segs.add(readFile(f));
                types.add(Integer.valueOf(t));
            }
        }
        return assemble(segs, types);
    }

    static byte[] assemble(List<byte[]> segs, List<Integer> types) throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        int count = segs.size();
        header.write(count & 0xFF);
        header.write((count >> 8) & 0xFF);
        int offset = 0;
        for (int i = 0; i < count; ++i) {
            int len = segs.get(i).length;
            int type = types.get(i).intValue();
            writeU32(header, offset);
            writeU32(header, len);
            header.write((type >> 24) & 0xFF);
            header.write((type >> 16) & 0xFF);
            header.write((type >> 8) & 0xFF);
            header.write(type & 0xFF);
            writeU32(header, 0);
            offset += len;
        }
        for (int i = 0; i < count; ++i) {
            header.write(segs.get(i));
        }
        return header.toByteArray();
    }

    /** Mesma limpeza do Compiler.cleanProjectFolder antes do PacBuilder.build (o runtime faz igual). */
    static void cleanProjectFolder(File root, String pacName) {
        File folder = new File(root, pacName);
        File pacFile = new File(root, pacName + ".pac");
        if (!pacFile.exists() || !folder.exists()) {
            return;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(pacFile);
            byte[] h = new byte[2];
            if (in.read(h) != 2) {
                return;
            }
            int count = (h[0] & 0xFF) | ((h[1] & 0xFF) << 8);
            byte[] table = new byte[count * 16];
            int got = 0;
            while (got < table.length) {
                int n = in.read(table, got, table.length - got);
                if (n <= 0) {
                    break;
                }
                got += n;
            }
            for (int i = 0; i < count && (i * 16) + 12 <= got; ++i) {
                int tp = (i * 16) + 8;
                int magic = ((table[tp] & 0xFF) << 24) | ((table[tp + 1] & 0xFF) << 16) | ((table[tp + 2] & 0xFF) << 8) | (table[tp + 3] & 0xFF);
                if (magic == TYPE_PNG || magic == TYPE_WAV || magic == TYPE_VID || magic == TYPE_WEBP) {
                    continue;
                }
                String idx = String.format("%03d", Integer.valueOf(i));
                if (magic == TYPE_SPR) {
                    File sprBin = new File(new File(folder, idx + ".spr"), idx + ".bin");
                    if (sprBin.exists()) {
                        sprBin.delete();
                    }
                } else {
                    String[] exts = {".bin", ".cnv", ".dac", ".act"};
                    for (String e : exts) {
                        File f = new File(folder, idx + e);
                        if (f.exists()) {
                            f.delete();
                        }
                    }
                }
            }
        } catch (IOException e) {
            // igual ao runtime: sem limpeza, o build regrava por cima
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // nada
                }
            }
        }
    }

    // ---- arquivos ----

    static void deleteTree(File f) {
        if (!f.exists()) {
            return;
        }
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                deleteTree(k);
            }
        }
        f.delete();
    }

    static byte[] readFile(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        try {
            int got = 0;
            while (got < b.length) {
                int n = in.read(b, got, b.length - got);
                if (n <= 0) {
                    break;
                }
                got += n;
            }
        } finally {
            in.close();
        }
        return b;
    }

    static void writeFile(File f, byte[] data) throws IOException {
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileOutputStream o = new FileOutputStream(f);
        try {
            o.write(data);
        } finally {
            o.close();
        }
    }

    static void copyFile(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        try {
            FileOutputStream o = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    o.write(buf, 0, n);
                }
            } finally {
                o.close();
            }
        } finally {
            in.close();
        }
    }

    private static void writeU32(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
        os.write((v >> 16) & 0xFF);
        os.write((v >> 24) & 0xFF);
    }
}
