package com.zue.gd.util;

import java.util.List;

public final class DacWriterCore {

    private final AnoInstance[] anos;

    public DacWriterCore(AnoInstance[] anos) {
        this.anos = anos;
    }

    /* ======================================================
       Writer interno
    ====================================================== */
    private static final class BinWriter {
        private byte[] buf = new byte[4096];
        private int pos = 0;

        private void grow(int extra) {
            int need = pos + extra;
            if (need <= buf.length) return;

            int newLen = buf.length * 2;
            while (newLen < need) newLen *= 2;

            byte[] nb = new byte[newLen];
            System.arraycopy(buf, 0, nb, 0, pos);
            buf = nb;
        }

        void writeByte(int v) {
            grow(1);
            buf[pos++] = (byte) (v & 0xFF);
        }

        void writeShortLE(int v) {
            grow(2);
            buf[pos++] = (byte) (v & 0xFF);
            buf[pos++] = (byte) ((v >> 8) & 0xFF);
        }

        void writeIntLE(int v) {
            grow(4);
            buf[pos++] = (byte) (v & 0xFF);
            buf[pos++] = (byte) ((v >> 8) & 0xFF);
            buf[pos++] = (byte) ((v >> 16) & 0xFF);
            buf[pos++] = (byte) ((v >> 24) & 0xFF);
        }

        void writeBytes(byte[] arr) {
            if (arr == null) return;
            grow(arr.length);
            System.arraycopy(arr, 0, buf, pos, arr.length);
            pos += arr.length;
        }

        void patchShortLE(int offset, int v) {
            buf[offset] = (byte) (v & 0xFF);
            buf[offset + 1] = (byte) ((v >> 8) & 0xFF);
        }

        int position() {
            return pos;
        }

        byte[] toByteArray() {
            byte[] out = new byte[pos];
            System.arraycopy(buf, 0, out, 0, pos);
            return out;
        }
    }

    private static boolean hitNeedsFull(MptInstance.HitData h) {
        if (h.forceFull) return true;
        if (h.x < -128 || h.x > 127) return true;
        if (h.y < -128 || h.y > 127) return true;
        if (h.w < 0 || h.w > 255) return true;
        if (h.h < 0 || h.h > 255) return true;
        return false;
    }

    /* ======================================================
       Build de MPT (binário)
    ====================================================== */
    private byte[] buildMpt(MptInstance mpt, int anoIndex) {

        final BinWriter w = new BinWriter();

        int flagsByte = 0;
        if (mpt.hasTime) flagsByte |= 32;
        if (mpt.hasMid) flagsByte |= 1;
        if (mpt.hasWact) flagsByte |= 2;
        if (mpt.hasSe) flagsByte |= 8;
        if (mpt.move.hasAny()) flagsByte |= 4;
        if (!mpt.hits.isEmpty()) flagsByte |= 16;
        // NOVO: a Flag 64 (Opções Adicionais) também precisa ativar quando
        // há função(ões) extra(s) pendente(s), mesmo sem $OPT normal —
        // mesma correção do bloco de escrita mais abaixo.
        if (mpt.opt.used || ExtrasBuilder.hasExtras(mpt)) flagsByte |= 64;
        if (mpt.raz.used) flagsByte |= 128;

        w.writeByte(flagsByte);

        if (mpt.hasTime) {
            w.writeByte(mpt.time);
        }

        w.writeShortLE(mpt.img);

        if (mpt.hasMid) {
            w.writeShortLE(mpt.midX);
            w.writeShortLE(mpt.midY);
        }

        if (mpt.hasWact)
            w.writeShortLE(mpt.wactFlag);

        if (mpt.hasSe)
            w.writeByte(mpt.se);

        if (mpt.move.hasAny()) {
            int mv = 0;
            if (mpt.move.hasXSpeed) mv |= 1;
            if (mpt.move.hasYSpeed) mv |= 2;
            if (mpt.move.hasXAccel) mv |= 4;
            if (mpt.move.hasYAccel) mv |= 8;
            w.writeByte(mv);

            if (mpt.move.hasXSpeed) w.writeIntLE(mpt.move.xSpeed);
            if (mpt.move.hasYSpeed) w.writeIntLE(mpt.move.ySpeed);
            if (mpt.move.hasXAccel) w.writeIntLE(mpt.move.xAccel);
            if (mpt.move.hasYAccel) w.writeIntLE(mpt.move.yAccel);
        }

        if (!mpt.hits.isEmpty()) {
            w.writeByte(mpt.hits.size());
            int id = 0;
            for (MptInstance.HitData h : mpt.hits) {
                boolean full = hitNeedsFull(h);
                int f = h.flagBase & 0x7F;
                if (full) f |= 0x80;
                w.writeByte(f);

                if (full) {
                    w.writeShortLE(h.x);
                    w.writeShortLE(h.y);
                    w.writeShortLE(h.w);
                    w.writeShortLE(h.h);
                } else {
                    w.writeByte(h.x);
                    w.writeByte(h.y);
                    w.writeByte(h.w);
                    w.writeByte(h.h);
                }
                id++;
            }
        }

        // NOVO: a condição precisa cobrir tanto "há OPT normal" quanto "há
        // função(ões) extra(s) mesmo sem OPT normal" — um $MPT com só $VID
        // (sem $OPT) precisa ativar a Flag 64 e entrar no ramo de escrita
        // de Extras, mesmo com mpt.opt.used == false.
        if (mpt.opt.used || ExtrasBuilder.hasExtras(mpt)) {
            if (ExtrasBuilder.hasExtras(mpt)) {
                ExtrasBuilder.write(new ExtrasBuilder.ByteSink() {
                    public void writeByte(int v) {
                        w.writeByte(v);
                    }

                    public void writeShortLE(int v) {
                        w.writeShortLE(v);
                    }
                }, mpt);
            } else {
                w.writeShortLE(mpt.opt.flag);
                w.writeByte(mpt.opt.data[0]);
                w.writeByte(mpt.opt.data[1]);
                w.writeByte(mpt.opt.data[2]);
                w.writeByte(mpt.opt.data[3]);
            }
        }

        if (mpt.raz.used) {
            w.writeShortLE(mpt.raz.rot);
            w.writeShortLE(mpt.raz.alpha);
            w.writeShortLE(mpt.raz.xzoom);
            w.writeShortLE(mpt.raz.yzoom);
        }

        return w.toByteArray();
    }

