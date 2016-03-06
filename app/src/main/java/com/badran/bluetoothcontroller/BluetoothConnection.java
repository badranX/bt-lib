package com.badran.bluetoothcontroller;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;



import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


public class BluetoothConnection {

    private int id;
    private boolean isIdAssigned = false;

    //control data
    private boolean WillRead = true;//== Is the unity needs this instance to read or not, it doesn't mean that it actually able to read or failed to read
    private boolean WillSend = true;//== Is the unity needs this instance to send or not, it doesn't mean that it actually able to send or failed to send

    //== Is the unity needs this instance to be connected or not, it doesn't mean that it actually connected or failed to connect
    //it's volatile because the connection thread check this to see if this instance still needs to connect
    volatile boolean WillConnect = false;
    boolean isConnected = false;
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
    public String SPP_UUID;
    private BluetoothDevice device;

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


    String getUUID () {
        return this.SPP_UUID;
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
        return  BtReader.getInstance().ReadPacket(this.id, this.readingThreadID);
    }

    public  void connect( int trialsNumber, int time, boolean allowPageScan) {
        this.WillConnect = true;
        BtInterface.getInstance().connect(this, trialsNumber, time, allowPageScan);
    }

    void releaseResources(){
        if (!BtReader.getInstance().Close(this.id, this.readingThreadID))
        {
            try
            {
                if (this.socket != null) {
                    this.socket.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            try
            {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        BtSender.getInstance().addCloseJob(this.bufferedOutputStream);
    }

    public void close()
    {
        BtInterface.getInstance().OnDeviceClosing(this);
        this.WillConnect = false;
        removeSocketServer();
        releaseResources();
        this.RaiseDISCONNECTED();
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
        BtSender.getInstance().addJob(bufferedOutputStream,msg.getBytes(),this);

    }


    public String sendChar(byte msg) {

        if(this.socket != null && this.bufferedOutputStream != null) {
            BtSender.getInstance().addJob(bufferedOutputStream, new byte[]{msg},this);
        }
        return this.name;
    }



    public void sendBytes(byte[] msg) {
        if(this.socket != null && this.bufferedOutputStream != null)
            BtSender.getInstance().addJob(this.bufferedOutputStream, msg,this);

    }

     void initializeStreams() {


        if (this.WillRead) {
            try {
                this.inputStream = this.socket.getInputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }


            BtReader.getInstance().EnableReading(this);
            //btConnection.READER.startListeningThread();

        }

        if (this.WillSend) {//add check for Sending
            try {
                this.outStream = this.socket.getOutputStream();
            } catch (IOException e) {
                Log.v("unity", "can't get input stream");
            }

            if (this.outStream != null) {
                this.bufferedOutputStream = new BufferedOutputStream(this.outStream);
            }

        }


    }


    //SETUP DATA

    static BluetoothConnection getInstFromDevice(BluetoothDevice device){
        if(map.containsKey(device))
            return map.get(device);
        return null;
    }


    void setDevice(BluetoothDevice device){
        this.device = device;
        this.connectionMode = ConnectionMode.UsingBluetoothDeviceReference;

        map.put(device,this);
    }
    void setSucket(BluetoothSocket socket){
        this.device = socket.getRemoteDevice();
        this.connectionMode = ConnectionMode.UsingSocket;

        map.put(device, this);
    }
    BluetoothDevice getDevice(){
        return this.device;
    }

    void removeSocketServer(){
        //It's only produced by a server and after disconnecting no need to save the socket
        //a device reference already there since the server found the remote device
        //No need to change connection mode in unity since it's only needed here, and the old connection mode won't be commited, because it's not going to change there
        if(this.connectionMode == ConnectionMode.UsingSocket) this.connectionMode = ConnectionMode.UsingBluetoothDeviceReference;
    }

    void RaiseCONNECTED(){
        this.isConnected = true;
        PluginToUnity.ControlMessages.CONNECTED.send(this.getID());
    }
    void RaiseDISCONNECTED(){
        this.isConnected = false;
        PluginToUnity.ControlMessages.DISCONNECTED.send(this.getID());
    }
    void RaiseNOT_FOUND(){
        PluginToUnity.ControlMessages.NOT_FOUND.send(this.getID());
    }
    void RaiseMODULE_OFF (){
        PluginToUnity.ControlMessages.MODULE_OFF.send(this.getID());
    }
    void RaiseSENDING_ERROR (){
        PluginToUnity.ControlMessages.SENDING_ERROR.send(this.getID());
    }
    void RaiseDISCOVERY_STARTED() {

    }
    //TODO add BtReader Raising Functionality
}

