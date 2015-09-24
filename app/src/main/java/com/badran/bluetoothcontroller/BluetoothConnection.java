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
import java.util.UUID;


public class BluetoothConnection {

    public final int id;



    public BluetoothConnection (int id) {
        this.id = id;

        setupData = new ConnectionSetupData();
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
        this.readingThreadID = readingThreadID;
        this.willRead = true;
    }


    public void setBluetoothDevice(BluetoothDevice bt) {
        setupData.setDevice(bt,this.id);
        setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingBluetoothDeviceReference;
    }


    public byte[] read(int size){
            return  BtReader.getInstance().readArray(this.id,this.readingThreadID,size);
    }

    public boolean isDataAvailable(){
        return  BtReader.getInstance().isDataAvailable(this.id,this.readingThreadID);
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



        BtReader.getInstance().close(id,readingThreadID);

    }



    public void setUUID (String SPP_UUID){

        setupData.SPP_UUID = SPP_UUID;
    }

    public void setMac (String mac){
        setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingMac;
        setupData.mac = mac;
    }

    public void setName (String name){//return data from the founded device
        setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingName;
        setupData.name = name;
    }

    public String getName (){//return data from the founded device
        BluetoothDevice d = setupData.getDevice();
        if(d != null)
            return setupData.getDevice().getName();
        return setupData.name;
    }

    public String getAddress (){
        BluetoothDevice d = setupData.getDevice();
        if(d != null)
            return setupData.getDevice().getAddress();
        return setupData.mac;
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
        Log.v("unity","sendBytes called");
        if(this.socket != null && this.bufferedOutputStream != null)
            BtSender.getInstance().addJob(bufferedOutputStream, msg);

    }

    public void initializeStreams() {


        if (this.willRead) {
            try {

                this.inputStream = this.socket.getInputStream();


            } catch (IOException e) {

                e.printStackTrace();
            }

            this.bufferReadder = new BufferedReader(new InputStreamReader(this.inputStream));
            BtReader.getInstance().enableReading(this);
            //btConnection.READER.startListeningThread();

        }

        if (true) {//add check for Sending
            Log.v("unity", "Initializing streams");
            try {


                this.outStream = this.socket.getOutputStream();
            } catch (IOException e) {
                Log.v("unity", "can't get input stream");


            }

            if (this.outStream != null) {

                this.bufferedOutputStream = new BufferedOutputStream(this.outStream);
                Log.v("unity", "bufferedOutputStream created and ready");
            }
            if (this.socket == null) Log.v("unity", " connect socket is null");
            if (this.bufferedOutputStream == null)
                Log.v("unity", "connect bufferedOutputStream is null");
        }


    }


}
