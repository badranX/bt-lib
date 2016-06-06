package com.badran.bluetoothcontroller;
/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 * 
 */

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;


import java.util.HashSet;
import java.util.LinkedList;
import java.io.IOException;
import java.lang.reflect.Method;


import java.util.Queue;
import java.util.Set;
import java.util.UUID;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothServerSocket;
import android.util.SparseArray;

public class BtInterface {
    private final String CREATE_RFcomm_Socket = "createRfcommSocket";
    private final String CREATE_INSECURE_RFcomm_Socket = "createInsecureRfcommSocket";
    BluetoothAdapter mBluetoothAdapter;

    private ServerReceiver serverReceiver;
    private DeviceDiscoveryReceiver deviceDiscoveryReceiver;
    private RSSI_DiscoveryReceiver rssi_DiscoveryReceiver;
    private volatile boolean isConnecting = false;

    private ConnectionTrial btConnectionForDiscovery;

    private class ConnectionTrial {
        final int trialsCount;
        final int time;
        final UUID uuid;
        private boolean willStop = false;
        boolean isNeedDiscovery = false;
        final boolean isBrutalConnection;
        final BluetoothDevice device;

        final BluetoothConnection btConnection;
        void stopConnecting(){
            this.willStop = true;
        }
        boolean isWillStop(){ return this.willStop;}

        public ConnectionTrial(BluetoothConnection btConnection, int trialsCount, int time,boolean isBrutalConnection) {
            this.trialsCount = trialsCount;
            this.time = time;
            this.uuid = UUID.fromString(btConnection.getUUID());
            this.btConnection = btConnection;
            this.device = btConnection.getDevice();
            this.isBrutalConnection = isBrutalConnection;
        }
    }

    Queue<ConnectionTrial> btConnectionsQueue = new LinkedList<ConnectionTrial>();
    SparseArray<Queue<ConnectionTrial>> sparseTrials = new SparseArray<Queue<ConnectionTrial>>();

    private Object ConnectThreadLock = new Object();

    private final String TAG = "PLUGIN . UNITY";
    private final String NAME_SPD = "com.badran.bluetoothcontroller.SPD";
    private static BtInterface instance = null;
    private static BtInterface ConnectThreadInstance = null;


