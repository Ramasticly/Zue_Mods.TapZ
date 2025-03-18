package com.namcobandaigames.dragonballtap.apk;

public final class Downloader {

    private static Downloader instance = null;

    public static Downloader getInstance() {
        if (instance == null) {
            instance = new Downloader();
        }
        return instance;
    }

    public void SetAPI(int type) {
    }

    public boolean SetURL(String url) {
        return false;
    }

    public boolean isDownload() {
        return true;
    }

    public byte[] GetData() {
        return new byte[0];
    }

    public int GetSize() {
        return 0;
    }

    public void Clear() {
    }
}
