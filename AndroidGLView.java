package com.namcobandaigames.dragonballtap.apk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class AndroidGLView extends GLSurfaceView {

    static final int REQUEST_BLUETOOTH_SETUP = 1;
    static final int REQUEST_SMAP_SETUP = 2;
    Context _context;
    private AndroidGLRender glRenderer;
    private GlobalWork gw;
    AndroidGLView self;

    public void setGlobalWork(GlobalWork gw) {
        this.gw = gw;
        this.gw.context = this._context;
        this.gw.sFilesPath = this._context.getFilesDir().getPath();
        this.gw.glview = this;
        if (this.glRenderer != null) {
            this.glRenderer.setGlobalWork(gw);
            setRenderer(this.glRenderer);
        }
    }

    public AndroidGLView(Context context) {
        super(context);
        this.self = null;
        this._context = null;
        this.glRenderer = null;
        this.gw = null;
        this.glRenderer = new AndroidGLRender();
        this._context = context;
        this.self = this;
    }

    public boolean onTouchEvent(MotionEvent event) {
        try {
            int action = event.getAction();
            int count = event.getPointerCount();
            int index = event.getActionIndex();
            switch (action & 255) {
                case 0:
                    this.gw.keyData.Set(((int) (event.getX(index) * this.gw.fScreenScale)) - this.gw.iScreenOffsetX, ((int) (event.getY(index) * this.gw.fScreenScale)) - this.gw.iScreenOffsetY, 1, event.getPointerId(0));
                    break;
                case 1:
                    this.gw.keyData.Clear(event.getPointerId(0));
                    break;
                case 2:
                    for (int i = 0; i < count; i++) {
                        this.gw.keyData.Set(((int) (event.getX(i) * this.gw.fScreenScale)) - this.gw.iScreenOffsetX, ((int) (event.getY(i) * this.gw.fScreenScale)) - this.gw.iScreenOffsetY, 0, event.getPointerId(i));
                    }
                    break;
                case 5:
                    if (index >= 0) {
                        this.gw.keyData.Set(((int) (event.getX(index) * this.gw.fScreenScale)) - this.gw.iScreenOffsetX, ((int) (event.getY(index) * this.gw.fScreenScale)) - this.gw.iScreenOffsetY, 1, event.getPointerId(index));
                        break;
                    }
                    break;
                case 6:
                    if (index >= 0) {
                        this.gw.keyData.Clear(event.getPointerId(index));
                        break;
                    }
                    break;
            }
        } catch (Exception e) {
        }
        return true;
    }

    public void onResume() {
        if (this.gw != null) {
            this.gw.bResume = true;
        }
        super.onResume();
        SoundEffect.getInstance().stopBgm();
    }

    public void onPause() {
        if (this.gw != null) {
            this.gw.bResume = false;
            this.gw.iPauseWait = 3;
            this.gw.bAppPause = true;
        }
        SoundEffect.getInstance().stopBgm();
    }

    public void onStop() {
        if (this.gw != null) {
            this.gw.bResume = false;
            this.gw.iPauseWait = 3;
            this.gw.bAppPause = true;
        }
        SoundEffect.getInstance().stopBgm();
    }

    public void LowMemory() {
    }

    public void Dispose() {
    }

    public void startBrowser(String url) {
        this.gw.bRenderStop = true;
        ((Activity) getContext()).startActivityForResult(new Intent("android.intent.action.VIEW", Uri.parse(url)), 0);
    }

    public void SmapStart() {
        this.gw.iSmapEnd = 1;
    }

    public void SmapEnd() {
        this.gw.iSmapEnd = 1;
        this.gw.bRenderStop = false;
    }

    public void BluetoothStart() {
        this.gw.iBluetoothOK = 0;
        Activity a = (Activity) this._context;
        this.gw.bRenderStop = true;
        this.gw.bluetoothIntent = new Intent(this._context, BluetoothSearch.class);
        a.startActivityForResult(this.gw.bluetoothIntent, 1);
    }

    public void BluetoothEnd() {
        BluetoothManajer.getInstance().dispose();
        BluetoothManajer.clearInstance();
    }

    protected void onActivityResult(int requestCode, int ResultCode, Intent date) {
        if (requestCode != 1) {
        }
    }
}
