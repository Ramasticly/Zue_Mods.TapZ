package com.namcobandaigames.dragonballtap.apk;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.KeyEvent;

public class dragonballtap extends Activity {

    static final int REQUEST_BLUETOOTH_SETUP = 1;
    static final int REQUEST_SMAP_SETUP = 2;
    private final BluetoothSocket btSocket = null;
    private GlobalWork gw = null;
    private AndroidGLView mGLSurfaceView = null;

    public void onCreate(Bundle savedInstanceState) {
        Utility.activ = this;
        int i;
        super.onCreate(savedInstanceState);
        int i2 = 0;
        int sub_version = 0;
        try {
            String[] version = VERSION.RELEASE.split("\\.");
            i2 = Integer.parseInt(version[0]);
            sub_version = Integer.parseInt(version[1]);
        } catch (Exception e) {
        }
        if (i2 > 2 || (i2 == 2 && sub_version >= 3)) {
            setRequestedOrientation(6);
        } else {
            setRequestedOrientation(0);
        }
        getWindow().addFlags(128);
        getWindow().addFlags(1024);
        requestWindowFeature(1);
        this.gw = new GlobalWork();
        Utility.SetGlobalWork(this.gw);
        getLocale();
        Graphics2D.getInstance().Init();
        this.mGLSurfaceView = new AndroidGLView(this);
        this.mGLSurfaceView.setGlobalWork(this.gw);
        setContentView(this.mGLSurfaceView);
    }

    protected void onActivityResult(int requestCode, int ResultCode, Intent date) {
        super.onActivityResult(requestCode, ResultCode, date);
        this.gw.bRenderStop = false;
        if (requestCode == 2) {
            this.gw.iSmapEnd = 1;
        } else if (requestCode == 1) {
            if (ResultCode == -1) {
                this.gw.iBluetoothOK = 1;
                BluetoothManajer.getInstance().start();
            } else {
                this.gw.iBluetoothOK = -1;
                BluetoothManajer.getInstance().dispose();
                BluetoothManajer.clearInstance();
            }
            this.gw.bluetoothIntent = null;
            this.gw.bRenderStop = false;
            try {
                System.gc();
                Thread.sleep(20);
            } catch (InterruptedException e) {
            }
        }
    }

    private void getLocale() {
        this.gw.iLocale = 0;
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onRestart() {
        super.onRestart();
    }

    protected void onPause() {
        if (this.mGLSurfaceView != null) {
            this.mGLSurfaceView.onPause();
        }
        super.onPause();
    }

    protected void onDestroy() {
        SoundEffect.getInstance().dispose();
        if (this.mGLSurfaceView != null) {
            this.mGLSurfaceView.Dispose();
        }
        this.mGLSurfaceView = null;
        if (this.gw != null) {
            this.gw.dispose();
            this.gw = null;
        }
        BluetoothManajer.getInstance().dispose();
        BluetoothManajer.clearInstance();
        Graphics2D.getInstance().dispose();
        getWindow().clearFlags(128);
        getWindow().clearFlags(1024);
        super.onDestroy();
    }

    protected void onStop() {
        if (this.mGLSurfaceView != null) {
            this.mGLSurfaceView.onStop();
        }
        super.onStop();
    }

    protected void onResume() {
        if (this.gw != null) {
            this.gw.Init();
            getLocale();
        }
        if (this.mGLSurfaceView != null) {
            this.mGLSurfaceView.onResume();
        }
        super.onResume();
    }

    public void onLowMemory() {
        super.onLowMemory();
        if (this.mGLSurfaceView != null) {
            this.mGLSurfaceView.LowMemory();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 4 && keyCode != 82) {
            return super.onKeyDown(keyCode, event);
        }
        if (this.gw == null) {
            return true;
        }
        this.gw.bBackKey = true;
        return true;
    }
}
