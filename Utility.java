package com.namcobandaigames.dragonballtap.apk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class Utility {

    private static GlobalWork gw = null;
    private static Vibrator vibrator = null;

    public static void SetGlobalWork(GlobalWork _gw) {
        gw = _gw;
    }

    public static GlobalWork GetGlobalWork() {
        return gw;
    }

    public static void startBrowser(Context context, String url) {
        Activity a = (Activity) context;
        a.startActivityForResult(new Intent("android.intent.action.VIEW", Uri.parse(url)), 0);
    }

    public static boolean deleteFile(Context context, String name) {
//        Log.e("delete", name);
        if (Environment.getExternalStorageState().equals("mounted")) {
            try {
                context.deleteFile(Environment.getExternalStorageDirectory() + "/" + name);
            } catch (final Exception e) {
//                Log.e("Utility", "67:" + e);
            }
        }
        try {
            return context.deleteFile(name);
        } catch (final Exception e2) {
            Log.e("Utility", "77:" + e2);
            return false;
        }
    }

    public static boolean checkFile(Context context, String name) {
        try {
            context.getAssets().open(name).close();
            return true;
        } catch (IOException iOException) {
        }
        return false;
    }

    public static Context activ;

    public static boolean writeDataFile(Context context, String name, byte[] w, boolean bSDcard) {
//        Log.e("write ls", name);
        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(activ.getFilesDir(), name));
            out.write(w, 0, w.length);
            out.flush();
            return true;
        } catch (Exception exception) {

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException iOException) {
                }
            }
        }
        return false;
    }

    public static boolean writeDataFile(String name, byte[] w, int dataOffset, int dataLength, int outOffset, boolean bSDcard) {
//        Log.e("write lf", name);

        if (dataOffset + dataLength > w.length) {
            dataLength = w.length - dataOffset;
        }
        if (dataLength <= 0) {
            return false;
        }
        RandomAccessFile out = null;
        try {
            out = new RandomAccessFile(new File(activ.getFilesDir(), name), "rw");
            out.seek(outOffset);
            out.write(w, dataOffset, dataLength);
            return true;
        } catch (Exception exception) {

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException iOException) {
                }
            }
        }
        return false;
    }

    public static String InttoString(int len, int val) {
        final String b = "0000000000000000" + val;
        return b.substring(b.length() - len, b.length());
    }
    private static final ByteArrayOutputStream bo = new ByteArrayOutputStream(5120);
    private static final byte[] arr = new byte[1024];
    private static int ac;

    public static final byte[] getBytes(InputStream in) throws IOException {
        bo.reset();
        while ((ac = in.read(arr)) != -1) {
            bo.write(arr, 0, ac);
        }
        in.close();
        return bo.toByteArray();
    }

    public static byte[] readDataFile(Context context, String name) {
//
//        try {
//            Log.e("Era 3", name);
//            throw new RuntimeException("MORADO");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
        if (name.equals("save.bin")) {
            try {
                if (!new File(activ.getFilesDir(), name).exists()) {
                    Utility.writeDataFile(context, name, getBytes(context.getAssets().open(name)), true);
                }
                return getBytes(new FileInputStream(new File(activ.getFilesDir(), name)));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                return getBytes(context.getAssets().open(name));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static byte[] readDataFile(Context context, String name, int offset, int size) {
//        try {
//            Log.e("Era 2", name);
//            throw new RuntimeException("MORADO");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
        byte[] zx = null;
        try {
            zx = new byte[size];
            final InputStream in = context.getAssets().open(name);
            in.skip(offset);
            in.read(zx, 0, size);
            in.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return zx;
    }

    public static byte[] readDataRaw(Context context, String name) throws Exception {
        return readDataFile(context, name);
    }

    public static void SetVibrator(Context context, int msec) {
        if (msec > 0) {
            if (vibrator == null) {
                vibrator = (Vibrator) ((Activity) context).getSystemService("vibrator");
            }
            vibrator.vibrate((long) msec);
        }
    }

    public static byte[] getPackBinary(byte[] data, int no) {
        if (data == null) {
            return null;
        }
        int iPos = (no << 3) + 2;
        int dpos = (((data[iPos] & 255) + ((data[iPos + 1] & 255) << 8)) + ((data[iPos + 2] & 255) << 16)) + ((data[iPos + 3] & 255) << 24);
        int dsize = (((data[iPos + 4] & 255) + ((data[iPos + 5] & 255) << 8)) + ((data[iPos + 6] & 255) << 16)) + ((data[iPos + 7] & 255) << 24);
        byte[] buf = new byte[dsize];
        System.arraycopy(data, dpos + ((((data[0] & 255) + ((data[1] & 255) << 8)) << 3) + 2), buf, 0, dsize);
        return buf;
    }

    public static int getPackCount(byte[] data, int offset) {
        int iPos = offset;
        return (data[iPos] & 255) + ((data[iPos + 1] & 255) << 8);
    }

    public static int getPackPos(byte[] data, int no, int offset) {
        int iPos = offset;
        int dcnt = (data[iPos] & 255) + ((data[iPos + 1] & 255) << 8);
        iPos += (no << 3) + 2;
        return ((((data[iPos] & 255) + ((data[iPos + 1] & 255) << 8)) + ((data[iPos + 2] & 255) << 16)) + ((data[iPos + 3] & 255) << 24)) + (((dcnt << 3) + 2) + offset);
    }

    public static int getPackSize(byte[] data, int no, int offset) {
        int iPos = (offset + 2) + (no << 3);
        return (((data[iPos + 4] & 255) + ((data[iPos + 5] & 255) << 8)) + ((data[iPos + 6] & 255) << 16)) + ((data[iPos + 7] & 255) << 24);
    }

    public static byte[] http2data(String path) throws Exception {
        return new byte[]{'1'};
    }

    public static void saveSharedPreferences(Context context, String pref, String key, String value) throws Exception {
        try {
            Editor editor = ((Activity) context).getSharedPreferences(pref, 0).edit();
            editor.putString(key, value);
            editor.commit();
        } catch (Exception e) {
            Exception e2 = e;
            Log.e("Utility", "484:" + e2);
            throw e2;
        }
    }

    public static String loadSharedPreferences(Context context, String pref, String key) throws Exception {
        try {
            return ((Activity) context).getSharedPreferences(pref, 0).getString(key, "");
        } catch (Exception e) {
            Exception e2 = e;
            Log.e("Utility", "500:" + e2);
            throw e2;
        }
    }
}
