package com.badran.bluetoothcontroller;


import android.bluetooth.BluetoothAdapter;
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

    private int id = 0;//0 means not assigned yet
    private boolean IdAssigned = false;

    //control data
    private boolean WillRead = true;//== Is the unity needs this instance to read or not, it doesn't mean that it actually able to read or failed to read
    private boolean WillSend = true;//== Is the unity needs this instance to send or not, it doesn't mean that it actually able to send or failed to send

    //== Is the unity needs this instance to be connected or not, it doesn't mean that it actually connected or failed to connect
    //it's volatile because the connection thread check this to see if this instance still needs to connect
    boolean isConnected = false;
    boolean isSizePacketized = false;
    boolean isEndBytePacketized = false;

    byte packetEndByte;
    int packetSize;
     BluetoothSocket socket;

     OutputStream outStream;

     BufferedOutputStream bufferedOutputStream = null;

     InputStream inputStream = null;

     int readingThreadID = 0;//default Value

    //SetupData
    private final String UUID_SERIAL = "00001101-0000-1000-8000-00805F9B34FB";
    private static Map<BluetoothDevice,BluetoothConnection> map = new HashMap<BluetoothDevice, BluetoothConnection>();
    private static Map<String,BluetoothConnection> mapAddress = new HashMap<String, BluetoothConnection>();
    public  String name;
    public  String mac;
    public String SPP_UUID;
    private BluetoothDevice device;

     enum ConnectionMode {
        UsingMac(0) , UsingName(1), UsingBluetoothDeviceReference(2),UsingSocket(3),NotSet(4);

         private final int value;
         private ConnectionMode(int value) {
             this.value = value;
         }

         public int getValue() {
             return value;
         }
    }  ConnectionMode connectionMode= ConnectionMode.NotSet;


    public int getConnectionMode() {
        return connectionMode.getValue();
    }
    //END SETUP DATA

    public BluetoothConnection (int id) {
        this.id = id;
        this.IdAssigned = true;
        this.SPP_UUID = UUID_SERIAL;
    }

    public BluetoothConnection () {
        this.IdAssigned = false;
        this.SPP_UUID = UUID_SERIAL;
    }


    String getUUID () {
        return this.SPP_UUID;
    }
    public void enableReading(int readingThreadID){
        if(this.isConnected){
            if(this.socket != null && this.inputStream == null) {
                try {
                    this.inputStream = this.socket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            BtReader.getInstance().EnableReading(this);
        }
        this.readingThreadID = readingThreadID;
        this.WillRead = true;
    }

    public void disableReading(){
        if(this.isConnected) {
            BtReader.getInstance().Close(this.id, this.readingThreadID);
        }
        this.WillRead = false;
    }

    public void enableSending(){
        this.WillSend = true;
    }
    public void disableSending(){
        this.WillSend = false;
    }
    public boolean isReading(){return BtReader.getInstance().IsReading(this);}




    public byte[] read(int size){
            return  BtReader.getInstance().ReadArray(this.id, this.readingThreadID, size);
    }

    public byte[] readAllPackets(){
        return  BtReader.getInstance().ReadAllPackets(this.id, this.readingThreadID);
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

    public void normal_connect(boolean isChinese,boolean isSecure){
        if(this.isConnected) return;
        BtInterface.getInstance().normal_connect(this,isChinese,isSecure);
    }
    public  void connect( int trialsNumber, int time, boolean allowPageScan) {
        if(this.isConnected) return;
        BtInterface.getInstance().connect(this, trialsNumber, time, allowPageScan);
    }



    public void brutal_connect(int trialsNumber, int time, boolean allowPageScan){
        if(this.isConnected) return;
        BtInterface.getInstance().brutal_connect(this, trialsNumber, time, allowPageScan);
    }
    private void reset(){
        //This must be called when this instance will be used for a different Remote Device
        if(this.isConnected) this.close();
    }
    void releaseResources() {
        BtReader.getInstance().Close(this.id, this.readingThreadID);
        //BtSender.getInstance().addCloseJob(this.bufferedOutputStream);
        if (this.bufferedOutputStream != null) {
            try {
                this.bufferedOutputStream.flush();
            } catch (IOException e) {
            }

            try {
                this.bufferedOutputStream.close();
            } catch (IOException e) {
                Log.e("unity", "bufferedOutputStream closing");
                e.printStackTrace();
            }
            this.bufferedOutputStream = null;
        }

        if (this.outStream != null) {
            try {
                this.outStream.flush();
            } catch (IOException e) {
            }


            try {

                if (this.outStream != null) {
                    this.outStream.close();
                }
            } catch (IOException e) {
                Log.e("unity", "outStream closing");

                e.printStackTrace();
            }
            this.outStream = null;
        }

        if (this.inputStream != null) {
            try {

                this.inputStream.close();

            }catch(IOException e)
            {
                Log.e("unity", "inputStream closing");

                e.printStackTrace();
            }
            this.inputStream = null;
        }


            int count =0;
            while(count <3) {
                try {
                    if (this.socket != null) {
                        this.socket.close();

                    }
                } catch (IOException e) {
                    Log.e("unity","socket closing");

                    e.printStackTrace();

                    count++;
                    continue;
                }

                break;
            }
        this.socket = null;

    }

    public void close()
    {
        removeSocketServer();
        releaseResources();
        BtInterface.getInstance().OnDeviceClosing(this);

        this.RaiseDISCONNECTED();
    }

    static void closeAll(){
        for (Map.Entry<BluetoothDevice, BluetoothConnection> entry : map.entrySet())
        {
            entry.getValue().close();
        }
    }

    public void setID(int id){
        this.id = id;
        this.IdAssigned = true;
    }

    public int getID(){
        return this.id;
    }

    public void setUUID (String SPP_UUID){
        reset();
        this.SPP_UUID = SPP_UUID;
    }

    public void setMac (String mac){
        if(BluetoothAdapter.checkBluetoothAddress(mac)){
            BluetoothDevice dev = BtInterface.getInstance().mBluetoothAdapter.getRemoteDevice(mac);
            this.setDevice(dev);
        }else {
            this.mac = mac;
            this.connectionMode = ConnectionMode.UsingMac;
        }

        reset();

        if(this.isConnected)this.close();


    }

    public void setName (String name){//return data from the founded device
        this.name = name;
        this.connectionMode = ConnectionMode.UsingName;
        reset();
        if(this.isConnected)this.close();


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
    boolean isBufferDynamic = true;
    int bufferSize = 1024;

    public void setBufferSize(int size){
        if(size <= 0) {
            throw new IllegalArgumentException("buffer is equal or less than zero");
        }
        this.isBufferDynamic = false;
        this.bufferSize = size;
    }

    public void sendBytes(byte[] msg) {
        if(this.socket != null && this.bufferedOutputStream != null)
            BtSender.getInstance().addJob(this.bufferedOutputStream, msg,this);

    }
    public void sendBytes_Blocking(byte[] msg,BluetoothConnection btConnection){
        try
        {
            if (btConnection.bufferedOutputStream != null)
            {
                btConnection.bufferedOutputStream.write(msg);
                btConnection.bufferedOutputStream.flush();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

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
    public boolean startDiscovery (){
        return BtInterface.getInstance().startDiscovery();
    }

    public void instanceRemoved() {
        if(map.containsKey(this)) {
             map.remove(this);
        }

        if(mapAddress.containsKey(this.getAddress())){
            mapAddress.remove(this.getAddress());
        }
        this.close();
    }


    //SETUP DATA
    static BluetoothConnection getInstFromDevice(BluetoothDevice device){
        if(map.containsKey(device)) {
            return map.get(device);
        }
        if(mapAddress.containsKey(device.getAddress())){
            return mapAddress.get(device.getAddress());
        }
        return null;
    }


    void setDevice(BluetoothDevice device){
        this.device = device;
        this.connectionMode = ConnectionMode.UsingBluetoothDeviceReference;

        map.put(device,this);
        mapAddress.put(device.getAddress(),this);
    }
    void setSucket(BluetoothSocket socket){
        this.socket = socket;
        this.device = socket.getRemoteDevice();
        this.connectionMode = ConnectionMode.UsingSocket;

        map.put(device, this);
        mapAddress.put(device.getAddress(),this);
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


    //TODO SYNCHRONIZATION BETWEEN RaiseCONNECTED/RaiseDISCONNECTED RESEARCH
    synchronized void RaiseCONNECTED() {
        if (!this.isConnected){
            PluginToUnity.ControlMessages.CONNECTED.send(this.getID());
        }
        this.isConnected = true;
    }

    synchronized void RaiseDISCONNECTED(){


        //TODO don't disconnect non connected yet device
        if(this.isConnected) {

            this.removeSocketServer();//TODO RESEARCH //To allow it to try a newer socket when it try to connect again
            PluginToUnity.ControlMessages.DISCONNECTED.send(this.getID());
        }
        this.isConnected = false;
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




