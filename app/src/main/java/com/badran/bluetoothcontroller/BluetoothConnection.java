package com.badran.bluetoothcontroller;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


import com.unity3d.player.UnityPlayer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;


public class BluetoothConnection {

    public final int id;
    public static volatile int counter;
    public BluetoothConnection (int id) {
        this.id = id;
        this.counter = id;
    }
    public final ConnectionSetupData setupData = new ConnectionSetupData();




    //control data
    public boolean isConnected;
    public boolean isSending;
    public boolean isReading;


    public BtReader READER;



    public BluetoothSocket socket;

    public OutputStream outStream;

    public BufferedOutputStream bufferedOutputStream = null;

    public InputStream inputStream = null;
    public BufferedReader bufferReadder = null;


    public void setBluetoothDevice(BluetoothDevice bt) {
        setupData.device =bt;
        setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingBluetoothDeviceReference;

    }

    public boolean isDataAvailable(){
        if(READER != null)
            return READER.available();



        return false;
    }
    public byte[] getBuffer(){
        Log.v("unity","getBuffer Called");
        if(READER != null)
            return  READER.readBuffer();
        return  null;
    }

    public  int connect( int trialsNumber) {

        Log.v("unity","Connect called");
        BtInterface.getInstance().connect(this,trialsNumber);
        return 1;

    }


    public void close() {
        Log.v("unity","closing");
        Log.v("unity"," Device ID IS : " + Integer.toString(this.id));
        PluginToUnity.ControlMessages.DISCONNECTED.send(this.id);
        UnityPlayer.UnitySendMessage("BtConnector","OnDisconnect","0");
        try {
            READER.close();

            if (socket != null) {
                Log.v("unity","socket is not null");
                socket.close();
                socket = null;
            }

            if(bufferedOutputStream != null) {
                bufferedOutputStream.flush();
                bufferedOutputStream.close();

            }



            isConnected = false;


        } catch (IOException e) {


        } finally {



        }
    }



    public void setUUID (String SPP_UUID){

        setupData.SPP_UUID = SPP_UUID;
    }

    public void setMac (String mac){
        setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingMac;
        setupData.mac = mac;
    }

    public void setName (String name){
        setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingName;
        setupData.name = name;
    }


    public void sendString(String msg) {

        if(this.socket != null && this.bufferedOutputStream != null)
        BtSender.getInstance().addJob(bufferedOutputStream,msg.getBytes());

    }


    public String sendChar(byte msg) {

        if(this.socket != null && this.bufferedOutputStream != null) {
            Log.v("unity","ready to sendChar");
            BtSender.getInstance().addJob(bufferedOutputStream, new byte[]{msg});
        }
        return this.setupData.name;
    }



    public void sendBytes(byte[] msg) {
        if(this.socket != null && this.bufferedOutputStream != null)
        BtSender.getInstance().addJob(bufferedOutputStream, msg);

    }



    public boolean isConnected() {
        return isConnected;
    }

    public boolean isSending() {
        return isSending;
    }

    public boolean isListening() {
        return isReading;
    }
}
