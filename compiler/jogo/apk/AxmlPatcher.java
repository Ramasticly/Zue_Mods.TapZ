package com.zue.gd.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mexe no AndroidManifest.xml binario (AXML) do base_apk antes de entrar no Mod APK:
 *   - package: troca so o nome do pacote (o APK instala separado do jogo). Para nao quebrar o APK, os
 *     nomes de classe relativos (".MainActivity", ".SketchApplication"...) viram absolutos com o pacote
 *     ANTIGO (onde as classes estao de verdade no dex), como o aapt --rename-manifest-package faz;
 *     authorities de provider com o pacote antigo passam a usar o novo (senao dois APKs nao instalam juntos).
 *   - uses-sdk: minSdkVersion / targetSdkVersion.
 * Os textos novos sao acrescentados no fim do string pool (os antigos ficam como estao: os outros
 * elementos que os usam nao mudam).
 */
final class AxmlPatcher {
    private AxmlPatcher() { }

    private static final int RES_XML = 0x0003, RES_STRING_POOL = 0x0001, RES_XML_START = 0x0102;
    private static final int TYPE_STRING = 0x03, TYPE_INT_DEC = 0x10;
    private static final int NO_ENTRY = 0xFFFFFFFF;

    /** newPkg null/vazio = nao troca; minSdk/targetSdk menor que 1 = nao troca. */
    static byte[] patch(byte[] in, String newPkg, int minSdk, int targetSdk) throws IOException {
        byte[] b = in.clone();
        if (u16(b, 0) != RES_XML || u16(b, 8) != RES_STRING_POOL) {
            throw new IOException("AndroidManifest.xml nao e um XML binario");
        }
        int poolStart = 8;
        int poolHeader = u16(b, poolStart + 2);
        int poolSize = u32(b, poolStart + 4);
        int count = u32(b, poolStart + 8);
        int styles = u32(b, poolStart + 12);
        int flags = u32(b, poolStart + 16);
        int stringsStart = u32(b, poolStart + 20);
        int stylesStart = u32(b, poolStart + 24);
        boolean utf8 = (flags & 0x100) != 0;

        List<String> strs = new ArrayList<String>();
        for (int i = 0; i < count; ++i) {
            strs.add(readString(b, poolStart + stringsStart + u32(b, poolStart + poolHeader + i * 4), utf8));
        }
        List<String> added = new ArrayList<String>();

        String oldPkg = null;
        boolean renamePkg = false;
        int pos = poolStart + poolSize;
        while (pos + 8 <= b.length) {
            int type = u16(b, pos);
            int size = u32(b, pos + 4);
            if (size <= 0) {
                break;
            }
            if (type == RES_XML_START) {
                String el = str(strs, added, u32(b, pos + 20));
                int attrStart = u16(b, pos + 24);
                int attrSize = u16(b, pos + 26);
                int attrCount = u16(b, pos + 28);
                for (int i = 0; i < attrCount; ++i) {
                    int a = pos + 16 + attrStart + i * attrSize;
                    String an = str(strs, added, u32(b, a + 4));
                    int raw = u32(b, a + 8);
                    int dataType = b[a + 15] & 0xFF;
                    String val = raw == NO_ENTRY ? null : str(strs, added, raw);

                    if ("manifest".equals(el) && "package".equals(an) && val != null) {
                        oldPkg = val;
                        if (newPkg != null && newPkg.length() > 0 && !newPkg.equals(val)) {
                            renamePkg = true;
                            setString(b, a, dataType, addString(strs, added, newPkg));
                        }
                    } else if ("uses-sdk".equals(el) && ("minSdkVersion".equals(an) || "targetSdkVersion".equals(an))) {
                        int v = "minSdkVersion".equals(an) ? minSdk : targetSdk;
                        if (v > 0) {
                            put32(b, a + 8, NO_ENTRY);
                            b[a + 15] = (byte) TYPE_INT_DEC;
                            put32(b, a + 16, v);
                        }
                    } else if (renamePkg && val != null && isComponent(el) && isClassAttr(an)) {
                        String abs = val.startsWith(".") ? oldPkg + val : (val.indexOf('.') < 0 ? oldPkg + "." + val : null);
                        if (abs != null) {
                            setString(b, a, dataType, addString(strs, added, abs));
                        }
                    } else if (renamePkg && val != null && "provider".equals(el) && "authorities".equals(an) && val.contains(oldPkg)) {
                        setString(b, a, dataType, addString(strs, added, val.replace(oldPkg, newPkg)));
                    }
                }
            }
            pos += size;
        }
        if (added.isEmpty()) {
            return b;                            // so numeros mudaram (uses-sdk): mesmo tamanho
        }
        if (styles != 0) {
            throw new IOException("string pool com estilos: troca de pacote nao suportada neste manifesto");
        }

        // string pool novo: offsets antigos + novos, dados antigos como estao + textos novos no fim
        int oldDataEnd = stylesStart != 0 ? stylesStart : poolSize;
        byte[] oldData = new byte[oldDataEnd - stringsStart];
        System.arraycopy(b, poolStart + stringsStart, oldData, 0, oldData.length);
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(oldData);
        int newCount = count + added.size();
        int[] offs = new int[newCount];
        for (int i = 0; i < count; ++i) {
            offs[i] = u32(b, poolStart + poolHeader + i * 4);
        }
        for (int i = 0; i < added.size(); ++i) {
            offs[count + i] = data.size();
            data.write(encode(added.get(i), utf8));
        }
        while (data.size() % 4 != 0) {
            data.write(0);
        }
        int newStringsStart = poolHeader + newCount * 4;
        int newPoolSize = newStringsStart + data.size();

        ByteArrayOutputStream pool = new ByteArrayOutputStream();
        byte[] head = new byte[poolHeader];
        System.arraycopy(b, poolStart, head, 0, poolHeader);
        put32(head, 4, newPoolSize);
        put32(head, 8, newCount);
        put32(head, 12, 0);
        put32(head, 16, flags & ~0x1);           // sem "SORTED"
        put32(head, 20, newStringsStart);
        put32(head, 24, 0);
        pool.write(head);
        byte[] o4 = new byte[4];
        for (int i = 0; i < newCount; ++i) {
            put32(o4, 0, offs[i]);
            pool.write(o4);
        }
        pool.write(data.toByteArray());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(b, 0, poolStart);
        out.write(pool.toByteArray());
        out.write(b, poolStart + poolSize, b.length - poolStart - poolSize);
        byte[] r = out.toByteArray();
        put32(r, 4, r.length);
        return r;
    }

