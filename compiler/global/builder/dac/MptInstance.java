package com.zue.gd.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um MPT dentro de um ANO.
 *
 * <p>Cada MPT pode ter: - TIME - IMG - MID - WACTFLAG - SE - MOVE - HITS - OPT - RAZ
 *
 * <p>Nenhum cálculo binário é feito aqui. Esta classe SOMENTE armazena os valores e as flags, para
 * o writer montar depois.
 */
public final class MptInstance {

    public final AnoInstance parent; // ANO dono deste MPT
    public final int index; // índice deste MPT
    public boolean used; // indica presença real

    /* ======================================================
       TIME
    ====================================================== */
    public boolean hasTime;
    public int time;

    /* ======================================================
       IMG
    ====================================================== */
    public int img;

    /* ======================================================
       MID
    ====================================================== */
    public boolean hasMid;
    public int midX;
    public int midY;

    /* ======================================================
       WACTFLAG
    ====================================================== */
    public boolean hasWact;
    public int wactFlag;

    /* ======================================================
       SE
    ====================================================== */
    public boolean hasSe;
    public int se;

    /* ======================================================
       MOVE
    ====================================================== */

    public static final class MoveData {
        public boolean hasXSpeed;
        public boolean hasYSpeed;
        public boolean hasXAccel;
        public boolean hasYAccel;

        public int xSpeed;
        public int ySpeed;
        public int xAccel;
        public int yAccel;

        /** Se o MPT tem qualquer movimento */
        public boolean hasAny() {
            return hasXSpeed || hasYSpeed || hasXAccel || hasYAccel;
        }
    }

    public final MoveData move = new MoveData();

    /* ======================================================
       HITS
    ====================================================== */

    public static final class HitData {
        /** flagBase é o valor do FLAG sem o bit 7 */
        public int flagBase;

        /** coordenadas */
        public int x;

        public int y;
        public int w;
        public int h;

        /** se deve forçar modo full mesmo se couber no modo curto */
        public boolean forceFull;
    }

    /** Lista de todos os hits deste MPT */
    public final List<HitData> hits = new ArrayList<HitData>();

    /* ======================================================
       OPT
    ====================================================== */

    public static final class OptData {
        public boolean used;
        public int flag;
        public int[] data = new int[4]; // B[4]
    }

    public final OptData opt = new OptData();

    /* ======================================================
       RAZ
    ====================================================== */

    public static final class RazData {
        public boolean used;
        public int rot;
        public int alpha;
        public int xzoom;
        public int yzoom;
    }

    public final RazData raz = new RazData();

    /* ======================================================
       CONSTRUTOR
    ====================================================== */

    public MptInstance(AnoInstance parent, int index) {
        this.parent = parent;
        this.index = index;
    }

    /* ======================================================
       HELPERS DE SETAGEM
    ====================================================== */

    public void setTime(int t) {
        this.hasTime = true;
        this.time = t;
        this.used = true;
    }

    public void setImg(int img) {
        this.img = img;
        this.used = true;
    }

    public void setMid(int x, int y) {
        this.hasMid = true;
        this.midX = x;
        this.midY = y;
        this.used = true;
    }

    public void setWactFlag(int flag) {
        this.hasWact = true;
        this.wactFlag = flag;
        this.used = true;
    }

    public void setSe(int v) {
        this.hasSe = true;
        this.se = v;
        this.used = true;
    }

    /* ----- MOVE ----- */
    public void setMoveXSpeed(int v) {
        this.move.hasXSpeed = true;
        this.move.xSpeed = v;
        this.used = true;
    }

    public void setMoveYSpeed(int v) {
        this.move.hasYSpeed = true;
        this.move.ySpeed = v;
        this.used = true;
    }

    public void setMoveXAccel(int v) {
        this.move.hasXAccel = true;
        this.move.xAccel = v;
        this.used = true;
    }

    public void setMoveYAccel(int v) {
        this.move.hasYAccel = true;
        this.move.yAccel = v;
        this.used = true;
    }

    /* ----- HITS ----- */

    /** Adiciona um hit. O parser decide sozinho se precisa de modo full. */
    public void addHit(int flagBase, int x, int y, int w, int h, boolean forceFull) {
        HitData d = new HitData();
        d.flagBase = flagBase;
        d.x = x;
        d.y = y;
        d.w = w;
        d.h = h;
        d.forceFull = forceFull;
        hits.add(d);
        this.used = true;
    }

    /* ----- OPT ----- */

    public void setOpt(int flag, int b0, int b1, int b2, int b3) {
        opt.used = true;
        opt.flag = flag;
        opt.data[0] = b0;
        opt.data[1] = b1;
        opt.data[2] = b2;
        opt.data[3] = b3;
        this.used = true;
    }

    /* ----- RAZ ----- */

    public void setRaz(int rot, int alpha, int xzoom, int yzoom) {
        raz.used = true;
        raz.rot = rot;
        raz.alpha = alpha;
        raz.xzoom = xzoom;
        raz.yzoom = yzoom;
        this.used = true;
    }
}