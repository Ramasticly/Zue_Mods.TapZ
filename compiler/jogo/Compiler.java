package com.zue.gd.util;

import java.io.*;
import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.namcobandaigames.dragonballtap.apk.*;

public class Compiler extends GameData {

    public static String MainPath = "/storage/emulated/0/Tap Battle/assets/";

    private static final int TYPE_PNG = 0x706E6700;
    private static final int TYPE_ACT = 0x61637400;
    private static final int TYPE_BIN = 0x62696E00;
    private static final int TYPE_CNV = 0x636E7600;
    private static final int TYPE_DAC = 0x64616300;
    private static final int TYPE_SPR = 0x73707200;
    private static final int TYPE_WAV = 0x77617600;
    private static final int TYPE_VID = 0x76696400; // NOVO — mesma tag "vid\0" do GameData.decodeData
    private static final int TYPE_WEBP = 0x77656200; // NOVO — mesma tag "web\0" do GameData.decodeData

    public Compiler() {
        super();
    }

    /** Espera o arquivo PAC ser extraído e liberado pelo sistema de arquivos. */
    private void waitForExtraction(String pacName) {
        File pacFile = new File(MainPath, pacName + ".pac");
        int attempts = 0;
        long lastSize = -1;

        while (!pacFile.exists() || pacFile.length() == 0) {
            if (attempts >= 50) return;
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            attempts++;
        }

        attempts = 0;
        while (attempts < 30) {
            long currentSize = pacFile.length();
            if (currentSize > 0 && currentSize == lastSize) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                }
                return;
            }
            lastSize = currentSize;
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            attempts++;
        }
    }

    public boolean startCompile(GlobalWork gw, TCBManajer tm, int slot) {
        try {
            int[] idList = (int[]) getReflectedField(tm, "IDList");
            int[] loadData = (int[]) getReflectedField(tm, "LoadData");
            int[] loadFilter = (int[]) getReflectedField(tm, "LoadFilter");
            Object[] chrGameData = (Object[]) getReflectedField(tm, "ChrGameData");

            String pacName = tm.GetResName2(gw, idList[slot], loadData[slot]);
            if (pacName == null) return false;

            File projectFolder = new File(MainPath, pacName);

            if (!projectFolder.exists()) {
                PacExtractor.extract(MainPath, pacName + ".pac");
                waitForExtraction(pacName);
            } else {
                cleanProjectFolder(MainPath, pacName);
            }

            PacBuilder.build(projectFolder.getAbsolutePath());

            if (pacName.equalsIgnoreCase("gamedata") || pacName.equalsIgnoreCase("text00")) {
                return true;
            }

            GameData result = this.loadFromProjectFolder(gw, pacName, 2, loadFilter[slot]);

            if (result != null) {
                chrGameData[slot] = result;
                return true;
            }
        } catch (Throwable th) {
        Logue.StackTrace(th);
        }
        return false;
    }

    public GameData loadSpecial(GlobalWork gw, String pacName, int cnvType, int loadFilter) {
        try {
            File projectFolder = new File(MainPath, pacName);

            if (!projectFolder.exists()) {
                PacExtractor.extract(MainPath, pacName + ".pac");
                waitForExtraction(pacName);
            } else {
                cleanProjectFolder(MainPath, pacName);
                PacBuilder.build(projectFolder.getAbsolutePath());
            }

            return this.loadFromProjectFolder(gw, pacName, cnvType, loadFilter);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private GameData loadFromProjectFolder(GlobalWork gw, String name, int cnvType, int loadFilter) {
        this.NAME = name;
        this.Dispose(gw);

        File folder = new File(MainPath, name);
        if (!folder.exists()) return null;

        try {
            Method decodeMethod = GameData.class.getDeclaredMethod("decodeData",
                    GlobalWork.class, byte
                            [].class, int.class, int.class, int.class, int.class, int.class);
            decodeMethod.setAccessible(true);

            File[] files = folder.listFiles();
            if (files == null) return null;

            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });

            for (File f : files) {
                String fileName = f.getName().toLowerCase();
                int type = -1;

                if (fileName.endsWith(".png")) type = TYPE_PNG;
                else if (fileName.endsWith(".act")) type = TYPE_ACT;
                else if (fileName.endsWith(".bin")) type = TYPE_BIN;
                else if (fileName.endsWith(".cnv")) type = TYPE_CNV;
                else if (fileName.endsWith(".dac")) type = TYPE_DAC;
                else if (fileName.endsWith(".wav")) type = TYPE_WAV;
                else if (fileName.endsWith(".spr")) type = TYPE_SPR;
                else if (fileName.endsWith(".mp4")) type = TYPE_VID; // NOVO
                else if (fileName.endsWith(".webp")) type = TYPE_WEBP; // NOVO — sem isto, .webp caía em "type == -1" e era ignorado (continue), nunca chegando ao decodeData

                if (type == -1) continue;

                if (type == TYPE_SPR) {
                    if ((loadFilter & 32) == 0) {
                        if (this.sprite != null) this.sprite.Dispose(gw);
                        this.sprite = new CompilerSpriteData(f.getAbsolutePath());
                        this.sprite.Init(gw, 0, (byte[]) null);
                    }
                } else {
                    byte[] fileData = readFullFile(f);
                    if (fileData != null) {
                        decodeMethod.invoke(this, gw, fileData, 0, fileData.length, type, cnvType, loadFilter);
                    }
                }
            }
            return this;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] getFileBytesByType(File folder, String idx, int type) throws IOException {
        File file = null;
        switch (type) {
            case TYPE_PNG:
                file = new File(folder, idx + ".png");
                break;
            case TYPE_WAV:
                file = new File(folder, idx + ".wav");
                break;
            case TYPE_ACT:
                file = new File(folder, idx + ".act");
                break;
            case TYPE_BIN:
                file = new File(folder, idx + ".bin");
                break;
            case TYPE_CNV:
                file = new File(folder, idx + ".cnv");
                break;
            case TYPE_DAC:
                file = new File(folder, idx + ".dac");
                break;
            case TYPE_VID:
                file = new File(folder, idx + ".mp4");
                break;
            case TYPE_WEBP:
                file = new File(folder, idx + ".webp");
                break;
            case TYPE_SPR:
                file = new File(new File(folder, idx + ".spr"), "000.bin");
                break;
        }
        return (file != null && file.exists()) ? readFullFile(file) : null;
    }

    private Object getReflectedField(Object obj, String fieldName) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(obj);
    }

    public void cleanProjectFolder(String path, String pacName) {
        String folderName = pacName.toLowerCase().endsWith(".pac") ? pacName.substring(0, pacName.length() - 4) : pacName;
        File folder = new File(path, folderName);
        File pacFile = new File(path, pacName + ".pac");
        if (!pacFile.exists() || !folder.exists()) return;
        try (FileInputStream in = new FileInputStream(pacFile)) {
            byte[] h = new byte[2];
            in.read(h);
            int count = (h[0] & 0xFF) | ((h[1] & 0xFF) << 8);
            byte[] table = new byte[count * 16];
            in.read(table);
            for (int i = 0; i < count; i++) {
                int typePos = (i * 16) + 8;
                int magic = ((table[typePos] & 0xFF) << 24) | ((table[typePos + 1] & 0xFF) << 16) |
                        ((table[typePos + 2] & 0xFF) << 8) | (table[typePos + 3] & 0xFF);
                if (magic != TYPE_PNG && magic != TYPE_WAV && magic != TYPE_VID && magic != TYPE_WEBP)
                    executeSelectiveDelete(folder, i, magic);
            }
        } catch (IOException e) {
        }
    }

    private void executeSelectiveDelete(File folder, int index, int magic) {
        String idx = String.format("%03d", index);
        if (magic == TYPE_SPR) {
            File sprBin = new File(new File(folder, idx + ".spr"), idx + ".bin");
            if (sprBin.exists()) sprBin.delete();
        } else {
            String[] exts = {".bin", ".cnv", ".dac", ".act"};
            for (String e : exts) {
                File f = new File(folder, idx + e);
                if (f.exists()) f.delete();
            }
        }
    }

    private byte[] readFullFile(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        try (FileInputStream in = new FileInputStream(f)) {
            in.read(b);
        }
        return b;
    }

    private byte[] readHeader(File f, int size) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[Math.min(size, (int) f.length())];
            in.read(b);
            return b;
        }
    }

    public static class CompilerSpriteData extends SpriteData {
        private String folderPath;

        public CompilerSpriteData(String path) {
            super();
            this.folderPath = path;
        }

        @Override
        public boolean Init(GlobalWork gw, int off, byte[] src) {
            try {
                Clear(gw);
                File sprFolder = new File(this.folderPath);
                if (!sprFolder.exists()) return false;

                File[] files = sprFolder.listFiles();
                if (files == null) return false;

                // CRÍTICO: File.listFiles() NÃO garante nenhuma ordem —
                // a ordem devolvida é interna do sistema de arquivos
                // (pode depender de data de criação, hash, ou qualquer
                // critério do SO), não necessariamente alfabética. Sem
                // este sort, a ordem em que os .webp são adicionados a
                // _animTexList/_animTex[] (e portanto o índice que
                // getAnimTex(N) resolve) fica IMPREVISÍVEL — causa
                // confirmada do bug "IMG:0 mostrando o webp errado"
                // (013.webp aparecendo no lugar de 012.webp). Mesma
                // ordenação usada em loadFromProjectFolder (alfabética,
                // compareToIgnoreCase) e implicitamente preservada pelo
                // mini-pac binário (que já vem ordenado por
                // RealPacBuilder/SprBuilder no momento do empacotamento).
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    }
                });

                Method loadTextureMethod = AndroidGLTexture.class.getDeclaredMethod("loadTexture",
                        javax.microedition.khronos.opengles.GL10.class, byte
                                [].class, int.class, int.class, boolean.class);
                loadTextureMethod.setAccessible(true);

                for (File f : files) {
                    String name = f.getName().toLowerCase();
                    byte[] fData = null;

                    try (FileInputStream in = new FileInputStream(f)) {
                        fData = new byte[(int) f.length()];
                        in.read(fData);
                    }

                    if (name.endsWith(".png")) {
                        AndroidGLTexture tex = new AndroidGLTexture();
                        boolean success = (boolean) loadTextureMethod.invoke(tex, gw.gl, fData, 0, fData.length, true);
                        if (success) {
                            this._imageList.add(tex);
                        }
                    } else if (name.endsWith(".bin")) {
                        this.pData[0] = fData;
                    } else if (name.endsWith(".webp")) {
                        // NOVO — mesmo princípio do branch "web" em
                        // SpriteData.Init(byte[]): fila própria
                        // _animTexList/_animTex[], separada de
                        // _imageList. Uma falha de decodificação aqui
                        // não aborta o resto do sprite (sem "return
                        // false"), mesmo espírito de png/bin ao lado.
                        AnimatedTexture animTex = new AnimatedWebpTexturePreloaded();
                        if (animTex.init(gw.gl, fData)) {
                            this._animTexList.add(animTex);
                            this._animTex = (AnimatedTexture[]) this._animTexList.toArray(
                                    new AnimatedTexture[this._animTexList.size()]);
                        }
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
