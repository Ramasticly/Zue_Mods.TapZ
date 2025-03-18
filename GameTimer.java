package com.namcobandaigames.dragonballtap.apk;

public class GameTimer {

    long startTime = 0;
    long suspendTime = 0;
    int time = 10;

    public GameTimer() {
        Init(10);
    }

    public GameTimer(int t) {
        Init(t);
    }

    public void SetTimer(int t) {
        Init(t);
    }

    private void Init(int t) {
        this.time = t;
    }

    public void Start() {
        this.startTime = System.currentTimeMillis();
        this.suspendTime = 0;
    }

    public void Stop() {
    }

    public void Suspend() {
        this.suspendTime = System.currentTimeMillis();
    }

    public void Resume() {
        long resumeTime = System.currentTimeMillis() - this.suspendTime;
        if (resumeTime > 0) {
            this.startTime += resumeTime;
        }
    }

    public void Release() {
    }

    public int GetTimerCnt() {
        return ((int) (System.currentTimeMillis() - this.startTime)) / this.time;
    }
}
