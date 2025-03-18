package com.namcobandaigames.dragonballtap.apk;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.opengl.GLUtils;
import android.util.Log;
import javax.microedition.khronos.opengles.GL10;

public class StringTexture {

    private static final String TAG = "StringTexture";
    Bitmap _base;
    boolean bFirstFlash;
    Canvas canvas;
    int drawCount;
    Paint paint;
    int[] tex;
    int ypos;

    public StringTexture() {
        this.tex = null;
        this._base = null;
        this.canvas = null;
        this.paint = null;
        this.ypos = 0;
        this.drawCount = 0;
        this.bFirstFlash = false;
        this.tex = new int[1];
        this.tex[0] = 0;
        CreateBitmap(512, 512);
    }

    public boolean CreateBitmap(int w, int h) {
        try {
            if (this._base != null) {
                this._base.recycle();
            }
            this._base = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            if (this._base != null) {
                this.canvas = new Canvas(this._base);
                this.paint = new Paint();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "63:" + e);
        }
        return false;
    }

    public int GetImage() {
        return this.tex[0];
    }

    public int GetWidth() {
        if (this._base != null) {
            return this._base.getWidth();
        }
        return 0;
    }

    public int GetHeight() {
        if (this._base != null) {
            return this._base.getHeight();
        }
        return 0;
    }

    public void Dispose(GL10 gl) {
        try {
            if (this.tex[0] != 0) {
                gl.glDeleteTextures(1, this.tex, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "96:" + e);
        }
        this.tex[0] = 0;
        if (this._base != null) {
            this._base.recycle();
            this._base = null;
        }
    }

    protected RectF drawString(String text, int size, int r, int g, int b, int a) {
        RectF rect = new RectF();
        try {
            if (this.ypos >= 512) {
                return null;
            }
            this.paint.setTextSize((float) size);
            this.paint.setAntiAlias(true);
            this.paint.setARGB(a, r, g, b);
            this.paint.setStyle(Style.FILL);
            FontMetrics fontMetrics = this.paint.getFontMetrics();
            int top_offset = (int) Math.ceil((double) (0.0f + fontMetrics.top));
            int bottom_offset = (int) Math.ceil((double) (0.0f + fontMetrics.bottom));
            this.canvas.drawText(text, 0.0f, (float) (this.ypos - top_offset), this.paint);
            float width = 0.0f;
            float[] w = new float[text.length()];
            this.paint.getTextWidths(text, w);
            for (int i = 0; i < text.length(); i++) {
                width += w[i];
            }
            rect.left = 0.0f;
            rect.right = width;
            rect.top = (float) this.ypos;
            rect.bottom = (float) ((this.ypos + Math.abs(top_offset)) + Math.abs(bottom_offset));
            this.ypos += Math.abs(top_offset) + Math.abs(bottom_offset);
            this.drawCount++;
        } catch (Exception e) {
            Log.e(TAG, "148:" + e);
            rect = null;
        }
        return rect;
    }

    protected void clear() {
        if (this._base != null) {
            this._base.eraseColor(0);
        }
        this.ypos = 0;
        this.drawCount = 0;
    }

    protected void flash(GL10 gl) {
        if (this._base != null) {
            if (this.bFirstFlash) {
                if (this._base != null && this.drawCount > 0) {
                    Graphics2D.getInstance().bindTexture(gl, this.tex[0]);
                    GLUtils.texSubImage2D(3553, 0, 0, 0, this._base);
                }
                this.drawCount = 0;
                return;
            }
            clear();
            gl.glGenTextures(1, this.tex, 0);
            gl.glBindTexture(3553, this.tex[0]);
            GLUtils.texImage2D(3553, 0, this._base, 0);
            gl.glTexParameterf(3553, 10241, 9729.0f);
            gl.glTexParameterf(3553, 10240, 9729.0f);
            gl.glTexEnvf(8960, 8704, 8448.0f);
            gl.glTexParameterf(3553, 10242, 33071.0f);
            gl.glTexParameterf(3553, 10243, 33071.0f);
            this.bFirstFlash = true;
        }
    }
}
