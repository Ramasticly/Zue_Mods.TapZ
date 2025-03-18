package com.namcobandaigames.dragonballtap.apk;

import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class GameData {

    public static final int SOUND_MAX = 20;
    public static final int TYPE_ACT = 1;
    public static final int TYPE_BIN = 2;
    public static final int TYPE_CNV = 0;
    public static final int TYPE_DAC = 3;
    public static final int TYPE_MAX = 4;
    protected final byte[][] data = new byte[4][];
    protected int iSoundCount = 0;
    protected final ArrayList imageList = new ArrayList();
    public short[] piGameData = null;
    public int[] piGameDataPos = null;
    public int[] piGameDataXSize = null;
    public int[] piGameDataYSize = null;
    protected final byte[][] sound = new byte[20][];
    protected SpriteData sprite = null;
    protected String NAME;

    public static class SpriteData {

        protected ArrayList _imageList = new ArrayList();
        protected byte[][] pData = new byte[1][];

        public byte[] getData() {
            return this.pData[0];
        }

        public boolean Init(GlobalWork gw, int off, ByteBuffer src) {
            return false;
        }

        public boolean Init(GlobalWork gw, int off, byte[] src) {
            try {
                Clear(gw);
                if (src == null) {
                    return false;
                }
                int dataCount = (src[off] & 255) | ((src[off + 1] & 255) << 8);
                for (int i = 0; i < dataCount; i++) {
                    int pos = off + (i << 4) + 2;
                    int dataSize = (src[pos + 4] & 255)
                            | ((src[pos + 5] & 255) << 8)
                            | ((src[pos + 6] & 255) << 16)
                            | ((src[pos + 7] & 255) << 24);
                    int dataPos = off + ((dataCount << 4) + 2) + ((src[pos + 0] & 255)
                            | ((src[pos + 1] & 255) << 8)
                            | ((src[pos + 2] & 255) << 16)
                            | ((src[pos + 3] & 255) << 24));
                    pos += 8;
                    //png
                    if (src[pos] == (byte) 112 && src[pos + 1] == (byte) 110 && src[pos + 2] == (byte) 103) {
                        AndroidGLTexture tex = new AndroidGLTexture();
//                        if (tex == null) {
//                            Dispose(gw);
//                            return false;
//                        } else 
                        if (!tex.loadTexture(gw.gl, src, dataPos, dataSize, true)) {
                            return false;
                        } else {
                            this._imageList.add(tex);
                        }
                        //bin
                    } else if (src[pos] == (byte) 98 && src[pos + 1] == (byte) 105 && src[pos + 2] == (byte) 110) {
                        this.pData[0] = new byte[dataSize];
                        if (this.pData[0] == null) {
                            return false;
                        }
                        System.arraycopy(src, dataPos, this.pData[0], 0, dataSize);
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public void Clear(GlobalWork gw) {
            int i;
            if (this.pData != null) {
                for (i = 0; i < this.pData.length; i++) {
                    this.pData[i] = null;
                }
            }
            for (i = 0; i < this._imageList.size(); i++) {
                ((AndroidGLTexture) this._imageList.get(i)).Dispose(gw.gl);
            }
            this._imageList.clear();
        }

        public void Dispose(GlobalWork gw) {
            Clear(gw);
            this.pData = (byte[][]) null;
            this._imageList = null;
        }

        public int getImage(int no) {
            if (no >= 0 && no < this._imageList.size()) {
                AndroidGLTexture tex = (AndroidGLTexture) this._imageList.get(no);
                if (tex != null) {
                    return tex.GetImage();
                }
            }
            return 0;
        }

        public int getImageWidth(int no) {
            if (no >= 0 && no < this._imageList.size()) {
                AndroidGLTexture tex = (AndroidGLTexture) this._imageList.get(no);
                if (tex != null) {
                    return tex.GetWidth();
                }
            }
            return 1;
        }

        public int getImageHeight(int no) {
            if (no >= 0 && no < this._imageList.size()) {
                AndroidGLTexture tex = (AndroidGLTexture) this._imageList.get(no);
                if (tex != null) {
                    return tex.GetHeight();
                }
            }
            return 1;
        }
    }

    public boolean Init(GlobalWork gw, String name, int cnvType, int loadFilter) {
        NAME = name;
        if (NAME.startsWith("back") && !NAME.contains("obj")) {
            try {
                throw new RuntimeException("BACK loaded " + NAME + " " + TCBManajer.MD);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        boolean z = false;
        Dispose(gw);
        if (name != null && name.length() > 0) {
            try {
                z = Init(gw, Utility.getBytes(gw.context.getAssets().open(name + ".pac")), cnvType, loadFilter);
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
        return z;
    }

    public boolean Init(GlobalWork gw, byte[] src, int cnvType, int loadFilter) {
        Dispose(gw);
        if (src == null) {
            return false;
        }
        int dataCount = (src[0] & 255) | ((src[1] & 255) << 8);
        for (int i = 0; i < dataCount; i++) {
            int pos = (i << 4) + 2;
            int dataSize = (src[pos + 4] & 255)
                    | ((src[pos + 5] & 255) << 8)
                    | ((src[pos + 6] & 255) << 16)
                    | ((src[pos + 7] & 255) << 24);
            int dataPos = ((dataCount << 4) + 2)
                    + ((src[pos + 0] & 255)
                    | ((src[pos + 1] & 255) << 8)
                    | ((src[pos + 2] & 255) << 16)
                    | ((src[pos + 3] & 255) << 24));
            pos += 8;
            int type = ((src[pos] & 255) << 24) | ((src[pos + 1] & 255) << 16) | ((src[pos + 2] & 255) << 8) | (src[pos + 3] & 255);
            if (!decodeData(gw, src, dataPos, dataSize, type, cnvType, loadFilter)) {
                return false;
            }
        }
        return true;
    }

    private boolean decodeData(GlobalWork gw, byte[] src, int dataPos, int dataSize, int dataType, int cnvType, int loadFilter) {
        switch (dataType) {
            case 0x706e6700: //png
                if ((loadFilter & 1) == 0) {
                    AndroidGLTexture tex = new AndroidGLTexture();
                    if (!tex.loadTexture(gw.gl, src, dataPos, dataSize, true)) {
                        this.imageList.add(tex);
                    } else {
                        this.imageList.add(tex);
                    }
                }
                break;
            case 0x61637400: //act
                if ((loadFilter & 2) == 0) {
                    if (cnvType != 1) {
                        this.data[1] = null;
                        this.data[1] = new byte[dataSize];
                        System.arraycopy(src, dataPos, this.data[1], 0, dataSize);
                    } else if (!binCnv(1, src, dataPos, dataSize)) {
                        Dispose(gw);
                        return false;
                    }
                }
                break;
            case 0x62696e00://bin
                if ((loadFilter & 4) == 0) {
                    if (cnvType != 2) {
                        this.data[2] = null;
                        this.data[2] = new byte[dataSize];
                        System.arraycopy(src, dataPos, this.data[2], 0, dataSize);
                    } else if (!binCnv(2, src, dataPos, dataSize)) {
                        Dispose(gw);
                        return false;
                    }
                }
                break;
            case 0x636e7600://cnv
                if ((loadFilter & 8) == 0) {
                    if (cnvType != 0) {
                        this.data[0] = null;
                        this.data[0] = new byte[dataSize];
                        System.arraycopy(src, dataPos, this.data[0], 0, dataSize);
                    } else if (!binCnv(3, src, dataPos, dataSize)) {
                        Dispose(gw);
                        return false;
                    }
                }
                break;
            case 0x64616300://dac
                if ((loadFilter & 16) == 0) {
                    if (cnvType != 3) {
                        this.data[3] = null;
                        this.data[3] = new byte[dataSize];
                        System.arraycopy(src, dataPos, this.data[3], 0, dataSize);
                    } else if (!binCnv(4, src, dataPos, dataSize)) {
                        Dispose(gw);
                        return false;
                    }
                }
                break;
            case 0x73707200:  //spr
                if ((loadFilter & 32) == 0) {
                    if (this.sprite != null) {
                        this.sprite.Dispose(gw);
                        this.sprite = null;
                    }
                    if (dataSize > 0) {
                        this.sprite = new SpriteData();
                        this.sprite.Init(gw, dataPos, src);
                    }
                }
                break;
            case 0x77617600:   //wav  
                this.sound[this.iSoundCount] = null;
                this.sound[this.iSoundCount] = new byte[dataSize];
                if (this.sound[this.iSoundCount] != null) {
                    System.arraycopy(src, dataPos, this.sound[this.iSoundCount++], 0, dataSize);
                }
        }
        return true;
    }

    public int getSoundCount() {
        return this.iSoundCount;
    }

    public byte[] getSoundData(int no) {
        return this.sound[no];
    }

    public byte[] getData(int type) {
//        if (NAME != null && NAME.startsWith("back") && type == 0) {
//            try {
//                throw new RuntimeException("CNV TYPE 0");
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
        return this.data[type];
    }

    public SpriteData getSprite() {
        return this.sprite;
    }

    private boolean binCnv(int t, byte[] src, int offset, int size) {

        Log.e("BIN_CNV " + (NAME == null ? "" : NAME), t == 1 ? "ACT" : t == 2 ? "BIN" : t == 3 ? "CNV" : "DAC");
        this.piGameData = null;
        this.piGameDataPos = null;
        this.piGameDataXSize = null;
        this.piGameDataYSize = null;
        try {
            int i;
            this.piGameData = new short[size];
            for (i = 0; i < size; i++) {
                this.piGameData[i] = (short) (src[offset + i] & 255);
            }
            int m = (this.piGameData[1] << 8) | this.piGameData[0];
            this.piGameDataPos = new int[m];
            this.piGameDataXSize = new int[m];
            this.piGameDataYSize = new int[m];
            for (i = 0; i < m; i++) {
                int pos = (i * 8) + 2;
                this.piGameDataPos[i] = (((this.piGameData[pos + 3] << 24) | (this.piGameData[pos + 2] << 16)) | (this.piGameData[pos + 1] << 8)) | this.piGameData[pos + 0];
                pos += 4;
                this.piGameDataXSize[i] = (this.piGameData[pos + 1] << 8) | this.piGameData[pos + 0];
                pos += 2;
                this.piGameDataYSize[i] = (this.piGameData[pos + 1] << 8) | this.piGameData[pos + 0];
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void Dispose(GlobalWork gw) {
        int i;
        this.piGameData = null;
        this.piGameDataPos = null;
        this.piGameDataXSize = null;
        this.piGameDataYSize = null;
        if (this.sprite != null) {
            this.sprite.Dispose(gw);
            this.sprite = null;
        }
        for (i = 0; i < this.data.length; i++) {
            this.data[i] = null;
        }
        for (i = 0; i < this.imageList.size(); i++) {
            ((AndroidGLTexture) this.imageList.get(i)).Dispose(gw.gl);
        }
        this.imageList.clear();
        this.iSoundCount = 0;
        for (i = 0; i < this.sound.length; i++) {
            this.sound[i] = null;
        }
    }

    public int getImage(int no) {
        if (no >= 0 && no < this.imageList.size()) {
            AndroidGLTexture tex = (AndroidGLTexture) this.imageList.get(no);
            if (tex != null) {
                return tex.GetImage();
            }
        }
        return 0;
    }

    public int getImageWidth(int no) {
        if (no >= 0 && no < this.imageList.size()) {
            AndroidGLTexture tex = (AndroidGLTexture) this.imageList.get(no);
            if (tex != null) {
                return tex.GetWidth();
            }
        }
        return 1;
    }

    public int getImageHeight(int no) {
        if (no >= 0 && no < this.imageList.size()) {
            AndroidGLTexture tex = (AndroidGLTexture) this.imageList.get(no);
            if (tex != null) {
                return tex.GetHeight();
            }
        }
        return 1;
    }

}
