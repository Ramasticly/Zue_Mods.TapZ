package com.namcobandaigames.dragonballtap.apk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Log;
import javax.microedition.khronos.opengles.GL10;

public class AndroidGLTexture {

    private static final String TAG = "AndroidGLTexture";
    int height;
    int[] tex;
    int width;

    public AndroidGLTexture() {
        this.tex = null;
        this.width = 0;
        this.height = 0;
        this.tex = new int[1];
        this.tex[0] = 0;
    }

    public int GetImage() {
        return this.tex[0];
    }

    public int GetWidth() {
        return this.width;
    }

    public int GetHeight() {
        return this.height;
    }

    public void Dispose(GL10 gl) {
        try {
            if (this.tex[0] != 0) {
                gl.glDeleteTextures(1, this.tex, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "53:" + e);
        }
        this.tex[0] = 0;
        this.width = 0;
        this.height = 0;
    }

    protected boolean loadTexture(GL10 gl, byte[] data, int offset, int length, boolean flt) {
        boolean ret;
        Bitmap bmp = null;
        this.tex[0] = 0;
        try {

            bmp = BitmapFactory.decodeByteArray(data, offset, length);
            ret = loadTexture(gl, bmp, flt);
        } catch (Exception e) {
            ret = false;
            this.tex[0] = 0;
        }
        if (bmp != null) {
            bmp.recycle();
        }
        return ret;
    }

    protected boolean loadTexture(GL10 gl, byte[] data) {
        boolean ret;
        Bitmap bmp = null;
        this.tex[0] = 0;
        try {
            bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            ret = loadTexture(gl, bmp, true);
        } catch (Exception e) {
            Log.e(TAG, "93:" + e);
            ret = false;
            this.tex[0] = 0;
        }
        if (bmp != null) {
            bmp.recycle();
        }
        return ret;
    }

    protected boolean loadTexture(GL10 gl, Bitmap bmp, boolean flt) {
        if (bmp == null) {
            return false;
        }
        this.width = bmp.getWidth();
        this.height = bmp.getHeight();
        gl.glGenTextures(1, this.tex, 0);
        gl.glBindTexture(3553, this.tex[0]);
        if (flt) {
            gl.glTexParameterf(3553, 10241, 9729.0f);
            gl.glTexParameterf(3553, 10240, 9729.0f);
        } else {
            gl.glTexParameterf(3553, 10241, 9728.0f);
            gl.glTexParameterf(3553, 10240, 9728.0f);
        }
        gl.glTexEnvf(8960, 8704, 8448.0f);
        gl.glTexParameterf(3553, 10242, 33071.0f);
        gl.glTexParameterf(3553, 10243, 33071.0f);
        GLUtils.texImage2D(3553, 0, bmp, 0);
        return true;
    }
}
