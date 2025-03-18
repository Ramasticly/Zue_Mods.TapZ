package com.namcobandaigames.dragonballtap.apk;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

public final class Graphics2D {

//    public static final int DRAW_FLIP_H = 1;
//    public static final int DRAW_FLIP_V = 2;
//    public static final int DRAW_MODE_ADD = 1;
//    public static final int DRAW_MODE_NORMAL = 0;
//    public static final int DRAW_MODE_NORMAL_ADD = 3;
//    public static final int DRAW_MODE_SUB = 2;
//    private static Graphics2D Instance = null;
//    private final int BUFFER_PLAN_MAX = 1;
//    private final int BUFFER_SIZE_MAX = 127;
//    private final float PI = 0.017453292f;
//    private final int PRIMITIVE_TYPE_FILLRECT = 1;
//    private final int PRIMITIVE_TYPE_RECT = 0;
//    private final int PRIMITIVE_TYPE_TEXURE = 3;
//    private final int PRIMITIVE_TYPE_TRIANGLE = 2;
//    private boolean _Init = false;
//    private float[][] _color = ((float[][]) null);
//    private FloatBuffer[] _colorBuffer = null;
//    private float[][] _coord = ((float[][]) null);
//    private FloatBuffer[] _coordBuffer = null;
//    private ByteBuffer _indexBuffer = null;
//    private int _pushMatrix = 0;
//    private int[] _scal = new int[3];
//    private int[] _translate = new int[3];
//    private short[][] _vertex = ((short[][]) null);
//    private ShortBuffer[] _vertexBuffer = null;
//    private float[] _vertexWork = null;
//    private byte[] _vertexindex = null;
//    private int[] _viewport = new int[4];
//    private int iAlpha = 0;
//    private int iBindID = -1;
//    private int iBlue = 0;
//    private int iColorCount = 0;
//    private int iCoordCount = 0;
//    private int iDrawMode = -1;
//    public int iFlashCount = 0;
//    private int iGlAlpha = 0;
//    private int iGlBlue = 0;
//    private int iGlGreen = 0;
//    private int iGlRed = 0;
//    private int iGreen = 0;
//    private int iIndexCount = 0;
//    private int iIndexPos = 0;
//    private int iPrimitiveType = -1;
//    private int iRed = 0;
//    private int iVertexCount = 0;
//
//    public static Graphics2D getInstance() {
//        if (Instance == null) {
//            Instance = new Graphics2D();
//        } else {
//            Instance.Init();
//        }
//        return Instance;
//    }
//
//    private void Graphics2D() {
//        dispose();
//        Init();
//    }
//
//    public void dispose() {
//        for (int i = 0; i < 1; i++) {
//            this._vertexBuffer[i] = null;
//            this._coordBuffer[i] = null;
//            this._colorBuffer[i] = null;
//        }
//        this._vertex = (short[][]) null;
//        this._vertexBuffer = null;
//        this._coord = (float[][]) null;
//        this._coordBuffer = null;
//        this._color = (float[][]) null;
//        this._colorBuffer = null;
//        this._vertexWork = null;
//        this._scal = null;
//        this._translate = null;
//        this._viewport = null;
//        Instance = null;
//        this._Init = false;
//    }
//
//    public void Init() {
//        if (!this._Init) {
//            this.iBindID = -1;
//            this.iRed = 0;
//            this.iGreen = 0;
//            this.iBlue = 0;
//            this.iAlpha = 0;
//            this.iGlRed = 0;
//            this.iGlGreen = 0;
//            this.iGlBlue = 0;
//            this.iGlAlpha = 0;
//            this.iIndexCount = 0;
//            this.iIndexPos = 0;
//            this._vertexindex = new byte[190];
//            this._indexBuffer = ByteBuffer.allocateDirect(190);
//            this._vertex = (short[][]) Array.newInstance(Short.TYPE, new int[]{1, 127});
//            this._vertexBuffer = new ShortBuffer[1];
//            this._coord = (float[][]) Array.newInstance(Float.TYPE, new int[]{1, 127});
//            this._coordBuffer = new FloatBuffer[1];
//            this._color = (float[][]) Array.newInstance(Float.TYPE, new int[]{1, 254});
//            this._colorBuffer = new FloatBuffer[1];
//            this._vertexWork = new float[8];
//            this.iVertexCount = 0;
//            this.iCoordCount = 0;
//            this.iColorCount = 0;
//            this.iPrimitiveType = -1;
//            this.iDrawMode = -1;
//            this._scal = new int[3];
//            this._translate = new int[3];
//            this._pushMatrix = 0;
//            this._viewport = new int[4];
//            this.iVertexCount = 0;
//            for (int i = 0; i < 1; i++) {
//                ByteBuffer byteBuffer;
//                if (this._vertexBuffer[i] == null) {
//                    byteBuffer = ByteBuffer.allocateDirect(this._vertex[i].length * 2);
//                    byteBuffer.order(ByteOrder.nativeOrder());
//                    this._vertexBuffer[i] = byteBuffer.asShortBuffer();
//                    this._vertexBuffer[i].position(0);
//                }
//                if (this._coordBuffer[i] == null) {
//                    byteBuffer = ByteBuffer.allocateDirect(this._coord[i].length * 4);
//                    byteBuffer.order(ByteOrder.nativeOrder());
//                    this._coordBuffer[i] = byteBuffer.asFloatBuffer();
//                    this._coordBuffer[i].position(0);
//                }
//                if (this._colorBuffer[i] == null) {
//                    byteBuffer = ByteBuffer.allocateDirect(this._color[i].length * 4);
//                    byteBuffer.order(ByteOrder.nativeOrder());
//                    this._colorBuffer[i] = byteBuffer.asFloatBuffer();
//                    this._colorBuffer[i].position(0);
//                }
//            }
//            this._scal[0] = 100;
//            this._scal[1] = 100;
//            this._scal[2] = 100;
//            this._translate[0] = 0;
//            this._translate[1] = 0;
//            this._translate[2] = 0;
//            this._viewport[0] = 0;
//            this._viewport[1] = 0;
//            this._viewport[2] = 0;
//            this._viewport[3] = 0;
//            this._Init = true;
//        }
//    }
//
//    public void viewport(GL10 gl, int x, int y, int width, int height) {
//        if (this._viewport[0] != x || this._viewport[1] != y || this._viewport[2] != width || this._viewport[3] != height) {
//            flash(gl);
//            this._viewport[0] = x;
//            this._viewport[1] = y;
//            this._viewport[2] = width;
//            this._viewport[3] = height;
//            gl.glViewport(x, y, width, height);
//            gl.glMatrixMode(5889);
//            gl.glLoadIdentity();
//        }
//    }
//
//    public void clear(GL10 gl) {
//        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
//        this.iBindID = -1;
//        this.iPrimitiveType = -1;
//        this.iIndexCount = 0;
//        this.iIndexPos = 0;
//        this.iVertexCount = 0;
//        this.iCoordCount = 0;
//        this.iColorCount = 0;
//    }
//
//    public void clearMatrix(GL10 gl) {
//        if (this._pushMatrix > 0) {
//            flash(gl);
//        }
//        for (int i = 0; i < this._pushMatrix; i++) {
//            gl.glPopMatrix();
//        }
//        this._pushMatrix = 0;
//        this._scal[0] = 100;
//        this._scal[1] = 100;
//        this._scal[2] = 100;
//        this._translate[0] = 0;
//        this._translate[1] = 0;
//        this._translate[2] = 0;
//    }
//
//    public void setMatrix(GL10 gl, int sx, int sy, int sz, int x, int y, int z) {
//        if (this._scal[0] != sx || this._scal[1] != sy || this._scal[2] != sz || this._translate[0] != x || this._translate[1] != y || this._translate[2] != z) {
//            flash(gl);
//            this._translate[0] = x;
//            this._translate[1] = y;
//            this._translate[2] = z;
//            this._scal[0] = sx;
//            this._scal[1] = sy;
//            this._scal[2] = sz;
//            gl.glPushMatrix();
//            this._pushMatrix++;
//            gl.glTranslatef((float) x, (float) y, (float) z);
//            gl.glScalef(((float) sx) / 100.0f, ((float) sy) / 100.0f, ((float) sz) / 100.0f);
//        }
//    }
//
//    public void scal(GL10 gl, int sx, int sy, int sz) {
//        if (this._scal[0] != sx || this._scal[1] != sy || this._scal[2] != sz) {
//            flash(gl);
//            this._scal[0] = sx;
//            this._scal[1] = sy;
//            this._scal[2] = sz;
//            gl.glPushMatrix();
//            this._pushMatrix++;
//            gl.glScalef(((float) sx) / 100.0f, ((float) sy) / 100.0f, ((float) sz) / 100.0f);
//        }
//    }
//
//    public void translate(GL10 gl, int x, int y, int z) {
//        if (this._translate[0] != x || this._translate[1] != y || this._translate[2] != z) {
//            flash(gl);
//            this._translate[0] = x;
//            this._translate[1] = y;
//            this._translate[2] = z;
//            gl.glPushMatrix();
//            this._pushMatrix++;
//            gl.glTranslatef((float) x, (float) y, (float) z);
//        }
//    }
//
//    public void bindTexture(GL10 gl, int id) {
//        if (this.iPrimitiveType != 3) {
//            flash(gl);
//            gl.glEnable(3553);
//            gl.glDisableClientState(32886);
//            gl.glEnableClientState(32888);
//            gl.glBindTexture(3553, id);
//            gl.glEnable(GL10.GL_ALPHA_TEST);
//            this.iBindID = id;
//        } else if (this.iBindID != id) {
//            flash(gl);
//            this.iBindID = id;
//            gl.glBindTexture(3553, id);
//        }
//    }
//
//    public void flash(GL10 gl) {
//        if (this.iVertexCount <= 0) {
//            this.iVertexCount = 0;
//            return;
//        }
//        this.iFlashCount++;
//        switch (this.iPrimitiveType) {
//            case 1:
//                this._vertexBuffer[0].put(this._vertex[0]);
//                this._colorBuffer[0].put(this._color[0]);
//                this._vertexBuffer[0].position(0);
//                this._colorBuffer[0].position(0);
//                gl.glColorPointer(4, 5126, 0, this._colorBuffer[0]);
//                gl.glVertexPointer(2, 5122, 0, this._vertexBuffer[0]);
//                this._indexBuffer.put(this._vertexindex);
//                this._indexBuffer.position(0);
//                gl.glDrawElements(4, this.iIndexCount, 5121, this._indexBuffer);
//                break;
//            case 3:
//                this._vertexBuffer[0].put(this._vertex[0]);
//                this._coordBuffer[0].put(this._coord[0]);
//                this._vertexBuffer[0].position(0);
//                this._coordBuffer[0].position(0);
//                gl.glVertexPointer(2, 5122, 0, this._vertexBuffer[0]);
//                gl.glTexCoordPointer(2, 5126, 0, this._coordBuffer[0]);
//                this._indexBuffer.put(this._vertexindex);
//                this._indexBuffer.position(0);
//                gl.glDrawElements(4, this.iIndexCount, 5121, this._indexBuffer);
//                break;
//        }
//        this.iIndexCount = 0;
//        this.iIndexPos = 0;
//        this.iVertexCount = 0;
//        this.iCoordCount = 0;
//        this.iColorCount = 0;
//    }
//
//    public void drawTexture(GL10 gl, int id, int x, int y, int width, int height, float ux, float uy, float uw, float uh) {
//        if (this.iPrimitiveType != 3) {
//            flash(gl);
//            gl.glEnable(3553);
//            gl.glDisableClientState(32886);
//            gl.glEnableClientState(32888);
//            gl.glBindTexture(3553, id);
//            gl.glEnable(GL10.GL_ALPHA_TEST);
//            this.iBindID = id;
//        }
//        if (this.iBindID != id) {
//            flash(gl);
//            this.iBindID = id;
//            gl.glBindTexture(3553, id);
//        } else if (this.iVertexCount > 115) {
//            flash(gl);
//        }
//        this.iPrimitiveType = 3;
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 1);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 3);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//        this.iIndexPos += 4;
//
//        _vertex[0][iVertexCount++] = (short) x;
//        _vertex[0][iVertexCount++] = (short) (-y);
//        _vertex[0][iVertexCount++] = (short) x;
//        _vertex[0][iVertexCount++] = (short) (-(y + height));
//        _vertex[0][iVertexCount++] = (short) (x + width);
//        _vertex[0][iVertexCount++] = (short) (-(y + height));
//        _vertex[0][iVertexCount++] = (short) (x + width);
//        _vertex[0][iVertexCount++] = (short) (-y);
//
//        _coord[0][iCoordCount++] = ux;
//        _coord[0][iCoordCount++] = uy;
//        _coord[0][iCoordCount++] = ux;
//        _coord[0][iCoordCount++] = uy + uh;
//        _coord[0][iCoordCount++] = ux + uw;
//        _coord[0][iCoordCount++] = uy + uh;
//        _coord[0][iCoordCount++] = ux + uw;
//        _coord[0][iCoordCount++] = uy;
//    }
//
//    public void drawTexture(GL10 gl, int id, int centerx, int centery, int offsetx, int offsety, int width, int height, float ux, float uy, float uw, float uh, int flip, int angle, float scalw, float scalh) {
//        if (width != 0 && height != 0) {
//            int i;
//            if (this.iPrimitiveType != 3) {
//                flash(gl);
//                gl.glEnable(3553);
//                gl.glDisableClientState(32886);
//                gl.glEnableClientState(32888);
//                gl.glBindTexture(3553, id);
//                gl.glEnable(GL10.GL_ALPHA_TEST);
//                this.iBindID = id;
//            }
//            if (this.iBindID != id) {
//                flash(gl);
//                this.iBindID = id;
//                gl.glBindTexture(3553, id);
//            } else if (this.iVertexCount > 115) {
//                flash(gl);
//            }
//            this.iPrimitiveType = 3;
//            if ((flip & 1) != 0 && (flip & 2) != 0) {
//                this._vertexWork[0] = ((float) offsetx) + (((float) width) * scalw);
//                this._vertexWork[1] = ((float) offsety) + (((float) height) * scalh);
//                this._vertexWork[2] = ((float) offsetx) + (((float) width) * scalw);
//                this._vertexWork[3] = (float) offsety;
//                this._vertexWork[4] = (float) offsetx;
//                this._vertexWork[5] = (float) offsety;
//                this._vertexWork[6] = (float) offsetx;
//                this._vertexWork[7] = ((float) offsety) + (((float) height) * scalh);
//            } else if ((flip & 1) != 0) {
//                this._vertexWork[0] = ((float) offsetx) + (((float) width) * scalw);
//                this._vertexWork[1] = (float) offsety;
//                this._vertexWork[2] = ((float) offsetx) + (((float) width) * scalw);
//                this._vertexWork[3] = ((float) offsety) + (((float) height) * scalh);
//                this._vertexWork[4] = (float) offsetx;
//                this._vertexWork[5] = ((float) offsety) + (((float) height) * scalh);
//                this._vertexWork[6] = (float) offsetx;
//                this._vertexWork[7] = (float) offsety;
//            } else if ((flip & 2) != 0) {
//                this._vertexWork[0] = (float) offsetx;
//                this._vertexWork[1] = ((float) offsety) + (((float) height) * scalh);
//                this._vertexWork[2] = (float) offsetx;
//                this._vertexWork[3] = (float) offsety;
//                this._vertexWork[4] = ((float) offsetx) + (((float) width) * scalw);
//                this._vertexWork[5] = (float) offsety;
//                this._vertexWork[6] = ((float) offsetx) + (((float) width) * scalw);
//                this._vertexWork[7] = ((float) offsety) + (((float) height) * scalh);
//            } else {
//                this._vertexWork[0] = (float) offsetx;
//                this._vertexWork[1] = (float) offsety;
//                this._vertexWork[2] = (float) offsetx;
//                this._vertexWork[3] = ((float) offsety) + (((float) height) * scalh);
//                this._vertexWork[4] = ((float) offsetx) + (((float) width) * scalw);
//                this._vertexWork[5] = ((float) offsety) + (((float) height) * scalh);
//                this._vertexWork[6] = ((float) offsetx) + (((float) width) * scalw);
//                this._vertexWork[7] = (float) offsety;
//            }
//            if (angle != 0) {
//                if ((flip & 1) != 0) {
//                    angle = 360 - (angle % 360);
//                } else {
//                    angle %= 360;
//                }
//                float rad = PI * ((float) angle);
//                float sin = (float) Math.sin((double) rad);
//                float cos = (float) Math.cos((double) rad);
//                for (int i2 = 0; i2 < 4; i2++) {
//                    float vx = this._vertexWork[i2 << 1];
//                    float vy = this._vertexWork[(i2 << 1) + 1];
//                    if ((flip & 1) != 0) {
//                        vx -= ((float) width) * scalw;
//                    }
//                    if ((flip & 2) != 0) {
//                        vy -= ((float) height) * scalh;
//                    }
//                    this._vertexWork[i2 << 1] = (vx * cos) - (vy * sin);
//                    this._vertexWork[(i2 << 1) + 1] = (vx * sin) + (vy * cos);
//                    if ((flip & 1) != 0) {
//                        i = i2 << 1;
//                        _vertexWork[i] = _vertexWork[i] + (((float) width) * scalw);
//                    }
//                    if ((flip & 2) != 0) {
//                        i = (i2 << 1) + 1;
//                        _vertexWork[i] = _vertexWork[i] + (((float) height) * scalh);
//                    }
//                }
//            }
//            _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//            _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 1);
//            _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//            _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//            _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 3);
//            _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//            _vertex[0][iVertexCount++] = (short) ((int) (((float) centerx) + this._vertexWork[0]));
//            _vertex[0][iVertexCount++] = (short) ((int) (-(((float) centery) + this._vertexWork[1])));
//            _vertex[0][iVertexCount++] = (short) ((int) (((float) centerx) + this._vertexWork[2]));
//            _vertex[0][iVertexCount++] = (short) ((int) (-(((float) centery) + this._vertexWork[3])));
//            _vertex[0][iVertexCount++] = (short) ((int) (((float) centerx) + this._vertexWork[4]));
//            _vertex[0][iVertexCount++] = (short) ((int) (-(((float) centery) + this._vertexWork[5])));
//            _vertex[0][iVertexCount++] = (short) ((int) (((float) centerx) + this._vertexWork[6]));
//            _vertex[0][iVertexCount++] = (short) ((int) (-(((float) centery) + this._vertexWork[7])));
//            _coord[0][iCoordCount++] = ux;
//            _coord[0][iCoordCount++] = uy;
//            _coord[0][iCoordCount++] = ux;
//            _coord[0][iCoordCount++] = uy + uh;
//            _coord[0][iCoordCount++] = ux + uw;
//            _coord[0][iCoordCount++] = uy + uh;
//            _coord[0][iCoordCount++] = ux + uw;
//            _coord[0][iCoordCount++] = uy;
//        }
//    }
//
//    public void drawTexture(GL10 gl, int id, int[] vertex, float[] uv) {
//        if (this.iPrimitiveType != 3) {
//            flash(gl);
//            gl.glEnable(3553);
//            gl.glDisableClientState(32886);
//            gl.glEnableClientState(32888);
//            gl.glBindTexture(3553, id);
//            gl.glEnable(GL10.GL_ALPHA_TEST);
//            this.iBindID = id;
//        }
//        if (this.iBindID != id) {
//            flash(gl);
//            this.iBindID = id;
//            gl.glBindTexture(3553, id);
//        } else if (this.iVertexCount > 115) {
//            flash(gl);
//        }
//        this.iPrimitiveType = 3;
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 1);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 3);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//        this.iIndexPos += 4;
//        _vertex[0][iVertexCount++] = (short) vertex[0];
//        _vertex[0][iVertexCount++] = (short) vertex[1];
//        _vertex[0][iVertexCount++] = (short) vertex[2];
//        _vertex[0][iVertexCount++] = (short) vertex[3];
//        _vertex[0][iVertexCount++] = (short) vertex[4];
//        _vertex[0][iVertexCount++] = (short) vertex[5];
//        _vertex[0][iVertexCount++] = (short) vertex[8];
//        _vertex[0][iVertexCount++] = (short) vertex[9];
//        _coord[0][iCoordCount++] = uv[0];
//        _coord[0][iCoordCount++] = uv[1];
//        _coord[0][iCoordCount++] = uv[2];
//        _coord[0][iCoordCount++] = uv[3];
//        _coord[0][iCoordCount++] = uv[4];
//        _coord[0][iCoordCount++] = uv[5];
//        _coord[0][iCoordCount++] = uv[8];
//        _coord[0][iCoordCount++] = uv[9];
//    }
//
//    public void drawFillRect(GL10 gl, int x, int y, int width, int height) {
//        if (this.iPrimitiveType != 1) {
//            flash(gl);
//            if (this.iPrimitiveType == 3) {
//                gl.glDisableClientState(32888);
//                gl.glDisable(3553);
//                gl.glEnableClientState(32886);
//            }
//            this.iBindID = -1;
//        } else if (this.iVertexCount > 109) {
//            flash(gl);
//        }
//        this.iPrimitiveType = 1;
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 1);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 3);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//        _vertex[0][iVertexCount++] = (short) x;
//        _vertex[0][iVertexCount++] = (short) (-y);
//        _vertex[0][iVertexCount++] = (short) x;
//        _vertex[0][iVertexCount++] = (short) (-(y + height));
//        _vertex[0][iVertexCount++] = (short) (x + width);
//        _vertex[0][iVertexCount++] = (short) (-(y + height));
//        _vertex[0][iVertexCount++] = (short) (x + width);
//        _vertex[0][iVertexCount++] = (short) (-y);
//        float r = ((float) this.iRed) / 255.0f;
//        float g = ((float) this.iGreen) / 255.0f;
//        float b = ((float) this.iBlue) / 255.0f;
//        float a = ((float) this.iAlpha) / 255.0f;
//        for (int i2 = 0; i2 < 4; i2++) {
//            _color[0][iColorCount++] = r;
//            _color[0][iColorCount++] = g;
//            _color[0][iColorCount++] = b;
//            _color[0][iColorCount++] = a;
//        }
//    }
//
//    public void drawRect(GL10 gl, int x, int y, int width, int height) {
//        if (this.iPrimitiveType != 0) {
//            flash(gl);
//            if (this.iPrimitiveType == 3) {
//                gl.glDisableClientState(32888);
//                gl.glDisable(3553);
//                gl.glEnableClientState(32886);
//            }
//            this.iBindID = -1;
//        } else if (this.iVertexCount > 109) {
//            flash(gl);
//        }
//        this.iPrimitiveType = 0;
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 1);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 0);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 3);
//        _vertexindex[iIndexCount++] = (byte) (this.iIndexPos + 2);
//        this.iIndexPos += 4;
//        _vertex[0][iVertexCount++] = (short) x;
//        _vertex[0][iVertexCount++] = (short) (-y);
//        _vertex[0][iVertexCount++] = (short) x;
//        _vertex[0][iVertexCount++] = (short) (-(y + height));
//        _vertex[0][iVertexCount++] = (short) (x + width);
//        _vertex[0][iVertexCount++] = (short) (-(y + height));
//        _vertex[0][iVertexCount++] = (short) (x + width);
//        _vertex[0][iVertexCount++] = (short) (-y);
//        float r = ((float) this.iRed) / 255.0f;
//        float g = ((float) this.iGreen) / 255.0f;
//        float b = ((float) this.iBlue) / 255.0f;
//        float a = ((float) this.iAlpha) / 255.0f;
//        for (int i2 = 0; i2 < 4; i2++) {
//            _color[0][iColorCount++] = r;
//            _color[0][iColorCount++] = g;
//            _color[0][iColorCount++] = b;
//            _color[0][iColorCount++] = a;
//        }
//    }
//
//    public void drawMode(GL10 gl, int mode) {
//        if (this.iDrawMode != mode) {
//            flash(gl);
//            if (this.iDrawMode == 2) {
//                this.iDrawMode = mode;
//            } else {
//                this.iDrawMode = mode;
//            }
//            switch (mode) {
//                case 0:
//                    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
//                    break;
//                case 1:
//                    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
//                    break;
//                case 3:
//                    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
//                    break;
//            }
//        }
//    }
//
//    public void setColorGl(GL10 gl, int r, int g, int b, int a) {
//        if (this.iGlRed != r || this.iGlGreen != g || this.iGlBlue != b || this.iGlAlpha != a) {
//            flash(gl);
//            gl.glColor4f(((float) r) / 255.0f, ((float) g) / 255.0f, ((float) b) / 255.0f, ((float) a) / 255.0f);
//            this.iGlRed = r;
//            this.iGlGreen = g;
//            this.iGlBlue = b;
//            this.iGlAlpha = a;
//        }
//    }
//
//    public void setColor(GL10 gl, int r, int g, int b, int a) {
//        this.iRed = r;
//        this.iGreen = g;
//        this.iBlue = b;
//        this.iAlpha = a;
//    }
    
        public static final int DRAW_FLIP_H = 1;
    public static final int DRAW_FLIP_V = 2;
    public static final int DRAW_MODE_ADD = 1;
    public static final int DRAW_MODE_NORMAL = 0;
    public static final int DRAW_MODE_NORMAL_ADD = 3;
    public static final int DRAW_MODE_SUB = 2;
    private static Graphics2D Instance = null;
    private final int BUFFER_PLAN_MAX = 1;
    private final int BUFFER_SIZE_MAX = 127;
    private final float PI = 0.017453292f;
    private final int PRIMITIVE_TYPE_FILLRECT = 1;
    private final int PRIMITIVE_TYPE_RECT = 0;
    private final int PRIMITIVE_TYPE_TEXURE = 3;
    private final int PRIMITIVE_TYPE_TRIANGLE = 2;
    private boolean _Init = false;
    private float[][] _color = ((float[][]) null);
    private FloatBuffer[] _colorBuffer = null;
    private float[][] _coord = ((float[][]) null);
    private FloatBuffer[] _coordBuffer = null;
    private ByteBuffer _indexBuffer = null;
    private int _pushMatrix = 0;
    private int[] _scal = new int[3];
    private int[] _translate = new int[3];
    private short[][] _vertex = ((short[][]) null);
    private ShortBuffer[] _vertexBuffer = null;
    private float[] _vertexWork = null;
    private byte[] _vertexindex = null;
    private int[] _viewport = new int[4];
    private int iAlpha = 0;
    private int iBindID = -1;
    private int iBlue = 0;
    private int iColorCount = 0;
    private int iCoordCount = 0;
    private int iDrawMode = -1;
    public int iFlashCount = 0;
    private int iGlAlpha = 0;
    private int iGlBlue = 0;
    private int iGlGreen = 0;
    private int iGlRed = 0;
    private int iGreen = 0;
    private int iIndexCount = 0;
    private int iIndexPos = 0;
    private int iPrimitiveType = -1;
    private int iRed = 0;
    private int iVertexCount = 0;

    public static Graphics2D getInstance() {
        if (Instance == null) {
            Instance = new Graphics2D();
        }
        Instance.Init();
        return Instance;
    }

    private void Graphics2D() {
        dispose();
        Init();
    }

    public void dispose() {
        for (int i = 0; i < 1; i++) {
            this._vertexBuffer[i] = null;
            this._coordBuffer[i] = null;
            this._colorBuffer[i] = null;
        }
        this._vertex = (short[][]) null;
        this._vertexBuffer = null;
        this._coord = (float[][]) null;
        this._coordBuffer = null;
        this._color = (float[][]) null;
        this._colorBuffer = null;
        this._vertexWork = null;
        this._scal = null;
        this._translate = null;
        this._viewport = null;
        Instance = null;
        this._Init = false;
    }

    public void Init() {
        if (!this._Init) {
            this.iBindID = -1;
            this.iRed = 0;
            this.iGreen = 0;
            this.iBlue = 0;
            this.iAlpha = 0;
            this.iGlRed = 0;
            this.iGlGreen = 0;
            this.iGlBlue = 0;
            this.iGlAlpha = 0;
            this.iIndexCount = 0;
            this.iIndexPos = 0;
            this._vertexindex = new byte[190];
            this._indexBuffer = ByteBuffer.allocateDirect(190);
            this._vertex = (short[][]) Array.newInstance(Short.TYPE, new int[]{1, 127});
            this._vertexBuffer = new ShortBuffer[1];
            this._coord = (float[][]) Array.newInstance(Float.TYPE, new int[]{1, 127});
            this._coordBuffer = new FloatBuffer[1];
            this._color = (float[][]) Array.newInstance(Float.TYPE, new int[]{1, 254});
            this._colorBuffer = new FloatBuffer[1];
            this._vertexWork = new float[8];
            this.iVertexCount = 0;
            this.iCoordCount = 0;
            this.iColorCount = 0;
            this.iPrimitiveType = -1;
            this.iDrawMode = -1;
            this._scal = new int[3];
            this._translate = new int[3];
            this._pushMatrix = 0;
            this._viewport = new int[4];
            this.iVertexCount = 0;
            for (int i = 0; i < 1; i++) {
                ByteBuffer byteBuffer;
                if (this._vertexBuffer[i] == null) {
                    byteBuffer = ByteBuffer.allocateDirect(this._vertex[i].length * 2);
                    byteBuffer.order(ByteOrder.nativeOrder());
                    this._vertexBuffer[i] = byteBuffer.asShortBuffer();
                    this._vertexBuffer[i].position(0);
                }
                if (this._coordBuffer[i] == null) {
                    byteBuffer = ByteBuffer.allocateDirect(this._coord[i].length * 4);
                    byteBuffer.order(ByteOrder.nativeOrder());
                    this._coordBuffer[i] = byteBuffer.asFloatBuffer();
                    this._coordBuffer[i].position(0);
                }
                if (this._colorBuffer[i] == null) {
                    byteBuffer = ByteBuffer.allocateDirect(this._color[i].length * 4);
                    byteBuffer.order(ByteOrder.nativeOrder());
                    this._colorBuffer[i] = byteBuffer.asFloatBuffer();
                    this._colorBuffer[i].position(0);
                }
            }
            this._scal[0] = 100;
            this._scal[1] = 100;
            this._scal[2] = 100;
            this._translate[0] = 0;
            this._translate[1] = 0;
            this._translate[2] = 0;
            this._viewport[0] = 0;
            this._viewport[1] = 0;
            this._viewport[2] = 0;
            this._viewport[3] = 0;
            this._Init = true;
        }
    }

    public void viewport(GL10 gl, int x, int y, int width, int height) {
        if (this._viewport[0] != x || this._viewport[1] != y || this._viewport[2] != width || this._viewport[3] != height) {
            flash(gl);
            this._viewport[0] = x;
            this._viewport[1] = y;
            this._viewport[2] = width;
            this._viewport[3] = height;
            gl.glViewport(x, y, width, height);
            gl.glMatrixMode(5889);
            gl.glLoadIdentity();
        }
    }

    public void clear(GL10 gl) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(16640);
        this.iBindID = -1;
        this.iPrimitiveType = -1;
        this.iIndexCount = 0;
        this.iIndexPos = 0;
        this.iVertexCount = 0;
        this.iCoordCount = 0;
        this.iColorCount = 0;
    }

    public void clearMatrix(GL10 gl) {
        if (this._pushMatrix > 0) {
            flash(gl);
        }
        for (int i = 0; i < this._pushMatrix; i++) {
            gl.glPopMatrix();
        }
        this._pushMatrix = 0;
        this._scal[0] = 100;
        this._scal[1] = 100;
        this._scal[2] = 100;
        this._translate[0] = 0;
        this._translate[1] = 0;
        this._translate[2] = 0;
    }

    public void setMatrix(GL10 gl, int sx, int sy, int sz, int x, int y, int z) {
        if (this._scal[0] != sx || this._scal[1] != sy || this._scal[2] != sz || this._translate[0] != x || this._translate[1] != y || this._translate[2] != z) {
            flash(gl);
            this._translate[0] = x;
            this._translate[1] = y;
            this._translate[2] = z;
            this._scal[0] = sx;
            this._scal[1] = sy;
            this._scal[2] = sz;
            gl.glPushMatrix();
            this._pushMatrix++;
            gl.glTranslatef((float) x, (float) y, (float) z);
            gl.glScalef(((float) sx) / 100.0f, ((float) sy) / 100.0f, ((float) sz) / 100.0f);
        }
    }

    public void scal(GL10 gl, int sx, int sy, int sz) {
        if (this._scal[0] != sx || this._scal[1] != sy || this._scal[2] != sz) {
            flash(gl);
            this._scal[0] = sx;
            this._scal[1] = sy;
            this._scal[2] = sz;
            gl.glPushMatrix();
            this._pushMatrix++;
            gl.glScalef(((float) sx) / 100.0f, ((float) sy) / 100.0f, ((float) sz) / 100.0f);
        }
    }

    public void translate(GL10 gl, int x, int y, int z) {
        if (this._translate[0] != x || this._translate[1] != y || this._translate[2] != z) {
            flash(gl);
            this._translate[0] = x;
            this._translate[1] = y;
            this._translate[2] = z;
            gl.glPushMatrix();
            this._pushMatrix++;
            gl.glTranslatef((float) x, (float) y, (float) z);
        }
    }

    public void bindTexture(GL10 gl, int id) {
        if (this.iPrimitiveType != 3) {
            flash(gl);
            gl.glEnable(3553);
            gl.glDisableClientState(32886);
            gl.glEnableClientState(32888);
            gl.glBindTexture(3553, id);
            this.iBindID = id;
        } else if (this.iBindID != id) {
            flash(gl);
            this.iBindID = id;
            gl.glBindTexture(3553, id);
        }
    }

    public void flash(GL10 gl) {
        if (this.iVertexCount <= 0) {
            this.iVertexCount = 0;
            return;
        }
        this.iFlashCount++;
        switch (this.iPrimitiveType) {
            case 1:
                this._vertexBuffer[0].put(this._vertex[0]);
                this._colorBuffer[0].put(this._color[0]);
                this._vertexBuffer[0].position(0);
                this._colorBuffer[0].position(0);
                gl.glColorPointer(4, 5126, 0, this._colorBuffer[0]);
                gl.glVertexPointer(2, 5122, 0, this._vertexBuffer[0]);
                this._indexBuffer.put(this._vertexindex);
                this._indexBuffer.position(0);
                gl.glDrawElements(4, this.iIndexCount, 5121, this._indexBuffer);
                break;
            case 3:
                this._vertexBuffer[0].put(this._vertex[0]);
                this._coordBuffer[0].put(this._coord[0]);
                this._vertexBuffer[0].position(0);
                this._coordBuffer[0].position(0);
                gl.glVertexPointer(2, 5122, 0, this._vertexBuffer[0]);
                gl.glTexCoordPointer(2, 5126, 0, this._coordBuffer[0]);
                this._indexBuffer.put(this._vertexindex);
                this._indexBuffer.position(0);
                gl.glDrawElements(4, this.iIndexCount, 5121, this._indexBuffer);
                break;
        }
        this.iIndexCount = 0;
        this.iIndexPos = 0;
        this.iVertexCount = 0;
        this.iCoordCount = 0;
        this.iColorCount = 0;
    }

    public void drawTexture(GL10 gl, int id, int x, int y, int width, int height, float ux, float uy, float uw, float uh) {
        if (this.iPrimitiveType != 3) {
            flash(gl);
            gl.glEnable(3553);
            gl.glDisableClientState(32886);
            gl.glEnableClientState(32888);
            gl.glBindTexture(3553, id);
            this.iBindID = id;
        }
        if (this.iBindID != id) {
            flash(gl);
            this.iBindID = id;
            gl.glBindTexture(3553, id);
        } else if (this.iVertexCount > 115) {
            flash(gl);
        }
        this.iPrimitiveType = 3;
        byte[] bArr = this._vertexindex;
        int i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 0);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 1);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 2);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 0);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 3);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 2);
        this.iIndexPos += 4;
        short[] sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) x;
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-y);
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) x;
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-(y + height));
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (x + width);
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-(y + height));
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (x + width);
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-y);
        float[] fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = ux;
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uy;
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = ux;
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uy + uh;
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = ux + uw;
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uy + uh;
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = ux + uw;
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uy;
    }

    public void drawTexture(GL10 gl, int id, int centerx, int centery, int offsetx, int offsety, int width, int height, float ux, float uy, float uw, float uh, int flip, int angle, float scalw, float scalh) {
        if (width != 0 && height != 0) {
            float[] fArr;
            int i;
            if (this.iPrimitiveType != 3) {
                flash(gl);
                gl.glEnable(3553);
                gl.glDisableClientState(32886);
                gl.glEnableClientState(32888);
                gl.glBindTexture(3553, id);
                this.iBindID = id;
            }
            if (this.iBindID != id) {
                flash(gl);
                this.iBindID = id;
                gl.glBindTexture(3553, id);
            } else if (this.iVertexCount > 115) {
                flash(gl);
            }
            this.iPrimitiveType = 3;
            if ((flip & 1) != 0 && (flip & 2) != 0) {
                this._vertexWork[0] = ((float) offsetx) + (((float) width) * scalw);
                this._vertexWork[1] = ((float) offsety) + (((float) height) * scalh);
                this._vertexWork[2] = ((float) offsetx) + (((float) width) * scalw);
                this._vertexWork[3] = (float) offsety;
                this._vertexWork[4] = (float) offsetx;
                this._vertexWork[5] = (float) offsety;
                this._vertexWork[6] = (float) offsetx;
                this._vertexWork[7] = ((float) offsety) + (((float) height) * scalh);
            } else if ((flip & 1) != 0) {
                this._vertexWork[0] = ((float) offsetx) + (((float) width) * scalw);
                this._vertexWork[1] = (float) offsety;
                this._vertexWork[2] = ((float) offsetx) + (((float) width) * scalw);
                this._vertexWork[3] = ((float) offsety) + (((float) height) * scalh);
                this._vertexWork[4] = (float) offsetx;
                this._vertexWork[5] = ((float) offsety) + (((float) height) * scalh);
                this._vertexWork[6] = (float) offsetx;
                this._vertexWork[7] = (float) offsety;
            } else if ((flip & 2) != 0) {
                this._vertexWork[0] = (float) offsetx;
                this._vertexWork[1] = ((float) offsety) + (((float) height) * scalh);
                this._vertexWork[2] = (float) offsetx;
                this._vertexWork[3] = (float) offsety;
                this._vertexWork[4] = ((float) offsetx) + (((float) width) * scalw);
                this._vertexWork[5] = (float) offsety;
                this._vertexWork[6] = ((float) offsetx) + (((float) width) * scalw);
                this._vertexWork[7] = ((float) offsety) + (((float) height) * scalh);
            } else {
                this._vertexWork[0] = (float) offsetx;
                this._vertexWork[1] = (float) offsety;
                this._vertexWork[2] = (float) offsetx;
                this._vertexWork[3] = ((float) offsety) + (((float) height) * scalh);
                this._vertexWork[4] = ((float) offsetx) + (((float) width) * scalw);
                this._vertexWork[5] = ((float) offsety) + (((float) height) * scalh);
                this._vertexWork[6] = ((float) offsetx) + (((float) width) * scalw);
                this._vertexWork[7] = (float) offsety;
            }
            if (angle != 0) {
                if ((flip & 1) != 0) {
                    angle = 360 - (angle % 360);
                } else {
                    angle %= 360;
                }
                float rad = 0.017453292f * ((float) angle);
                float sin = (float) Math.sin((double) rad);
                float cos = (float) Math.cos((double) rad);
                for (int i2 = 0; i2 < 4; i2++) {
                    float vx = this._vertexWork[i2 << 1];
                    float vy = this._vertexWork[(i2 << 1) + 1];
                    if ((flip & 1) != 0) {
                        vx -= ((float) width) * scalw;
                    }
                    if ((flip & 2) != 0) {
                        vy -= ((float) height) * scalh;
                    }
                    this._vertexWork[i2 << 1] = (vx * cos) - (vy * sin);
                    this._vertexWork[(i2 << 1) + 1] = (vx * sin) + (vy * cos);
                    if ((flip & 1) != 0) {
                        fArr = this._vertexWork;
                        i = i2 << 1;
                        fArr[i] = fArr[i] + (((float) width) * scalw);
                    }
                    if ((flip & 2) != 0) {
                        fArr = this._vertexWork;
                        i = (i2 << 1) + 1;
                        fArr[i] = fArr[i] + (((float) height) * scalh);
                    }
                }
            }
            byte[] bArr = this._vertexindex;
            i = this.iIndexCount;
            this.iIndexCount = i + 1;
            bArr[i] = (byte) (this.iIndexPos + 0);
            bArr = this._vertexindex;
            i = this.iIndexCount;
            this.iIndexCount = i + 1;
            bArr[i] = (byte) (this.iIndexPos + 1);
            bArr = this._vertexindex;
            i = this.iIndexCount;
            this.iIndexCount = i + 1;
            bArr[i] = (byte) (this.iIndexPos + 2);
            bArr = this._vertexindex;
            i = this.iIndexCount;
            this.iIndexCount = i + 1;
            bArr[i] = (byte) (this.iIndexPos + 0);
            bArr = this._vertexindex;
            i = this.iIndexCount;
            this.iIndexCount = i + 1;
            bArr[i] = (byte) (this.iIndexPos + 3);
            bArr = this._vertexindex;
            i = this.iIndexCount;
            this.iIndexCount = i + 1;
            bArr[i] = (byte) (this.iIndexPos + 2);
            this.iIndexPos += 4;
            short[] sArr = this._vertex[0];
            i = this.iVertexCount;
            this.iVertexCount = i + 1;
            sArr[i] = (short) ((int) (((float) centerx) + this._vertexWork[0]));
            sArr = this._vertex[0];
            i = this.iVertexCount;
            this.iVertexCount = i + 1;
            sArr[i] = (short) ((int) (-(((float) centery) + this._vertexWork[1])));
            sArr = this._vertex[0];
            i = this.iVertexCount;
            this.iVertexCount = i + 1;
            sArr[i] = (short) ((int) (((float) centerx) + this._vertexWork[2]));
            sArr = this._vertex[0];
            i = this.iVertexCount;
            this.iVertexCount = i + 1;
            sArr[i] = (short) ((int) (-(((float) centery) + this._vertexWork[3])));
            sArr = this._vertex[0];
            i = this.iVertexCount;
            this.iVertexCount = i + 1;
            sArr[i] = (short) ((int) (((float) centerx) + this._vertexWork[4]));
            sArr = this._vertex[0];
            i = this.iVertexCount;
            this.iVertexCount = i + 1;
            sArr[i] = (short) ((int) (-(((float) centery) + this._vertexWork[5])));
            sArr = this._vertex[0];
            i = this.iVertexCount;
            this.iVertexCount = i + 1;
            sArr[i] = (short) ((int) (((float) centerx) + this._vertexWork[6]));
            sArr = this._vertex[0];
            i = this.iVertexCount;
            this.iVertexCount = i + 1;
            sArr[i] = (short) ((int) (-(((float) centery) + this._vertexWork[7])));
            fArr = this._coord[0];
            i = this.iCoordCount;
            this.iCoordCount = i + 1;
            fArr[i] = ux;
            fArr = this._coord[0];
            i = this.iCoordCount;
            this.iCoordCount = i + 1;
            fArr[i] = uy;
            fArr = this._coord[0];
            i = this.iCoordCount;
            this.iCoordCount = i + 1;
            fArr[i] = ux;
            fArr = this._coord[0];
            i = this.iCoordCount;
            this.iCoordCount = i + 1;
            fArr[i] = uy + uh;
            fArr = this._coord[0];
            i = this.iCoordCount;
            this.iCoordCount = i + 1;
            fArr[i] = ux + uw;
            fArr = this._coord[0];
            i = this.iCoordCount;
            this.iCoordCount = i + 1;
            fArr[i] = uy + uh;
            fArr = this._coord[0];
            i = this.iCoordCount;
            this.iCoordCount = i + 1;
            fArr[i] = ux + uw;
            fArr = this._coord[0];
            i = this.iCoordCount;
            this.iCoordCount = i + 1;
            fArr[i] = uy;
        }
    }

    public void drawTexture(GL10 gl, int id, int[] vertex, float[] uv) {
        if (this.iPrimitiveType != 3) {
            flash(gl);
            gl.glEnable(3553);
            gl.glDisableClientState(32886);
            gl.glEnableClientState(32888);
            gl.glBindTexture(3553, id);
            this.iBindID = id;
        }
        if (this.iBindID != id) {
            flash(gl);
            this.iBindID = id;
            gl.glBindTexture(3553, id);
        } else if (this.iVertexCount > 115) {
            flash(gl);
        }
        this.iPrimitiveType = 3;
        byte[] bArr = this._vertexindex;
        int i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 0);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 1);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 2);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 0);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 3);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 2);
        this.iIndexPos += 4;
        short[] sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) vertex[0];
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) vertex[1];
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) vertex[2];
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) vertex[3];
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) vertex[4];
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) vertex[5];
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) vertex[8];
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) vertex[9];
        float[] fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uv[0];
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uv[1];
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uv[2];
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uv[3];
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uv[4];
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uv[5];
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uv[8];
        fArr = this._coord[0];
        i = this.iCoordCount;
        this.iCoordCount = i + 1;
        fArr[i] = uv[9];
    }

    public void drawFillRect(GL10 gl, int x, int y, int width, int height) {
        if (this.iPrimitiveType != 1) {
            flash(gl);
            if (this.iPrimitiveType == 3) {
                gl.glDisableClientState(32888);
                gl.glDisable(3553);
                gl.glEnableClientState(32886);
            }
            this.iBindID = -1;
        } else if (this.iVertexCount > 109) {
            flash(gl);
        }
        this.iPrimitiveType = 1;
        byte[] bArr = this._vertexindex;
        int i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 0);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 1);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 2);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 0);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 3);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 2);
        this.iIndexPos += 4;
        short[] sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) x;
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-y);
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) x;
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-(y + height));
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (x + width);
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-(y + height));
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (x + width);
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-y);
        float r = ((float) this.iRed) / 255.0f;
        float g = ((float) this.iGreen) / 255.0f;
        float b = ((float) this.iBlue) / 255.0f;
        float a = ((float) this.iAlpha) / 255.0f;
        for (int i2 = 0; i2 < 4; i2++) {
            float[] fArr = this._color[0];
            i = this.iColorCount;
            this.iColorCount = i + 1;
            fArr[i] = r;
            fArr = this._color[0];
            i = this.iColorCount;
            this.iColorCount = i + 1;
            fArr[i] = g;
            fArr = this._color[0];
            i = this.iColorCount;
            this.iColorCount = i + 1;
            fArr[i] = b;
            fArr = this._color[0];
            i = this.iColorCount;
            this.iColorCount = i + 1;
            fArr[i] = a;
        }
    }

    public void drawRect(GL10 gl, int x, int y, int width, int height) {
        if (this.iPrimitiveType != 0) {
            flash(gl);
            if (this.iPrimitiveType == 3) {
                gl.glDisableClientState(32888);
                gl.glDisable(3553);
                gl.glEnableClientState(32886);
            }
            this.iBindID = -1;
        } else if (this.iVertexCount > 109) {
            flash(gl);
        }
        this.iPrimitiveType = 0;
        byte[] bArr = this._vertexindex;
        int i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 0);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 1);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 2);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 0);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 3);
        bArr = this._vertexindex;
        i = this.iIndexCount;
        this.iIndexCount = i + 1;
        bArr[i] = (byte) (this.iIndexPos + 2);
        this.iIndexPos += 4;
        short[] sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) x;
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-y);
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) x;
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-(y + height));
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (x + width);
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-(y + height));
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (x + width);
        sArr = this._vertex[0];
        i = this.iVertexCount;
        this.iVertexCount = i + 1;
        sArr[i] = (short) (-y);
        float r = ((float) this.iRed) / 255.0f;
        float g = ((float) this.iGreen) / 255.0f;
        float b = ((float) this.iBlue) / 255.0f;
        float a = ((float) this.iAlpha) / 255.0f;
        for (int i2 = 0; i2 < 4; i2++) {
            float[] fArr = this._color[0];
            i = this.iColorCount;
            this.iColorCount = i + 1;
            fArr[i] = r;
            fArr = this._color[0];
            i = this.iColorCount;
            this.iColorCount = i + 1;
            fArr[i] = g;
            fArr = this._color[0];
            i = this.iColorCount;
            this.iColorCount = i + 1;
            fArr[i] = b;
            fArr = this._color[0];
            i = this.iColorCount;
            this.iColorCount = i + 1;
            fArr[i] = a;
        }
    }

    public void drawMode(GL10 gl, int mode) {
        if (this.iDrawMode != mode) {
            flash(gl);
            if (this.iDrawMode == 2) {
                this.iDrawMode = mode;
            } else {
                this.iDrawMode = mode;
            }
            switch (mode) {
                case 0:
                    gl.glBlendFunc(770, 771);
                    return;
                case 1:
                    gl.glBlendFunc(770, 1);
                    return;
                case 3:
                    gl.glBlendFunc(1, 771);
                    return;
                default:
                    return;
            }
        }
    }

    public void setColorGl(GL10 gl, int r, int g, int b, int a) {
        if (this.iGlRed != r || this.iGlGreen != g || this.iGlBlue != b || this.iGlAlpha != a) {
            flash(gl);
            gl.glColor4f(((float) r) / 255.0f, ((float) g) / 255.0f, ((float) b) / 255.0f, ((float) a) / 255.0f);
            this.iGlRed = r;
            this.iGlGreen = g;
            this.iGlBlue = b;
            this.iGlAlpha = a;
        }
    }

    public void setColor(GL10 gl, int r, int g, int b, int a) {
        this.iRed = r;
        this.iGreen = g;
        this.iBlue = b;
        this.iAlpha = a;
    }
    
}
