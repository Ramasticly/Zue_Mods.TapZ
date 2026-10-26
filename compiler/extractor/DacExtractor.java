package com.zue.gd.util;

import java.io.*;
import java.util.Locale;

/**
 * Versão limpa do dumper DAC. Funcionalidade idêntica ao original — apenas nomes e organização
 * melhorados.
 */
public class DacExtractor {

    // ---- estruturas internas (renomeadas / compactas) ----

    public static final class ActNode {
        public int flags;
        public int optFlag;
        public ActNode[] subPoints;
        public int mptBase;
        public int priority;
        public int alpha;
        public int hitCount;
        public int linkCount;
        public int rot;
        public int accelX;
        public int speedX;
        public int xzoom;
        public int accelY;
        public int speedY;
        public int yzoom;
        public int jr; // reservado (não usado explicitamente)
        public int img;
        public int mptCount;
        public int midX;
        public int midY;
        public int se;
        public int time;
        public int timeBackup;

        public int[] linkFlags = new int[13];
        public int[] optData = new int[4];
        public int[] jd = new int[7]; // reservado (mantido tamanho original)
        public int[] hits = new int[50]; // 5 values per hit
        public int[] jl = new int[24]; // reservado

        public ActNode[] jt = new ActNode[13]; // reservado
    }

    public static final class Vars {
        final Object[] values;
        public int pos;

        private Vars() {
            this.values = new Object[255];
            this.pos = 0;
        }

        Vars(byte b) {
            this();
        }

        public final Vars add(Object obj) {
            this.values[this.pos++] = obj;
            return this;
        }

        public final Vars addShort(short s) {
            this.values[this.pos++] = Short.valueOf(s);
            return this;
        }

        public final Vars addInt(int i) {
            this.values[this.pos++] = Integer.valueOf(i);
            return this;
        }
    }

    public static final class Printer {
        private final PrintStream out;
        private final StringBuffer buf = new StringBuffer();
        public final Vars vars = new Vars((byte) 0);
        private int indentLevel = 0;
        private final Object[] padArg = {" "};

