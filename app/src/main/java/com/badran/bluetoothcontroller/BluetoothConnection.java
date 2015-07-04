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
import java.util.HashMap;
import java.util.Map;


public class BluetoothConnection {

    public final int id;



    public BluetoothConnection (int id) {
        this.id = id;

        setupData = new ConnectionSetupData(id);
    }

    public final ConnectionSetupData setupData ;




    //control data
    boolean willRead = false;





    public BluetoothSocket socket;

    public OutputStream outStream;

    public BufferedOutputStream bufferedOutputStream = null;

    public InputStream inputStream = null;
    public BufferedReader bufferReadder = null;

    public int readingThreadID;

    public void enableReading(int readingThreadID){
        Log.v("unity","Plugin ENable Reading");
        this.readingThreadID = readingThreadID;
        this.willRead = true;

    }


    public void setBluetoothDevice(BluetoothDevice bt) {
        setupData.setDevice(bt);
        setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingBluetoothDeviceReference;

    }


    public byte[] read(int size){
        Log.v("unity","getBuffer Called with size");
            return  BtReader.getInstance().readArray(this.id,this.readingThreadID,size);
    }

    public byte[] read(){//must change to PACKETIZTION
        Log.v("unity","getBuffer Called");
        return  BtReader.getInstance().readPacket(this.id,this.readingThreadID);
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
            BtReader.getInstance().close(id,readingThreadID);

            if (socket != null) {
                Log.v("unity","socket is not null");
                socket.close();
                socket = null;
            }

            if(bufferedOutputStream != null) {
                bufferedOutputStream.flush();
                bufferedOutputStream.close();

            }






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




}