    /* ======================================================
       Build de ANO
    ====================================================== */
    private byte[] buildAno(AnoInstance ano) {

        BinWriter w = new BinWriter();

        int pflags = 0;
        if (!ano.links.isEmpty()) pflags |= 2;
        if (ano.hasPriority) pflags |= 4;

        int mptCount = ano.getMptCountUsed();

        w.writeByte(pflags);
        w.writeByte(mptCount);

        if (ano.hasPriority)
            w.writeByte(ano.priority);

        if (!ano.links.isEmpty()) {
            w.writeByte(ano.links.size());
            for (AnoInstance.Link l : ano.links) {
                int combined = ((l.flag4() & 0xF) << 12) | (l.ano12 & 0xFFF);
                w.writeShortLE(l.bx);
                w.writeShortLE(l.by);
                w.writeShortLE(combined);
            }
        }

        int tablePos = w.position();
        for (int i = 0; i < mptCount; i++)
            w.writeShortLE(0);

        int mptStartBase = w.position();

        for (int i = 0; i < mptCount; i++) {
            MptInstance m = ano.mpts.get(i);

            if (!m.used) {
                w.patchShortLE(tablePos + i * 2, 0);
                continue;
            }

            int startPos = w.position();
            int ptr = startPos - mptStartBase;
            w.patchShortLE(tablePos + i * 2, ptr);
            w.writeBytes(buildMpt(m, ano.index));
        }

        return w.toByteArray();
    }

    /* ======================================================
       Build final do DAC
    ====================================================== */
    public byte[] build() {
        BinWriter w = new BinWriter();

        int count = anos.length;

        w.writeShortLE(0);
        w.writeShortLE(count);
        w.writeShortLE(8);
        int dataOffset = 8 + count * 2;
        w.writeShortLE(dataOffset);

        int tablePos = w.position();
        for (int i = 0; i < count; i++)
            w.writeShortLE(0);

        int[] ptrTable = new int[count];

        for (int i = 0; i < count; i++) {
            AnoInstance a = anos[i];

            if (!a.used) {
                ptrTable[i] = -1;
                continue;
            }

            int rel = w.position() - dataOffset;
            int pad = (4 - (rel & 3)) & 3;

            for (int z = 0; z < pad; z++)
                w.writeByte(0);

            rel = w.position() - dataOffset;
            ptrTable[i] = rel / 4;

            w.writeBytes(buildAno(a));
        }

        for (int i = 0; i < count; i++) {
            w.patchShortLE(tablePos + i * 2, ptrTable[i]);
        }

        return w.toByteArray();
    }
}