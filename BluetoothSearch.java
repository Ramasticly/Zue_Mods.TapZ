package com.namcobandaigames.dragonballtap.apk;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothSearch extends Activity implements OnClickListener {

    private static final int REQUEST_DISCOVERABLE = 2;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothAdapter BtAdapter;
    private final BroadcastReceiver DevieFoundReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean bCheck;
            BluetoothDevice foundDevice;
            int i;
            String action = intent.getAction();
            if ("android.bluetooth.adapter.action.DISCOVERY_STARTED".equals(action)) {
                BluetoothSearch.this.bDeviceSearch = true;
            }
            if ("android.bluetooth.device.action.FOUND".equals(action)) {
                bCheck = false;
                foundDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (foundDevice.getName() != null) {
                    for (i = 0; i < BluetoothSearch.this.aDeviceList.size(); i++) {
                        if (((BluetoothDevice) BluetoothSearch.this.aDeviceList.get(i)).getAddress().equals(foundDevice.getAddress())) {
                            bCheck = true;
                            break;
                        }
                    }
                    if (!bCheck) {
                        BluetoothSearch.this.aDeviceList.add(foundDevice);
                    }
                }
            }
            if ("android.bluetooth.device.action.NAME_CHANGED".equals(action)) {
                foundDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                bCheck = false;
                for (i = 0; i < BluetoothSearch.this.aDeviceList.size(); i++) {
                    if (((BluetoothDevice) BluetoothSearch.this.aDeviceList.get(i)).getAddress().equals(foundDevice.getAddress())) {
                        bCheck = true;
                        break;
                    }
                }
                if (!bCheck) {
                    BluetoothSearch.this.aDeviceList.add(foundDevice);
                }
            }
            if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(action)) {
                BluetoothSearch.this.searchHandler.sendMessage(new Message());
            }
        }
    };
    private ArrayList aDeviceList;
    private Builder ab;
    private boolean bClientwaiot = false;
    private boolean bDeviceSearch = false;
    private boolean bRegisterReceiver = false;
    private boolean bSearchEnd = false;
    Intent btOn = null;
    private Button cancelBtn = null;
    private BluetoothClientThread client = null;
    Intent discoverableOn = null;
    BluetoothDevice foundDevice = null;
    private ProgressDialog loadingDialog = null;
    private Context mContext = null;
    private ArrayAdapter nonPairedDeviceAdapter;
    private Object objLock = new Object();
    private Button retryBtn = null;
    private final Handler searchHandler = new Handler() {
        public void handleMessage(Message msg) {
            for (int i = 0; i < BluetoothSearch.this.aDeviceList.size(); i++) {
                BluetoothSearch.this.nonPairedDeviceAdapter.add(((BluetoothDevice) BluetoothSearch.this.aDeviceList.get(i)).getName());
            }
            if (BluetoothSearch.this.bRegisterReceiver) {
                try {
                    BluetoothSearch.this.unregisterReceiver(BluetoothSearch.this.DevieFoundReceiver);
                } catch (Exception e) {
                }
                BluetoothSearch.this.bRegisterReceiver = false;
            }
            BluetoothSearch.this.loadingDialog.dismiss();
            BluetoothSearch.this.cancelBtn.setEnabled(true);
            BluetoothSearch.this.retryBtn.setEnabled(true);
            ListView nonpairedList = (ListView) BluetoothSearch.this.findViewById(K.id.nonPairedDeviceList);
            nonpairedList.setOnItemClickListener(new ClickEvent());
            nonpairedList.setAdapter(BluetoothSearch.this.nonPairedDeviceAdapter);
            BluetoothSearch.this.ServerStart(BluetoothSearch.this.mContext, BluetoothSearch.this.BtAdapter);
        }
    };
    private BluetoothServerThread server = null;

    public class BluetoothClientThread extends AsyncTask {

        public final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private boolean bCanceled = false;
        private boolean bServerChange = false;
        private final BluetoothSocket clientSocket;
        private Context mContext;
        private final BluetoothDevice mDevice;
        public BluetoothAdapter myClientAdapter;

        public BluetoothClientThread(Context context, BluetoothDevice device, BluetoothAdapter btAdapter) {
            this.mContext = context;
            BluetoothSocket tmpSock = null;
            this.mDevice = device;
            this.myClientAdapter = btAdapter;
            try {
                tmpSock = device.createRfcommSocketToServiceRecord(this.TECHBOOSTER_BTSAMPLE_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.clientSocket = tmpSock;
            BluetoothSearch.this.loadingDialog = new ProgressDialog(this.mContext);
            BluetoothSearch.this.loadingDialog.setProgressStyle(0);
            BluetoothSearch.this.loadingDialog.setTitle(K.string.bt_text04);
            BluetoothSearch.this.loadingDialog.setMessage(BluetoothSearch.this.getString(K.string.bt_text05));
            BluetoothSearch.this.loadingDialog.show();
        }

        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Object doInBackground(Object[] params) {
            if (this.myClientAdapter.isDiscovering()) {
                this.myClientAdapter.cancelDiscovery();
            }
            if (isCancelled()) {
                return "cancel";
            }
            try {
                this.clientSocket.connect();
                BluetoothManajer.getInstance().setSocket(this.clientSocket, false);
                return "ok";
            } catch (IOException e) {
                IOException e2 = e;
                try {
                    this.clientSocket.close();
                } catch (IOException e3) {
                    IOException closeException = e3;
                    e2.printStackTrace();
                }
                return "err";
            }
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            BluetoothSearch.this.loadingDialog.dismiss();
            if (result.equals("ok")) {
                BluetoothSearch.this.killBluetooth(-1);
            } else if (result.equals("cancel")) {
                BluetoothSearch.this.killBluetooth(-1);
            } else {
                BluetoothSearch.this.bClientwaiot = false;
                try {
                    if (this.clientSocket != null) {
                        this.clientSocket.close();
                    }
                } catch (IOException e) {
                }
                BluetoothSearch.this.bClientwaiot = false;
                BluetoothSearch.this.ServerStart(this.mContext, BluetoothSearch.this.BtAdapter);
            }
        }

        protected void onCancelled() {
            super.onCancelled();
            BluetoothSearch.this.loadingDialog.dismiss();
            try {
                if (this.clientSocket != null) {
                    this.clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            BluetoothSearch.this.bClientwaiot = false;
            BluetoothSearch.this.ServerStart(this.mContext, BluetoothSearch.this.BtAdapter);
        }
    }

    public class BluetoothServerThread extends AsyncTask {

        public final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private boolean bCanceled = false;
        private boolean bClientChange = false;
        private Context mContext;
        public String myNumber;
        private BluetoothAdapter myServerAdapter;
        private final BluetoothServerSocket servSock;

        public void clear() {
            try {
                this.servSock.close();
            } catch (IOException e) {
            }
        }

        public BluetoothServerThread(Context context, BluetoothAdapter btAdapter) {
            this.mContext = context;
            BluetoothServerSocket tmpServSock = null;
            this.myServerAdapter = btAdapter;
            try {
                tmpServSock = this.myServerAdapter.listenUsingRfcommWithServiceRecord("DragonballTap", this.TECHBOOSTER_BTSAMPLE_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.servSock = tmpServSock;
        }

        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Object doInBackground(Object[] params) {
            String ret = "ok";
            if (isCancelled()) {
                return "cancel";
            }
            try {
                BluetoothManajer.getInstance().setSocket(this.servSock.accept(), true);
            } catch (IOException e) {
                ret = "";
            }
            try {
                this.servSock.close();
            } catch (IOException e2) {
            }
            return ret;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result.equals("ok")) {
                BluetoothSearch.this.killBluetooth(-1);
            } else if (result.equals("cancel")) {
                BluetoothSearch.this.killBluetooth();
            }
        }

        protected void onCancelled() {
            super.onCancelled();
            try {
                this.servSock.close();
            } catch (IOException e) {
            }
        }
    }

    class ClickEvent implements OnItemClickListener {

        ClickEvent() {
        }

        public void onItemClick(AdapterView adapter, View view, int position, long id) {
            BluetoothSearch.this.foundDevice = (BluetoothDevice) BluetoothSearch.this.aDeviceList.get(position);
            if (BluetoothSearch.this.foundDevice != null && !BluetoothSearch.this.bClientwaiot) {
                if (BluetoothSearch.this.server != null) {
                    try {
                        BluetoothSearch.this.server.clear();
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                    BluetoothSearch.this.server = null;
                }
                BluetoothSearch.this.bClientwaiot = true;
                BluetoothSearch.this.ClientStart(BluetoothSearch.this.mContext, BluetoothSearch.this.foundDevice, BluetoothSearch.this.BtAdapter);
            }
        }
    }

    class OkAdapter2 implements DialogInterface.OnClickListener {

        OkAdapter2() {
        }

        public void onClick(DialogInterface arg0, int arg1) {
            if (-1 == arg1) {
                BluetoothSearch.this.killBluetooth();
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int mejer_version = 0;
        int sub_version = 0;
        try {
            String[] version = VERSION.RELEASE.split("\\.");
            mejer_version = Integer.parseInt(version[0]);
            sub_version = Integer.parseInt(version[1]);
        } catch (Exception e) {
        }
        if (mejer_version > 2 || (mejer_version == 2 && sub_version >= 3)) {
            setRequestedOrientation(6);
        } else {
            setRequestedOrientation(0);
        }
        getWindow().addFlags(128);
        requestWindowFeature(1);
        setContentView(K.layout.btsearch);
        this.cancelBtn = (Button) findViewById(K.id.bt_cancel);
        this.cancelBtn.setOnClickListener(this);
        this.retryBtn = (Button) findViewById(K.id.bt_retry);
        this.retryBtn.setOnClickListener(this);
        this.BtAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = this;
        checkBluetooth();
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != 1 || event.getKeyCode() != 4) {
            return super.dispatchKeyEvent(event);
        }
        if (!this.bSearchEnd) {
            killBluetooth(255);
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 4 && keyCode != 82) {
            return super.onKeyDown(keyCode, event);
        }
        if (!this.bSearchEnd) {
            getWindow().clearFlags(128);
            killBluetooth(255);
        }
        return true;
    }

    public void onDestroy() {
        getWindow().clearFlags(128);
        if (!this.bSearchEnd) {
            releaseBluetooth();
        }
        super.onDestroy();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case K.id.bt_cancel /*2131099649*/:
                killBluetooth();
                return;
            case K.id.bt_retry /*2131099650*/:
                releaseBluetooth();
                releaseSoket();
                checkBluetooth();
        }
    }

    public void killBluetooth() {
        try {
            this.bSearchEnd = true;
            releaseBluetooth();
            setResult(255, new Intent());
            finish();
        } catch (Exception e) {
        }
    }

    public void killBluetooth(int res) {
        try {
            this.bSearchEnd = true;
            releaseBluetooth();
            setResult(res, new Intent());
            finish();
        } catch (Exception e) {
        }
    }

    public void releaseSoket() {
        if (this.server != null) {
            try {
                this.server.cancel(true);
            } catch (Exception e) {
            }
            this.server = null;
        }
        if (this.client != null) {
            try {
                this.client.cancel(true);
            } catch (Exception e2) {
            }
            this.client = null;
        }
    }

    public void releaseBluetooth() {
        try {
            if (this.BtAdapter != null) {
                if (this.BtAdapter.isDiscovering()) {
                    this.BtAdapter.cancelDiscovery();
                }
                if (this.bRegisterReceiver) {
                    try {
                        unregisterReceiver(this.DevieFoundReceiver);
                    } catch (Exception e) {
                    }
                    this.bRegisterReceiver = false;
                }
            }
        } catch (Exception e2) {
        }
        releaseSoket();
        try {
            if (this.nonPairedDeviceAdapter != null) {
                this.nonPairedDeviceAdapter.clear();
                this.nonPairedDeviceAdapter = null;
            }
        } catch (Exception e3) {
        }
        if (this.aDeviceList != null) {
            for (int i = 0; i < this.aDeviceList.size(); i++) {
                BluetoothDevice fd = (BluetoothDevice) this.aDeviceList.get(i);
            }
            this.aDeviceList.clear();
        }
    }

    public void checkBluetooth() {
        if (this.BtAdapter == null) {
            killBluetooth();
        } else if (this.BtAdapter.isEnabled()) {
            reqDiscoverable();
        } else {
            this.btOn = null;
            this.btOn = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
            startActivityForResult(this.btOn, 1);
        }
    }

    public void reqDiscoverable() {
        this.discoverableOn = null;
        this.discoverableOn = new Intent("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
        this.discoverableOn.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 300);
        startActivityForResult(this.discoverableOn, 2);
    }

    public void searchBluetooth() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.adapter.action.DISCOVERY_STARTED");
        filter.addAction("android.bluetooth.device.action.FOUND");
        filter.addAction("android.bluetooth.device.action.NAME_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        if (this.bRegisterReceiver) {
            try {
                unregisterReceiver(this.DevieFoundReceiver);
            } catch (Exception e) {
            }
        }
        registerReceiver(this.DevieFoundReceiver, filter);
        this.bRegisterReceiver = true;
        if (this.nonPairedDeviceAdapter != null) {
            this.nonPairedDeviceAdapter = null;
        }
        this.nonPairedDeviceAdapter = new ArrayAdapter(this, K.layout.rowdata);
        if (this.BtAdapter.isDiscovering()) {
            this.BtAdapter.cancelDiscovery();
        }
        if (this.aDeviceList == null) {
            this.aDeviceList = new ArrayList();
        }
        this.aDeviceList.clear();
        this.cancelBtn.setEnabled(false);
        this.retryBtn.setEnabled(false);
        this.loadingDialog = new ProgressDialog(this.mContext);
        this.loadingDialog.setProgressStyle(0);
        this.loadingDialog.setTitle(K.string.bt_text00);
        this.loadingDialog.setMessage(getString(K.string.bt_text01));
        this.loadingDialog.show();
        this.bDeviceSearch = false;
        this.BtAdapter.startDiscovery();
    }

    protected void onActivityResult(int requestCode, int ResultCode, Intent date) {
        this.btOn = null;
        this.discoverableOn = null;
        switch (requestCode) {
            case 1:
                if (ResultCode != -1) {
                    killBluetooth();
                    break;
                } else {
                    reqDiscoverable();
                    break;
                }
            case 2:
                if (ResultCode != 0) {
                    searchBluetooth();
                    break;
                } else {
                    killBluetooth();
                    break;
                }
        }
        try {
            System.gc();
            Thread.sleep(20);
        } catch (InterruptedException e) {
        }
    }

    public void ServerStart(Context context, BluetoothAdapter btAdapter) {
        this.server = null;
        this.server = new BluetoothServerThread(context, btAdapter);
        this.server.execute((Object[]) new Void[0]);
    }

    public void ClientStart(Context context, BluetoothDevice device, BluetoothAdapter btAdapter) {
        this.client = null;
        this.client = new BluetoothClientThread(context, device, btAdapter);
        this.client.execute((Object[]) new Void[0]);
    }
}
