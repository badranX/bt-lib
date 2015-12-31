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

    private int id;
    private boolean isIdAssigned = false;

    //control data
    private boolean WillRead = true;
    private boolean WillSend = true;


    public boolean isSizePacketized = false;
    public boolean isEndBytePacketized = false;

    public byte packetEndByte;
    public int packetSize;

    public BluetoothSocket socket;

    public OutputStream outStream;

    public BufferedOutputStream bufferedOutputStream = null;

    public InputStream inputStream = null;

    public int readingThreadID = 0;//default Value

    //SetupData
    private final String UUID_SERIAL = "00001101-0000-1000-8000-00805F9B34FB";
    private static Map<BluetoothDevice,BluetoothConnection> map = new HashMap<BluetoothDevice, BluetoothConnection>();

    public  String name;
    public  String mac;


    public int bufferLength;
    public boolean isUsingMac;

    public String SPP_UUID;


    public boolean isDevicePicked;
    private BluetoothDevice device;






    public int maxBufferLength;
    public byte stopByte ;





    public enum ConnectionMode {
        UsingMac , UsingName, UsingBluetoothDeviceReference,UsingSocket,NotSet
    } public ConnectionMode connectionMode= ConnectionMode.NotSet;

    public enum ReadingMode {
        STRINGS , ENDBYTE, LENGTH
    } public ReadingMode readMode;


    //END SETUP DATA


    public BluetoothConnection (int id) {
        this.id = id;
        this.isIdAssigned = true;
        this.SPP_UUID = UUID_SERIAL;
    }

    public BluetoothConnection () {
        this.isIdAssigned = false;
        this.SPP_UUID = UUID_SERIAL;


    }



    public void enableReading(int readingThreadID){
        this.readingThreadID = readingThreadID;
        this.WillRead = true;
    }

    public void disableReading(){
        this.WillRead = false;
    }

    public void enableSending(){
        this.WillSend = true;
    }
    public void disableSending(){
        this.WillRead = false;
    }
    public boolean isReading(){return BtReader.getInstance().IsReading(this);}




    public byte[] read(int size){
            return  BtReader.getInstance().ReadArray(this.id, this.readingThreadID, size);
    }

    public void setPacketSize(int size){
        this.packetSize = size;
        this.isSizePacketized = true;
        this.isEndBytePacketized = false;
        BtReader.getInstance().setPacketSize(this.id, this.readingThreadID, size);
    }

    public void setEndByte(byte byt){
        this.packetEndByte = byt;
        this.isSizePacketized = false;
        this.isEndBytePacketized = true;
        BtReader.getInstance().setEndByte(this.id, this.readingThreadID, byt);
    }


    public boolean isDataAvailable(){
        return  BtReader.getInstance().IsDataAvailable(this.id, this.readingThreadID);
    }

    public byte[] read(){//must change to PACKETIZTION
        Log.v("unity", "getBuffer Called");
        return  BtReader.getInstance().ReadPacket(this.id, this.readingThreadID);
    }

    public  void connect( int trialsNumber) {

        Log.v("unity", "Connect called");
        BtInterface.getInstance().connect(this, trialsNumber);


    }



    public void close() {

        this.removeSocketServer();
        BtReader.getInstance().Close(id, readingThreadID);
        PluginToUnity.ControlMessages.DISCONNECTED.send(id);

    }




    public void setID(int id){
        this.id = id;
        this.isIdAssigned = true;


    }

    public int getID(){
        return this.id;
    }

    public void setUUID (String SPP_UUID){
        this.SPP_UUID = SPP_UUID;
    }

    public void setMac (String mac){
        this.connectionMode = ConnectionMode.UsingMac;
        this.mac = mac;
    }

    public void setName (String name){//return data from the founded device
        this.connectionMode = ConnectionMode.UsingName;
        this.name = name;
    }

    public String getName (){//return data from the founded device
        BluetoothDevice d = this.getDevice();
        if(d != null)
            return d.getName();
        return this.name;
    }

    public String getAddress (){
        BluetoothDevice d = this.getDevice();
        if(d != null)
            return d.getAddress();
        return this.mac;
    }

    public void sendString(String msg) {

        if(this.socket != null && this.bufferedOutputStream != null)
        BtSender.getInstance().addJob(bufferedOutputStream,msg.getBytes(),this.id);

    }


    public String sendChar(byte msg) {

        if(this.socket != null && this.bufferedOutputStream != null) {
            Log.v("unity","ready to sendChar");
            BtSender.getInstance().addJob(bufferedOutputStream, new byte[]{msg},this.id);
        }
        return this.name;
    }



    public void sendBytes(byte[] msg) {
        Log.v("unity","sendBytes called");
        if(this.socket != null && this.bufferedOutputStream != null)
            BtSender.getInstance().addJob(this.bufferedOutputStream, msg,this.id);

    }

    public void initializeStreams() {


        if (this.WillRead) {
            Log.v("unity", "Initializing streams and will read");
            try {
                this.inputStream = this.socket.getInputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }


            BtReader.getInstance().EnableReading(this);
            //btConnection.READER.startListeningThread();

        }

        if (this.WillSend) {//add check for Sending
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


    //SETUP DATA

    public static BluetoothConnection getInstFromDevice(BluetoothDevice device){
        if(map.containsKey(device))
            return map.get(device);
        return null;
    }


    public void setDevice(BluetoothDevice device){
        this.device = device;
        this.connectionMode = ConnectionMode.UsingBluetoothDeviceReference;

        map.put(device,this);
    }
    public void setSucket(BluetoothSocket socket,int id){
        this.device = socket.getRemoteDevice();
        this.connectionMode = ConnectionMode.UsingSocket;

        map.put(device, this);
    }
    public BluetoothDevice getDevice(){
        return this.device;
    }

    public void removeSocketServer(){
        //It's only produced by a server and after disconnecting no need to save the socket
        //a device reference already there since the server found the remote device
        //No need to change connection mode in unity since it's only needed here, and the old connection mode won't be commited, because it's not going to change there
        if(this.connectionMode == ConnectionMode.UsingSocket) this.connectionMode = ConnectionMode.UsingBluetoothDeviceReference;
    }
}
