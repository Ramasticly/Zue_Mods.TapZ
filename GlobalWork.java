package com.namcobandaigames.dragonballtap.apk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import java.util.HashMap;
import javax.microedition.khronos.opengles.GL10;

public class GlobalWork {

    public BluetoothAdapter BtAdapter;
    public HashMap TouchPoints = new HashMap();
    public int TouchPush;
    public HashMap TouchStatus = new HashMap();
    smapInit _smapInit = null;
    public boolean bAppPause;
    public boolean bBackKey;
    public boolean bBluetoothEnebled;
    public boolean bRenderStop;
    public boolean bResume;
    public boolean bThreadActive;
    public Intent bluetoothIntent;
    public String build_model = "";
    public int[] build_version = new int[6];
    public Context context;
    public Downloader download = null;
    public float fScreenGameScale = 1.0f;
    public float fScreenScale = 1.0f;
    public GL10 gl = null;
    public AndroidGLView glview;
    public int iAppFree = 0;
    public int iBluetoothOK;
    public int iFrameScalHeight;
    public int iFrameScalWidth;
    public int iLocale;
    public int iPauseWait = 0;
    public int iScreenBaseHeight;
    public int iScreenBaseWidth;
    public int iScreenDrawOffsetX;
    public int iScreenDrawOffsetY;
    public int iScreenOffsetX = 0;
    public int iScreenOffsetY = 0;
    public int iScreenScalHeight;
    public int iScreenScalWidth;
    public int iScreenScalWidthMax;
    public int iScreenX;
    public int iScreenY;
    public int iSmapEnd = 0;
    public KeyData keyData = null;
    public String sFilesPath = "";
    public String smapK = "";
    private TCBManajer tm = null;

    public TCBManajer getTCBM() {
        return this.tm;
    }

    public void setTCBM(TCBManajer _tm) {
        this.tm = _tm;
    }

    public final void Init() {
        this.bBackKey = false;
        this.bRenderStop = false;
        this.bResume = false;
        this.bAppPause = false;
        this.bThreadActive = true;
    }

    public GlobalWork() {
        Init();
        for (int i = 0; i < this.build_version.length; i++) {
            this.build_version[i] = 0;
        }
        this.keyData = null;
        this.keyData = new KeyData();
        this.BtAdapter = BluetoothAdapter.getDefaultAdapter();
        this.bBluetoothEnebled = this.BtAdapter != null;
    }

    public void dispose() {
        this.bThreadActive = false;
        this.keyData = null;
        this.bluetoothIntent = null;
    }
}
