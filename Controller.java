//
// Decompiled by FernFlower - 3049ms
//
package com.namcobandaigames.dragonballtap.apk;

public final class Controller {
    int OldID = 0;
    int TapNG = 0;
    int TapPullX = 0;
    int TapPullXBuf = 0;
    int TapPullY = 0;
    int TapPullYBuf = 0;
    int TapSingleX = 0;
    int TapSingleY = 0;
    long TapStartTime = 0L;
    long TapTime = 0L;
    int TapType = -1;
    int TapX = 0;
    int TapY = 0;
    GamePad gamePad = null;

    public Controller() {
        this.gamePad = new GamePad(this);
    }

    int AddPad(int var1, int var2, int var3, int var4, int var5, int var6) {
        int var7 = this.gamePad.buttonCount;
        this.gamePad.buttonObj[var7].SkipKey = 0;
        this.gamePad.buttonObj[var7].iKeepFrame = 0;
        this.gamePad.buttonObj[var7].Pos[0] = var1;
        this.gamePad.buttonObj[var7].Pos[1] = var2;
        if (var5 != 7 && var5 != 5 && var5 != 2 && var5 != 3 && var5 != 6) {
            this.gamePad.buttonObj[var7].Range[0] = var3 >> 1;
            this.gamePad.buttonObj[var7].Range[1] = var4 >> 1;
        } else {
            this.gamePad.buttonObj[var7].Range[0] = var3;
            this.gamePad.buttonObj[var7].Range[1] = var4;
            this.gamePad.buttonObj[var7].PosNow[0] = -100;
            this.gamePad.buttonObj[var7].PosNow[1] = -100;
        }

        this.gamePad.buttonObj[var7].Type = var5;
        this.gamePad.buttonObj[var7].KeyCode[0] = var6;
        this.gamePad.buttonObj[var7].Pross = 0;
        this.gamePad.buttonObj[var7].Status = 0;
        this.gamePad.buttonObj[var7].Key[0] = 0;
        this.gamePad.buttonObj[var7].Key[1] = 0;
        this.gamePad.buttonObj[var7].Key[2] = 0;
        GamePad var8 = this.gamePad;
        ++var8.buttonCount;
        return var7;
    }

    void ClearKey() {
        for(int var1 = 0; var1 < this.gamePad.buttonCount; ++var1) {
            this.gamePad.buttonObj[var1].clear();
        }

    }

    int GetCenterX(int var1) {
        if (this.gamePad.buttonObj[var1].Type != 5 && this.gamePad.buttonObj[var1].Type != 7 && this.gamePad.buttonObj[var1].Type != 6) {
            if (this.gamePad.buttonObj[var1].Type == 2) {
                var1 = this.gamePad.buttonObj[var1].PosRange[0];
            } else {
                var1 = this.gamePad.buttonObj[var1].Pos[0];
            }
        } else {
            var1 = this.gamePad.buttonObj[var1].PosNow[0];
        }

        return var1;
    }

    int GetCenterY(int var1) {
        if (this.gamePad.buttonObj[var1].Type != 5 && this.gamePad.buttonObj[var1].Type != 7 && this.gamePad.buttonObj[var1].Type != 6) {
            if (this.gamePad.buttonObj[var1].Type == 2) {
                var1 = this.gamePad.buttonObj[var1].PosRange[1];
            } else {
                var1 = this.gamePad.buttonObj[var1].Pos[1];
            }
        } else {
            var1 = this.gamePad.buttonObj[var1].PosNow[1];
        }

        return var1;
    }

    int GetCount() {
        return this.gamePad.buttonCount;
    }

    int GetKeepFrame(int var1) {
        return this.gamePad.buttonObj[var1].iKeepFrame;
    }

    int GetKey(int var1, int var2) {
        if (var1 < 0) {
            var1 = 0;
        } else {
            var1 = this.gamePad.buttonObj[var1].Key[var2];
        }

        return var1;
    }

    int GetRange(int var1, int var2) {
        return this.gamePad.buttonObj[var1].Range[var2];
    }

    int GetSpeed(int var1) {
        return Math.abs(this.gamePad.buttonObj[var1].Speed);
    }

    int GetStatus(int var1, int var2) {
        return this.gamePad.buttonObj[var1].Status & var2;
    }

    int GetStickAngles(int var1) {
        return this.gamePad.buttonObj[var1].Angle;
    }

    float GetStickRange(int var1) {
        return this.gamePad.buttonObj[var1].StickRange;
    }

    int GetTapPullX() {
        return this.TapPullX;
    }

    int GetTapPullY() {
        return this.TapPullY;
    }

    int GetTapType() {
        return this.TapType;
    }

    int GetTapX() {
        return this.TapX;
    }

    int GetTapY() {
        return this.TapY;
    }

