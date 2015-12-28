package com.badran.bluetoothcontroller;
/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 * 
 */

import android.bluetooth.BluetoothAdapter;


import java.util.concurrent.ConcurrentLinkedQueue;
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

        boolean isNeedDiscovery;



        public ConnectionTrial(BluetoothConnection btConnection, int trialsCount) {
            this.btConnection = btConnection;
            this.trialsCount = trialsCount;
            this.isNeedDiscovery = false;
        }

        public ConnectionTrial(BluetoothConnection btConnection, int trialsCount,boolean isNeedDiscovery) {
            this.btConnection = btConnection;
            this.trialsCount = trialsCount;
            this.isNeedDiscovery = isNeedDiscovery;
        }

    }

    Queue<ConnectionTrial> btConnectionsQueue = new ConcurrentLinkedQueue<ConnectionTrial>();





    private Object ConnectThreadLock = new Object();

    private final String TAG = "PLUGIN";

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


    public void connect(BluetoothConnection btConnection, int trialsCount) {

        boolean deviceIsAvailable = false;
        boolean socketIsAvailable = false;
        if(btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingSocket){

            Log.v("unity","Trying to connect a ready Socket ::: SERVER :::");
            socketIsAvailable = true;

        }else if (btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference) {
            deviceIsAvailable = true;
            Log.v("unity","Trying to connect a DEVICE ::: NOT SERVER :::");

        }else {
            deviceIsAvailable = findBluetoothDevice(btConnection);
            Log.v("unity","Trying to connect a NAME OR MAC ::: NOT SERVER :::");
        }
        if(socketIsAvailable) {
            //Connected should be broadcasts before initializing streams
             PluginToUnity.ControlMessages.CONNECTED.send(btConnection.getID());
            btConnection.initializeStreams();
            Log.v("unity", "Connection Sucess");
        }else if (deviceIsAvailable ) {

            Log.v("unity", "Found Device");
            addConnectionTrial(new ConnectionTrial(btConnection, trialsCount));


        } else {

            Log.v("unity", "Device Not found and will try to Query Devices");
            addConnectionTrial(new ConnectionTrial(btConnection, trialsCount,true));
        }
    }

    private void addConnectionTrial(ConnectionTrial connectionTrial) {
        synchronized (ConnectThreadLock) {
            //there's no test for dublication
            btConnectionsQueue.add(connectionTrial);
            if (!isConnecting) {
                isConnecting = true;
                (new Thread(new ConnectThread())).start();
                Log.v("unity","Started connection thread  and will query device");
            }

        }
    }


    private boolean findBluetoothDevice(BluetoothConnection setupData) {

        Log.v("unity","findBluetoothDevice");

        boolean foundModule = false;

            Set<BluetoothDevice> setPairedDevices;
            setPairedDevices = mBluetoothAdapter.getBondedDevices();

        boolean useMac = setupData.connectionMode == BluetoothConnection.ConnectionMode.UsingMac;
        Log.v("unity",setupData.connectionMode.toString());
        for (BluetoothDevice pairedDevice : setPairedDevices) {

            Log.v("unity","findBluetoothDevice1aa");
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

                Log.v("unity","DIscoverable STARTED");
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
                Log.v("unity","Action Found Device");

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
                    Log.v("unity",setupData.name == null ? "name is NULL" : "name is NOT_NULL");
                    if (setupData.connectionMode == BluetoothConnection.ConnectionMode.UsingName && setupData.name != null && setupData.name.equals(device.getName()))
                    {
                        setupData.setDevice(device);
                        foundIt = true;
                    }

                    if (foundIt) {
                        Log.v("unity","Action FOUND Actiual DEVICE");
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
                Log.v("unity","Action Discovery Finished");
                synchronized (ConnectThreadLock) {

                        if (btConnectionsQueue.size() > 0 && btConnectionForDiscovery != null) {
                            PluginToUnity.ControlMessages.MODULE_OFF.send(btConnectionForDiscovery.btConnection.getID());
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
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device != null)
                    Log.v("unity", device.getName() + " : Brodcasted as CONECTED X");


            }

        else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.v("unity", device.getName() + " : Disconnect requist");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(BluetoothConnection.getInstFromDevice(device) != null) {
                    BluetoothConnection tmpBt;
                    if((tmpBt = BluetoothConnection.getInstFromDevice(device)) != null) {
                        tmpBt.removeSocketServer();
                        PluginToUnity.ControlMessages.DISCONNECTED.send(tmpBt.getID());
                    }
                }
                    Log.v("unity", device.getName() + " : Disconnected");
            }else if (BluetoothAdapter.ACTION_REQUEST_ENABLE.equals(action)) {
                Log.v("unity","BLUETOOTH Requested to be enabled");
            }
        }
    };



    private void startDiscoveryForConnection(ConnectionTrial btConnectionTrial){
        Log.v("unity","discovery Started");
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

        BluetoothConnection btConnection;




        private void createSocket(boolean isChineseMobile) {//this method returns TRUE if Socket != null

            btConnection.socket = null;
            final UUID SPP_UUID = UUID.fromString(btConnection.SPP_UUID);

            BluetoothDevice tmpDevice = btConnection.getDevice();

            if (tmpDevice != null) {
                Log.v("unity", "Found Device and trying to create socket");
                BluetoothSocket tmpSocket = null;

                try {

                    if (isChineseMobile) {

                        Method m;
                        try {
                            m = tmpDevice.getClass().getMethod(CREATE_INSECURE_RFcomm_Socket, new Class[]{int.class});
                            tmpSocket = (BluetoothSocket) m.invoke(btConnection.getDevice(), 1);
                        } catch (Exception e) {
                            Log.v(TAG, e.getMessage());


                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                        tmpSocket = Level10.getInstance().createRfcommSocket(tmpDevice, SPP_UUID);

                    } else
                        tmpSocket = tmpDevice.createRfcommSocketToServiceRecord(SPP_UUID);//for API 9
                } catch (IOException mainError) {
                    Log.v(TAG, mainError.getMessage());

                    //Bridge.controlMessage(-1);


                }

                btConnection.socket = tmpSocket;


            } else {

                PluginToUnity.ControlMessages.NOT_FOUND.send(btConnection.getID());

            }

        }




        public void run() {


            int counter;
            while (true) {

                ConnectionTrial tmpConnection;

                synchronized (ConnectThreadLock) {

                    Log.v("unity", "acquired Lock2");
                    if (btConnectionsQueue.size() <= 0) {
                        isConnecting = false;
                        break;//thread must end
                    }

                    tmpConnection = btConnectionsQueue.poll();

                    if(tmpConnection.isNeedDiscovery) { //if device is not found yet, need to start discovery
                        startDiscoveryForConnection(tmpConnection);
                        break;//thread must end
                    }
                }
                Log.v("unity", "connect Thread started");

                btConnection = tmpConnection.btConnection;

                int connectionTrials = tmpConnection.trialsCount;

                boolean isChineseMobile = false;


                counter = 0;
                boolean sucess = true;
                do {
                        createSocket(isChineseMobile);

                    if (btConnection.socket != null) {
                        mBluetoothAdapter.cancelDiscovery();
                        try {
                            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                                if (!btConnection.socket.isConnected() ) {
                                    btConnection.socket.connect();
                                }else {
                                    sucess = false;
                                }
                            }else {
                                btConnection.socket.connect();
                            }
                        } catch (IOException e) {
                            Log.v("unity", "Connection Failed");
                            Log.v("unity", e.getMessage());
                             e.printStackTrace();

                            sucess = false;
                            //btConnection.controlMessage(-3);
                            //also UUID COULD BE DIFFERENT .MODULE_UUID_WRONG
                        }


                        if (sucess) {
                            PluginToUnity.ControlMessages.CONNECTED.send(btConnection.getID());

                            btConnection.initializeStreams();

                            Log.v("unity", "Connection Sucess");

                            break; //success no need for trials

                        } else try {
                            try {
                                btConnection.socket.close();
                            }catch (IOException ioE){
                                //ignore
                            }
                            Thread.sleep(1000);

                        } catch (InterruptedException e) {
                            Log.v("unity", "Sleep Thread Interrupt Exception");
                        }
                    }


                    counter++;
                    isChineseMobile = !isChineseMobile;


                } while (counter <= connectionTrials);
                if(!sucess) {
                    btConnection.close();
                    PluginToUnity.ControlMessages.MODULE_OFF.send(btConnection.getID());
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
            int i =0;
            while ( true) {
                try {
                    Log.v("unity","ACCEPTING");
                        if (mmServerSocket != null) socket = mmServerSocket.accept(this.discoverable_Time_Duration*1000);
                } catch (IOException e) {

                    e.printStackTrace();
                    Log.v("unity", "ACCEPTING FCk");
                    //TO_DO ADDING TOLLERANCE FOR THE TIME OF AVAILABLE FOR SERVER
                        if (!this.willStop) {
                            createServerSocket();
                            continue;
                        }else break;
                }
                // If a connection was accepted
                Log.v("unity","ACCEPTING FINISHED");
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    Log.v("unity", "ACEEPTING Called manage SOCKET");

                    if(willConnectOneDevice)
                    {
                        cancel();
                        break;
                    }
                }
            }
            isAccepting = false;
            Log.v("unity","ACCEPTING FINISHED");
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

            Log.v("unity","ACCEPTING SEND TO UNITY");
            PluginToUnity.ControlMessages.SERVER_DISCOVERED_DEVICE.send();
        }
    }
}
