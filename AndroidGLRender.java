package com.namcobandaigames.dragonballtap.apk;

import android.app.Activity;
import android.app.KeyguardManager;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.os.Build.VERSION;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AndroidGLRender implements Renderer {

    private static final int FPS = 40;
    private static final long WAIT_FRAME_MILLITS = 25;
    private KeyguardManager _KeyguardManager;
    private boolean bInit = false;
    private GlobalWork gw = null;
    private long startTime = 0;
    private TCBManajer tm = new TCBManajer();

    public void setGlobalWork(GlobalWork gw) {
        this.gw = gw;
        if (!this.tm.Init(this.gw)) {
            gw.bThreadActive = false;
        }
        this._KeyguardManager = (KeyguardManager) gw.context.getSystemService("keyguard");
    }

    public void suspendBGM() {
        if (this.tm != null) {
            this.tm.suspendBGM();
        }
    }

    public void resumeBGM() {
        if (this.tm != null) {
            this.tm.resumeBGM(this.gw);
        }
    }

    public void onDrawFrame(GL10 gl) {
        if (this.bInit && this.gw != null && this.tm != null) {
            try {
                if (!this._KeyguardManager.inKeyguardRestrictedInputMode()) {
                    this.gw.setTCBM(this.tm);
                    this.gw.gl = gl;
                    if (!this.gw.bThreadActive) {
                        if (!(this.tm == null || this.gw == null)) {
                            this.tm.Dispose(this.gw);
                            this.gw.gl = null;
                        }
                        ((Activity) this.gw.context).finish();
                    } else if (this.gw.bAppPause || this.gw.iPauseWait > 0) {
                        if (this.gw.iPauseWait == 3) {
                            this.gw.bAppPause = false;
                            this.gw.gl = null;
                        }
                        GlobalWork globalWork = this.gw;
                        globalWork.iPauseWait--;
                    } else {
                        this.startTime = 0;
                        if (!this.gw.bRenderStop) {
                            this.tm.Run(this.gw);
                            this.gw.gl = null;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        InitScreen(gl, width, height);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
        if (this.gw.iScreenBaseWidth > 0) {
            InitScreen(gl, this.gw.iScreenBaseWidth, this.gw.iScreenBaseHeight);
        }
    }

    public void InitScreen(GL10 gl, int width, int height) {
        this.gw.iScreenBaseWidth = width;
        this.gw.iScreenBaseHeight = height;
        this.gw.fScreenScale = 320.0f / ((float) height);
        this.gw.iScreenScalWidth = (int) (((float) width) * this.gw.fScreenScale);
        this.gw.iScreenScalHeight = 320;
        this.gw.iScreenScalWidthMax = 568;
        this.gw.iFrameScalWidth = 0;
        this.gw.iFrameScalHeight = 0;
        this.gw.build_model = Build.MODEL;
        try {
            String[] version = VERSION.RELEASE.split("\\.");
            int mejer_version = Integer.parseInt(version[0]);
            int sub_version = Integer.parseInt(version[1]);
            for (int i = 0; i < version.length; i++) {
                this.gw.build_version[i] = Integer.parseInt(version[i]);
            }
        } catch (Exception e) {
        }
        this.gw.fScreenGameScale = ((float) this.gw.iScreenScalWidth) / 480.0f;
//        gl.glViewport(0, 0, width, height);
//        gl.glMatrixMode(GL10.GL_PROJECTION);
//        gl.glLoadIdentity();
//        gl.glDisable(GL10.GL_DEPTH_BUFFER_BIT);
//        gl.glDisable(GL10.GL_COLOR_MATERIAL);
//        gl.glOrthof((float) (-(this.gw.iScreenScalWidth / 2)), (float) (this.gw.iScreenScalWidth / 2), (float) (-(this.gw.iScreenScalHeight / 2)), (float) (this.gw.iScreenScalHeight / 2), -100.0f, 100.0f);
//        this.gw.iScreenOffsetX = (this.gw.iScreenScalWidth - 480) / 2;
//        this.gw.iScreenDrawOffsetX = -((this.gw.iScreenScalWidth / 2) - this.gw.iScreenOffsetX);
//        this.gw.iScreenDrawOffsetY = -(this.gw.iScreenScalHeight / 2);
//        gl.glMatrixMode(GL10.GL_MODELVIEW);
//        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
//        gl.glDisable(GL10.GL_TEXTURE_2D);
//
//        gl.glEnable(GL10.GL_BLEND);
////        gl.glEnable(GL10.GL_ALPHA_TEST);
////        gl.glAlphaFunc(GL10.GL_NOTEQUAL, 0);
//        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
//        gl.glShadeModel(GL10.GL_FLAT);
//        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
//        gl.glHint(GL10.GL_FOG_HINT, GL10.GL_FASTEST);
//        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_FASTEST);
//        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
//        gl.glHint(GL10.GL_POINT_SMOOTH_HINT, GL10.GL_FASTEST);
//        gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_FASTEST);
//        gl.glDisable(GL10.GL_FOG);
//        gl.glDisable(GL10.GL_LIGHTING);
//        gl.glDisable(GL10.GL_SMOOTH);
//        gl.glDisable(GL10.GL_POINT_SMOOTH);
//        gl.glDisable(GL10.GL_LINE_SMOOTH);

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(5889);
        gl.glLoadIdentity();
        gl.glDisable(256);
        gl.glOrthof((float) (-(this.gw.iScreenScalWidth / 2)), (float) (this.gw.iScreenScalWidth / 2), (float) (-(this.gw.iScreenScalHeight / 2)), (float) (this.gw.iScreenScalHeight / 2), -100.0f, 100.0f);
        this.gw.iScreenOffsetX = (this.gw.iScreenScalWidth - 480) / 2;
        this.gw.iScreenDrawOffsetX = -((this.gw.iScreenScalWidth / 2) - this.gw.iScreenOffsetX);
        this.gw.iScreenDrawOffsetY = -(this.gw.iScreenScalHeight / 2);
        gl.glMatrixMode(5888);
        gl.glEnableClientState(32884);
        gl.glDisableClientState(32888);
        gl.glDisable(3553);
        gl.glEnable(3042);
        gl.glBlendFunc(770, 771);
        gl.glShadeModel(7424);
        gl.glEnableClientState(32886);
        gl.glHint(3156, 4353);
        gl.glHint(3154, 4353);
        gl.glHint(3152, 4353);
        gl.glHint(3153, 4353);
        gl.glHint(3155, 4353);
        gl.glDisable(2912);
        gl.glDisable(2896);
        gl.glDisable(7425);
        gl.glDisable(2832);
        gl.glDisable(2848);

        this.bInit = true;
    }
}
