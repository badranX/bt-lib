package com.badran.bluetoothcontroller;
/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 * 
 */

import android.bluetooth.BluetoothAdapter;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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


    private volatile boolean isConnecting = false;

    private ConnectionTrial btConnectionForDiscovery;

    private class ConnectionTrial {
        final BluetoothConnection btConnection;
        final int trialsCount;

        final boolean isNeedDiscovery;



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


        IntentFilter filter6 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filter7 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter1);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter2);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter3);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter4);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter5);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter6);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter7);

        // Exists only to defeat instantiation.

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
            socketIsAvailable = true;

        }else if (btConnection.connectionMode == BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference)
            deviceIsAvailable = true;
        else
            deviceIsAvailable = findBluetoothDevice(btConnection);

        if(socketIsAvailable) {
            btConnection.initializeStreams();
            Log.v("unity", "Connection Sucess");
            PluginToUnity.ControlMessages.CONNECTED.send(btConnection.getID());
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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
           if(getResultCode() == 100) Log.v("unity","YES ITs 100");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if(btConnectionForDiscovery != null) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.v("unity", device.getName() + " : is found");

                    BluetoothConnection setupData = btConnectionForDiscovery.btConnection;
                    boolean foundIt = false;
                    if (setupData.connectionMode == BluetoothConnection.ConnectionMode.UsingMac)

                        if (setupData.mac.equals(device.getAddress())) {

                            setupData.setDevice(device);
                            foundIt = true;
                        } else if (setupData.name.equals(device.getName())) {
                            setupData.setDevice(device);
                            foundIt = true;
                        }

                    if (foundIt) {

                        setupData.connectionMode = BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference;
                        synchronized (ConnectThreadLock) {
                            Log.v("unity","will start connection thread again");
                            btConnectionsQueue.add(btConnectionForDiscovery);
                            btConnectionForDiscovery = null;
                            (new Thread(new ConnectThread())).start();
                            Log.v("unity", "finish starting connection thread again");

                        }
                        //Start the thread again

                    }

                }

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device != null)
                    Log.v("unity", device.getName() + " : Brodcasted as CONECTED X");
//                if(ConnectionSetupData.getIdFromDevice(device) != null) {
//                   Log.v("unity", "Accepting : " + ConnectionSetupData.getIdFromDevice(device));
//                    PluginToUnity.ControlMessages.CONNECTED.send(ConnectionSetupData.getIdFromDevice(device));
//                }

            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    synchronized (ConnectThreadLock) {
                        if(!isConnecting) {

                            if (btConnectionsQueue.size() > 0) {
                                (new Thread(new ConnectThread())).start();
                            }
                        }

                }
            }
        else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.v("unity", device.getName() + " : Disconnect requist");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(BluetoothConnection.getIdFromDevice(device) != null) {
                    PluginToUnity.ControlMessages.DISCONNECTED.send(BluetoothConnection.getIdFromDevice(device));
                }
                    Log.v("unity", device.getName() + " : Disconnected");
            }else if (BluetoothAdapter.ACTION_REQUEST_ENABLE.equals(action)) {
                Log.v("unity","BLUETOOTH Requested to be enabled");
            }else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
                int scan_mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                Log.v("unity","DIscoverable STARTED");
                if(scan_mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Log.v("unity", "XYXYXYX 300");
                    BtInterface.getInstance().startServer();
                }
            }
        }
    };



    private void startDiscoveryForConnection(ConnectionTrial btConnectionTrial){
        Log.v("unity","discovery Started");
        this.btConnectionForDiscovery = btConnectionTrial;
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
                        tmpSocket = tmpDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID);

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
                    BluetoothSocket socket = btConnection.socket;
                    if (socket != null) {


                        mBluetoothAdapter.cancelDiscovery();
                        try {

                            socket.connect();

                        } catch (IOException e) {
                            Log.v("unity", "Connection Failed");
                            Log.v(TAG, e.getMessage());

                            sucess = false;
                            //btConnection.controlMessage(-3);
                            //also UUID COULD BE DIFFERENT .MODULE_UUID_WRONG
                        }


                        if (sucess) {

                            btConnection.initializeStreams();



                            Log.v("unity", "Connection Sucess");
                            PluginToUnity.ControlMessages.CONNECTED.send(btConnection.getID());

                            break; //success no need for trials

                        } else try {
                            Thread.sleep(100);

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



    private String serverUUID;
    private int Discoverable_Time_Duration;
    public void initServer(String serverUUID,int time){
        this.serverUUID = serverUUID;
        boolean allowToRun = false;
        if(acceptThread != null) {
            if (!acceptThread.isRunning()) {
                allowToRun = true;
            }
        }else allowToRun = true;

        if(allowToRun) {
            Log.v("unity", "INTENTING");
            Discoverable_Time_Duration = time;
            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,time);
                UnityPlayer.currentActivity.startActivity(discoverableIntent);
        }
    }
    public void startServer(){
        if ( acceptThread == null || !acceptThread.isRunning()) {
            acceptThread = new AcceptThread(serverUUID);
            Log.v("unity","ACCEPTING");
            ( new Thread(acceptThread)).start();
        }
    }
    private class AcceptThread implements Runnable {
        private  BluetoothServerSocket mmServerSocket;
        private volatile boolean isAccepting = true;
        private UUID UNITY_UUID;
        public synchronized boolean isRunning(){
            return isAccepting;
        }
        private final int NumberOfTrials = 2;
        public AcceptThread(String UNITY_UUID) {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            this.UNITY_UUID  = UUID.fromString(UNITY_UUID);
            createServerSocket();
        }
        private void createServerSocket(){
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Server", this.UNITY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {



            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            int i =0;
            while (i<NumberOfTrials  ) {
                try {
                    Log.v("unity","ACCEPTING");

                    socket = mmServerSocket.accept();
                } catch (IOException e) {

                    e.printStackTrace();
                    Log.v("unity", "ACCEPTING FCk");
                    //TO_DO ADDING TOLLERANCE FOR THE TIME OF AVAILABLE FOR SERVER
                    i++;
                    createServerSocket();
                    continue;
                }
                // If a connection was accepted
                Log.v("unity","ACCEPTING FINISHED");
                if (socket != null) {

                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    Log.v("unity","ACEEPTING Called manage SOCKET");
                    cancel();
                    break;
                }

            }
            synchronized (this) {
                isAccepting = false;
            }

        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
        private void manageConnectedSocket(BluetoothSocket socket){


            PluginToUnity.ControlMessages.socket = socket;//Saving it to pick it by unity

            Log.v("unity","ACCEPTING SEND TO UNITY");
            PluginToUnity.ControlMessages.SERVER_DISCOVERED_DEVICE.send();

        }


    }


}
