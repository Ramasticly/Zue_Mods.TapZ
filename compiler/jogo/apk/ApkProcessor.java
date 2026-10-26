package com.zue.gd.util;

import android.content.Context;
import android.content.res.AssetManager;
import com.android.apksig.ApkSigner;
import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Gera o Mod APK (mesmo processo do app Tap Ult): os arquivos de assets/base_apk/ (dentro do APK do jogo)
 * vao para a raiz do APK novo, os arquivos da pasta do mod vao para assets/, e assina (V1 + V2 + V3) com
 * a chave assets/zuekeytst.p12. Precisa da biblioteca local "apksign" (com.android.apksig) no projeto.
 */
public class ApkProcessor {

    /** Opcoes do Mod APK (menu APK do jogo). null / -1 = como esta no base_apk. */
    public static final class Options {
        /** Nome de pacote novo (so o manifesto; as classes continuam onde estao). */
        public String packageName;
        public int minSdk = -1, targetSdk = -1;
        /** Icone novo (vai para os res/mipmap-.../ic_launcher.png e res/drawable-.../icon.png). */
        public android.graphics.Bitmap icon;
    }

    public interface ApkCallback {
        void onStep(String message);

        void onSuccess(File apkFile);

        void onError(String error);
    }

    // Centralizando o diretório temporário (no jogo: Tap Battle/Temp)
    private static File getTempFolder() {
        File tempDir = new File("/storage/emulated/0/Tap Battle/Temp/");
        if (!tempDir.exists()) tempDir.mkdirs();
        return tempDir;
    }

    public static void buildApk(Context context, File modAssetsSource, File outputApk, ApkCallback callback) {
        buildApk(context, modAssetsSource, outputApk, null, callback);
    }

