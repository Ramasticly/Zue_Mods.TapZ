package com.namcobandaigames.dragonballtap.apk;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLUtils;
import android.util.Log;
import java.nio.IntBuffer;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

public class offscreen {

    private static final String TAG = "offscreen";
    private final float[] __coord = new float[12];
    private final int[] __vertex = new int[12];
    private Bitmap _base = null;
    private final IntBuffer _frameBuffer = null;
    private boolean bFOB = true;
    private final int[] colorbuffers = new int[1];
    private final int[] depthbuffers = new int[1];
    private final int[] framebuffers = new int[1];
    private final int[] framebuffersOld = new int[1];
    private int height;
    private final int[] tex = new int[1];
    private final int texHeight = 512;
    private int texWidth = 1024;
    private int width;

    public offscreen() {
        this.framebuffers[0] = 0;
        this.colorbuffers[0] = 0;
        this.depthbuffers[0] = 0;
        this.tex[0] = 0;
        this.width = 0;
        this.height = 0;
    }

    private boolean checkIfContextSupportsExtension(GL10 gl, String extension) {
        return (" " + gl.glGetString(7939) + " ").indexOf(" " + extension + " ") >= 0;
    }

    public boolean Init(GL10 gl, int w, int h) {
        if (w <= 512) {
            this.texWidth = 512;
        }
        Dispose(gl);
        if (!checkIfContextSupportsExtension(gl, "GL_OES_framebuffer_object")) {
            this.bFOB = false;
            return false;
        } else if (!CreateBitmap(gl, this.texWidth, this.texHeight)) {
            return false;
        } else {
            boolean ret = true;
            GL11ExtensionPack gl11ep = (GL11ExtensionPack) gl;
            try {
                this.width = w;
                this.height = h;
                gl11ep.glGenFramebuffersOES(1, this.framebuffers, 0);
                gl11ep.glGenRenderbuffersOES(1, this.colorbuffers, 0);
                gl11ep.glBindFramebufferOES(36160, this.framebuffers[0]);
                gl11ep.glFramebufferTexture2DOES(36160, 36064, 3553, this.tex[0], 0);
                if (gl11ep.glCheckFramebufferStatusOES(36160) != 36053) {
                    Dispose(gl);
                    this.bFOB = false;
                    ret = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "109:" + e);
                this.bFOB = false;
                ret = false;
            }
            gl11ep.glBindFramebufferOES(36160, 0);
            return ret;
        }
    }

    public void bind(GL10 gl) {
        if (this.bFOB) {
            ((GL11ExtensionPack) gl).glBindFramebufferOES(36160, this.framebuffers[0]);
        }
    }

    public void bindClear(GL10 gl) {
        if (this.bFOB) {
            ((GL11ExtensionPack) gl).glBindFramebufferOES(36160, 0);
        }
    }

    public void draw(GL10 gl, int x, int y, int w, int h) {
        float __uw = ((float) this.width) / ((float) this.texWidth);
        float __uh = ((float) this.height) / ((float) this.texHeight);
        this.__coord[0] = 0.0f;
        this.__coord[1] = 0.0f;
        this.__coord[2] = 0.0f;
        this.__coord[3] = 0.0f + __uh;
        this.__coord[4] = 0.0f + __uw;
        this.__coord[5] = 0.0f + __uh;
        this.__coord[8] = 0.0f + __uw;
        this.__coord[9] = 0.0f;
        int _w = w;
        int _h = h;
        this.__vertex[0] = x;
        this.__vertex[1] = y;
        this.__vertex[2] = x;
        this.__vertex[3] = y + _h;
        this.__vertex[4] = x + _w;
        this.__vertex[5] = y + _h;
        this.__vertex[8] = x + _w;
        this.__vertex[9] = y;
        Graphics2D.getInstance().drawMode(gl, 3);
        Graphics2D.getInstance().drawTexture(gl, this.tex[0], this.__vertex, this.__coord);
        Graphics2D.getInstance().clearMatrix(gl);
        Graphics2D.getInstance().drawMode(gl, 0);
    }

    private boolean CreateBitmap(GL10 gl, int w, int h) {
        try {
            if (this._base != null) {
                this._base.recycle();
            }
            this._base = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            gl.glEnable(3553);
            gl.glGenTextures(1, this.tex, 0);
            Graphics2D.getInstance().bindTexture(gl, this.tex[0]);
            GLUtils.texImage2D(3553, 0, this._base, 0);
            gl.glTexParameterf(3553, 10241, 9729.0f);
            gl.glTexParameterf(3553, 10240, 9729.0f);
            gl.glTexEnvf(8960, 8704, 8448.0f);
            gl.glTexParameterf(3553, 10242, 33071.0f);
            gl.glTexParameterf(3553, 10243, 33071.0f);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "233:" + e);
            return false;
        }
    }

    public void Dispose(GL10 gl) {
        if (this.bFOB) {
            GL11ExtensionPack gl11ep = (GL11ExtensionPack) gl;
            try {
                if (this.framebuffers[0] != 0) {
                    gl11ep.glDeleteFramebuffersOES(1, this.framebuffers, 0);
                    this.framebuffers[0] = 0;
                }
            } catch (Exception e) {
            }
            try {
                if (this.colorbuffers[0] != 0) {
                    gl11ep.glDeleteRenderbuffersOES(1, this.colorbuffers, 0);
                    this.colorbuffers[0] = 0;
                }
            } catch (Exception e2) {
            }
            try {
                if (this.depthbuffers[0] != 0) {
                    gl11ep.glDeleteRenderbuffersOES(1, this.depthbuffers, 0);
                    this.depthbuffers[0] = 0;
                }
            } catch (Exception e3) {
            }
        }
        try {
            if (this.tex[0] != 0) {
                gl.glDeleteTextures(1, this.tex, 0);
            }
        } catch (Exception e4) {
        }
        this.tex[0] = 0;
        if (this._base != null) {
            this._base.recycle();
            this._base = null;
        }
    }
}