    int GetX(int var1) {
        if (this.gamePad.buttonObj[var1].Type == 2) {
            var1 = this.gamePad.buttonObj[var1].PosNow[0];
        } else {
            var1 = this.gamePad.buttonObj[var1].Pos[0];
        }

        return var1;
    }

    int GetY(int var1) {
        if (this.gamePad.buttonObj[var1].Type == 2) {
            var1 = this.gamePad.buttonObj[var1].PosNow[1];
        } else {
            var1 = this.gamePad.buttonObj[var1].Pos[1];
        }

        return var1;
    }

    void Init() {
        this.gamePad.buttonCount = 0;
    }

    void KeyLock(int var1, boolean var2) {
        ButtonObject var3;
        if (var2) {
            var3 = this.gamePad.buttonObj[var1];
            var3.Status |= 2;
        } else {
            var3 = this.gamePad.buttonObj[var1];
            var3.Status &= -3;
        }

    }

    void SetKey(KeyData var1) {
        this.TapType = -1;
        this.TapX = 0;
        this.TapY = 0;
        int var6;
        int var7;
        int var8;
        if (this.TapSingleX != 0 && this.TapSingleY != 0) {
            var7 = var1.GetIndex(this.OldID);
            if (var7 >= 0) {
                if (var1.X[var7] != 0 && var1.Y[var7] != 0) {
                    var8 = Math.abs(this.TapSingleX - var1.X[var7]);
                    var6 = Math.abs(this.TapSingleY - var1.Y[var7]);
                    if (var8 > 20 || var6 > 20) {
                        this.TapNG = 1;
                        this.TapPullX = 0;
                        this.TapPullY = 0;
                    }
                }

                this.TapPullXBuf = var1.X[var7];
                this.TapPullYBuf = var1.Y[var7];
            }

            if (var7 < 0 && this.OldID >= 0) {
                this.OldID = -1;
                if (this.TapNG == 0) {
                    this.TapPullX = this.TapPullXBuf;
                    this.TapPullY = this.TapPullYBuf;
                }
            }

            if (System.currentTimeMillis() - this.TapStartTime > 200L) {
                this.OldID = -1;
                this.TapType = -1;
                if (this.TapNG == 0) {
                    this.TapX = this.TapSingleX;
                    this.TapY = this.TapSingleY;
                    this.TapType = 0;
                }

                this.TapSingleX = 0;
                this.TapSingleY = 0;
            } else {
                for(var6 = 0; var6 < 10; ++var6) {
                    var7 = var1.ID[var6];
                    if (var1.isBeginTouch(var7)) {
                        var7 = var1.GetIndex(var7);
                        if (var7 >= 0) {
                            this.OldID = -1;
                            var6 = Math.abs(this.TapSingleX - var1.X[var7]);
                            var8 = Math.abs(this.TapSingleY - var1.Y[var7]);
                            if (var6 < 35 && var8 < 35) {
                                this.TapX = var1.X[var7];
                                this.TapY = var1.Y[var7];
                                this.TapType = 1;
                            } else {
                                if (this.TapNG == 0) {
                                    this.TapX = this.TapSingleX;
                                    this.TapY = this.TapSingleY;
                                }

                                this.TapType = 0;
                            }

                            this.TapSingleX = 0;
                            this.TapSingleY = 0;
                            break;
                        }
                    }
                }
            }
        } else {
            this.TapTime = 0L;
            this.TapSingleX = 0;
            this.TapSingleY = 0;
            this.TapPullX = 0;
            this.TapPullY = 0;

            for(var6 = 0; var6 < 10; ++var6) {
                var8 = var1.ID[var6];
                if (var1.isBeginTouch(var8)) {
                    var7 = var1.GetIndex(var8);
                    if (var7 >= 0) {
                        this.TapStartTime = System.currentTimeMillis();
                        this.OldID = var8;
                        this.TapNG = 0;
                        this.TapSingleX = var1.X[var7];
                        this.TapSingleY = var1.Y[var7];
                        break;
                    }
                }
            }
        }

        for(var8 = 0; var8 < this.gamePad.buttonCount; ++var8) {
            int var16 = this.gamePad.buttonObj[var8].Key[2];
            this.gamePad.buttonObj[var8].Key[0] = 0;
            this.gamePad.buttonObj[var8].Key[1] = 0;
            this.gamePad.buttonObj[var8].Key[2] = 0;
            boolean var13 = false;
            boolean var9 = false;
            boolean var10 = false;
            boolean var15 = false;
            boolean var12 = false;
            boolean var11 = false;
            boolean var19 = false;
            boolean var14 = false;
            if ((this.gamePad.buttonObj[var8].Status & 2) == 0) {
                double var2;
                double var4;
                ButtonObject var17;
                boolean var18;
                int var20;
                int[] var23;
                switch (this.gamePad.buttonObj[var8].Type) {
                    case 1:
                        var6 = 0;

                        while(true) {
                            var19 = var9;
                            if (var6 >= 10) {
                                break;
                            }

                            if (var1.ID[var6] >= 0) {
                                var2 = (double)(var1.X[var6] - 10) - (double)this.gamePad.buttonObj[var8].Pos[0];
                                var4 = (double)(var1.Y[var6] - 10) - (double)this.gamePad.buttonObj[var8].Pos[1];
                                int var21 = Math.abs((int)var2);
                                var7 = Math.abs((int)var4);
                                if (this.gamePad.buttonObj[var8].Range[0] > var21 && this.gamePad.buttonObj[var8].Range[0] > var7 && (var21 > 16 || var7 > 16)) {
                                    var7 = (int)(Math.atan2(var4, var2) * 180.0 / Math.PI) + 90;
                                    var6 = var7;
                                    if (var7 < 0) {
                                        var6 = var7 + 360;
                                    }

                                    this.gamePad.buttonObj[var8].Angle = var6;
                                    if (var6 > 20 && var6 < 160) {
                                        var23 = this.gamePad.buttonObj[var8].Key;
                                        var23[2] |= 8;
                                    }

                                    if (var6 > 110 && var6 < 260) {
                                        var23 = this.gamePad.buttonObj[var8].Key;
                                        var23[2] |= 2;
                                    }

                                    if (var6 > 200 && var6 < 340) {
                                        var23 = this.gamePad.buttonObj[var8].Key;
                                        var23[2] |= 4;
                                    }

                                    if (var6 > 300 || var6 < 60) {
                                        var23 = this.gamePad.buttonObj[var8].Key;
                                        var23[2] |= 1;
                                    }

                                    var18 = true;
                                    var17 = this.gamePad.buttonObj[var8];
                                    ++var17.iKeepFrame;
                                    this.gamePad.buttonObj[var8].Key[0] = 0;
                                    var19 = var18;
                                    if ((this.gamePad.buttonObj[var8].Status & 1) == 0) {
                                        var17 = this.gamePad.buttonObj[var8];
                                        var17.Status |= 1;
                                        this.gamePad.buttonObj[var8].Key[0] = this.gamePad.buttonObj[var8].Key[2];
                                        var19 = var18;
                                    }
                                    break;
                                }
                            }

                            ++var6;
                        }

                        if (!var19) {
                            this.gamePad.buttonObj[var8].iKeepFrame = 0;
                            if ((this.gamePad.buttonObj[var8].Status & 1) != 0) {
                                var17 = this.gamePad.buttonObj[var8];
                                var17.Status &= -2;
                                this.gamePad.buttonObj[var8].Key[1] = var16;
                            }
                        }
                        break;
                    case 2:
                        var7 = 0;

                        while(true) {
                            var18 = var13;
                            if (var7 >= 10) {
                                break;
                            }

                            if (var1.ID[var7] >= 0) {
                                this.gamePad.buttonObj[var8].PosNow[0] = -100;
                                this.gamePad.buttonObj[var8].PosNow[1] = -100;
                                if (this.gamePad.buttonObj[var8].Pos[0] < var1.X[var7] && this.gamePad.buttonObj[var8].Pos[0] + this.gamePad.buttonObj[var8].Range[0] > var1.X[var7] && this.gamePad.buttonObj[var8].Pos[1] < var1.Y[var7] && this.gamePad.buttonObj[var8].Pos[1] + this.gamePad.buttonObj[var8].Range[1] > var1.Y[var7]) {
                                    this.gamePad.buttonObj[var8].PosNow[0] = var1.X[var7];
                                    this.gamePad.buttonObj[var8].PosNow[1] = var1.Y[var7];
                                    if ((this.gamePad.buttonObj[var8].Status & 1) == 0) {
                                        var17 = this.gamePad.buttonObj[var8];
                                        var17.Status |= 1;
                                        this.gamePad.buttonObj[var8].PosRange[0] = var1.X[var7];
                                        this.gamePad.buttonObj[var8].PosRange[1] = var1.Y[var7];
                                        this.gamePad.buttonObj[var8].Key[2] = 0;
                                        this.gamePad.buttonObj[var8].Key[0] = 0;
                                        this.gamePad.buttonObj[var8].OldKey[2] = 0;
                                    } else {
                                        var2 = (double)var1.X[var7] - (double)this.gamePad.buttonObj[var8].PosRange[0];
                                        var4 = (double)var1.Y[var7] - (double)this.gamePad.buttonObj[var8].PosRange[1];
                                        var7 = Math.abs((int)var2);
                                        var6 = Math.abs((int)var4);
                                        if (var7 > 10 || var6 > 10) {
                                            var6 = (int)(Math.atan2(var4, var2) * 180.0 / Math.PI) + 90;
                                            var20 = var6;
                                            if (var6 < 0) {
                                                var20 = var6 + 360;
                                            }

                                            this.gamePad.buttonObj[var8].Angle = var20;
                                            this.gamePad.buttonObj[var8].Key[0] = 0;
                                            byte var22 = 0;
                                            var6 = var22;
                                            if (var20 > 20) {
                                                var6 = var22;
                                                if (var20 < 160) {
                                                    var6 = 0 | 8;
                                                }
                                            }

                                            var7 = var6;
                                            if (var20 > 110) {
                                                var7 = var6;
                                                if (var20 < 260) {
                                                    var7 = var6 | 2;
                                                }
                                            }

                                            var6 = var7;
                                            if (var20 > 200) {
                                                var6 = var7;
                                                if (var20 < 340) {
                                                    var6 = var7 | 4;
                                                }
                                            }

                                            label395: {
                                                if (var20 <= 300) {
                                                    var7 = var6;
                                                    if (var20 >= 60) {
                                                        break label395;
                                                    }
                                                }

                                                var7 = var6 | 1;
                                            }

                                            if (var7 != this.gamePad.buttonObj[var8].OldKey[2]) {
                                                this.gamePad.buttonObj[var8].Key[0] = var7;
                                            }

                                            this.gamePad.buttonObj[var8].OldKey[2] = var7;
                                            this.gamePad.buttonObj[var8].Key[2] = var7;
                                        }
                                    }

                                    var17 = this.gamePad.buttonObj[var8];
                                    ++var17.iKeepFrame;
                                    var18 = true;
                                    break;
                                }
                            }

                            ++var7;
                        }

                        if (!var18) {
                            this.gamePad.buttonObj[var8].iKeepFrame = 0;
                            if ((this.gamePad.buttonObj[var8].Status & 1) != 0) {
                                var17 = this.gamePad.buttonObj[var8];
                                var17.Status &= -2;
                                this.gamePad.buttonObj[var8].Key[1] = var16;
                                this.gamePad.buttonObj[var8].PosRange[0] = -65535;
                                this.gamePad.buttonObj[var8].PosRange[1] = -65535;
                                this.gamePad.buttonObj[var8].PosNow[0] = -100;
                                this.gamePad.buttonObj[var8].PosNow[1] = -100;
                            }
                        }
                        break;
                    case 3:
                        var7 = 0;

                        while(true) {
                            var18 = var14;
                            if (var7 >= 10) {
                                break;
                            }

                            if (var1.ID[var7] >= 0 && this.gamePad.buttonObj[var8].Pos[0] < var1.X[var7] && this.gamePad.buttonObj[var8].Pos[0] + this.gamePad.buttonObj[var8].Range[0] > var1.X[var7] && this.gamePad.buttonObj[var8].Pos[1] < var1.Y[var7] && this.gamePad.buttonObj[var8].Pos[1] + this.gamePad.buttonObj[var8].Range[1] > var1.Y[var7]) {
                                if ((this.gamePad.buttonObj[var8].Status & 1) == 0) {
                                    var17 = this.gamePad.buttonObj[var8];
                                    var17.Status |= 1;
                                    this.gamePad.buttonObj[var8].PosRange[0] = var1.X[var7];
                                    this.gamePad.buttonObj[var8].PosRange[1] = var1.Y[var7];
                                    this.gamePad.buttonObj[var8].Counter = 0;
                                    this.gamePad.buttonObj[var8].GestureDirection = -1;
                                    this.gamePad.buttonObj[var8].Key[2] = 0;
                                    this.gamePad.buttonObj[var8].Key[0] = 0;
                                    this.gamePad.buttonObj[var8].Work[0] = var1.X[var7];
                                    this.gamePad.buttonObj[var8].Work[1] = var1.Y[var7];
                                    this.gamePad.buttonObj[var8].Speed = 0;
                                } else {
                                    if (this.gamePad.buttonObj[var8].GestureDirection < 0) {
                                        if (Math.abs(var1.X[var7] - this.gamePad.buttonObj[var8].PosRange[0]) > Math.abs(var1.Y[var7] - this.gamePad.buttonObj[var8].PosRange[1])) {
                                            if (this.gamePad.buttonObj[var8].PosRange[0] + 5 < var1.X[var7]) {
                                                this.gamePad.buttonObj[var8].GestureDirection = 2;
                                            } else if (this.gamePad.buttonObj[var8].PosRange[0] - 5 > var1.X[var7]) {
                                                this.gamePad.buttonObj[var8].GestureDirection = 3;
                                            }
                                        } else if (this.gamePad.buttonObj[var8].PosRange[1] + 5 < var1.Y[var7]) {
                                            this.gamePad.buttonObj[var8].GestureDirection = 0;
                                        } else if (this.gamePad.buttonObj[var8].PosRange[1] - 5 > var1.Y[var7]) {
                                            this.gamePad.buttonObj[var8].GestureDirection = 1;
                                        }
                                    }

                                    var17 = this.gamePad.buttonObj[var8];
                                    ++var17.Counter;
                                    if (this.gamePad.buttonObj[var8].Counter == 10) {
                                        this.gamePad.buttonObj[var8].GestureDirection = 255;
                                    }

                                    if (this.gamePad.buttonObj[var8].GestureDirection >= 0 && this.gamePad.buttonObj[var8].GestureDirection != 255) {
                                        switch (this.gamePad.buttonObj[var8].GestureDirection) {
                                            case 0:
                                                if (this.gamePad.buttonObj[var8].PosRange[0] - 35 < var1.X[var7] && this.gamePad.buttonObj[var8].PosRange[0] + 25 > var1.X[var7] && this.gamePad.buttonObj[var8].PosRange[1] + 50 < var1.Y[var7]) {
                                                    var23 = this.gamePad.buttonObj[var8].Key;
                                                    var23[2] |= ~this.gamePad.buttonObj[var8].SkipKey & 1;
                                                    this.gamePad.buttonObj[var8].GestureDirection = 255;
                                                    this.gamePad.buttonObj[var8].Speed = this.gamePad.buttonObj[var8].PosRange[1] - var1.Y[var7];
                                                }
                                                break;
                                            case 1:
                                                if (this.gamePad.buttonObj[var8].PosRange[0] - 35 < var1.X[var7] && this.gamePad.buttonObj[var8].PosRange[0] + 25 > var1.X[var7] && this.gamePad.buttonObj[var8].PosRange[1] - 50 > var1.Y[var7]) {
                                                    var23 = this.gamePad.buttonObj[var8].Key;
                                                    var23[2] |= ~this.gamePad.buttonObj[var8].SkipKey & 2;
                                                    this.gamePad.buttonObj[var8].GestureDirection = 255;
                                                    this.gamePad.buttonObj[var8].Speed = this.gamePad.buttonObj[var8].PosRange[1] - var1.Y[var7];
                                                }
                                                break;
                                            case 2:
                                                if (this.gamePad.buttonObj[var8].PosRange[1] - 25 < var1.Y[var7] && this.gamePad.buttonObj[var8].PosRange[1] + 35 > var1.Y[var7] && this.gamePad.buttonObj[var8].PosRange[0] + 50 < var1.X[var7]) {
                                                    var23 = this.gamePad.buttonObj[var8].Key;
                                                    var23[2] |= ~this.gamePad.buttonObj[var8].SkipKey & 4;
                                                    this.gamePad.buttonObj[var8].GestureDirection = 255;
                                                    this.gamePad.buttonObj[var8].Speed = this.gamePad.buttonObj[var8].PosRange[0] - var1.X[var7];
                                                }
                                                break;
                                            case 3:
                                                if (this.gamePad.buttonObj[var8].PosRange[1] - 25 < var1.Y[var7] && this.gamePad.buttonObj[var8].PosRange[1] + 35 > var1.Y[var7] && this.gamePad.buttonObj[var8].PosRange[0] - 50 > var1.X[var7]) {
                                                    var23 = this.gamePad.buttonObj[var8].Key;
                                                    var23[2] |= ~this.gamePad.buttonObj[var8].SkipKey & 8;
                                                    this.gamePad.buttonObj[var8].GestureDirection = 255;
                                                    this.gamePad.buttonObj[var8].Speed = this.gamePad.buttonObj[var8].PosRange[0] - var1.X[var7];
                                                }
                                        }

                                        this.gamePad.buttonObj[var8].Work[0] = var1.X[var7];
                                        this.gamePad.buttonObj[var8].Work[1] = var1.Y[var7];
                                    }
                                }

                                var17 = this.gamePad.buttonObj[var8];
                                ++var17.iKeepFrame;
                                var18 = true;
                                break;
                            }

                            ++var7;
                        }

                        if (!var18) {
                            this.gamePad.buttonObj[var8].iKeepFrame = 0;
                            if ((this.gamePad.buttonObj[var8].Status & 1) != 0) {
                                var17 = this.gamePad.buttonObj[var8];
                                var17.Status &= -2;
                                this.gamePad.buttonObj[var8].Key[1] = var16;
                            }
                        }
                        break;
                    case 4:
                        var6 = 0;

                        while(true) {
                            var19 = var10;
                            if (var6 >= 10) {
                                break;
                            }

                            if (var1.ID[var6] >= 0) {
                                var20 = Math.abs(this.gamePad.buttonObj[var8].Pos[0] - var1.X[var6]);
                                var7 = Math.abs(this.gamePad.buttonObj[var8].Pos[1] - var1.Y[var6]);
                                if (this.gamePad.buttonObj[var8].Range[0] > var20 && this.gamePad.buttonObj[var8].Range[0] > var7) {
                                    var18 = true;
                                    this.gamePad.buttonObj[var8].Key[2] = this.gamePad.buttonObj[var8].KeyCode[0];
                                    this.gamePad.buttonObj[var8].Key[0] = 0;
                                    var17 = this.gamePad.buttonObj[var8];
                                    ++var17.iKeepFrame;
                                    var19 = var18;
                                    if ((this.gamePad.buttonObj[var8].Status & 1) == 0) {
                                        var17 = this.gamePad.buttonObj[var8];
                                        var17.Status |= 1;
                                        this.gamePad.buttonObj[var8].Key[0] = this.gamePad.buttonObj[var8].KeyCode[0];
                                        var19 = var18;
                                    }
                                    break;
                                }
                            }

                            ++var6;
                        }

                        if (!var19) {
                            this.gamePad.buttonObj[var8].iKeepFrame = 0;
                            if ((this.gamePad.buttonObj[var8].Status & 1) != 0) {
                                var17 = this.gamePad.buttonObj[var8];
                                var17.Status &= -2;
                                this.gamePad.buttonObj[var8].Key[1] = this.gamePad.buttonObj[var8].KeyCode[0];
                            }
                        }
                        break;
                    case 5:
                        var6 = 0;

                        while(true) {
                            var19 = var15;
                            if (var6 >= 10) {
                                break;
                            }

                            if (var1.ID[var6] >= 0) {
                                this.gamePad.buttonObj[var8].PosNow[0] = -100;
                                this.gamePad.buttonObj[var8].PosNow[1] = -100;
                                if (this.gamePad.buttonObj[var8].Pos[0] < var1.X[var6] && this.gamePad.buttonObj[var8].Pos[0] + this.gamePad.buttonObj[var8].Range[0] > var1.X[var6] && this.gamePad.buttonObj[var8].Pos[1] < var1.Y[var6] && this.gamePad.buttonObj[var8].Pos[1] + this.gamePad.buttonObj[var8].Range[1] > var1.Y[var6]) {
                                    var19 = true;
                                    this.gamePad.buttonObj[var8].PosNow[0] = var1.X[var6] - this.gamePad.buttonObj[var8].Pos[0];
                                    this.gamePad.buttonObj[var8].PosNow[1] = var1.Y[var6] - this.gamePad.buttonObj[var8].Pos[1];
                                    this.gamePad.buttonObj[var8].Key[2] = this.gamePad.buttonObj[var8].KeyCode[0];
                                    if ((this.gamePad.buttonObj[var8].Status & 1) == 0) {
                                        var17 = this.gamePad.buttonObj[var8];
                                        var17.Status |= 1;
                                        this.gamePad.buttonObj[var8].Key[0] = this.gamePad.buttonObj[var8].KeyCode[0];
                                    }

                                    var17 = this.gamePad.buttonObj[var8];
                                    ++var17.iKeepFrame;
                                    break;
                                }
                            }

                            ++var6;
                        }

                        if (!var19) {
                            this.gamePad.buttonObj[var8].iKeepFrame = 0;
                            if ((this.gamePad.buttonObj[var8].Status & 1) != 0) {
                                var17 = this.gamePad.buttonObj[var8];
                                var17.Status &= -2;
                                this.gamePad.buttonObj[var8].Key[1] = this.gamePad.buttonObj[var8].KeyCode[0];
                            }
                        }
                        break;
                    case 6:
                        var6 = 0;

                        while(true) {
                            var19 = var12;
                            if (var6 >= 10) {
                                break;
                            }

                            if (var1.ID[var6] >= 0) {
                                this.gamePad.buttonObj[var8].PosNow[0] = -100;
                                this.gamePad.buttonObj[var8].PosNow[1] = -100;
                                if (this.gamePad.buttonObj[var8].Pos[0] < var1.X[var6] && this.gamePad.buttonObj[var8].Pos[0] + this.gamePad.buttonObj[var8].Range[0] > var1.X[var6] && this.gamePad.buttonObj[var8].Pos[1] < var1.Y[var6] && this.gamePad.buttonObj[var8].Pos[1] + this.gamePad.buttonObj[var8].Range[1] > var1.Y[var6]) {
                                    this.gamePad.buttonObj[var8].PosNow[0] = var1.X[var6] - this.gamePad.buttonObj[var8].Pos[0];
                                    this.gamePad.buttonObj[var8].PosNow[1] = var1.Y[var6] - this.gamePad.buttonObj[var8].Pos[1];
                                    var9 = true;
                                    var17 = this.gamePad.buttonObj[var8];
                                    ++var17.iKeepFrame;
                                    var19 = var9;
                                    if ((this.gamePad.buttonObj[var8].Status & 1) == 0) {
                                        this.gamePad.buttonObj[var8].PosRange[0] = var1.X[var6] - this.gamePad.buttonObj[var8].Pos[0];
                                        this.gamePad.buttonObj[var8].PosRange[1] = var1.Y[var6] - this.gamePad.buttonObj[var8].Pos[1];
                                        var17 = this.gamePad.buttonObj[var8];
                                        var17.Status |= 1;
                                        var19 = var9;
                                    }
                                    break;
                                }
                            }

                            ++var6;
                        }

                        if (var19) {
                            break;
                        }

                        if ((this.gamePad.buttonObj[var8].Status & 1) != 0) {
                            var17 = this.gamePad.buttonObj[var8];
                            var17.Status &= -2;
                            if (this.gamePad.buttonObj[var8].iKeepFrame >= 0 && this.gamePad.buttonObj[var8].iKeepFrame <= 5) {
                                this.gamePad.buttonObj[var8].Key[2] = this.gamePad.buttonObj[var8].KeyCode[0];
                            } else if (this.gamePad.buttonObj[var8].iKeepFrame >= 6 && this.gamePad.buttonObj[var8].iKeepFrame <= 10) {
                                this.gamePad.buttonObj[var8].Key[2] = this.gamePad.buttonObj[var8].KeyCode[1];
                            } else if (this.gamePad.buttonObj[var8].iKeepFrame >= 11 && this.gamePad.buttonObj[var8].iKeepFrame <= 15) {
                                this.gamePad.buttonObj[var8].Key[2] = this.gamePad.buttonObj[var8].KeyCode[2];
                            } else if (this.gamePad.buttonObj[var8].iKeepFrame >= 16 && this.gamePad.buttonObj[var8].iKeepFrame <= 20) {
                                this.gamePad.buttonObj[var8].Key[2] = this.gamePad.buttonObj[var8].KeyCode[3];
                            } else if (this.gamePad.buttonObj[var8].iKeepFrame >= 21) {
                                this.gamePad.buttonObj[var8].Key[2] = this.gamePad.buttonObj[var8].KeyCode[4];
                            }

                            this.gamePad.buttonObj[var8].Key[0] = this.gamePad.buttonObj[var8].Key[2];
                        }

                        this.gamePad.buttonObj[var8].iKeepFrame = 0;
                        this.gamePad.buttonObj[var8].PosRange[0] = -65535;
                        this.gamePad.buttonObj[var8].PosRange[1] = -65535;
                        break;
                    case 7:
                        var20 = 0;

                        label323:
                        while(true) {
                            var18 = var19;
                            if (var20 >= 10) {
                                break;
                            }

                            if (var1.ID[var20] >= 0) {
                                var19 = var11;
                                if (this.gamePad.buttonObj[var8].Pos[0] < var1.X[var20]) {
                                    var19 = var11;
                                    if (this.gamePad.buttonObj[var8].Pos[0] + this.gamePad.buttonObj[var8].Range[0] > var1.X[var20]) {
                                        var19 = var11;
                                        if (this.gamePad.buttonObj[var8].Pos[1] < var1.Y[var20]) {
                                            var19 = var11;
                                            if (this.gamePad.buttonObj[var8].Pos[1] + this.gamePad.buttonObj[var8].Range[1] > var1.Y[var20]) {
                                                var19 = true;
                                            }
                                        }
                                    }
                                }

                                switch (this.gamePad.buttonObj[var8].Pross) {
                                    case 0:
                                        var18 = var19;
                                        if (var19) {
                                            this.gamePad.buttonObj[var8].PosNow[0] = -100;
                                            this.gamePad.buttonObj[var8].PosNow[1] = -100;
                                            this.gamePad.buttonObj[var8].PosNow[0] = var1.X[var20];
                                            this.gamePad.buttonObj[var8].PosNow[1] = var1.Y[var20];
                                            this.gamePad.buttonObj[var8].Pross = 1;
                                            var17 = this.gamePad.buttonObj[var8];
                                            var17.Status |= 1;
                                            var18 = var19;
                                        }
                                        break label323;
                                    case 1:
                                        var18 = var19;
                                        if ((this.gamePad.buttonObj[var8].Status & 1) == 0) {
                                            var18 = var19;
                                            if (var1.X[var20] < this.gamePad.buttonObj[var8].PosNow[0] + 10) {
                                                var18 = var19;
                                                if (var1.X[var20] > this.gamePad.buttonObj[var8].PosNow[0] - 10) {
                                                    var18 = var19;
                                                    if (var1.Y[var20] < this.gamePad.buttonObj[var8].PosNow[1] + 10) {
                                                        var18 = var19;
                                                        if (var1.Y[var20] > this.gamePad.buttonObj[var8].PosNow[1] - 10) {
                                                            this.gamePad.buttonObj[var8].Key[2] = this.gamePad.buttonObj[var8].KeyCode[0];
                                                            var18 = var19;
                                                            if ((this.gamePad.buttonObj[var8].Status & 1) == 0) {
                                                                var17 = this.gamePad.buttonObj[var8];
                                                                var17.Status |= 1;
                                                                this.gamePad.buttonObj[var8].Key[0] = this.gamePad.buttonObj[var8].KeyCode[0];
                                                                this.gamePad.buttonObj[var8].iKeepFrame = 0;
                                                                this.gamePad.buttonObj[var8].Pross = 0;
                                                                var18 = false;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        break label323;
                                    default:
                                        var18 = var19;
                                        break label323;
                                }
                            }

                            ++var20;
                        }

                        if (!var18) {
                            if (this.gamePad.buttonObj[var8].Pross != 0) {
                                var17 = this.gamePad.buttonObj[var8];
                                ++var17.iKeepFrame;
                                if (this.gamePad.buttonObj[var8].iKeepFrame > 10) {
                                    this.gamePad.buttonObj[var8].iKeepFrame = 0;
                                    this.gamePad.buttonObj[var8].Pross = 0;
                                }
                            }

                            if ((this.gamePad.buttonObj[var8].Status & 1) != 0) {
                                var17 = this.gamePad.buttonObj[var8];
                                var17.Status &= -2;
                                this.gamePad.buttonObj[var8].Key[1] = this.gamePad.buttonObj[var8].KeyCode[0];
                            }
                        }
                }
            }
        }

    }

    void SetKeyCode(int var1, int var2, int var3) {
        this.gamePad.buttonObj[var1].KeyCode[var2] = var3;
    }

    void SetPad(int var1, int var2, int var3, int var4, int var5) {
        this.gamePad.buttonObj[var1].Pos[0] = var2;
        this.gamePad.buttonObj[var1].Pos[1] = var3;
        if (this.gamePad.buttonObj[var1].Type != 5 && this.gamePad.buttonObj[var1].Type != 2 && this.gamePad.buttonObj[var1].Type != 3 && this.gamePad.buttonObj[var1].Type != 7 && this.gamePad.buttonObj[var1].Type != 6) {
            this.gamePad.buttonObj[var1].Range[0] = var4 >> 1;
            this.gamePad.buttonObj[var1].Range[1] = var5 >> 1;
        } else {
            this.gamePad.buttonObj[var1].Range[0] = var4;
            this.gamePad.buttonObj[var1].Range[1] = var5;
        }

        this.gamePad.buttonObj[var1].Status = 0;
        this.gamePad.buttonObj[var1].Key[0] = 0;
        this.gamePad.buttonObj[var1].Key[1] = 0;
        this.gamePad.buttonObj[var1].Key[2] = 0;
    }

    void SetPad_Pos(int var1, int var2, int var3) {
        this.gamePad.buttonObj[var1].Pos[0] = var2;
        this.gamePad.buttonObj[var1].Pos[1] = var3;
        this.gamePad.buttonObj[var1].Status = 0;
        this.gamePad.buttonObj[var1].Key[0] = 0;
        this.gamePad.buttonObj[var1].Key[1] = 0;
        this.gamePad.buttonObj[var1].Key[2] = 0;
    }

    void SetSkipKey(int var1, int var2) {
        this.gamePad.buttonObj[var1].SkipKey = var2;
    }
    public class ButtonObject {
    int Angle;
    int Counter;
    int GestureDirection;
    int[] Key;
    int[] KeyCode;
    int[] OldKey;
    int[] Pos;
    int[] PosNow;
    int[] PosRange;
    int Pross;
    int[] Range;
    int SkipKey;
    int Speed;
    int Status;
    float StickRange;
    int Type;
    int[] Work;
    int iKeepFrame;
    int iTouchCount;
    final Controller this$0;

    public ButtonObject(Controller var1) {
        this.this$0 = var1;
        this.Pos = new int[2];
        this.PosNow = new int[2];
        this.PosRange = new int[2];
        this.Range = new int[2];
        this.Key = new int[3];
        this.OldKey = new int[3];
        this.KeyCode = new int[6];
        this.Work = new int[2];
        this.clear();
    }

    public void clear() {
        this.Status = 0;
        this.GestureDirection = 0;
        this.Counter = 0;

        int var1;
        for(var1 = 0; var1 < 2; ++var1) {
            this.Pos[var1] = 0;
            this.PosNow[var1] = 0;
            this.PosRange[var1] = 0;
            this.Range[var1] = 0;
            this.Work[var1] = 0;
        }

        this.Type = 0;

        for(var1 = 0; var1 < 3; ++var1) {
            this.Key[var1] = 0;
            this.OldKey[var1] = 0;
        }

        this.Angle = 0;
        this.StickRange = 0.0F;

        for(var1 = 0; var1 < 6; ++var1) {
            this.KeyCode[var1] = 0;
        }

        this.SkipKey = 0;
        this.iKeepFrame = 0;
        this.iTouchCount = 0;
        this.Pross = 0;
        this.Speed = 0;
    }
}
public class GamePad {
    int buttonCount;
    Controller.ButtonObject[] buttonObj;
    final Controller this$0;

    public GamePad(Controller var1) {
        this.this$0 = var1;
        this.buttonObj = new Controller.ButtonObject[10];

        for(int var2 = 0; var2 < 10; ++var2) {
            this.buttonObj[var2] = new Controller.ButtonObject(var1);
        }

        this.buttonCount = 0;
    }
}
}