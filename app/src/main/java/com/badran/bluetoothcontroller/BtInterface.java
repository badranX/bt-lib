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

        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter1);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter2);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter3);
        UnityPlayer.currentActivity.registerReceiver(mReceiver, filter4);
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
        if (btConnection.setupData.connectionMode == ConnectionSetupData.ConnectionMode.UsingBluetoothDeviceReference)
            deviceIsAvailable = true;
        else
            deviceIsAvailable = findBluetoothDevice(btConnection.setupData);


        if (deviceIsAvailable) {

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
                    setupData.device = pairedDevice;
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
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if(btConnectionForDiscovery != null) {
                    Log.v("unity", device.getName() + " : is found");

                    ConnectionSetupData setupData = btConnectionForDiscovery.btConnection.setupData;
                    boolean foundIt = false;
                    if (setupData.connectionMode == ConnectionSetupData.ConnectionMode.UsingMac)

                        if (setupData.mac.equals(device.getAddress())) {

                            setupData.device = device;

                            foundIt = true;
                        } else if (setupData.name.equals(device.getName())) {
                            setupData.device = device;

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
                Log.v("unity", device.getName() + " : is connected");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
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
                Log.v("unity", device.getName() + " : Disconnect requist");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.v("unity", device.getName() + " : Disconnected");
            }else if (BluetoothAdapter.ACTION_REQUEST_ENABLE.equals(action)) {
                Log.v("unity","BLUETOOTH Requested to be enabled");
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


            BluetoothDevice tmpDevice = btConnection.setupData.device;

            if (tmpDevice != null) {
                Log.v("unity", "Found Device and trying to create socket");
                BluetoothSocket tmpSocket = null;

                try {

                    if (isChineseMobile) {

                        Method m;
                        try {
                            m = tmpDevice.getClass().getMethod(CREATE_RFcomm_Socket, new Class[]{int.class});
                            tmpSocket = (BluetoothSocket) m.invoke(btConnection.setupData.device, 1);
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


        private void initializeStreams(boolean isReading, boolean isSending) {


            if (isReading) {
                try {

                    btConnection.inputStream = btConnection.socket.getInputStream();


                } catch (IOException e) {

                    Log.v(TAG, e.getMessage());
                }

                btConnection.bufferReadder = new BufferedReader(new InputStreamReader(btConnection.inputStream));

                btConnection.READER = new BtReader(btConnection);
                //btConnection.READER.startListeningThread();

            }

            if (isSending) {
                Log.v("unity", "Initializing streams");
                try {


                    btConnection.outStream = btConnection.socket.getOutputStream();
                } catch (IOException e) {
                    Log.v("unity", "can't get input stream");
                    Log.v(TAG, e.getMessage());

                }

                if (btConnection.outStream != null) {

                    btConnection.bufferedOutputStream = new BufferedOutputStream(btConnection.outStream);
                    Log.v("unity", "bufferedOutputStream created and ready");
                }
                if (btConnection.socket == null) Log.v("unity", " connect socket is null");
                if (btConnection.bufferedOutputStream == null)
                    Log.v("unity", "connect bufferedOutputStream is null");
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
                        }


                        if (sucess) {

                            initializeStreams(true, true);
                            btConnection.sendChar((byte) 55);
                            btConnection.isConnected = true;

                            Log.v("unity", "Connection Sucess");
                            PluginToUnity.ControlMessages.CONNECTED.send(0);

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


}
