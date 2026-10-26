package com.zue.gd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
 * O empacotamento e o do RealPacBuilder.packFolder (o mesmo do modo app): so os tipos que o jogo
 * carrega (.png .act .bin .cnv .dac .wav .spr .mp4 .webp), na mesma ordem do
 * Compiler.loadFromProjectFolder (compareToIgnoreCase); arquivos extras na pasta (ex.: os backups
 * .antes_editor do editor) nao entram no .pac e nao mudam a posicao dos blocos.
 * Rodar com o jogo parado (o PacBuilder e estatico e o jogo tambem o usa no SetLoad).
 */
public final class ModApkBuilder {
    private ModApkBuilder() { }

    public interface Progress {
        void step(String message, int percent);
    }

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
                PacBuilder.cleanProjectFolder(dir);
                PacBuilder.build(dir.getAbsolutePath());
                writeFile(new File(out, name + ".pac"), RealPacBuilder.packFolder(dir));
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
            if (f.isFile() && (n.endsWith(".txt") || RealPacBuilder.detectType(n) != -1)) {
                return true;
            }
            if (f.isDirectory() && n.endsWith(".spr")) {
                return true;
            }
        }
        return false;
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
}
