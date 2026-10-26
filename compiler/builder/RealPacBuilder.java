package com.zue.gd.util;

import java.io.*;
import java.util.*;

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
    private static final int TYPE_VID = 0x76696400; // NOVO — mesma tag "vid\0" do GameData.decodeData
    private static final int TYPE_WEBP = 0x77656200; // NOVO — mesma tag "web\0" do GameData.decodeData

    public static void buildSingle(File packDir, File outRoot, ProgressCallback cb) {
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

    private static byte[] packFolderToBytes(File folder, String currentPacName, ProgressCallback cb) throws Exception {
        File[] files = folder.listFiles();
        if (files == null) return new byte[0];

        Arrays.sort(files, new Comparator<File>() {
            @Override public int compare(File a, File b) { return a.getName().compareTo(b.getName()); }
        });

        List<byte[]> segments = new ArrayList<byte[]>();
        List<Integer> types = new ArrayList<Integer>();

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String name = f.getName().toLowerCase();

            if (name.endsWith(".txt") || name.endsWith(".json")) continue;

            if (f.isDirectory() && name.endsWith(".spr")) {
                byte[] subPac = packFolderToBytes(f, name, null);
                segments.add(subPac);
                types.add(TYPE_SPR);
            } else if (f.isFile()) {
                segments.add(readBytes(f));
                types.add(detectType(name));
            }

            if (cb != null) cb.onSegmentPacked(currentPacName, i, f.getName(), 0);
        }

        return assemble(segments, types);
    }

    private static int detectType(String name) {
        if (name.endsWith(".png")) return TYPE_PNG;
        if (name.endsWith(".wav")) return TYPE_WAV;
        if (name.endsWith(".act")) return TYPE_ACT;
        if (name.endsWith(".cnv")) return TYPE_CNV;
        if (name.endsWith(".dac")) return TYPE_DAC;
        if (name.endsWith(".spr")) return TYPE_SPR;
        if (name.endsWith(".mp4")) return TYPE_VID; // NOVO — sem isto, cai no fallback TYPE_BIN abaixo, errado
        if (name.endsWith(".webp")) return TYPE_WEBP; // NOVO — idem, sem isto webp cairia como TYPE_BIN
        return TYPE_BIN;
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

    private static byte[] readBytes(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            byte[] b = new byte[(int) f.length()];
            in.read(b);
            return b;
        } finally { in.close(); }
    }

    private static void saveBytes(File f, byte[] data) throws IOException {
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(f);
        try { fos.write(data); } finally { fos.close(); }
    }
}
