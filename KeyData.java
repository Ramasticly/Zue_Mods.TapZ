package com.namcobandaigames.dragonballtap.apk;

public class KeyData {

    public static final int BUF_MAX = 10;
    int[] BaseX = new int[10];
    int[] BaseY = new int[10];
    int[] BeginTouch = new int[10];
    int[] ID = new int[10];
    int[] MoveX = new int[10];
    int[] MoveY = new int[10];
    int[] OldX = new int[10];
    int[] OldY = new int[10];
    int[] Space = new int[10];
    int[] SpeedX = new int[10];
    int[] SpeedY = new int[10];
    int[] X = new int[10];
    int[] Y = new int[10];

    public KeyData() {
        Init();
    }

    void Init() {
        for (int i = 0; i < 10; i++) {
            this.X[i] = 0;
            this.Y[i] = 0;
            this.Space[i] = 0;
            this.BeginTouch[i] = 0;
            this.ID[i] = -1;
        }
    }

    int GetSpace() {
        for (int i = 0; i < 10; i++) {
            if (this.ID[i] == -1) {
                return i;
            }
        }
        return -1;
    }

    int GetIndex(int _ID) {
        for (int i = 0; i < 10; i++) {
            if (_ID == this.ID[i]) {
                return i;
            }
        }
        return -1;
    }

    void Set(int x, int y, int beginTouch, int _ID) {
        int index = GetIndex(_ID);
        if (index < 0) {
            index = GetSpace();
            if (index < 0) {
                return;
            }
        }
        if (beginTouch != 0) {
            this.BaseX[index] = x;
            this.BaseY[index] = y;
            this.OldX[index] = this.BaseX[index];
            this.OldY[index] = this.BaseY[index];
        } else {
            this.OldX[index] = this.X[index];
            this.OldY[index] = this.Y[index];
        }
        this.X[index] = x;
        this.Y[index] = y;
        this.SpeedX[index] = this.X[index] - this.OldX[index];
        this.SpeedY[index] = this.Y[index] - this.OldY[index];
        this.MoveX[index] = this.X[index] - this.BaseX[index];
        this.MoveY[index] = this.Y[index] - this.BaseY[index];
        this.Space[index] = 1;
        this.ID[index] = _ID;
        this.BeginTouch[index] = beginTouch;
    }

    void ClearBegin() {
        for (int i = 0; i < 10; i++) {
            this.BeginTouch[i] = 0;
        }
    }

    boolean isBeginTouch(int _ID) {
        int index = GetIndex(_ID);
        if (index < 0 || this.BeginTouch[index] == 0) {
            return false;
        }
        return true;
    }

    void Clear(int _ID) {
        int index = GetIndex(_ID);
        if (index >= 0) {
            this.BaseX[index] = 0;
            this.BaseY[index] = 0;
            this.X[index] = 0;
            this.Y[index] = 0;
            this.Space[index] = 0;
            this.BeginTouch[index] = 0;
            this.ID[index] = -1;
        }
    }

    int getX(int _ID) {
        int index = GetIndex(_ID);
        if (index < 0) {
            return 0;
        }
        return this.X[index];
    }

    int getY(int _ID) {
        int index = GetIndex(_ID);
        if (index < 0) {
            return 0;
        }
        return this.Y[index];
    }

    int getSpeedX(int _ID) {
        int index = GetIndex(_ID);
        if (index < 0) {
            return 0;
        }
        return this.SpeedX[index];
    }

    int getSpeedY(int _ID) {
        int index = GetIndex(_ID);
        if (index < 0) {
            return 0;
        }
        return this.SpeedY[index];
    }

    int getMoveX(int _ID) {
        int index = GetIndex(_ID);
        if (index < 0) {
            return 0;
        }
        return this.MoveX[index];
    }

    int getMoveY(int _ID) {
        int index = GetIndex(_ID);
        if (index < 0) {
            return 0;
        }
        return this.MoveY[index];
    }
}
