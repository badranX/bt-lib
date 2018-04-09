package com.techtweaking.bluetoothcontroller;
/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 *
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;
import android.os.ParcelFileDescriptor;

import com.techtweaking.libextra.IOUtils;
import com.unity3d.player.UnityPlayer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class BtInterface {

    private static final String TAG = "PLUGIN . UNITY";

    private final String CREATE_RFcomm_Socket = "createRfcommSocket";
    private final String CREATE_INSECURE_RFcomm_Socket = "createInsecureRfcommSocket";
    private BluetoothAdapter mBluetoothAdapter;
    //    private ServerReceiver serverReceiver;
    private DeviceDiscoveryReceiver_WhileConnecting deviceDiscoveryReceiverWhileConnecting;
    private RSSI_DiscoveryReceiver rssi_DiscoveryReceiver;
    private volatile boolean isConnecting = false;

    private ConnectionTrial btConnectionForDiscovery;

    private BtInterface() {
        initBluetoothAdapter();//will be nulled only when close everything

        //IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter disconnect_Intent = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        disconnect_Intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);

        //IntentFilter filter5 = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);


        //UnityPlayer.currentActivity.registerReceiver(mReceiver, filter1);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, disconnect_Intent);
        //UnityPlayer.currentActivity.registerReceiver(mReceiver, filter5);


        // Exists only to defeat instantiation.

    }

    public static BtInterface getInstance() {
        if (instance == null) {
            instance = new BtInterface();
        }
        return instance;
    }

    BluetoothAdapter BtAdapter() {
        return mBluetoothAdapter;
    }
    private class ConnectionTrial {
        final int trialsCount;
        final int time;
        final UUID uuid;
        private boolean willStop = false;
        boolean isNeedDiscovery = false;
        final boolean isBrutalConnection;
        final boolean switchingBrutalNormal;
        final BluetoothDevice device;

        final BluetoothConnection btConnection;


        void stopConnecting() {
            this.willStop = true;
        }

        boolean isWillStop() {
            return this.willStop;
        }

        public ConnectionTrial(BluetoothConnection btConnection, int trialsCount, int time, boolean isNormalConnection, boolean switchingBrutalNormal) {
            this.trialsCount = trialsCount;
            this.time = time;
            this.uuid = UUID.fromString(btConnection.getUUID());
            this.btConnection = btConnection;
            this.device = btConnection.getDevice();
            this.isBrutalConnection = !isNormalConnection;
            this.switchingBrutalNormal = switchingBrutalNormal;
        }
    }

    private final Queue<ConnectionTrial> btConnectionsQueue = new LinkedList<ConnectionTrial>();
    private final SparseArray<Queue<ConnectionTrial>> sparseTrials = new SparseArray<Queue<ConnectionTrial>>();

    private final Object ConnectThreadLock = new Object();

    private static BtInterface instance = null;





    //Advice by someone that it the method should be private (make no sense)
    //Advice reinabling Bluetooth Fixes some issues.
    //initialize the mBluetoothAdapter only
    private  void initBluetoothAdapter() {
        if (mBluetoothAdapter != null) {
            return; // already initialized
        }


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "This Android does not support Bluetooth");
        }
    }


    boolean cancelDiscovery() {
        if (mBluetoothAdapter != null) {
            try {
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
            } catch (Exception e) {
                //do nothing
            }
        }
        return true;//TODO no need for this [Should be change from Unity first as cancelDiscovery in Unity return bolean]
    }





    public void OnDestroy() {
        try {
            if (mReceiver != null)
                UnityPlayer.currentActivity.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            //Ignore
        }
        try {
            if (deviceDiscoveryReceiverWhileConnecting != null)//autmatic discovery to find unpaired devices
                UnityPlayer.currentActivity.unregisterReceiver(deviceDiscoveryReceiverWhileConnecting);
        } catch (IllegalArgumentException e) {
            //Ignore
        }
        try {
            if (rssi_DiscoveryReceiver != null)//rssi_discovery is the main discovery
                UnityPlayer.currentActivity.unregisterReceiver(rssi_DiscoveryReceiver);
            rssi_DiscoveryReceiver = null;
        } catch (IllegalArgumentException e) {
            //Ignore
        }

        /*
        if(serverReceiver != null) {
            try {
                UnityPlayer.currentActivity.unregisterReceiver(serverReceiver);
            } catch (IllegalArgumentException e) {
                //Ignore
            }
        }
        */

    }





    public void normal_connect(BluetoothConnection btConnection, boolean isChinese, boolean isSecure) {

        boolean deviceIsAvailable = false;
        boolean socketIsAvailable = false;
        if (btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingSocket) {

            //Trying to connect a ready Socket ::: SERVER :::
            socketIsAvailable = true;

        } else if (btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference) {
            deviceIsAvailable = true;
            //Trying to connect a DEVICE ::: NOT SERVER :::
        } else {
            deviceIsAvailable = findBluetoothDevice(btConnection);
            //Trying to connect a NAME OR MAC ::: NOT SERVER :::
        }

        if (socketIsAvailable) {
            //Connected should be broadcasts before initializing streams
            btConnection.RaiseCONNECTED();
            btConnection.initializeStreams();
        } else if (deviceIsAvailable) {
            UUID uuid = UUID.fromString(btConnection.getUUID());

            NormalConnectThread NCT = new NormalConnectThread(btConnection, uuid, isChinese, isSecure);
            NCT.start();
        } else {
            btConnection.RaiseConnectionError("couldn't find this remote device");
            btConnection.RaiseNOT_FOUND();
        }

    }

    public void connect(BluetoothConnection btConnection, int trialsCount, int time,
                        boolean allowPageScan,
                        boolean startNormalConnection,
                        boolean switchingBrutalNormal) {


        boolean deviceIsAvailable = false;
        boolean socketIsAvailable = false;
        if (btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingSocket) {

            //Trying to connect a ready Socket ::: SERVER :::
            socketIsAvailable = true;

        } else if (btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference) {
            deviceIsAvailable = true;
            //Trying to connect a DEVICE ::: NOT SERVER :::
        } else {
            deviceIsAvailable = findBluetoothDevice(btConnection);
            //Trying to connect a NAME OR MAC ::: NOT SERVER :::
        }

        if (socketIsAvailable) {
            //Connected should be broadcasts before initializing streams
            btConnection.RaiseCONNECTED();
            btConnection.initializeStreams();
        } else if (deviceIsAvailable) {
            //Found Device
            ConnectionTrial trial = new ConnectionTrial(btConnection, trialsCount, time, startNormalConnection, switchingBrutalNormal);//false for isBrutalConnection
            trial.isNeedDiscovery = false;

            addConnectionTrial(trial);

        } else if (allowPageScan) {
            //Device Not found and will try to Query Devices
            ConnectionTrial trial = new ConnectionTrial(btConnection, trialsCount, time, startNormalConnection, switchingBrutalNormal);//false for isBrutalConnection
            trial.isNeedDiscovery = true;
            addConnectionTrial(trial);
        } else {
            btConnection.RaiseConnectionError("couldn't find this remote device as a paired device.");
            btConnection.RaiseNOT_FOUND();
        }
    }

    private void addConnectionTrial(ConnectionTrial connectionTrial) {
        synchronized (ConnectThreadLock) {
            //there's no test for dublication//added twice means doing it twice

            btConnectionsQueue.add(connectionTrial);
            int id = connectionTrial.btConnection.getID();

            Queue<ConnectionTrial> conList = sparseTrials.get(id);
            if (conList == null) {
                conList = new LinkedList<ConnectionTrial>();
                conList.add(connectionTrial);
                sparseTrials.put(id, conList);
            } else {
                conList.add(connectionTrial);
            }
            if (!isConnecting) {
                isConnecting = true;
                (new Thread(new ConnectThread())).start();
                //Started connection thread  and will query device
            }
        }
    }



    private boolean findBluetoothDevice(BluetoothConnection setupData) {
        boolean foundModule = false;

        Set<BluetoothDevice> setPairedDevices;
        setPairedDevices = mBluetoothAdapter.getBondedDevices();

        boolean useMac = setupData.connectionMode == BluetoothConnection.ConnectionMode.UsingMac;
        for (BluetoothDevice pairedDevice : setPairedDevices) {
            if (useMac)
                foundModule = pairedDevice.getAddress().equalsIgnoreCase(setupData.mac);
            else
                foundModule = pairedDevice.getName().equals(setupData.name);

            if (foundModule) {
                setupData.connectionMode = BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference;
                setupData.setDevice(pairedDevice);
                break;
            }
        }

        /*
        if(!foundModule && CachedDeviceAdresses != null){
            for (BluetoothDevice pairedDevice : CachedDeviceAdresses) {
                if (useMac)
                    foundModule = pairedDevice.getAddress().equals(setupData.mac);
                else
                    foundModule = pairedDevice.getName().equals(setupData.name);

                if (foundModule) {
                    setupData.connectionMode = BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference;
                    setupData.setDevice(pairedDevice);
                    break;
                }
            }
        }
        */

        setPairedDevices = null;

        return foundModule;

    }

    /*
    private class ServerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
                Log.v("Unity","SCAN MODE");
                int scan_mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                if(scan_mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                       // BtInterface.getInstance().startServer();//it will start if was asked
                }
                else if(scan_mode == BluetoothAdapter.SCAN_MODE_NONE || scan_mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE){
                        BtInterface.getInstance().abortServer();
                        if(serverReceiver != null) {
                            try {
                                UnityPlayer.currentActivity.unregisterReceiver(serverReceiver);
                            } catch (IllegalArgumentException e) {
                                //ignore
                            }
                        }
                }
            }
        }
    }
*/
    public class DeviceDiscoveryReceiver_WhileConnecting extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {


                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;


                //The next is for connecting to unpaired device, that has the instance btConnectionForDiscovery
                if (btConnectionForDiscovery != null) {
                    BluetoothConnection setupData = btConnectionForDiscovery.btConnection;
                    boolean foundIt = false;
                    switch (setupData.connectionMode) {
                        case UsingMac:
                            if (setupData.mac != null &&
                                    setupData.mac.equals(device.getAddress())) {
                                foundIt = true;
                            }
                            break;
                        case UsingName:
                            if (setupData.name != null &&
                                    setupData.name.equals(device.getName())) {
                                foundIt = true;
                            }
                            break;

                    }

                    if (foundIt) {

                        mBluetoothAdapter.cancelDiscovery();

                        setupData.setDevice(device);
                        setupData.connectionMode = BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference;
                        synchronized (ConnectThreadLock) {
                            btConnectionForDiscovery.isNeedDiscovery = false;
                            btConnectionForDiscovery = null;
                            (new Thread(new ConnectThread())).start();
                        }
                        //Start the thread again
                        if (deviceDiscoveryReceiverWhileConnecting != null) {
                            try {
                                UnityPlayer.currentActivity.unregisterReceiver(deviceDiscoveryReceiverWhileConnecting);
                            } catch (IllegalArgumentException e) {
                                //ignore
                            }
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                synchronized (ConnectThreadLock) {
                    //finished discovery so we need to check if the connection thread needs to continue
                    //No need for synchronization as while this reciever is registered and discovering the thread is off
                    if (btConnectionForDiscovery != null) {
                        btConnectionForDiscovery.btConnection.RaiseConnectionError("couldn't find this remote device after scanning nearby devices.");
                        btConnectionForDiscovery.btConnection.RaiseNOT_FOUND();


                        synchronized (ConnectThreadLock) {
                            btConnectionsQueue.poll();//so it doesn't try to connect to it when it starts again
                        }

                        btConnectionForDiscovery = null;

                        if (btConnectionsQueue.size() > 0) {
                            (new Thread(new ConnectThread())).start();
                        } else {
                            isConnecting = false;
                        }
                    }
                }
                if (deviceDiscoveryReceiverWhileConnecting != null) {
                    try {
                        UnityPlayer.currentActivity.unregisterReceiver(deviceDiscoveryReceiverWhileConnecting);
                    } catch (IllegalArgumentException e) {
                        //ignore
                    }
                }
            }
        }
    }


    public class RSSI_DiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

//                BluetoothConnection bt = BluetoothConnection.getInstFromDevice(device);
//                if(bt != null){
//                   bt.setRSSI(rssi);
//                }
//                bt = null;// no need for it.


                String name = device.getName();
                name = name == null ? "" : name;//Sometimes name isn't available
                PluginToUnity.ControlMessages.DISCOVERED_DEVICE.send(name, device.getAddress(), Short.toString(rssi));

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                PluginToUnity.ControlMessages.ACTION_DISCOVERY_FINISHED.send();
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            //NO need for checking for connection
//            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
//
//                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    BluetoothConnection tmpBt;
//
//                    Log.v(TAG, "device Cnnected ::::::: " + device.getName());
//
//                    if ((tmpBt = BluetoothConnection.getInstFromDevice(device)) != null) {
//                        synchronized (ConnectThreadLock) {
//                            if(!tmpBt.isConnected && sparseTrials.get(tmpBt.getID()) == null){
//                                tmpBt.RaiseCONNECTED();
//                            }
//                        }
//
//                    }
//
//            }
//
//        else

            //One connection after the other. Every failed one will raise this action on the remote device
            if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                Log.v(TAG, "ACTION_ACL_DISCONNECT_REQUESTED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Set<BluetoothConnection> tmpBt;


                if ((tmpBt = BluetoothConnection.getInstFromDevice(device)) != null) {

                    synchronized (ConnectThreadLock) {
                        for (BluetoothConnection Bt : tmpBt) {
                            if (Bt.isConnected && sparseTrials.get(Bt.getID()) == null) {
                                Bt.close();
                            }
                        }

                    }

                }


            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.v(TAG, "ACTION_ACL_DISCONNECT_REQUESTED");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Set<BluetoothConnection> tmpBt;

                //sparseTrials.get(tmpBt.getID()) == null//means that device isn't trying to connect, there's no attempts
                if ((tmpBt = BluetoothConnection.getInstFromDevice(device)) != null) {

                    synchronized (ConnectThreadLock) {
                        for (BluetoothConnection Bt : tmpBt) {
                            if (Bt.isConnected && sparseTrials.get(Bt.getID()) == null) {
                                Bt.close();
                            }
                        }
                    }
                }

            }
        }
    };


    void OnDeviceClosing(BluetoothConnection con) {

        synchronized (ConnectThreadLock) {
            Queue<ConnectionTrial> list = sparseTrials.get(con.getID());
            if (list != null) {
                for (ConnectionTrial c : list) {
                    c.stopConnecting();
                }
                sparseTrials.remove(con.getID());
            }

            //Close Discovery for a device that has been closed
            if (mBluetoothAdapter.isDiscovering() && this.btConnectionForDiscovery != null && con.equals(this.btConnectionForDiscovery.btConnection)) {
                //finished discovery so we need to check if the connection thread needs to continue
                boolean isNeedToContinueConection = false;
                synchronized (ConnectThreadLock) {
                    ConnectionTrial tmpCon = btConnectionsQueue.peek();
                    if (con.equals(tmpCon.btConnection)) {
                        btConnectionsQueue.poll();
                    }
                    isNeedToContinueConection = btConnectionsQueue.size() > 0;
                }

                if (isNeedToContinueConection) {
                    btConnectionForDiscovery = null;
                    new ConnectThread().start();
                } else {
                    btConnectionForDiscovery = null;
                    isConnecting = false;
                }

                try {
                    if (deviceDiscoveryReceiverWhileConnecting != null)
                        UnityPlayer.currentActivity.unregisterReceiver(deviceDiscoveryReceiverWhileConnecting);
                } catch (IllegalArgumentException e) {
                    //Ignore
                }
                mBluetoothAdapter.cancelDiscovery();
            } else if (this.btConnectionForDiscovery != null && con.equals(this.btConnectionForDiscovery.btConnection)) {
                this.btConnectionForDiscovery = null;
            }
        }
    }

    private boolean startDiscoveryForConnection(ConnectionTrial btConnectionTrial) {

        //TODO startDiscovery multiple times during discovering won't affect discovery at all.
        //TODO NOT : startDiscovery while discovering simply do nothing at all.
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        this.btConnectionForDiscovery = btConnectionTrial;

        if (deviceDiscoveryReceiverWhileConnecting == null)
            deviceDiscoveryReceiverWhileConnecting = new DeviceDiscoveryReceiver_WhileConnecting();

        IntentFilter tmp_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        tmp_filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        UnityPlayer.currentActivity.registerReceiver(deviceDiscoveryReceiverWhileConnecting, tmp_filter);


        return mBluetoothAdapter.startDiscovery();
    }

    boolean startDiscovery() {


        if (rssi_DiscoveryReceiver == null) {
            rssi_DiscoveryReceiver = new RSSI_DiscoveryReceiver();

            IntentFilter tmp_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            tmp_filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            UnityPlayer.currentActivity.registerReceiver(rssi_DiscoveryReceiver, tmp_filter);
        }

        return mBluetoothAdapter.startDiscovery();
    }

    boolean refreshDiscovery() {

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        return this.startDiscovery();

    }



    void releaseDiscoveryResources() {
        try {
            if (rssi_DiscoveryReceiver != null) {

                UnityPlayer.currentActivity.unregisterReceiver(rssi_DiscoveryReceiver);
                rssi_DiscoveryReceiver = null;
            }
        } catch (IllegalArgumentException e) {
            //Ignore
        }
        mBluetoothAdapter.cancelDiscovery();
    }

    private class NormalConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothConnection btConnection;

        public NormalConnectThread(BluetoothConnection btConnection, UUID MY_UUID, boolean isChinese, boolean isSecure) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            BluetoothDevice device = btConnection.getDevice();

            try {
                if (isChinese) {
                    Method m;
                    try {
                        if (isSecure) {
                            m = device.getClass().getMethod(CREATE_RFcomm_Socket, new Class[]{int.class});
                        } else {
                            m = device.getClass().getMethod(CREATE_INSECURE_RFcomm_Socket, new Class[]{int.class});
                        }
                        tmp = (BluetoothSocket) m.invoke(device, 1);
                    }catch (IllegalAccessException e) {
                        Log.e(TAG,"failed creating socket by reflections. Method is inaccessible",e);
                    }catch (IllegalArgumentException e) {
                        Log.e(TAG,"failed creating socket by reflections. Method Arguments are wrong",e);
                    }catch (Exception e) {
                        Log.e(TAG, "problem creating socket, with normal_connect (hacked connection method)",e);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1 && !isSecure) {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                } else {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                }

            } catch (IOException e) {
                Log.e(TAG, "problem creating socket with normal_connect createRfcommSocketToServiceRecord",e);
            }
            this.mmSocket = tmp;
            this.btConnection = btConnection;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "problem while connecting (by normal_connect).",e);
                // Unable to connect; close the socket and
                IOUtils.close_socket(mmSocket);
                return;
            }

            // Do work to manage the connection (in a separate thread)
            btConnection.setSocket(mmSocket);
            btConnection.RaiseCONNECTED();
            btConnection.initializeStreams();
        }



    }

    private class ConnectThread extends Thread {
        //int reflexCounter = 1;
        private BluetoothSocket createSocket(boolean isChineseMobile, ConnectionTrial conAttempt) {//this method returns TRUE if Socket != null
            //Found Device and trying to create socket
            BluetoothSocket tmpSocket = null;
            BluetoothDevice device = conAttempt.device;
            if (device == null) return null;
            try {

                if (isChineseMobile) {

                    Method m;
                    try {
                        m = device.getClass().getMethod(CREATE_INSECURE_RFcomm_Socket, new Class[]{int.class});

                        for(int i=1; i<4;i++) {
                            tmpSocket = (BluetoothSocket) m.invoke(device, 1);
                            if(tmpSocket == null) Log.v(TAG,"Hacked connection failed with parameter : " + i);
                            //if nothing works it will be counted as a failed attempt with null socket.
                        }

                        //reflexCounter = reflexCounter > 2 ? 1 : reflexCounter + 1;
                    } catch (IllegalAccessException e) {
                        Log.w(TAG,"failed creating socket by reflections. Method is inaccessible",e);

                    }
                    catch (IllegalArgumentException e) {
                        Log.w(TAG,"failed creating socket by reflections. Method Arguments are wrong",e);
                    }
                    catch (Exception e){
                        Log.w(TAG,"failed creating socket by reflections",e);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {

                    tmpSocket = device.createInsecureRfcommSocketToServiceRecord(conAttempt.uuid);

                } else
                    tmpSocket = device.createRfcommSocketToServiceRecord(conAttempt.uuid);//for API 9
            } catch (IOException e) {
                if(isChineseMobile) {
                    Log.w(TAG, "Couldn't create socket. Connection method is chinese (hacked connection by reflections)",e);

                }else {
                    Log.w(TAG, "Couldn't create socket. Connection method is normal RfcommSocket",e);
                }
            }

            return tmpSocket;

        }


        public void run() {
            int counter;
            while (true) {
                ConnectionTrial conAttempt;

                synchronized (ConnectThreadLock) {
                    if (btConnectionsQueue.size() <= 0) {
                        isConnecting = false;
                        break;//thread must end
                    }
                    //peek to element and poll it when it's not needed for discovery.
                    //Every ConAttempt must have a device reference or it already asked for discovery to find a reference
                    conAttempt = btConnectionsQueue.peek();

                    //check if close has been called || it's already connected
                    if (conAttempt.isWillStop() || conAttempt.btConnection.isConnected) {
                        btConnectionsQueue.poll();
                        continue;
                    }

                    if (conAttempt.isNeedDiscovery) { //if device is not found yet, need to start discovery
                        //TODO (FIXED BUT NEED TESTING) bug when device isn't found, it act as if it find it
                        if (startDiscoveryForConnection(conAttempt)) {
                            break;//thread must end
                        } else {
                            conAttempt.btConnection.RaiseConnectionError("failed to start discovery for the unpaired device");
                            conAttempt.btConnection.RaiseMODULE_OFF();
                            btConnectionsQueue.poll();
                            continue;
                        }
                    }
                    conAttempt = btConnectionsQueue.poll();

                }


                boolean isChineseMobile = conAttempt.isBrutalConnection;
                BluetoothSocket socket = null;
                counter = 0;
                boolean success = true;

                do {

                    socket = createSocket(isChineseMobile, conAttempt);

                    if (socket != null) {
                        mBluetoothAdapter.cancelDiscovery();
                        try {
                            //TODO : commented Area ...  Research it again

                            /*
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                                if (!socket.isConnected()) {//Sometimes it's connected before calling connect(), because of an error in previous-attempt
                                    socket.connect();
                                } else {
                                    success = false;
                                    Log.w(TAG, "socket.isConnected is True from previous failed connection attempt.");

                                }
                            } else {

                                socket.connect();
                            }
                            */


                                socket.connect();

                        } catch (IOException e) {
                            //The reason for this exception : UUID COULD BE DIFFERENT .MODULE_UUID_WRONG
                            Log.w(TAG, "connection attempt error.",e);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                                success = socket.isConnected();
                                Log.v(TAG, "connection attempt error, but socket.isConnected = " + success,e);
                            }else {
                                success = false;
                            }


                        } catch (NullPointerException e) {
                            Log.e(TAG,"Unknown behaviour", e);
                            /*
                                Reported by Mr. Thomas : Caused by java.lang.NullPointerException: FileDescriptor must not be null
                                at android.os.ParcelFileDescriptor.<init>(ParcelFileDescriptor.java:174)
                                at android.os.ParcelFileDescriptor$1.createFromParcel(ParcelFileDescriptor.java:905)
                             */
                            success = false;
                            break;
                        }


                        synchronized (ConnectThreadLock) {
                            if (conAttempt.isWillStop()) {
                                    IOUtils.close_socket(socket);
                                //break. the user has closed it.
                                //Then the Queue will be emptied from Attempts to connect
                                break;
                            }
                            if (success) {
                                conAttempt.btConnection.setSocket(socket);
                            }
                        }

                        if (success) {
                            conAttempt.btConnection.RaiseCONNECTED();
                            conAttempt.btConnection.initializeStreams();
                            break; //success no need for trials
                        }
                    }

                    //success will be always false in the following line
                    if (socket != null && !success) {
                            IOUtils.close_socket(socket);
                    }

                    counter++;
                    if (conAttempt.switchingBrutalNormal) {
                        isChineseMobile = !isChineseMobile;
                    }

                    if (counter >= conAttempt.trialsCount) {
                        break;
                    }

                    try {
                        Thread.sleep(conAttempt.time);
                    } catch (InterruptedException e) {
                        Log.v(TAG, "Sleep Thread Interrupt Exception");
                    }


                } while (true);

                synchronized (ConnectThreadLock) {

                    if (success || conAttempt.isWillStop()) {//if success or user wants closing we don't need other elements in the Queue
                        Queue<ConnectionTrial> list = sparseTrials.get(conAttempt.btConnection.getID());
                        if (list != null) {
                            btConnectionsQueue.removeAll(list);
                            sparseTrials.remove(conAttempt.btConnection.getID());
                        }
                    } else {
                        Queue<ConnectionTrial> list = sparseTrials.get(conAttempt.btConnection.getID());
                        if (list != null) list.poll();
                    }
                }
                if (!success) {
                    conAttempt.btConnection.RaiseConnectionError("failed to connect." +
                            " this remote device might be off," +
                            " or you provided a wrong UUID");

                    conAttempt.btConnection.RaiseMODULE_OFF();
                }
            }
            //end of the biggest while
        }


    }

    //Accepting Thread

    private AcceptThread acceptThread;
    //private final Object acceptThreadLock = new Object();


    //First method called to initialize the server. Called by Unity
    //if (oneDevice) it will abort after finding the first device
    public void initServer(String serverUUID, int time, boolean willConnectOneDevice) {

       if (acceptThread != null)
           acceptThread.abortServer();

        //TODO: Research using one thread
        acceptThread = new AcceptThread(UUID.fromString(serverUUID), time, willConnectOneDevice);

        //IntentFilter filter_scan_mode = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        // if(serverReceiver == null) serverReceiver = new ServerReceiver();
        // UnityPlayer.currentActivity.registerReceiver(serverReceiver, filter_scan_mode);

        ForwardingActivity.makeDiscoverable(time);
        //makeDiscoverable(time);

    }

    void makeDiscoverable(int time) {
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, time);
        UnityPlayer.currentActivity.startActivity(discoverableIntent);
    }

    void startServer() {//Shouldn't be called while server running, abort first. [This is called by ForwardActivity]

            if (acceptThread != null) {
                (new Thread(acceptThread)).start();
            }

    }

     void abortServer() {//called by unity, cause imediate closing
            if (acceptThread != null) {
                acceptThread.abortServer();
                acceptThread = null;
            }

    }

    private class AcceptThread implements Runnable {
        private volatile boolean willStop = false;
        private BluetoothServerSocket mmServerSocket;


        private final boolean willConnectOneDevice;
        private final UUID serverUUID;
        private final int discoverable_Time_Duration;

        public AcceptThread(UUID serverUUID, int discoverable_Time_Duration, boolean willConnectOneDevice) {
            this.serverUUID = serverUUID;
            this.willConnectOneDevice = willConnectOneDevice;
            this.discoverable_Time_Duration = discoverable_Time_Duration;
            this.mmServerSocket = createServerSocket();
        }



        public void stopServer_from_looping() {//won't stop immedeatly
            this.willStop = true;
        }

        public void abortServer() {//will stop immidiately


            this.willStop = true;
            cancel();
            /*
            if(serverReceiver != null) {
                try {
                    UnityPlayer.currentActivity.unregisterReceiver(serverReceiver);
                } catch (IllegalArgumentException e) {
                    //ignore
                }
            }
            */

            PluginToUnity.ControlMessages.SERVER_FINISHED_LISTENING.send();
        }


        private BluetoothServerSocket createServerSocket() {
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("TechTweakingServer", serverUUID);
                } else {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("TechTweakingServer", serverUUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to create a BluetoothServerSocket", e);
            }
            return tmp;

        }

        public void run() {


            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    if (mmServerSocket != null)
                    {
                        //Warning mmServerSocket.close is called from outside this thread. Don't null it.
                        socket = mmServerSocket.accept(this.discoverable_Time_Duration * 1000);
                    }
                } catch (IOException e) {



                    if(this.willStop) {//has been aborted by user
                        cancel();
                        break;
                    }
                    //TODO (MAYBE) ADDING TOLLERANCE FOR THE TIME AVAILABLE FOR SERVER
                    /*
                    if (mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                            && !this.willStop && counter <3) {
                        this.mmServerSocket = createServerSocket();

                        continue;
                    } else {
                        cancel();
                        break;
                    }
                    */

                }

                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    if (willConnectOneDevice) {
                        cancel();
                        break;
                    }
                }

                Log.v(TAG,"SCAN_MODE_CONNECTABLE_DISCOVERABLE\n" +
                        "                                || this.willStop ?");

                if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                                || this.willStop)
                {
                    Log.v(TAG,"yes");
                    cancel();
                    break;

                }
                socket = null;
            }

            PluginToUnity.ControlMessages.SERVER_FINISHED_LISTENING.send("1");

/*
            synchronized (acceptThreadLock) {
                /*
                if(serverReceiver != null) {
                    try {
                        UnityPlayer.currentActivity.unregisterReceiver(serverReceiver);
                    } catch (IllegalArgumentException e) {
                        //ignore
                    }
                }

                acceptThread = null;
            }
*/

        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            if (mmServerSocket != null) {
                IOUtils.close_serverSocket(mmServerSocket);
            }
        }

        private void manageConnectedSocket(BluetoothSocket socket) {
            PluginToUnity.socket = socket;//Saving it to pick it by unity
            PluginToUnity.ControlMessages.SERVER_DISCOVERED_DEVICE.send();
        }
    }
}
