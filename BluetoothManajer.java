package com.namcobandaigames.dragonballtap.apk;

import android.bluetooth.BluetoothSocket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothManajer extends Thread {

    private static BluetoothManajer instance = null;
    private boolean bCanceled = false;
    private boolean bConnect = true;
    private final byte[] bReadData = new byte[1024];
    private boolean bReadOK = false;
    private boolean bResdStop = false;
    private boolean bSendOK = false;
    private boolean bServer = false;
    private boolean bWrite = false;
    private byte[] bWriteBuf = null;
    private byte[] bWriteData = null;
    public BluetoothSocket btSocket = null;
    private int iBufferPos = 0;
    private int iBusyCount = 0;
    private InputStream in = null;
    private OutputStream out = null;

    public static BluetoothManajer getInstance() {
        if (instance == null) {
            instance = new BluetoothManajer();
        }
        instance.bCanceled = false;
        return instance;
    }

    public boolean checkSocket() {
        return this.btSocket != null;
    }

    public void setSocket(BluetoothSocket s, boolean server) {
        this.btSocket = s;
        this.bServer = server;
        try {
            this.in = this.btSocket.getInputStream();
            this.out = this.btSocket.getOutputStream();
        } catch (Exception e) {
        }
    }

    public boolean isServer() {
        return this.bServer;
    }

    public void cancel() {
        try {
            this.btSocket.close();
            this.btSocket = null;
        } catch (IOException e) {
        }
    }

    public static void clearInstance() {
        if (instance != null) {
            instance.bCanceled = true;
            try {
                instance.join();
            } catch (Exception e) {
            }
            if (instance.btSocket != null) {
                try {
                    instance.btSocket.close();
                } catch (Exception e2) {
                }
                instance.btSocket = null;
            }
            instance = null;
        }
    }

    public void dispose() {
        this.bCanceled = true;
        try {
            join();
        } catch (Exception e) {
        }
        if (this.btSocket != null) {
            try {
                this.btSocket.close();
            } catch (Exception e2) {
            }
            this.btSocket = null;
        }
        this.bResdStop = false;
    }

    public void setData(byte[] buf, byte[] bindata) {
        if (bindata == null) {
            setData(buf);
            return;
        }
        this.bWriteData = null;
        int size = buf.length + bindata.length;
        try {
            if (!(this.bWriteData == null || this.bWriteData.length == size)) {
                this.bWriteData = null;
            }
            if (this.bWriteData == null) {
                this.bWriteData = new byte[size];
            }
            System.arraycopy(buf, 0, this.bWriteData, 0, buf.length);
            System.arraycopy(bindata, 0, this.bWriteData, 90, bindata.length);
            this.bWrite = true;
        } catch (Exception e) {
            this.bCanceled = true;
            this.bConnect = false;
        }
    }

    public void setData(byte[] buf) {
        this.bWriteData = null;
        if (this.bWriteBuf == null) {
            this.bWriteBuf = new byte[90];
        }
        System.arraycopy(buf, 0, this.bWriteBuf, 0, buf.length);
        this.bWriteData = this.bWriteBuf;
        this.bWrite = true;
    }

    public byte[] getData() {
        if (this.bReadOK) {
            return this.bReadData;
        }
        return null;
    }

    public byte[] getSendData() {
        return this.bWriteData;
    }

    public int getReadDataSize() {
        return this.iBufferPos;
    }

    public void reset() {
        try {
            if (this.in != null && this.in.available() > 0) {
                byte[] aaa = new byte[this.in.available()];
                this.in.read(aaa, 0, aaa.length);
            }
        } catch (Exception e) {
        }
        this.bWrite = false;
        this.bSendOK = false;
        this.bReadOK = false;
        this.iBusyCount = 0;
        this.iBufferPos = 0;
    }

    public void clearData() {
        this.bReadOK = false;
        this.iBufferPos = 0;
    }

    public boolean isConnect() {
        return this.bConnect;
    }

    public void ReadStop(boolean stop) {
        this.bResdStop = stop;
    }

    public void run() {
        int __count = 0;
        boolean bOK = false;
        while (!this.bCanceled && this.in != null && this.out != null) {
            try {
                if (this.btSocket == null) {
                    if (bOK) {
                        break;
                    }
                    __count++;
                    if (__count > 600) {
                        break;
                    }
                } else {
                    bOK = true;
                    if (this.out != null && this.bWrite) {
                        this.bWrite = false;
                        this.out.write(this.bWriteData);
                        this.bSendOK = true;
                        this.bReadOK = false;
                    }
                    if (!(this.in == null || this.bResdStop || !this.bSendOK || this.bReadOK)) {
                        this.iBusyCount++;
                        if (this.in.available() > 0 || this.iBusyCount > 200) {
                            int size = this.in.read(this.bReadData, this.iBufferPos, this.bReadData.length);
                            this.iBusyCount = 0;
                            if (size > 0) {
                                this.iBufferPos += size;
                                this.bSendOK = false;
                                this.bReadOK = true;
                            }
                        }
                    }
                    try {
                        Thread.sleep(5);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e2) {
            }
        }
        if (this.btSocket != null) {
            try {
                this.btSocket.close();
            } catch (Exception e3) {
            }
            this.btSocket = null;
        }
        this.bConnect = false;
    }
}