    protected BtInterface() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);

        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        //IntentFilter filter5 = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);




        //UnityPlayer.currentActivity.registerReceiver(mReceiver, filter1);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter2);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter3);
        //UnityPlayer.currentActivity.registerReceiver(mReceiver, filter5);


        // Exists only to defeat instantiation.

    }

    public void OnDestroy(){
        try {
            if(mReceiver != null)
            UnityPlayer.currentActivity.unregisterReceiver(mReceiver);
        }catch(IllegalArgumentException e){
              //Ignore
            }
        try {
            if(deviceDiscoveryReceiver != null)
                UnityPlayer.currentActivity.unregisterReceiver(deviceDiscoveryReceiver);
        }catch(IllegalArgumentException e){
            //Ignore
        }
        if(serverReceiver != null) {
            try {
                UnityPlayer.currentActivity.unregisterReceiver(serverReceiver);
            } catch (IllegalArgumentException e) {
                //Ignore
            }
        }
    }

    public static BtInterface getInstance() {
        if (instance == null) {
            instance = new BtInterface();
        }
        return instance;
    }

    public void brutal_connect(BluetoothConnection btConnection, int trialsCount , int time, boolean allowPageScan) {
        boolean deviceIsAvailable = false;
        boolean socketIsAvailable = false;
        if(btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingSocket){

            //Trying to connect a ready Socket ::: SERVER :::
            socketIsAvailable = true;

        }else if (btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference) {
            deviceIsAvailable = true;
            //Trying to connect a DEVICE ::: NOT SERVER :::

        }else {
            deviceIsAvailable = findBluetoothDevice(btConnection);
            //Trying to connect a NAME OR MAC ::: NOT SERVER :::
        }

        if(socketIsAvailable) {
            //Connected should be broadcasts before initializing streams
            btConnection.RaiseCONNECTED();


            btConnection.initializeStreams();
        }else if (deviceIsAvailable ) {
            //Found Device
            ConnectionTrial trial = new ConnectionTrial(btConnection, trialsCount, time,true);//true for isBrutalConnection
            trial.isNeedDiscovery = false;
            addConnectionTrial(trial);
        } else if(allowPageScan) {
            //Device Not found and will try to Query Devices
            ConnectionTrial trial = new ConnectionTrial(btConnection, trialsCount,time,true);//true for isBrutalConnection
            trial.isNeedDiscovery = true;
            addConnectionTrial(trial);
        } else {
            btConnection.RaiseNOT_FOUND();
        }


    }
    public void normal_connect(BluetoothConnection btConnection,boolean isChinese,boolean isSecure){
        boolean deviceIsAvailable = false;
        boolean socketIsAvailable = false;
        if(btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingSocket){

            //Trying to connect a ready Socket ::: SERVER :::
            socketIsAvailable = true;

        }else if (btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference) {
            deviceIsAvailable = true;
            //Trying to connect a DEVICE ::: NOT SERVER :::
        }else {
            deviceIsAvailable = findBluetoothDevice(btConnection);
            //Trying to connect a NAME OR MAC ::: NOT SERVER :::
        }

        if(socketIsAvailable) {
            //Connected should be broadcasts before initializing streams
            btConnection.RaiseCONNECTED();
            btConnection.initializeStreams();
        }else if (deviceIsAvailable ) {
            UUID uuid = UUID.fromString(btConnection.getUUID());

            NormalConnectThread NCT = new NormalConnectThread(btConnection,uuid,isChinese,isSecure);
            NCT.start();
        } else {
            btConnection.RaiseNOT_FOUND();
        }

    }
    public void connect(BluetoothConnection btConnection, int trialsCount , int time, boolean allowPageScan) {


        boolean deviceIsAvailable = false;
        boolean socketIsAvailable = false;
        if(btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingSocket){

            //Trying to connect a ready Socket ::: SERVER :::
            socketIsAvailable = true;

        }else if (btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference) {
            deviceIsAvailable = true;
            //Trying to connect a DEVICE ::: NOT SERVER :::
        }else {
            deviceIsAvailable = findBluetoothDevice(btConnection);
            //Trying to connect a NAME OR MAC ::: NOT SERVER :::
        }

        if(socketIsAvailable) {
            //Connected should be broadcasts before initializing streams
            btConnection.RaiseCONNECTED();
            btConnection.initializeStreams();
        }else if (deviceIsAvailable ) {
            //Found Device
            ConnectionTrial trial = new ConnectionTrial(btConnection, trialsCount, time,false);//false for isBrutalConnection
            trial.isNeedDiscovery = false;

            addConnectionTrial(trial);

        } else if(allowPageScan) {
            //Device Not found and will try to Query Devices
            ConnectionTrial trial = new ConnectionTrial(btConnection, trialsCount,time,false);//false for isBrutalConnection
            trial.isNeedDiscovery = true;
            addConnectionTrial(trial);
        } else {
            btConnection.RaiseNOT_FOUND();
        }

    }

    private void addConnectionTrial(ConnectionTrial connectionTrial) {
        synchronized (ConnectThreadLock) {
            //there's no test for dublication//added twice means doing it twice

            btConnectionsQueue.add(connectionTrial);
            int id = connectionTrial.btConnection.getID();

            Queue<ConnectionTrial> conList =  sparseTrials.get(id);
            if(conList  == null) {
                conList = new LinkedList<ConnectionTrial>();
                conList.add(connectionTrial);
                sparseTrials.put(id,conList);
            }else {
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
                foundModule = pairedDevice.getAddress().equals(setupData.mac);
            else
                foundModule = pairedDevice.getName().equals(setupData.name);

                if (foundModule) {
                    setupData.connectionMode = BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference;
                    setupData.setDevice(pairedDevice);
                    break;
                }
        }

        if(!foundModule && AlreadyFoundDevices != null){
            for (BluetoothDevice pairedDevice : AlreadyFoundDevices) {
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

            setPairedDevices = null;

        return foundModule;

    }

    private class ServerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
                int scan_mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                if(scan_mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                        BtInterface.getInstance().startServer();//it will start if was asked
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

    public class DeviceDiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device == null) return;

                if(!mBluetoothAdapter.getBondedDevices().contains(device)) {
                    if(AlreadyFoundDevices == null) AlreadyFoundDevices = new HashSet<BluetoothDevice>();
                    AlreadyFoundDevices.add(device);
                }

                //The next is for connecting to unpaired device, that has the instance btConnectionForDiscovery
                if (btConnectionForDiscovery != null) {
                    BluetoothConnection setupData = btConnectionForDiscovery.btConnection;
                    boolean foundIt = false;
                    switch(setupData.connectionMode){
                        case UsingMac:
                            if(setupData.mac != null &&
                              setupData.mac.equals(device.getAddress())){
                                foundIt = true;
                            }
                            break;
                        case UsingName:
                            if(setupData.name != null &&
                                    setupData.name.equals(device.getName())){
                                foundIt = true;
                            }
                            break;

                    }

                    if (foundIt) {

                        setupData.setDevice(device);
                        setupData.connectionMode = BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference;
                        synchronized (ConnectThreadLock) {
                            btConnectionForDiscovery.isNeedDiscovery = false;
                            btConnectionForDiscovery = null;
                            (new Thread(new ConnectThread())).start();
                        }
                        //Start the thread again
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                synchronized (ConnectThreadLock) {
                        //finished discovery so we need to check if the connection thread needs to continue
                        //No need for synchronization as while this reciever is registered and discovering the thread is off
                        if ( btConnectionForDiscovery  != null && btConnectionsQueue.size() > 0 ) {

                            btConnectionForDiscovery.btConnection.RaiseMODULE_OFF();

                            synchronized (ConnectThreadLock) {
                                btConnectionsQueue.poll();//so it doesn't try to connect to it when it starts again
                            }
                            btConnectionForDiscovery = null;
                            (new Thread(new ConnectThread())).start();
                        }else if (btConnectionForDiscovery != null) {
                            btConnectionForDiscovery = null;
                            isConnecting = false;
                        }
                }
                if(deviceDiscoveryReceiver != null) {
                    try {
                        UnityPlayer.currentActivity.unregisterReceiver(deviceDiscoveryReceiver);
                    } catch (IllegalArgumentException e) {
                        //ignore
                    }
                }
            }
        }
    }

    Set<BluetoothDevice> AlreadyFoundDevices;

    public class RSSI_DiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device == null) return;
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

//                BluetoothConnection bt = BluetoothConnection.getInstFromDevice(device);
//                if(bt != null){
//                   bt.setRSSI(rssi);
//                }
//                bt = null;// no need for it.

                if(!mBluetoothAdapter.getBondedDevices().contains(device)) {
                    if(AlreadyFoundDevices == null) AlreadyFoundDevices = new HashSet<BluetoothDevice>();
                    AlreadyFoundDevices.add(device);
                }

                PluginToUnity.ControlMessages.DISCOVERED_DEVICE.send(device.getName(),device.getAddress(),Short.toString(rssi));

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                if(deviceDiscoveryReceiver != null) {
                    try {
                        UnityPlayer.currentActivity.unregisterReceiver(rssi_DiscoveryReceiver);
                    } catch (IllegalArgumentException e) {
                        //ignore
                    }
                }
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
            if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    BluetoothConnection tmpBt;


                    if ((tmpBt = BluetoothConnection.getInstFromDevice(device)) != null) {

                        synchronized (ConnectThreadLock) {
                            if(tmpBt.isConnected && sparseTrials.get(tmpBt.getID()) == null ){
                                tmpBt.close();
                            }
                        }

                    }


            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothConnection tmpBt;

                //sparseTrials.get(tmpBt.getID()) == null//means that device isn't trying to connect, there's no attempts
                    if((tmpBt = BluetoothConnection.getInstFromDevice(device)) != null) {
                        synchronized (ConnectThreadLock) {
                            if(tmpBt.isConnected && (sparseTrials.get(tmpBt.getID()) == null )){
                                tmpBt.close();
                            }
                        }
                    }

            }
        }
    };


    void OnDeviceClosing (BluetoothConnection con) {

        synchronized (ConnectThreadLock) {
            Queue<ConnectionTrial> list = sparseTrials.get(con.getID());
            if(list != null){
                for(ConnectionTrial c : list){
                    c.stopConnecting();
                }
                sparseTrials.remove(con.getID());
            }

            //Close Discovery for a device that has been closed
            if (mBluetoothAdapter.isDiscovering() && this.btConnectionForDiscovery != null && con.equals(this.btConnectionForDiscovery.btConnection) ) {
                //finished discovery so we need to check if the connection thread needs to continue
                boolean isNeedToContinueConection = false;
                synchronized (ConnectThreadLock) {
                    ConnectionTrial tmpCon = btConnectionsQueue.peek();
                    if(con.equals(tmpCon)){
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
                    if(deviceDiscoveryReceiver != null)
                        UnityPlayer.currentActivity.unregisterReceiver(deviceDiscoveryReceiver);
                }catch(IllegalArgumentException e){
                    //Ignore
                }
                mBluetoothAdapter.cancelDiscovery();
            }else if (this.btConnectionForDiscovery != null && con.equals(this.btConnectionForDiscovery.btConnection)){
                this.btConnectionForDiscovery = null;
            }
        }
    }
    private boolean startDiscoveryForConnection(ConnectionTrial btConnectionTrial){
        this.btConnectionForDiscovery = btConnectionTrial;

        if(deviceDiscoveryReceiver == null) deviceDiscoveryReceiver = new DeviceDiscoveryReceiver();

        IntentFilter action_found = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter action_finished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        UnityPlayer.currentActivity.registerReceiver(deviceDiscoveryReceiver,action_found);
        UnityPlayer.currentActivity.registerReceiver(deviceDiscoveryReceiver,action_finished);

        //TODO check if canceling the discovery is important
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        return mBluetoothAdapter.startDiscovery();
    }

    boolean startDiscovery(){

        if(rssi_DiscoveryReceiver == null) rssi_DiscoveryReceiver = new RSSI_DiscoveryReceiver();

        IntentFilter action_found = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter action_finished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        UnityPlayer.currentActivity.registerReceiver(rssi_DiscoveryReceiver,action_found);
        UnityPlayer.currentActivity.registerReceiver(rssi_DiscoveryReceiver,action_finished);

        //TODO check if canceling the discovery is important
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        return mBluetoothAdapter.startDiscovery();
    }

    private class NormalConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothConnection btConnection;
        public NormalConnectThread(BluetoothConnection btConnection,UUID MY_UUID,boolean isChinese,boolean isSecure) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            BluetoothDevice device = btConnection.getDevice();

            try {
            if(isChinese){
                Method m;
                try {
                    if(isSecure) {
                        m = device.getClass().getMethod(CREATE_RFcomm_Socket, new Class[]{int.class});
                    }else {
                        m = device.getClass().getMethod(CREATE_INSECURE_RFcomm_Socket, new Class[]{int.class});
                    }
                    tmp = (BluetoothSocket) m.invoke(device, 1);
                } catch (Exception e) {
                    Log.v(TAG, e.getMessage());
                    Log.v(TAG,"problem create socket,createChinese");

                }

            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1 && !isSecure) {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } else {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }

            } catch (IOException e) {
                Log.v(TAG,e.getMessage());
                Log.v(TAG,"problem create socket,createRfcommSocketToServiceRecord");
            }
            this.mmSocket = tmp;
            this.btConnection = btConnection;
        }

        public void run() {
            Log.v(TAG,"Started running connecting");
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.v(TAG,connectException.getMessage());
                Log.v(TAG,"problem cwhile connecting");
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.v(TAG,closeException.getMessage());
                    Log.v(TAG,"problem closing");
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            btConnection.setSucket(mmSocket);
            btConnection.RaiseCONNECTED();
            btConnection.initializeStreams();
        }


        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectThread extends Thread {
        //int reflexCounter = 1;
        private BluetoothSocket createSocket(boolean isChineseMobile, ConnectionTrial conAttempt) {//this method returns TRUE if Socket != null
            //Found Device and trying to create socket
            BluetoothSocket tmpSocket = null;
            BluetoothDevice device = conAttempt.device;
            if(device == null) return null;
            try {

                if (isChineseMobile) {

                    Method m;
                    try {
                        m = device.getClass().getMethod(CREATE_INSECURE_RFcomm_Socket, new Class[]{int.class});
                        tmpSocket = (BluetoothSocket) m.invoke(device, 1);
                        //reflexCounter = reflexCounter > 2 ? 1 : reflexCounter + 1;
                    } catch (Exception e) {
                        Log.v(TAG, e.getMessage());
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {

                    tmpSocket = device.createInsecureRfcommSocketToServiceRecord(conAttempt.uuid);

                } else
                    tmpSocket = device.createRfcommSocketToServiceRecord(conAttempt.uuid);//for API 9
            } catch (IOException mainError) {
                Log.v(TAG, mainError.getMessage());
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
                    //TODO peek to element and poll it when it's not needed for discovery.
                    //Every ConAttempt must have a device reference or it already asked for discovery to find a reference
                    conAttempt = btConnectionsQueue.peek();

                    //check if close has been called || it's already connected
                    if(conAttempt.isWillStop() || conAttempt.btConnection.isConnected) {
                        btConnectionsQueue.poll();
                        continue;
                    }

                    if(conAttempt.isNeedDiscovery) { //if device is not found yet, need to start discovery
                        //TODO bug when device isn't found, it act is if it find it
                        if(startDiscoveryForConnection(conAttempt)) {
                            break;//thread must end
                        }else {
                            Log.v("unity","Failed to start discovery for the unpaired device");
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

                    socket = createSocket(isChineseMobile, conAttempt );

                    if (socket != null) {
                        mBluetoothAdapter.cancelDiscovery();
                        try {
                            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                                if (!socket.isConnected() ) {//Sometimes it's connected before calling connect(), because of an error in previous-attempt
                                    socket.connect();
                                }else {
                                    success = false;
                                }
                            }else {
                                socket.connect();
                            }
                        } catch (IOException e) {
                            //Log.v(TAG, e.getMessage());
                            //e.printStackTrace();

                            success = false;
                            //The reason : UUID COULD BE DIFFERENT .MODULE_UUID_WRONG
                        }



                        synchronized (ConnectThreadLock){
                            if(conAttempt.isWillStop()) {
                                try {
                                    socket.close();
                                }catch (IOException ioE){
                                    //ignore
                                }
                                //Considered success and break. it's success because the user has closed it.
                                break;
                            }
                            if(success) {
                                conAttempt.btConnection.setSucket(socket);
                            }
                        }

                        if(success){
                            conAttempt.btConnection.RaiseCONNECTED();
                            conAttempt.btConnection.initializeStreams();
                            break; //success no need for trials
                        }
                    }else {
                        success = false;
                    }

                    if(socket != null && !success) {
                        try {
                            socket.close();
                        }catch (IOException ioE){
                            //ignore
                        }
                    }
                    counter++;
                    if(counter >= conAttempt.trialsCount){

                        break;
                    }

                    try {
                        Thread.sleep(conAttempt.time);
                    } catch (InterruptedException e) {
                        Log.v(TAG, "Sleep Thread Interrupt Exception");
                    }


                } while (true);

                synchronized (ConnectThreadLock) {

                    if(success) {
                        Queue<ConnectionTrial> list = sparseTrials.get(conAttempt.btConnection.getID());
                        if (list != null) {
                            btConnectionsQueue.removeAll(list);
                            sparseTrials.remove(conAttempt.btConnection.getID());
                        }
                    }else {
                        Queue<ConnectionTrial> list = sparseTrials.get(conAttempt.btConnection.getID());
                        if(list != null) list.poll();
                    }
                }
                if(!success) {
                    conAttempt.btConnection.RaiseMODULE_OFF();
                }
            }
            //end of the biggest while
        }


    }

    //Accepting Thread

    private volatile AcceptThread acceptThread;
    public final Object acceptThreadLock = new Object();

    public void initServer(String serverUUID,int time,boolean willConnectOneDevice){

        if(acceptThread == null) {
            acceptThread = new AcceptThread(UUID.fromString(serverUUID), time, willConnectOneDevice);
        }else {
            synchronized (acceptThreadLock) {
                acceptThread.abortServer();
                acceptThread = new AcceptThread(UUID.fromString(serverUUID), time, willConnectOneDevice);
            }
        }
        IntentFilter filter_scan_mode = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        if(serverReceiver == null) serverReceiver = new ServerReceiver();
        UnityPlayer.currentActivity.registerReceiver(serverReceiver, filter_scan_mode);


        makeDiscoverable(time);

    }

    void makeDiscoverable(int time){
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,time);
        UnityPlayer.currentActivity.startActivity(discoverableIntent);
    }

    private void startServer(){//Shouldn't be called while server running, abort first
        synchronized (acceptThreadLock) {
            if (acceptThread != null) (new Thread(acceptThread)).start();
        }
    }
    private void stopServer(){//called by plugin. When the device is not discoverable
        synchronized (acceptThreadLock) {
            if (acceptThread != null)  acceptThread.stopServer();
        }
    }

    public void abortServer(){//called by unity, cause imediate closing
        synchronized (acceptThreadLock) {
            if (acceptThread != null) {
                acceptThread.abortServer();
                acceptThread = null;
            }
        }
    }
    private class AcceptThread implements Runnable {
        private volatile boolean  willStop= false;
        private  volatile BluetoothServerSocket mmServerSocket;
        private volatile boolean isAccepting = false;


        private boolean willConnectOneDevice;
        private UUID serverUUID;
        private int discoverable_Time_Duration;

        public AcceptThread(UUID serverUUID,int discoverable_Time_Duration,boolean willConnectOneDevice) {
            this.serverUUID = serverUUID;
            this.willConnectOneDevice = willConnectOneDevice;
            this.discoverable_Time_Duration = discoverable_Time_Duration;
            this.mmServerSocket = createServerSocket();
        }

        public  boolean isRunning(){
            return isAccepting;
        }

        public void stopServer(){//won't stop immedeatly
            this.willStop = true;
        }

        public void abortServer(){//will stop immidiately
            this.willStop =true;
            if(mmServerSocket != null) {
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    //Ignore
                }
            }
            if(serverReceiver != null) {
                try {
                    UnityPlayer.currentActivity.unregisterReceiver(serverReceiver);
                } catch (IllegalArgumentException e) {
                    //ignore
                }
            }
        }




        private BluetoothServerSocket createServerSocket(){
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("PluginServer", serverUUID);
                }else {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("PluginServer", serverUUID);
                }
            } catch (IOException e) {
                Log.e(TAG,"Can't Create BluetoothServerSocket : ",e);
               }
            return tmp;

        }

        public void run() {
            isAccepting = true;
            BluetoothSocket socket = null;
            int tolleranceCounter = 0;
            // Keep listening until exception occurs or a socket is returned
            while ( true) {
                try {
                        if (mmServerSocket != null) socket = mmServerSocket.accept(this.discoverable_Time_Duration*1000);
                } catch (IOException e) {

                    e.printStackTrace();
                    //TODO: ADDING TOLLERANCE FOR THE TIME AVAILABLE FOR SERVER
                        if (!this.willStop && tolleranceCounter <3) {
                            this.mmServerSocket = createServerSocket();
                            tolleranceCounter++;
                            continue;
                        }else break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    if(willConnectOneDevice)
                    {
                        cancel();
                        break;
                    }
                }
            }
            isAccepting = false;
            synchronized (acceptThreadLock) {
                if(serverReceiver != null) {
                    try {
                        UnityPlayer.currentActivity.unregisterReceiver(serverReceiver);
                    } catch (IllegalArgumentException e) {
                        //ignore
                    }
                }
                acceptThread = null;
            }


        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            if(mmServerSocket != null) {
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                }
            }
        }
        private void manageConnectedSocket(BluetoothSocket socket){
            PluginToUnity.socket = socket;//Saving it to pick it by unity
            PluginToUnity.ControlMessages.SERVER_DISCOVERED_DEVICE.send();
        }
    }
}
