package com.badran.bluetoothcontroller;
/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 * 
 */

import android.bluetooth.BluetoothAdapter;


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
    private BluetoothAdapter mBluetoothAdapter;

    private ServerReceiver serverReceiver;
    private DeviceDiscoveryReceiver deviceDiscoveryReceiver;
    private volatile boolean isConnecting = false;

    private ConnectionTrial btConnectionForDiscovery;

    private class ConnectionTrial {
        final BluetoothConnection btConnection;
        final int trialsCount;
        final int time;
        final UUID uuid;
        final BluetoothDevice device;
        private boolean willStop = false;
        boolean isNeedDiscovery;

        void stopConnecting(){
            this.willStop = true;
        }
        boolean isWillStop(){ return this.willStop;}

        public ConnectionTrial(BluetoothConnection btConnection, int trialsCount, int time) {
            this.btConnection = btConnection;
            this.trialsCount = trialsCount;
            this.isNeedDiscovery = false;
            this.time = time;
            this.device = btConnection.getDevice();
            this.uuid = UUID.fromString(btConnection.getUUID());
        }

        public ConnectionTrial(BluetoothConnection btConnection, int trialsCount, int time ,boolean isNeedDiscovery) {
            this.btConnection = btConnection;
            this.trialsCount = trialsCount;
            this.isNeedDiscovery = isNeedDiscovery;
            this.time = time;
            this.device = btConnection.getDevice();
            this.uuid = UUID.fromString(btConnection.getUUID());
        }

    }

    Queue<ConnectionTrial> btConnectionsQueue = new LinkedList<ConnectionTrial>();
    SparseArray<LinkedList<ConnectionTrial>> sparseTrials = new SparseArray<LinkedList<ConnectionTrial>>();

    private Object ConnectThreadLock = new Object();

    private final String TAG = "PLUGIN . UNITY";

    private static BtInterface instance = null;
    private static BtInterface ConnectThreadInstance = null;


    protected BtInterface() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);

        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        IntentFilter filter4 = new IntentFilter(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        IntentFilter filter5 = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);




        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter1);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter2);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter3);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter4);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter5);


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


    public void connect(BluetoothConnection btConnection, int trialsCount , int time, boolean allowPageScan) {

        if(btConnection.isConnected) return;
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
             PluginToUnity.ControlMessages.CONNECTED.send(btConnection.getID());

            btConnection.initializeStreams();
        }else if (deviceIsAvailable ) {
            //Found Device
            addConnectionTrial(new ConnectionTrial(btConnection, trialsCount, time));


        } else if(allowPageScan) {
            //Device Not found and will try to Query Devices
            addConnectionTrial(new ConnectionTrial(btConnection, trialsCount,time,true));
        } else {
            btConnection.RaiseMODULE_OFF();
        }
    }

    private void addConnectionTrial(ConnectionTrial connectionTrial) {
        synchronized (ConnectThreadLock) {
            //there's no test for dublication//added twice means doing it twice

            btConnectionsQueue.add(connectionTrial);
            int id = connectionTrial.btConnection.getID();
            LinkedList<ConnectionTrial> conList =  sparseTrials.get(id);
            if(conList  == null) {
                conList = new LinkedList<ConnectionTrial>();
                conList.add(connectionTrial);
                sparseTrials.put(id,conList);
            }else {
                conList = new LinkedList<ConnectionTrial>();
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
                }
            }
        }
    }

    public class DeviceDiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (btConnectionForDiscovery != null) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(device == null) return;

                    BluetoothConnection setupData = btConnectionForDiscovery.btConnection;
                    boolean foundIt = false;
                    if (setupData.connectionMode == BluetoothConnection.ConnectionMode.UsingMac && setupData.mac != null && setupData.mac.equals(device.getAddress()))
                        {
                            setupData.setDevice(device);
                            foundIt = true;
                        }
                    if (setupData.connectionMode == BluetoothConnection.ConnectionMode.UsingName && setupData.name != null && setupData.name.equals(device.getName()))
                    {
                        setupData.setDevice(device);
                        foundIt = true;
                    }

                    if (foundIt) {
                        setupData.connectionMode = BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference;
                        synchronized (ConnectThreadLock) {
                            btConnectionForDiscovery.isNeedDiscovery = false;
                            btConnectionsQueue.add(btConnectionForDiscovery);
                            btConnectionForDiscovery = null;
                            (new Thread(new ConnectThread())).start();
                        }
                        //Start the thread again
                    }

                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                synchronized (ConnectThreadLock) {
                        //finished discovery so we need to check if the connection thread needs to continue
                        if (btConnectionsQueue.size() > 0 && btConnectionForDiscovery != null) {
                            btConnectionForDiscovery.btConnection.RaiseMODULE_OFF();
                            btConnectionForDiscovery = null;
                            (new Thread(new ConnectThread())).start();
                        }else if (btConnectionForDiscovery != null) {
                            btConnectionForDiscovery = null;
                            isConnecting = false;
                        }
                }
                try {
                    UnityPlayer.currentActivity.unregisterReceiver(this);
                }catch (IllegalArgumentException e){
                    //ignore
                }
            }
        }
    }



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //TODO: we don't need this yet, but might use it in the future
            }

        else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //TODO: we don't need this yet, but might use it in the future

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothConnection tmpBt;


                    if((tmpBt = BluetoothConnection.getInstFromDevice(device)) != null) {
                        tmpBt.removeSocketServer();
                        tmpBt.RaiseDISCONNECTED();
                    }

            }else if (BluetoothAdapter.ACTION_REQUEST_ENABLE.equals(action)) {
                //TODO: we don't need this yet, but might use it in the future
            }
        }
    };


    void OnDeviceClosing (BluetoothConnection con) {

        synchronized (ConnectThreadLock) {
            LinkedList<ConnectionTrial> list = sparseTrials.get(con.getID());
            if(list != null){
                for(ConnectionTrial c : list){
                    c.stopConnecting();
                }
                sparseTrials.remove(con.getID());
            }

            //Close Discovery for a device that has been closed
            if (mBluetoothAdapter.isDiscovering() && con == this.btConnectionForDiscovery.btConnection ) {
                //finished discovery so we need to check if the connection thread needs to continue
                if (btConnectionsQueue.size() > 0 && btConnectionForDiscovery != null) {
                    btConnectionForDiscovery = null;
                    (new Thread(new ConnectThread())).start();
                } else if (btConnectionForDiscovery != null) {
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
            }
        }
    }
    private void startDiscoveryForConnection(ConnectionTrial btConnectionTrial){
        this.btConnectionForDiscovery = btConnectionTrial;

        if(deviceDiscoveryReceiver == null) deviceDiscoveryReceiver = new DeviceDiscoveryReceiver();

        IntentFilter action_found = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter action_finished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        UnityPlayer.currentActivity.registerReceiver(deviceDiscoveryReceiver,action_found);
        UnityPlayer.currentActivity.registerReceiver(deviceDiscoveryReceiver,action_finished);
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }
    private class ConnectThread implements Runnable {

        private BluetoothSocket createSocket(boolean isChineseMobile, ConnectionTrial conAttempt) {//this method returns TRUE if Socket != null
                //Found Device and trying to create socket
                BluetoothSocket tmpSocket = null;

                try {

                    if (isChineseMobile) {

                        Method m;
                        try {
                            m = conAttempt.device.getClass().getMethod(CREATE_INSECURE_RFcomm_Socket, new Class[]{int.class});
                            tmpSocket = (BluetoothSocket) m.invoke(conAttempt.device, 1);
                        } catch (Exception e) {
                            Log.v(TAG, e.getMessage());
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                        tmpSocket = Level10.getInstance().createRfcommSocket(conAttempt.device, conAttempt.uuid);

                    } else
                        tmpSocket = conAttempt.device.createRfcommSocketToServiceRecord(conAttempt.uuid);//for API 9
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
                    conAttempt = btConnectionsQueue.poll();

                    //check if close has been called || it's already connected
                    if(conAttempt.isWillStop() || conAttempt.btConnection.isConnected) continue;

                    if(conAttempt.isNeedDiscovery) { //if device is not found yet, need to start discovery
                        startDiscoveryForConnection(conAttempt);
                        break;//thread must end
                    }
                }


                boolean isChineseMobile = false;

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
                            Log.v(TAG, "Connection Failed");
                            Log.v(TAG, e.getMessage());
                            e.printStackTrace();

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
                        }else {
                            try {
                                socket.close();
                            }catch (IOException ioE){
                                //ignore
                            }
                        }
                    }else {
                        success = false;
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

                    isChineseMobile = !isChineseMobile;


                } while (true);
                if(!success) {
                    conAttempt.btConnection.RaiseMODULE_OFF();
                }
            }
            //end of the biggest while
        }


    }
    //Accepting Thread

    AcceptThread acceptThread;
    public final Object acceptThreadLock = new Object();

    public void initServer(String serverUUID,int time,boolean willConnectOneDevice){

        if(acceptThread == null) {
            acceptThread = new AcceptThread(UUID.fromString(serverUUID), time, willConnectOneDevice);
        }else {

            acceptThread.abortServer();
            acceptThread = new AcceptThread(UUID.fromString(serverUUID), time, willConnectOneDevice);
        }
            IntentFilter filter_scan_mode = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            if(serverReceiver == null) serverReceiver = new ServerReceiver();
            UnityPlayer.currentActivity.registerReceiver(serverReceiver, filter_scan_mode);


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
        private  BluetoothServerSocket mmServerSocket;
        private volatile boolean isAccepting = false;


        private boolean willConnectOneDevice;
        private UUID serverUUID;
        private int discoverable_Time_Duration;

        public AcceptThread(UUID serverUUID,int discoverable_Time_Duration,boolean willConnectOneDevice) {
            this.serverUUID = serverUUID;
            this.willConnectOneDevice = willConnectOneDevice;
            this.discoverable_Time_Duration = discoverable_Time_Duration;
            createServerSocket();
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




        private void createServerSocket(){
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("PluginServer", serverUUID);
                }else {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("PluginServer", serverUUID);
                }
            } catch (IOException e) { }
            this.mmServerSocket = tmp;
        }

        public void run() {
            isAccepting = true;
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while ( true) {
                try {
                    Log.v("unity","ACCEPTING");
                        if (mmServerSocket != null) socket = mmServerSocket.accept(this.discoverable_Time_Duration*1000);
                } catch (IOException e) {

                    e.printStackTrace();
                    Log.v(TAG, "Accepting Socket Failed to IOException");
                    //TODO: ADDING TOLLERANCE FOR THE TIME AVAILABLE FOR SERVER
                        if (!this.willStop) {
                            createServerSocket();
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
            PluginToUnity.ControlMessages.socket = socket;//Saving it to pick it by unity
            PluginToUnity.ControlMessages.SERVER_DISCOVERED_DEVICE.send();
        }
    }
}