    private static boolean isComponent(String el) {
        return "application".equals(el) || "activity".equals(el) || "activity-alias".equals(el) || "service".equals(el)
                || "receiver".equals(el) || "provider".equals(el) || "instrumentation".equals(el);
    }

    private static boolean isClassAttr(String an) {
        return "name".equals(an) || "targetActivity".equals(an) || "parentActivityName".equals(an)
                || "backupAgent".equals(an) || "manageSpaceActivity".equals(an);
    }

    private static void setString(byte[] b, int a, int dataType, int idx) {
        put32(b, a + 8, idx);
        if (dataType == TYPE_STRING) {
            put32(b, a + 16, idx);
        }
    }

    private static int addString(List<String> strs, List<String> added, String s) {
        int i = strs.indexOf(s);
        if (i >= 0) {
            return i;
        }
        int j = added.indexOf(s);
        if (j < 0) {
            added.add(s);
            j = added.size() - 1;
        }
        return strs.size() + j;
    }

    private static String str(List<String> strs, List<String> added, int idx) {
        if (idx < 0 || idx == NO_ENTRY) {
            return null;
        }
        if (idx < strs.size()) {
            return strs.get(idx);
        }
        idx -= strs.size();
        return idx < added.size() ? added.get(idx) : null;
    }

    private static String readString(byte[] b, int p, boolean utf8) throws IOException {
        if (utf8) {
            int n = b[p] & 0xFF;
            p += (n & 0x80) != 0 ? 2 : 1;
            int len = b[p] & 0xFF;
            if ((len & 0x80) != 0) {
                len = ((len & 0x7F) << 8) | (b[p + 1] & 0xFF);
                p += 2;
            } else {
                p += 1;
            }
            return new String(b, p, len, "UTF-8");
        }
        int len = u16(b, p);
        p += 2;
        if ((len & 0x8000) != 0) {
            len = ((len & 0x7FFF) << 16) | u16(b, p);
            p += 2;
        }
        char[] c = new char[len];
        for (int i = 0; i < len; ++i) {
            c[i] = (char) u16(b, p + i * 2);
        }
        return new String(c);
    }

    private static byte[] encode(String s, boolean utf8) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        if (utf8) {
            byte[] bytes = s.getBytes("UTF-8");
            writeLen8(o, s.length());
            writeLen8(o, bytes.length);
            o.write(bytes);
            o.write(0);
        } else {
            int n = s.length();
            if (n > 0x7FFF) {
                int hi = (n >> 16) | 0x8000;
                o.write(hi & 0xFF);
                o.write((hi >> 8) & 0xFF);
                o.write(n & 0xFF);
                o.write((n >> 8) & 0xFF);
            } else {
                o.write(n & 0xFF);
                o.write((n >> 8) & 0xFF);
            }
            for (int i = 0; i < n; ++i) {
                char ch = s.charAt(i);
                o.write(ch & 0xFF);
                o.write((ch >> 8) & 0xFF);
            }
            o.write(0);
            o.write(0);
        }
        return o.toByteArray();
    }

    private static void writeLen8(ByteArrayOutputStream o, int n) {
        if (n > 0x7F) {
            o.write(0x80 | ((n >> 8) & 0x7F));
        }
        o.write(n & 0xFF);
    }

    private static int u16(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
    }

    private static int u32(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
    }

    private static void put32(byte[] b, int p, int v) {
        b[p] = (byte) v;
        b[p + 1] = (byte) (v >> 8);
        b[p + 2] = (byte) (v >> 16);
        b[p + 3] = (byte) (v >> 24);
    }
}
