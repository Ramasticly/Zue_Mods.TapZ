package com.zue.gd.util;

import java.io.*;
import java.util.*;
import java.lang.reflect.Field;
import com.namcobandaigames.dragonballtap.apk.*;

// Lado do JOGO (só existe no projeto do jogo — depende de GameData/TCBManajer).
// Em runtime, PacBuilder.isApp fica sempre false: extrai/compila na própria
// pasta do pack (MainPath/<pack>/) e carrega os binários soltos de lá.
public class Compiler extends GameData {

    public static String MainPath = "/storage/emulated/0/Tap Battle/assets/";

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
        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    };

    // Pasta/parâmetros do último loadFromProjectFolder — usados por GLReload.
    // null = este objeto foi carregado do .pac original (GameData.Init).
    private File loadedFolder = null;
    private int loadedFilter = 0;

    public Compiler() {
        super();
    }

    /**
     * Garante a pasta do projeto (extrai o .pac se ela ainda não existe) e
     * recompila os .txt. Retorna false se não há nem pasta nem .pac em MainPath.
     *
     * PacExtractor.extract é síncrono: a espera por arquivo que existia aqui
     * (waitForExtraction) olhava o próprio .pac de origem e só dormia
     * ~300 ms por pack — até ~9 s num SetLoad de 31 slots.
     */
    private static boolean prepareProjectFolder(String pacName) throws Exception {
        HelpFolder.garantir(new File(MainPath)); // #HELP junto das pastas dos packs (uma vez por execução)
        File projectFolder = new File(MainPath, pacName);
        if (!projectFolder.isDirectory()) {
            if (!new File(MainPath, pacName + ".pac").isFile()) return false;
            PacExtractor.extract(MainPath, pacName + ".pac");
        }
        PacBuilder.cleanProjectFolder(projectFolder);
        PacBuilder.build(projectFolder.getAbsolutePath());
        return true;
    }

    public boolean startCompile(GlobalWork gw, TCBManajer tm, int slot) {
        Object[] chrGameData;
        try {
            int[] idList = (int[]) getReflectedField(tm, "IDList");
            int[] loadData = (int[]) getReflectedField(tm, "LoadData");
            int[] loadFilter = (int[]) getReflectedField(tm, "LoadFilter");
            chrGameData = (Object[]) getReflectedField(tm, "ChrGameData");

            String pacName = tm.GetResName2(gw, idList[slot], loadData[slot]);
            if (pacName == null) return false;

            if (pacName.equalsIgnoreCase("gamedata") || pacName.equalsIgnoreCase("text00")) {
                // carregados à parte por InitGameData (loadSpecial); aqui só compila
                return prepareProjectFolder(pacName);
            }

            // O SetLoad original fazia ChrGameData[slot].Init(...), que libera as
            // texturas do pack anterior antes de carregar. Trocar o objeto do slot
            // sem Dispose vazava todas as texturas GL de cada pack a cada luta.
            GameData old = (GameData) chrGameData[slot];
            if (old != null && old != this) old.Dispose(gw);
            chrGameData[slot] = this; // em caso de falha o slot fica com um GameData vazio, como no Init original

            boolean fromFolder = prepareProjectFolder(pacName);

            if (fromFolder) {
                return this.loadFromProjectFolder(gw, pacName, 2, loadFilter[slot]) != null;
            }
            // Sem pasta nem .pac em MainPath: usa o .pac original do APK
            // (mesmo caminho do SetLoad original).
            return this.loadFromAssets(gw, pacName, 2, loadFilter[slot]);
        } catch (Throwable th) {
            Logue.StackTrace(th);
        }
        return false;
    }

    public GameData loadSpecial(GlobalWork gw, String pacName, int cnvType, int loadFilter) {
        try {
            // Antes, na primeira execução (pasta ainda não existia) só extraía e
            // NÃO compilava: o gamedata/text00 carregado ficava sem o 000.dac e
            // piGameData nulo derrubava o jogo logo no início.
            if (prepareProjectFolder(pacName)) {
                GameData r = this.loadFromProjectFolder(gw, pacName, cnvType, loadFilter);
                if (r != null) return r;
            }
            return this.loadFromAssets(gw, pacName, cnvType, loadFilter) ? this : null;
        } catch (Exception e) {
            Logue.StackTrace(e);
            return this.loadFromAssets(gw, pacName, cnvType, loadFilter) ? this : null;
        }
    }

    private boolean loadFromAssets(GlobalWork gw, String pacName, int cnvType, int loadFilter) {
        this.loadedFolder = null;
        return super.Init(gw, pacName, cnvType, loadFilter);
    }

    private GameData loadFromProjectFolder(GlobalWork gw, String name, int cnvType, int loadFilter) {
        this.Dispose(gw);
        this.NAME = name;
        this.loadedFolder = null;

        File folder = new File(MainPath, name);
        if (!folder.isDirectory()) return null;

        try {
            File[] files = folder.listFiles();
            if (files == null) return null;
            Arrays.sort(files, BY_NAME);

            for (File f : files) {
                String fileName = f.getName().toLowerCase();

                if (f.isDirectory()) {
                    if (fileName.endsWith(".spr") && (loadFilter & 32) == 0) {
                        if (this.sprite != null) this.sprite.Dispose(gw);
                        this.sprite = new CompilerSpriteData(f.getAbsolutePath());
                        this.sprite.Init(gw, 0, (byte[]) null);
                    }
                    continue;
                }

                int type = typeOf(fileName);
                if (type == -1) continue;

                byte[] fileData = readFullFile(f);
                if (!this.decodeData(gw, fileData, 0, fileData.length, type, cnvType, loadFilter)) {
                    return null; // decodeData já fez Dispose (bin/cnv/dac inválido)
                }
            }

            this.loadedFolder = folder;
            this.loadedFilter = loadFilter;
            // Init(byte[]) não é usado aqui, então o registro para recarga após
            // perda do contexto GL (app em segundo plano) precisa ser feito à mão
            // — sem isto, as texturas deste pack ficavam com ID 0 (invisíveis).
            registerForGLReload(this);
            return this;
        } catch (Exception e) {
            Logue.StackTrace(e);
            return null;
        }
    }

    /**
     * Recarga pós-perda de contexto EGL de um pack montado a partir da pasta:
     * relê os .png/.spr da MESMA pasta, na MESMA ordem do load, para os MESMOS
     * objetos (índices continuam válidos). O GLReload da classe base reabriria
     * o .pac ORIGINAL dos assets, com outras imagens.
     */
    @Override
    public boolean GLReload(GlobalWork gw) {
        if (this.loadedFolder == null) return super.GLReload(gw);

        File[] files = this.loadedFolder.listFiles();
        if (files == null) return false;
        Arrays.sort(files, BY_NAME);

        int k = 0;
        for (File f : files) {
            String n = f.getName().toLowerCase();
            try {
                if (f.isDirectory()) {
                    if (n.endsWith(".spr") && (this.loadedFilter & 32) == 0 && this.sprite != null) {
                        this.sprite.Init(gw, 0, (byte[]) null); // CompilerSpriteData relê a pasta
                    }
                } else if (n.endsWith(".png") && (this.loadedFilter & 1) == 0) {
                    if (k < this.imageList.size()) {
                        GameData.loadPng(gw, (AndroidGLTexture) this.imageList.get(k), readFullFile(f));
                    }
                    k++;
                }
            } catch (IOException e) {
                Logue.StackTrace(e);
            }
        }
        reloadDerivedGL(gw, this.loadedFilter);
        return true;
    }

    private static int typeOf(String lowerName) {
        if (lowerName.endsWith(".png")) return TYPE_PNG;
        if (lowerName.endsWith(".act")) return TYPE_ACT;
        if (lowerName.endsWith(".bin")) return TYPE_BIN;
        if (lowerName.endsWith(".cnv")) return TYPE_CNV;
        if (lowerName.endsWith(".dac")) return TYPE_DAC;
        if (lowerName.endsWith(".wav")) return TYPE_WAV;
        if (lowerName.endsWith(".mp4")) return TYPE_VID;
        if (lowerName.endsWith(".webp")) return TYPE_WEBP; // sem isto, .webp era ignorado e nunca chegava ao decodeData
        return -1;
    }

    private Object getReflectedField(Object obj, String fieldName) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(obj);
    }

    /** Mantido por compatibilidade: mesma limpeza segura do PacBuilder. */
    public void cleanProjectFolder(String path, String pacName) {
        String folderName = pacName.toLowerCase().endsWith(".pac") ? pacName.substring(0, pacName.length() - 4) : pacName;
        PacBuilder.cleanProjectFolder(new File(path, folderName));
    }

    // Lê até o fim (um único in.read pode devolver menos que o tamanho do arquivo).
    private static byte[] readFullFile(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        try {
            int off = 0;
            while (off < b.length) {
                int r = in.read(b, off, b.length - off);
                if (r <= 0) break;
                off += r;
            }
        } finally {
            in.close();
        }
        return b;
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
                // compareToIgnoreCase) e no RealPacBuilder.packSpr.
                Arrays.sort(files, BY_NAME);

                for (File f : files) {
                    if (!f.isFile()) continue;
                    String name = f.getName().toLowerCase();
                    if (!name.endsWith(".png") && !name.endsWith(".bin") && !name.endsWith(".webp")) {
                        continue; // .bin.txt e afins: não lê à toa
                    }
                    byte[] fData = readFullFile(f);

                    if (name.endsWith(".png")) {
                        // Entra na lista mesmo se falhar (GetImage() == 0, não
                        // desenha), como em GameData.decodeData: pular a textura
                        // deslocava o índice de todas as imagens seguintes.
                        AndroidGLTexture tex = new AndroidGLTexture();
                        GameData.loadPng(gw, tex, fData);
                        this._imageList.add(tex);
                    } else if (name.endsWith(".bin")) {
                        this.pData[0] = fData;
                    } else {
                        // mesmo princípio do branch "web" em
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
                Logue.StackTrace(e);
                return false;
            }
        }
    }
}