        public Printer(OutputStream os) {
            if (os == null) {
                this.out = System.out;
                return;
            }
            try {
                this.out = new PrintStream(os, true, "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public final void indent(int delta) {
            if (this.indentLevel + delta < 0) this.indentLevel = 0;
            else this.indentLevel += delta;
        }

        public final void println() {
            if (this.buf.length() > 0) {
                if (this.indentLevel > 0) {
                    this.out.printf(new StringBuffer("%").append(this.indentLevel).append("s").toString(), this.padArg);
                }
                this.out.print(this.buf.toString());
                this.buf.delete(0, this.buf.length());
            }
            this.out.println();
        }

        public final boolean hasPendingLine() {
            return this.buf.length() > 0;
        }

        public final void println(String s) {
            this.buf.append(s);
            println();
        }

        public final Printer writef(String fmt) {
            this.buf.append(String.format(Locale.US, fmt, this.vars.values));
            return this;
        }

        public final void writelnf(String fmt) {
            this.buf.append(String.format(Locale.US, fmt, this.vars.values));
            println();
        }
    }

    /**
     * Buffer reader similar ao original, com nomes legíveis. Mantém a mesma semântica e
     * comportamento (LE / BE conforme original).
     */
    public static class BufReader extends InputStream implements Closeable {

        private int markPos;
        private byte[] scratch;
        protected byte[] buf;
        private int count;
        protected int pos;

        public BufReader() {
            this.markPos = 0;
            this.scratch = new byte[8];
            this.buf = null;
            this.pos = 0;
            this.count = 0;
        }

        public BufReader(byte[] data) {
            this.markPos = 0;
            this.scratch = new byte[8];
            setBuffer(data, 0, data.length);
        }

        /** lê 4 bytes little-endian (original a()) */
        public final int readIntLE() {
            try {
                int b0 = read();
                int b1 = read();
                int b2 = read();
                int b3 = read();
                if ((b0 | b1 | b2 | b3) < 0) throw new EOFException();
                return b0 + (b1 << 8) + (b2 << 16) + (b3 << 24);
            } catch (Exception e) {
                return 0;
            }
        }

        /** lê string com encoding (original a(int,str)) */
        public final String readString(int len, String enc) {
            try {
                if (len > this.count - this.pos) len = this.count - this.pos;
                String s = new String(this.buf, this.pos, len, enc);
                this.pos += len;
                return s;
            } catch (Exception e) {
                return "";
            }
        }

        /** cria uma janela sobre outro reader (original a(ByteArrayReader,...)) */
        public final void setWindow(BufReader src, int offset, int length) {
            this.buf = src.buf;
            this.count = Math.min(offset + length, src.count - src.markPos) + src.markPos;
            int p = src.markPos + offset;
            this.pos = p;
            this.markPos = p;
        }

        @Override
        public final synchronized int available() {
            return this.count - this.pos;
        }

        /** lê short little-endian (original b()) */
        public final short readShortLE() {
            try {
                int b0 = read();
                int b1 = read();
                if ((b0 | b1) < 0) throw new EOFException();
                return (short) (b0 + (b1 << 8));
            } catch (Exception e) {
                return 0;
            }
        }

        /** configura buffer (original c(byte[],...)) */
        public final void setBuffer(byte[] data, int ignored1, int len) {
            this.buf = data;
            this.pos = 0;
            this.markPos = 0;
            this.count = Math.min(len + 0, data.length);
        }

        @Override
        public final void close() {}

        public final long getFilePointer() {
            return this.pos - this.markPos;
        }

        public final long length() {
            return this.count - this.markPos;
        }

        @Override
        public final void mark(int readlimit) {
            this.markPos = this.pos;
        }

        @Override
        public final boolean markSupported() {
            return true;
        }

        /** lê unsigned byte (original p()) */
        public final int readUnsignedByte() {
            try {
                int r = read();
                if (r < 0) throw new EOFException();
                return r;
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public final synchronized int read() {
            int v;
            if (this.pos < this.count) {
                byte[] b = this.buf;
                int p = this.pos;
                this.pos = p + 1;
                v = b[p] & 0xFF;
            } else {
                v = -1;
            }
            return v;
        }

        @Override
        public final synchronized int read(byte[] bArr, int off, int len) {
            int readCount = 0;
            synchronized (this) {
                if (bArr == null) throw new NullPointerException();
                if (off < 0 || len < 0 || len > bArr.length - off)
                    throw new IndexOutOfBoundsException();
                if (len != 0) {
                    if (this.pos >= this.count) {
                        readCount = -1;
                    } else {
                        int available = this.count - this.pos;
                        if (len <= available) available = len;
                        if (available > 0) {
                            System.arraycopy(this.buf, this.pos, bArr, off, available);
                            this.pos += available;
                            readCount = available;
                        }
                    }
                }
            }
            return readCount;
        }

        /** lê byte com sinal (original readByte()) */
        public final byte readSByte() {
            try {
                int r = read();
                if (r < 0) throw new EOFException();
                return (byte) r;
            } catch (Exception e) {
                return 0;
            }
        }

        /** lê int big-endian (original readInt()) */
        public final int readIntBE() {
            try {
                int b0 = read();
                int b1 = read();
                int b2 = read();
                int b3 = read();
                if ((b0 | b1 | b2 | b3) < 0) throw new EOFException();
                return (b0 << 24) + (b1 << 16) + (b2 << 8) + (b3);
            } catch (Exception e) {
                return 0;
            }
        }

        /** lê short big-endian (original readShort()) */
        public final short readShortBE() {
            try {
                int b0 = read();
                int b1 = read();
                if ((b0 | b1) < 0) throw new EOFException();
                return (short) ((b0 << 8) + b1);
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public final synchronized void reset() {
            this.pos = this.markPos;
        }

        public final void seek(long j) {
            try {
                if (j < 0) throw new IOException("Negative seek offset");
                if (j > this.count) throw new EOFException();
                this.pos = ((int) j) + this.markPos;
            } catch (Exception e) {
                // manter compatibilidade: swallow
            }
        }

        @Override
        public final synchronized long skip(long n) {
            try {
                long avail = this.count - this.pos;
                if (n >= avail) n = avail;
                else if (n < 0) n = 0;
                this.pos = (int) (this.pos + n);
                return n;
            } catch (Exception e) {
                return 0;
            }
        }

        public final int skipBytes(int n) {
            try {
                if (n <= 0) return 0;
                long fp = getFilePointer();
                long len = length();
                long want = n + fp;
                if (want <= len) len = want;
                seek(len);
                return (int) (len - fp);
            } catch (Exception e) {
                return 0;
            }
        }

        public final void writeTo(OutputStream out) {
            try {
                out.write(this.buf, this.markPos, this.count - this.markPos);
            } catch (Exception e) {
                // swallow
            }
        }
    }

    // ---- estado global usado pelo extractor (único) ----
    static ActNode[] nodes = new ActNode[0];

    // ---- utilitários de I/O simples (mantidos) ----
    public static byte[] readAllBytes(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            int total = 0;
            while (total < buffer.length) {
                int n = fis.read(buffer, total, buffer.length - total);
                if (n < 0) break;
                total += n;
            }
            fis.close();
            return buffer;
        } catch (IOException e) {
            return null;
        }
    }

    public static void process(String pacName,
            String index,
            byte[] src,
            int dataPos,
            int dataSize,
            int cnvType,
            File outDir)
            throws Exception {

        // caminho final desejado
        File dumpFile = new File(outDir, index + ".dac.txt");

        // usa extrator que lê direto do array
        dumpDacBytes(src, dataPos, dataSize, dumpFile);
    }

    public static void dumpDacBytes(byte[] src, int offset, int len, File outputFile) {
        try {
            // Criar sub-buffer apontando para o trecho DAC
            byte[] local = new byte[len];
            System.arraycopy(src, offset, local, 0, len);

            BufReader reader = new BufReader();
            reader.setBuffer(local, 0, local.length);

            FileOutputStream os = new FileOutputStream(outputFile);
            Printer printer = new Printer(os);

            DacExtractor(reader, printer);

            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---- função pública que grava o dump (nome mantido similar) ----
    public static void dumpDac(File inputDAC, File outputFile) {
        try {
            byte[] buffer = readAllBytes(inputDAC);

            BufReader reader = new BufReader();
            reader.setBuffer(buffer, 0, buffer.length);

            OutputStream os = new FileOutputStream(outputFile);
            Printer printer = new Printer(os);

            DacExtractor(reader, printer);

            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Valor interno de movimento (fixo 16.16) → pixels em decimal EXATO.
    // Todo int/65536 tem expansão decimal finita (até 16 casas) e
    // DacParser.pixelsToInternal volta exatamente ao mesmo int. A versão
    // anterior arredondava para 2 casas: 0x2000 (0.125 px) virava "0.13" e
    // voltava como 8519, mudando a física de todo golpe recompilado.
    static String fixedToPixels(int v) {
        java.math.BigDecimal d = new java.math.BigDecimal(v)
                .divide(java.math.BigDecimal.valueOf(65536)).stripTrailingZeros();
        return d.scale() <= 0 ? d.toBigInteger().toString() : d.toPlainString();
    }

    // ---- O extrator limpo (mesma lógica) ----
    public static void DacExtractor(BufReader reader, Printer printer) {
        reader.seek(2L);
        int count = reader.readShortLE(); // originally aVar.b() returned short LE
        short tableOffset = reader.readShortLE();
        short dataOffset = reader.readShortLE();

        printer.writelnf("DAC_ACT_ {");
        printer.indent(3);
        nodes = new ActNode[count];

        for (int idx = 0; idx < count; idx++) {
            nodes[idx] = new ActNode();
            ActNode node = nodes[idx];

            reader.seek((idx * 2) + tableOffset);
            short entryPtr = reader.readShortLE();

            // prepare vars for formatted printing
            Vars v = printer.vars;
            v.pos = 0;
            v.addInt(idx);
            printer.writelnf("$ANO_%d: {");
            printer.indent(3);

            if (entryPtr != -1) {
                reader.seek((entryPtr << 2) + dataOffset);
                int pflags = reader.readUnsignedByte();
                node.mptCount = reader.readUnsignedByte();

                if ((pflags & 4) != 0) {
                    node.priority = reader.readSByte();
                    Vars v2 = printer.vars;
                    v2.pos = 0;
                    v2.addInt(node.priority);
                    printer.writef("$PRIO: %d, ");
                } else {
                    node.priority = 0;
                }

                int filePointer = (int) reader.getFilePointer();

                if ((pflags & 2) != 0) {
                    node.linkCount = reader.readUnsignedByte();
                    if ((pflags & 4) != 0) {
                        printer.println();
                    }
                    printer.writelnf("$LNKS:{");
                    printer.indent(3);
                    filePointer = (int) reader.getFilePointer();

                    for (int l = 0; l < node.linkCount; l++) {
                        reader.seek(filePointer);
                        short bx = reader.readShortLE();
                        short by = reader.readShortLE();
                        node.linkFlags[l] = reader.readShortLE();
                        String linkSpawnPos = ((node.linkFlags[l] >> 12) & 1) != 0 ? "YES" : "NOT";
                        String linkXY = ((node.linkFlags[l] >> 14) & 1) != 0 ? "YES" : "NOT";
                        String linkMpt = ((node.linkFlags[l] >> 15) & 1) != 0 ? "YES" : "NOT";
                        Vars v3 = printer.vars;
                        v3.pos = 0;
                        v3.addInt(l).addInt(bx).addInt(by)
                                .add(linkSpawnPos)
                                .add(linkXY)
                                .add(linkMpt)
                                .addInt(node.linkFlags[l] & 4095);
                        printer.writelnf("$LINK_%d: {$BX: %d, $BY: %d, $SYNC{SPAWN_POS: %s, XY: %s, MPT: %s}, $ANO: %d},");
                        filePointer = (int) reader.getFilePointer();
                    }
                    printer.indent(-3);
                    printer.println("}, ");
                } else if ((pflags & 4) != 0) {
                    printer.println();
                }

                node.mptBase = filePointer;
                reader.seek(node.mptBase);
                node.subPoints = new ActNode[node.mptCount];

                if (node.mptCount > 0) {
                    printer.writelnf("$MPTS:{ ");
                    printer.indent(3);
                    int mptIndex = 0;
                    while (mptIndex < node.mptCount) {
                        node.subPoints[mptIndex] = new ActNode();
                        ActNode mpt = node.subPoints[mptIndex];

                        reader.seek(node.mptBase + (mptIndex * 2));
                        reader.seek(reader.readShortLE() + node.mptBase + (node.mptCount * 2));

                        byte flagsByte = reader.readSByte();
                        Vars v4 = printer.vars;
                        v4.pos = 0;
                        v4.addInt(mptIndex);
                        printer.writelnf("$MPT_%d:{");
                        printer.indent(3);

                        if ((flagsByte & 32) != 0) {
                            mpt.time = reader.readUnsignedByte();
                            Vars v5 = printer.vars;
                            v5.pos = 0;
                            v5.addInt(mpt.time);
                            printer.writef("$TIME: %d, ");
                            mpt.timeBackup = mpt.time;
                        } else {
                            mpt.time = 0;
                            mpt.timeBackup = 0;
                        }

                        mpt.img = reader.readShortLE();
                        Vars v6 = printer.vars;
                        v6.pos = 0;
                        v6.addInt(mpt.img);
                        printer.writef("$IMG: %d, ");

                        if ((flagsByte & 1) != 0) {
                            mpt.midX = reader.readShortLE();
                            mpt.midY = reader.readShortLE();
                            Vars v7 = printer.vars;
                            v7.pos = 0;
                            v7.addInt(mpt.midX).addInt(mpt.midY);
                            printer.writef("$MID:[%d, %d], ");
                        } else {
                            mpt.midX = 0;
                            mpt.midY = 0;
                        }

                        mpt.flags &= 40960;
                        if ((flagsByte & 2) != 0) {
                            mpt.flags = reader.readShortLE();
                            Vars v8 = printer.vars;
                            v8.pos = 0;
                            v8.addInt(mpt.flags);
                            printer.writef("$WACTFLAG: %d, ");
                        }

                        mpt.se = -1;
                        if ((flagsByte & 8) != 0) {
                            byte seVal = reader.readSByte();
                            Vars v9 = printer.vars;
                            v9.pos = 0;
                            v9.addInt(seVal);
                            printer.writef("$SE: %d, ");
                            if (80 <= seVal) mpt.se = seVal;
                        }

                        if ((mpt.flags & 64) == 0) {
                            mpt.speedX = 0;
                            mpt.speedY = 0;
                        }

                        mpt.accelX = 0;
                        mpt.accelY = 0;

                        if ((flagsByte & 4) != 0) {
                            printer.println();
                            byte mvFlags = reader.readSByte();
                            printer.writef("$MOVE:{");
                            if ((mvFlags & 1) != 0) {
                                mpt.speedX = reader.readIntLE();
                                Vars v10 = printer.vars;
                                v10.pos = 0;
                                v10.add(fixedToPixels(mpt.speedX));
                                printer.writef("$XSPEED: %s, ");
                            }
                            if ((mvFlags & 2) != 0) {
                                mpt.speedY = reader.readIntLE();
                                Vars v11 = printer.vars;
                                v11.pos = 0;
                                v11.add(fixedToPixels(mpt.speedY));
                                printer.writef("$YSPEED: %s, ");
                            }
                            if ((mvFlags & 4) != 0) {
                                mpt.accelX = reader.readIntLE();
                                Vars v12 = printer.vars;
                                v12.pos = 0;
                                v12.add(fixedToPixels(mpt.accelX));
                                printer.writef("$XACCEL: %s, ");
                            } else {
                                mpt.accelX = 0;
                            }
                            if ((mvFlags & 8) != 0) {
                                mpt.accelY = reader.readIntLE();
                                Vars v13 = printer.vars;
                                v13.pos = 0;
                                v13.add(fixedToPixels(mpt.accelY));
                                printer.writef("$YACCEL: %s, ");
                            } else {
                                mpt.accelY = 0;
                            }
                            printer.println("}, ");
                        }

                        if ((flagsByte & 16) != 0) {
                            if ((flagsByte & 4) == 0) printer.println();
                            mpt.hitCount = reader.readUnsignedByte();
                            printer.writelnf("$HITS:{");
                            printer.indent(3);
                            for (int h = 0; h < mpt.hitCount; h++) {
                                byte flagByte = reader.readSByte();
                                int baseIdx = h * 5;
                                mpt.hits[baseIdx + 0] = flagByte & Byte.MAX_VALUE;
                                if ((flagByte & 128) != 0) {
                                    mpt.hits[baseIdx + 1] = reader.readShortLE();
                                    mpt.hits[baseIdx + 2] = reader.readShortLE();
                                    mpt.hits[baseIdx + 3] = reader.readShortLE();
                                    mpt.hits[baseIdx + 4] = reader.readShortLE();
                                } else {
                                    mpt.hits[baseIdx + 1] = reader.readSByte();
                                    mpt.hits[baseIdx + 2] = reader.readSByte();
                                    mpt.hits[baseIdx + 3] = reader.readUnsignedByte();
                                    mpt.hits[baseIdx + 4] = reader.readUnsignedByte();
                                }
                                Vars v14 = printer.vars;
                                v14.pos = 0;
                                v14.addInt(h).addInt(flagByte & Byte.MAX_VALUE)
                                        .addInt(mpt.hits[baseIdx + 1]).addInt(mpt.hits[baseIdx + 2])
                                        .addInt(mpt.hits[baseIdx + 3]).addInt(mpt.hits[
                                        baseIdx + 4]);
                                printer.writelnf("$HT_%d:{$FLAG: %d, $COORDS: [%d,%d,%d,%d]}, ");
                            }
                            printer.indent(-3);
                            printer.println("},");
                        } else {
                            mpt.hitCount = 0;
                        }

                        if ((flagsByte & 64) != 0) {
                            mpt.optFlag = reader.readShortLE();
                            if (ExtrasExtractor.isExtrasFlag(mpt.optFlag)) {
                                ExtrasExtractor.extractAndPrint(reader, printer);
                                mpt.optData[0] = mpt.optData[1] = mpt.optData[2] = mpt.optData[
                                3] = 0;
                            } else {
                                for (int k = 0; k < 4; k++) {
                                    mpt.optData[k] = reader.readUnsignedByte();
                                }
                                Vars v15 = printer.vars;
                                v15.pos = 0;
                                v15.addInt(mpt.optFlag).addInt(mpt.optData[0]).addInt(mpt.optData[
                                1]).addInt(mpt.optData[2]).addInt(mpt.optData[3]);
                                if (printer.hasPendingLine()) {
                                    printer.println();
                                }
                                printer.writef("$OPT:{$FLAG: %d, $DATA: B[%d,%d,%d,%d]}, ");
                                printer.println();
                            }
                        } else {
                            mpt.optFlag = 0;
                            mpt.optData[0] = mpt.optData[1] = mpt.optData[2] = mpt.optData[3] = 0;
                        }

                        if ((flagsByte & 128) != 0) {
                            mpt.rot = reader.readShortLE();
                            mpt.alpha = reader.readShortLE();
                            mpt.xzoom = reader.readShortLE();
                            mpt.yzoom = reader.readShortLE();
                            Vars v16 = printer.vars;
                            v16.pos = 0;
                            v16.addInt(mpt.rot).addInt(mpt.alpha).addInt(mpt.xzoom).addInt(mpt.yzoom);
                            if (printer.buf.length() > 0) {
                                printer.println();
                            }
                            printer.writef("$RAZ:{$ROT: %d, $ALPHA: %d, $XZOOM: %d, $YZOOM: %d}, ");
                            printer.println();
                            mpt.flags |= 2048;
                            if (mpt.alpha != 0) {
                                int top = mpt.alpha >> 8;
                                mpt.alpha = 255 - (mpt.alpha & 255);
                                if (top != 0) {
                                    if (top == 1) mpt.flags |= 512;
                                    else mpt.flags |= 1024;
                                }
                            } else {
                                mpt.alpha = 255;
                            }
                        } else {
                            mpt.alpha = 255;
                            mpt.rot = 0;
                            mpt.xzoom = 100;
                            mpt.yzoom = 100;
                        }

                        if ((flagsByte & 20) == 0) {
                            printer.println();
                        }

                        printer.indent(-3);
                        printer.writelnf("}, ");
                        mptIndex++;
                    }
                    printer.indent(-3);
                    printer.println("},");
                } else if ((pflags & 6) == 0) {
                    // ANO existente mas sem PRIO/LINK/MPT: sem esta linha ele
                    // sairia idêntico a um ANO inexistente (ponteiro -1) e o
                    // DacParser não teria como distinguir os dois.
                    printer.println("$MPTS:{ },");
                }
            }

            printer.indent(-3);
            printer.println("},");
        }

        printer.indent(-3);
        printer.println("}");
    }
}