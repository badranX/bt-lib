package com.badran.bluetoothcontroller;
/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 * 
 */

import android.bluetooth.BluetoothAdapter;


import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
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


    Map<String, ConnectionSetupData> waitingDiscoveryDevices = new ConcurrentHashMap<String, ConnectionSetupData>();



    private Object lock1 = new Object();

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

    public static BtInterface getInstance() {
        if (instance == null) {
            instance = new BtInterface();
        }
        return instance;
    }


    public void connect(BluetoothConnection btConnection, int trialsCount) {

        boolean deviceIsAvailable = false;
        boolean socketIsAvailable = false;
        if(btConnection.setupData.connectionMode == ConnectionSetupData.ConnectionMode.UsingSocket){
            socketIsAvailable = true;

        }else if (btConnection.setupData.connectionMode == ConnectionSetupData.ConnectionMode.UsingBluetoothDeviceReference)
            deviceIsAvailable = true;
        else
            deviceIsAvailable = findBluetoothDevice(btConnection.setupData);

        if(socketIsAvailable) {
            btConnection.initializeStreams();
            Log.v("unity", "Connection Sucess");
            PluginToUnity.ControlMessages.CONNECTED.send(btConnection.id);
        }else if (deviceIsAvailable ) {

            Log.v("unity", "Found Device");
            synchronized (lock1) {
                btConnectionsQueue.add(new ConnectionTrial(btConnection, trialsCount));

                if (!isConnecting) {

                    isConnecting = true;
                    (new Thread(new ConnectThread())).start();

                }
            }

        } else {
            Log.v("unity", "Device Not found and will try to Query Devices");
            synchronized (lock1) {


                if (!isConnecting) {

                    isConnecting = true;

                    this.startDiscoveryForConnection(new ConnectionTrial(btConnection,trialsCount));

                } else btConnectionsQueue.add(new ConnectionTrial(btConnection, trialsCount,true));
            }

        }
    }



    private boolean findBluetoothDevice(ConnectionSetupData setupData) {

        boolean foundModule = false;

            Set<BluetoothDevice> setPairedDevices;
            setPairedDevices = mBluetoothAdapter.getBondedDevices();

            BluetoothDevice[] pairedDevices = setPairedDevices.toArray(new BluetoothDevice[setPairedDevices.size()]);


            for (BluetoothDevice pairedDevice : pairedDevices) {


                if (setupData.connectionMode == ConnectionSetupData.ConnectionMode.UsingMac)
                    foundModule = pairedDevice.getAddress().equals(setupData.mac);
                else
                    foundModule = pairedDevice.getName().equals(setupData.name);

                if (foundModule) {
                    setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingBluetoothDeviceReference;
                    setupData.setDevice(pairedDevice,btConnectionForDiscovery.btConnection.id);
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

                    ConnectionSetupData setupData = btConnectionForDiscovery.btConnection.setupData;
                    boolean foundIt = false;
                    if (setupData.connectionMode == ConnectionSetupData.ConnectionMode.UsingMac)

                        if (setupData.mac.equals(device.getAddress())) {

                            setupData.setDevice(device,btConnectionForDiscovery.btConnection.id);
                            foundIt = true;
                        } else if (setupData.name.equals(device.getName())) {
                            setupData.setDevice(device,btConnectionForDiscovery.btConnection.id);
                            foundIt = true;
                        }

                    if (foundIt) {

                        setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingBluetoothDeviceReference;
                        synchronized (lock1) {
                            btConnectionsQueue.add(btConnectionForDiscovery);
                            btConnectionForDiscovery = null;
                            (new Thread(new ConnectThread())).start();

                        }
                        //Start the thread again

                    }

                }

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.v("unity", device.getName() + " : Brodcasted as CONECTED X");
//                if(ConnectionSetupData.getIdFromDevice(device) != null) {
//                   Log.v("unity", "Accepting : " + ConnectionSetupData.getIdFromDevice(device));
//                    PluginToUnity.ControlMessages.CONNECTED.send(ConnectionSetupData.getIdFromDevice(device));
//                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.v("unity", device.getName() + " : discovery finished");
                if(btConnectionForDiscovery != null) { //using null to tell if we did found the device or not
                    Log.v("unity", device.getName() + " : discovery finished and will try to complete connection for others");
                    synchronized (lock1) {

                        if (btConnectionsQueue.size() > 0) {
                            (new Thread(new ConnectThread())).start();

                        }else isConnecting = false;
                    }

                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.v("unity", device.getName() + " : Disconnect requist");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(ConnectionSetupData.getIdFromDevice(device) != null) {
                    PluginToUnity.ControlMessages.DISCONNECTED.send(ConnectionSetupData.getIdFromDevice(device));
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
        ConnectionSetupData setupData;



        private void createSocket(boolean isChineseMobile) {//this method returns TRUE if Socket != null

            btConnection.socket = null;
            final UUID SPP_UUID = UUID.fromString(setupData.SPP_UUID);

            BluetoothDevice tmpDevice = setupData.getDevice();

            if (tmpDevice != null) {
                Log.v("unity", "Found Device and trying to create socket");
                BluetoothSocket tmpSocket = null;

                try {

                    if (isChineseMobile) {

                        Method m;
                        try {
                            m = tmpDevice.getClass().getMethod(CREATE_RFcomm_Socket, new Class[]{int.class});
                            tmpSocket = (BluetoothSocket) m.invoke(btConnection.setupData.getDevice(), 1);
                        } catch (Exception e) {
                            Log.v(TAG, e.getMessage());


                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                        tmpSocket = Level10.getInstance().createRfcommSocket(tmpDevice, SPP_UUID);

                    } else
                        tmpSocket = tmpDevice.createRfcommSocketToServiceRecord(SPP_UUID);


                } catch (IOException mainError) {
                    Log.v(TAG, mainError.getMessage());

                    //Bridge.controlMessage(-1);


                }

                btConnection.socket = tmpSocket;


            } else {


                PluginToUnity.ControlMessages.NOT_FOUND.send(3);


            }

        }




        public void run() {


            int counter;
            while (true) {

                ConnectionTrial tmpConnection;
                synchronized (lock1) {

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
                setupData = btConnection.setupData;
                int connectionTrials = tmpConnection.trialsCount;
                tmpConnection = null;

                boolean isChineseMobile = false;


                counter = 0;
                do {

                        createSocket(isChineseMobile);


                    if (btConnection.socket != null) {
                        boolean sucess = true;


                        mBluetoothAdapter.cancelDiscovery();
                        try {

                            btConnection.socket.connect();

                        } catch (IOException e) {
                            Log.v("unity", "Connection Failed");
                            Log.v(TAG, e.getMessage());

                            btConnection.close();
                            sucess = false;
                            //btConnection.controlMessage(-3);
                            PluginToUnity.ControlMessages.MODULE_OFF.send(0);
                            //also UUID COULD BE DIFFERENT .MODULE_UUID_WRONG
                        }


                        if (sucess) {

                            btConnection.initializeStreams();



                            Log.v("unity", "Connection Sucess");
                            PluginToUnity.ControlMessages.CONNECTED.send(btConnection.id);

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


            }

            //end of the biggest while

        }


    }
    //Accepting Thread

    AcceptThread acceptThread;



    private String serverUUID;
    public void initServer(String serverUUID){
        this.serverUUID = serverUUID;
        boolean allowToRun = false;
        if(acceptThread != null) {
            if (!acceptThread.isRunning()) {
                allowToRun = true;
            }
        }else allowToRun = true;

        if(allowToRun) {
            Log.v("unity", "INTENTING");

            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 100);
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

        public AcceptThread(String UNITY_UUID) {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            this.UNITY_UUID  = UUID.fromString(UNITY_UUID);
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Server", this.UNITY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {



            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    Log.v("unity","ACCEPTING");

                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.v("unity", "ACCEPTING FCk");
                    break;
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


            PluginToUnity.ControlMessages.socket = socket;
            Log.v("unity","ACCEPTING SEND TO UNITY");
            PluginToUnity.ControlMessages.DEVICE_DISCOVERED.send();

        }


    }


}
