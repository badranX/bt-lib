package com.badran.bluetoothcontroller;
/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 * 
 */

import android.bluetooth.BluetoothAdapter;


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
import android.os.Build;
import android.util.Log;


import com.unity3d.player.UnityPlayer;


public class BtInterface {


    private final String CREATE_RFcomm_Socket = "createRfcommSocket";
    private BluetoothAdapter mBluetoothAdapter;


    private volatile boolean isConnecting = false;


    private class ConnectionTrial {
        BluetoothConnection btConnection;
        int trialsCount;

        public ConnectionTrial(BluetoothConnection btConnection, int trialsCount) {
            this.btConnection = btConnection;
            this.trialsCount = trialsCount;

        }

    }

    Queue<ConnectionTrial> btConnectionsQueue = new ConcurrentLinkedQueue<ConnectionTrial>();

    private Object lock1 = new Object();
    //private volatile AbstractQueue<BluetoothConnection> devicesQueue = new ArrayBlockingQueue<BluetoothConnection>();

    private final String TAG = "PLUGIN";

    private static BtInterface instance = null;
    private static BtInterface ConnectThreadInstance = null;


    protected BtInterface() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Exists only to defeat instantiation.

    }

    public static BtInterface getInstance() {
        if (instance == null) {
            instance = new BtInterface();
        }
        return instance;
    }


    public void connect(BluetoothConnection btConnection, int trialsCount) {

        if (findBluetoothDevice(btConnection.setupData)) {

            Log.v("unity","Found Device");
            synchronized (lock1) {
                btConnectionsQueue.add(new ConnectionTrial(btConnection, trialsCount));
                Log.v("unity","Acquired Lock");
                if (!isConnecting) {
                    Log.v("unity","Started A thread");
                    isConnecting = true;
                    (new Thread(new ConnectThread())).start();

                }
            }

        }

    }


    private boolean findBluetoothDevice(ConnectionSetupData setupData) {

        boolean foundModule = false;
        if (!setupData.isDevicePicked) {
            Set<BluetoothDevice> setPairedDevices;
            setPairedDevices = mBluetoothAdapter.getBondedDevices();

            BluetoothDevice[] pairedDevices = setPairedDevices.toArray(new BluetoothDevice[setPairedDevices.size()]);


            for (BluetoothDevice pairedDevice : pairedDevices) {


                if (setupData.isUsingMac)
                    foundModule = pairedDevice.getAddress().equals(setupData.mac);
                else
                    foundModule = pairedDevice.getName().equals(setupData.name);

                if (foundModule) {
                    setupData.device = pairedDevice;
                    break;
                }
            }
            setPairedDevices = null;
        }

        return foundModule;

    }


    private class ConnectThread implements Runnable {



        BluetoothConnection btConnection ;
        ConnectionSetupData setupData ;

        private void createSocket( boolean isChineseMobile) {//this method returns TRUE if Socket != null
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


        private void initializeStreams( boolean isReading, boolean isSending) {


            if (isReading) {
                try {

                    btConnection.inputStream = btConnection.socket.getInputStream();


                } catch (IOException e) {

                    Log.v(TAG, e.getMessage());
                }

                btConnection.bufferReadder = new BufferedReader(new InputStreamReader(btConnection.inputStream));

                btConnection.READER = new BtReader(btConnection.socket, btConnection.inputStream, btConnection.bufferReadder);
                //btConnection.READER.startListeningThread();

            }

            if (isSending) {
                Log.v("unity","Initializing streams");
                try {


                    btConnection.outStream = btConnection.socket.getOutputStream();
                } catch (IOException e) {
                    Log.v("unity","can't get input stream");
                    Log.v(TAG, e.getMessage());

                }

                if(btConnection.outStream != null) {

                    btConnection.bufferedOutputStream = new BufferedOutputStream(btConnection.outStream);
                    Log.v("unity","bufferedOutputStream created and ready");
                }
                if(btConnection.socket == null)Log.v("unity"," connect socket is null");
                if(btConnection.bufferedOutputStream == null)Log.v("unity","connect bufferedOutputStream is null");
            }


        }

        public void run() {



            int counter;
            while (true) {
                ConnectionTrial tmpConnection;
                synchronized (lock1) {

                    Log.v("unity","acquired Lock2");
                    if (btConnectionsQueue.size() <= 0) {
                        isConnecting = false;
                        break;
                    } else tmpConnection = btConnectionsQueue.poll();
                }
                Log.v("unity","connect Thread started");

                btConnection = tmpConnection.btConnection;
                setupData = btConnection.setupData;
                int connectionTrials = tmpConnection.trialsCount;
                tmpConnection = null;

                boolean isChineseMobile = false;


                counter = 0;
                do {
                    Log.v("unity", "is Connecting and creating socket");
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

                            initializeStreams( false, true);
                            btConnection.sendChar((byte)55);
                            btConnection.isConnected = true;

                            Log.v("unity", "Connection Sucess");
                            PluginToUnity.ControlMessages.CONNECTED.send(0);

                            break; //success no need for trials

                        }else try {
                            Thread.sleep(100);

                        } catch (InterruptedException e) {

                            Log.v(TAG, "Sleep Thread Interrupt Exception");
                        }
                    }






                        counter++;
                        isChineseMobile = !isChineseMobile;



                } while (counter <= connectionTrials);


            }

            //Synchronized check if there's a need for more

        }


    }


}