    public static void buildApk(Context context, File modAssetsSource, File outputApk, Options opt, ApkCallback callback) {
        // 1. Tratamento do nome do arquivo (evitar .apk.apk)
        String finalPath = outputApk.getAbsolutePath();
        if (finalPath.toLowerCase().endsWith(".apk.apk")) {
            finalPath = finalPath.substring(0, finalPath.length() - 4);
            outputApk = new File(finalPath);
        }

        File tempUnsigned = new File(getTempFolder(), "unsigned_temp.apk");

        try {
            callback.onStep("Construindo estrutura do APK...");

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempUnsigned))) {
                // 2. Adiciona os arquivos BASE na RAIZ (sem o prefixo assets/)
                // Passamos "" como zipPath para que os arquivos de base_apk fiquem na raiz
                addFolderFromAssets(context, "base_apk", "", zos, opt);

                // 3. Adiciona os arquivos do MOD garantindo que fiquem em assets/
                if (modAssetsSource.exists()) {
                    addModFiles(modAssetsSource, modAssetsSource, zos);
                }
            }

            callback.onStep("Assinando APK (V2 + V3)...");
            signWithInternalKey(context, tempUnsigned, outputApk, callback);

            if (tempUnsigned.exists()) tempUnsigned.delete();
            callback.onSuccess(outputApk);

        } catch (Exception e) {
            callback.onError("Falha: " + e.getMessage());
            android.util.Log.e("ApkProcessor", "falha ao gerar o APK", e);
        }
    }

    private static void addFolderFromAssets(Context ctx, String assetPath, String zipPath, ZipOutputStream zos, Options opt)
            throws IOException {
        AssetManager am = ctx.getAssets();
        String[] files = am.list(assetPath);

        if (files != null && files.length > 0) {
            for (String file : files) {
                String newAssetPath = assetPath + "/" + file;
                // Se zipPath for vazio, o arquivo vai para a raiz.
                String newZipPath = zipPath.isEmpty() ? file : zipPath + "/" + file;
                addFolderFromAssets(ctx, newAssetPath, newZipPath, zos, opt);
            }
        } else {
            // Se chegamos aqui, é um arquivo.
            // IMPORTANTE: Ignoramos o nome da pasta pai "base_apk" no ZIP
            byte[] data;
            try (InputStream is = am.open(assetPath)) {
                data = inputToByte(is);
            }
            if (opt != null && zipPath.equals("AndroidManifest.xml")
                    && ((opt.packageName != null && opt.packageName.length() > 0) || opt.minSdk > 0 || opt.targetSdk > 0)) {
                data = AxmlPatcher.patch(data, opt.packageName, opt.minSdk, opt.targetSdk);
            }
            if (opt != null && opt.icon != null && isIcon(zipPath)) {
                data = iconPng(data, opt.icon);
            }
            ZipEntry entry = new ZipEntry(zipPath);
            if (zipPath.equals("resources.arsc")) {
                // target 30+ exige o resources.arsc sem compressao (o apksig alinha em 4 bytes ao assinar)
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(data.length);
                entry.setCompressedSize(data.length);
                entry.setCrc(calculateCrc(data));
            }
            zos.putNextEntry(entry);
            zos.write(data);
            zos.closeEntry();
        }
    }

    private static boolean isIcon(String zipPath) {
        return (zipPath.startsWith("res/mipmap") && zipPath.endsWith("/ic_launcher.png"))
                || (zipPath.startsWith("res/drawable") && zipPath.endsWith("/icon.png"));
    }

    /** Icone novo no tamanho do original (centralizado, sem distorcer, fundo transparente). */
    private static byte[] iconPng(byte[] original, android.graphics.Bitmap icon) throws IOException {
        android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeByteArray(original, 0, original.length, o);
        int w = o.outWidth > 0 ? o.outWidth : 96;
        int h = o.outHeight > 0 ? o.outHeight : 96;
        android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(out);
        float sc = Math.min((float) w / icon.getWidth(), (float) h / icon.getHeight());
        float dw = icon.getWidth() * sc, dh = icon.getHeight() * sc;
        android.graphics.RectF dst = new android.graphics.RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG | android.graphics.Paint.ANTI_ALIAS_FLAG);
        c.drawBitmap(icon, null, dst, p);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        out.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bos);
        out.recycle();
        return bos.toByteArray();
    }

    private static void addModFiles(File root, File source, ZipOutputStream zos)
            throws IOException {
        File[] files = source.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                addModFiles(root, file, zos);
            } else {
                String relativePath = root.toURI().relativize(file.toURI()).getPath();
                if (relativePath.startsWith("assets/")) relativePath = relativePath.substring(7);
                String zipEntryPath = "assets/" + relativePath;

                ZipEntry entry = new ZipEntry(zipEntryPath);

                // LÓGICA INVERSA: Comprimir apenas arquivos de estrutura/código
                // O que não estiver aqui, será armazenado (STORED) sem compressão
                boolean shouldCompress = zipEntryPath.endsWith(".dex") || zipEntryPath.endsWith(".xml") || zipEntryPath.endsWith(".dat") || zipEntryPath.endsWith(".txt");

                if (!shouldCompress) {
                    // Modo STORED (Para Áudios, Imagens, resources.arsc, etc.)
                    byte[] data = inputToByte(new FileInputStream(file));
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(data.length);
                    entry.setCompressedSize(data.length);
                    entry.setCrc(calculateCrc(data));

                    zos.putNextEntry(entry);
                    zos.write(data);
                } else {
                    // Modo DEFLATED (Compressão normal para código e manifestos)
                    entry.setMethod(ZipEntry.DEFLATED);
                    zos.putNextEntry(entry);
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[32768];
                        int len;
                        while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    private static void signWithInternalKey(Context context, File input, File output, ApkCallback callback)
            throws Exception {
        String ksName = "zuekeytst.p12";
        char[] pass = "12345678".toCharArray();

        File ksFile = new File(context.getCacheDir(), "temp_key.p12");
        copyAssetToFile(context, ksName, ksFile);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(ksFile)) {
            ks.load(fis, pass);
        }

        // MELHORIA: Em vez de fixar o alias, pegamos o primeiro que existir no arquivo
        // Isso evita o erro de "Chave não encontrada" se o alias mudou na conversão
        String alias = ks.aliases().nextElement();

        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, pass);
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        if (privateKey == null) {
            throw new Exception("Chave privada não encontrada no alias: " + alias);
        }

        ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder(
        alias, privateKey, Collections.singletonList(cert)).build();

        new ApkSigner.Builder(Collections.singletonList(config))
                .setInputApk(input)
                .setOutputApk(output)
                .setV1SigningEnabled(true) // Importante para Android 11
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
                .sign();

        ksFile.delete();
    }

    private static void copyAssetToFile(Context ctx, String name, File dest) throws IOException {
        try (InputStream is = ctx.getAssets().open(name);
                OutputStream os = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.flush();
        }
    }

    private static long calculateCrc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

// Converte InputStream para byte[] (necessário para calcular CRC antes de escrever)
    private static byte[] inputToByte(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[32768];
        int len;
        while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
        return bos.toByteArray();
    }
}
