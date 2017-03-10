package com.techtweaking.bluetoothcontroller;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


import com.techtweaking.library.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class BluetoothConnection {

    private static final String TAG = "PLUGIN . UNITY";

    private int id = 0;//0 means not assigned yet

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

    private OutputStream outStream;

    private BufferedOutputStream bufferedOutputStream = null;

    InputStream inputStream = null;

    int readingThreadID = 0;//default Value

    //SetupData
    //private final String UUID_SERIAL = "00001101-0000-1000-8000-00805F9B34FB";

    //TODO A BluetoothDevice might be of use to multiple BluetoothConnection instances
    //TODO more testing on Devices Data Structures
    private static final Map<String,Set<BluetoothConnection>> mapAddress = new HashMap<String, Set<BluetoothConnection>>();
    String name;
    String mac;
    private String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private BluetoothDevice device;

    enum ConnectionMode {
        UsingMac , UsingName, UsingBluetoothDeviceReference,UsingSocket,NotSet;

         /*
         private final int value;
         private ConnectionMode(int value) {
             this.value = value;
         }

         public int getValue() {
             return value;
         }
         */
    }  ConnectionMode connectionMode= ConnectionMode.NotSet;



    //END SETUP DATA

    public BluetoothConnection (int id) {
        this.id = id;
    }

    public BluetoothConnection () {
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

    /*
    Using Unity send messages instead
    public boolean isDataAvailable(){
        return  BtReader.getInstance().IsDataAvailable(this.id, this.readingThreadID);
    }

    */

    public byte[] read(){//must change to PACKETIZTION
        return  BtReader.getInstance().ReadPacket(this.id, this.readingThreadID);
    }

    public void normal_connect(boolean isChinese,boolean isSecure){
        if(this.isConnected) return;
        BtInterface.getInstance().normal_connect(this,isChinese,isSecure);
    }
    public  void connect( int trialsNumber, int time, boolean allowPageScan,boolean startNormalConnection,boolean swtitchNormalBrutal) {
        if(this.isConnected) return;
        BtInterface.getInstance().connect(this, trialsNumber, time, allowPageScan,startNormalConnection,swtitchNormalBrutal);
    }


    private void reset(){
        //TODO must change map and mapAdress
        //This must be called when this instance will be used for a different Remote Device
        if(this.isConnected){
            this.close();

        }
    }
    private void releaseResources() {
        BtReader.getInstance().Close(this.id, this.readingThreadID);
        //BtSender.getInstance().addCloseJob(this.bufferedOutputStream);
        if (this.bufferedOutputStream != null) {
            IOUtils.flushQuietly(this.bufferedOutputStream);
            IOUtils.closeQuietly(this.bufferedOutputStream);
            this.bufferedOutputStream = null;
        }

        if (this.outStream != null) {
            IOUtils.flushQuietly(this.outStream);
            IOUtils.closeQuietly(this.outStream);
            this.outStream = null;
        }

        if (this.inputStream != null) {
            IOUtils.closeQuietly(this.inputStream);
            this.inputStream = null;
        }


        if (this.socket != null) {
            IOUtils.closeQuietly(this.socket);
            this.socket = null;
        }
    }

    public void close()
    {
        removeSocketServer();
        releaseResources();
        BtInterface.getInstance().OnDeviceClosing(this);

        this.RaiseDISCONNECTED();
    }

    static void closeAll(){
        for (Map.Entry<String, Set<BluetoothConnection>> entry : mapAddress.entrySet())
        {
            for(BluetoothConnection c : entry.getValue()){
                c.close();
            }
        }
    }

    public void setID(int id){
        this.id = id;
        //this.idAssigned = true; I used such a variable before, it was useless
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
        if(d != null) {
            return d.getName();

        }

        return this.name;
    }

    //TODO NOT: IMPORTANT :Public because Unity access it
    public String getAddress (){
        BluetoothDevice d = this.getDevice();
        if(d != null)
            return d.getAddress();
        return this.mac;
    }

// --Commented out by Inspection START (22/09/16, 11:25 PM):
//    public void sendString(String msg) {
//
//        if(this.socket != null && this.bufferedOutputStream != null)
//        BtSender.getInstance().addJob(bufferedOutputStream,msg.getBytes(),this);
//
//    }
// --Commented out by Inspection STOP (22/09/16, 11:25 PM)


    // --Commented out by Inspection START (22/09/16, 11:25 PM):
//    public String sendChar(byte msg) {
//
//        if(this.socket != null && this.bufferedOutputStream != null) {
//            BtSender.getInstance().addJob(bufferedOutputStream, new byte[]{msg},this);
//        }
//        return this.name;
//    }
// --Commented out by Inspection STOP (22/09/16, 11:25 PM)
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
        // WTF : it's possible that user chose not to allow sending (bufferedOutputStream == null)
        // WTF : it's possible that BluetoothDevice is still disconnected (socket == null)
        // WTF : I can use this.WillRead & this.WillSend
        if(this.socket != null && this.bufferedOutputStream != null)
            BtSender.getInstance().addJob(this.bufferedOutputStream, msg, this);

    }
    public void sendBytes_Blocking(byte[] msg){

        if (this.bufferedOutputStream != null) // WTF : User can choose to prevent sending (bufferedOutputStream == null)
        {
            try {
                this.bufferedOutputStream.write(msg);
            }catch (IOException e)
            {
                this.RaiseSENDING_ERROR();
                Log.w("PLUGIN . UNITY", "failed sending data while write/sending data", e);

            }
            try {
                this.bufferedOutputStream.flush();
            }catch (IOException e)
            {
                Log.w("PLUGIN . UNITY", "failed flushing the buffer while write/sending data", e);
            }
        }



    }


    void initializeStreams() {
        if (this.WillRead) {
            try {
                this.inputStream = this.socket.getInputStream();

            } catch (IOException e) {
                Log.e("PLUGIN . UNITY", "failed creating InputStream after connecting the device : " + this.getName() + " with address : " + this.getAddress(),e );
            }

            BtReader.getInstance().EnableReading(this);
        }

        if (this.WillSend) {//add check for Sending
            try {
                this.outStream = this.socket.getOutputStream();
            } catch (IOException e) {
                Log.e("PLUGIN . UNITY", "failed creating OutputStream for the device : " + this.getName() + " with address : " + this.getAddress(), e );
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
        BluetoothDevice tmp = this.getDevice();


        if(tmp != null ){
            Set<BluetoothConnection> tmpSet = mapAddress.get(tmp.getAddress());
            if(tmpSet != null) tmpSet.remove(this);
        }
        //TODO NOT : THE USER HAS TO MAKE SURE HIS DEVICE IS CLOSED BEFORE DESTROYING IT (NULL IT )this.close();
        //TODO NOT : SOMETIMES 2 or more UNITY BLUETOOTHDevices refer to one Physical device
    }


    //SETUP DATA
    //TODO I assumed One Device With One BtConnection Instantce, it's possible to have otherwise
    //TODO in UNITY You're not allowed to do so.
    //TODO Last
    static Set<BluetoothConnection> getInstFromDevice(BluetoothDevice device){


        return mapAddress.get(device.getAddress());

    }

    //TODO more testing on Devices Data Structures
    void setDevice(BluetoothDevice device){//TODO return false when it finds any BluetoothConnection instace has the same device
        if(device.equals(this.getDevice())) return;
        BluetoothDevice preDev = this.getDevice();
        if(preDev != null) {
            BluetoothConnection.mapAddress.get(preDev.getAddress()).remove(this);
        }

        this.device = device;
        this.connectionMode = ConnectionMode.UsingBluetoothDeviceReference;
        Set<BluetoothConnection> tmp = BluetoothConnection.getInstFromDevice(device);
        if(tmp == null) {

            tmp = new HashSet<BluetoothConnection>();
            tmp.add(this);
            mapAddress.put(device.getAddress(), tmp);
        }else {

            Log.w(TAG,"Trying to set the same physical device for different Unity BluetoothDevice instances ");
            tmp.add(this);
        }
    }
    void setSocket(BluetoothSocket socket){
        this.socket = socket;
        this.setDevice(socket.getRemoteDevice());
        this.connectionMode = ConnectionMode.UsingSocket;

    }
    BluetoothDevice getDevice(){
        return this.device;
    }

    private void removeSocketServer(){
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

    private synchronized void RaiseDISCONNECTED(){
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

    void RaiseConnectionError (String msg) {
        PluginToUnity.ControlMessages.CONNECTION_ERROR.send(this.getID(), msg);
    }
    void RaiseSENDING_ERROR (){
        PluginToUnity.ControlMessages.SENDING_ERROR.send(this.getID());
    }

    //TODO add BtReader Raising Functionality
}


