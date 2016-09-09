package com.badran.bluetoothcontroller;

/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 */


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.unity3d.player.UnityPlayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;


import java.util.LinkedList;
import java.util.Set;

public class Bridge {


    private static BluetoothAdapter mBluetoothAdapter;


    private static Bridge instance = null;


    protected Bridge() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Exists only to defeat instantiation.
    }

    public static Bridge getInstance() {
        if (instance == null) {
            instance = new Bridge();
        }
        return instance;
    }

    public  String test (){
        return "XXXXXXXXXXXXXYYYYYYYYYYYYYYYYXIIIIIII";
        //com.badran.library.NativeBuffer.add();
    }
    public BluetoothConnection createBlutoothConnectionObject( int id) {
        BluetoothConnection btConnection = new BluetoothConnection(id);
        return  btConnection;
    }


    //FROM UNITY


    public  void askEnableBluetooth() {

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            UnityPlayer.currentActivity.startActivityForResult(enableBtIntent,8);
    }

    BtStateReceiver btStateReceiver;
    public void registerStateReceiver (){
        if(btStateReceiver == null) btStateReceiver = new BtStateReceiver();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

        UnityPlayer.currentActivity.registerReceiver(btStateReceiver, filter);

    }
    public void deRegisterStateReceiver (){
        try {
            if(btStateReceiver != null)
                UnityPlayer.currentActivity.unregisterReceiver(btStateReceiver);
        }catch(IllegalArgumentException e){
            //Ignore
        }

    }

    private class BtStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        PluginToUnity.ControlMessages.BLUETOOTH_OFF.send();
                        break;

                    case BluetoothAdapter.STATE_ON:
                        PluginToUnity.ControlMessages.BLUETOOTH_ON.send();
                        break;

                }
            }
        }
    };

    public  boolean enableBluetooth() {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.enable();
        } else return false;


    }


    public  boolean disableBluetooth() {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.disable();
        } else return false;

    }

    public  boolean isBluetoothEnabled() {

        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.isEnabled();
        } else return false;
    }


    // show devices
    BluetoothDevicePickerReceiver mBluetoothPickerReceiver;
    public  void showDevices () {
        if(mBluetoothPickerReceiver == null) mBluetoothPickerReceiver = new BluetoothDevicePickerReceiver();
            IntentFilter deviceSelectedFilter = new IntentFilter();
            deviceSelectedFilter.addAction(BluetoothDevicePicker.ACTION_DEVICE_SELECTED);
            UnityPlayer.currentActivity.registerReceiver(mBluetoothPickerReceiver, deviceSelectedFilter);


        UnityPlayer.currentActivity.startActivity(new Intent(BluetoothDevicePicker.ACTION_LAUNCH)
                .putExtra(BluetoothDevicePicker.EXTRA_NEED_AUTH, false)
                .putExtra(BluetoothDevicePicker.EXTRA_FILTER_TYPE, BluetoothDevicePicker.FILTER_TYPE_ALL)
                .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));

    }

     private class BluetoothDevicePickerReceiver extends BroadcastReceiver implements  BluetoothDevicePicker  {

        /*
         * (non-Javadoc)
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            if (ACTION_DEVICE_SELECTED.equals(intent.getAction())) {
                // context.unregisterReceiver(this);
                PickedBtDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(PickedBtDevice != null) {
                    PluginToUnity.ControlMessages.DEVICE_PICKED.send();
                    UnityPlayer.currentActivity.unregisterReceiver(this);
                }
            }
        }

    }


    public boolean startDiscovery(){
        return BtInterface.getInstance().startDiscovery();
    }

    public void makeDiscoverable(int time) {
        BtInterface.getInstance().makeDiscoverable(time);
    }
    public  void initServer( String unityUUID,int time,boolean oneDevice) {
        BtInterface.getInstance().initServer(unityUUID, time,oneDevice);
    }

    private  BluetoothDevice PickedBtDevice;
    public  BluetoothConnection getPickedDevice (int id){
        if(PickedBtDevice != null) {
            BluetoothConnection btConnection = new BluetoothConnection(id);
            btConnection.connectionMode = BluetoothConnection.ConnectionMode.UsingBluetoothDeviceReference;
            btConnection.setDevice(PickedBtDevice);

            return btConnection;
        }
        return  null;
    }

    //Serever Discoverd device
    public  BluetoothConnection getDiscoveredDeviceForServer (int id){
        if(PluginToUnity.socket != null) {

            BluetoothConnection btConnection = new BluetoothConnection(id);
            btConnection.socket = PluginToUnity.socket;
            btConnection.connectionMode = BluetoothConnection.ConnectionMode.UsingSocket;
            btConnection.setSucket(PluginToUnity.socket);
            return btConnection;
        }return null;
    }

    public  BluetoothConnection[]  getPairedDevices (){
        Set<BluetoothDevice> setPairedDevices;

        setPairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothConnection[] returned = new BluetoothConnection[setPairedDevices.size()];
        int i =0;
        for (BluetoothDevice pairedDevice : setPairedDevices) {

            BluetoothConnection btConnection = new BluetoothConnection();
            btConnection.setDevice(pairedDevice);
            returned[i] = btConnection;
            i++;
        }
        return returned;

    }

    public byte[] TEST(byte[] x) {

        x[0] =100;
        int startingIndex = 4;
         LinkedList<Integer> marks = new LinkedList<Integer>() ;
        marks.add(10);

        marks.add(30);
        marks.add(40);
        int marksSize = marks.size();
        byte [] r = new byte[(marksSize -1 )*4 + 4];

         IntToBytes(r,0,marksSize - 1);

        if(marksSize > 1) {
            //Insert all Marks inside the Array as header
            int index = 4;//the first 4 used by the Num. of marks

           // int reached_size = headerSize;


            while(marks.size() > 1 ) {
                int bytTail = marks.poll();
                //int packetSize = bytTail - head + (bytTail < head ? n : 0);
                IntToBytes(r, index, bytTail);//every indx will contain the size from zero up to the last indx of the packet
                //reached_size += packetSize;
                index += 4;
            }

        }

        marks.clear();

     return r;


    }
    void IntToBytes(byte[] out, int index,int val){
        out[index] = (byte)val;
        out[index + 1] = (byte)(val >>> 8);
        out[index + 2] = (byte)(val >>> 16);
        out[index + 3] = (byte)(val >>> 24);
    }

    // The following commented function doesn't allow dublicate instances
//    public  BluetoothConnection[]  getPairedDevices (){
//        Set<BluetoothDevice> setPairedDevices;
//
//        setPairedDevices = mBluetoothAdapter.getBondedDevices();
//        BluetoothConnection[] returned = new BluetoothConnection[setPairedDevices.size()];
//        int i =0;
//        for (BluetoothDevice pairedDevice : setPairedDevices) {
//            BluetoothConnection bt = BluetoothConnection.getInstFromDevice(pairedDevice);
//            if(bt != null) {
//                returned[i] = bt;
//            }else {
//                BluetoothConnection btConnection = new BluetoothConnection();
//                btConnection.setDevice(pairedDevice);
//                returned[i] = btConnection;
//            }
//            i++;
//        }
//        return returned;
//    }

    /*
    //Not Server Discoverd devices, but general devices
    //When id == 0 it means that Unity already has a reference, and it need to skip
    public  BluetoothConnection  getNextDiscoveredDevice (int id){
        BluetoothConnection bt =PluginToUnity.getNextDiscoveredDevice();
        if(bt != null && id != 0) {
            bt.setID(id);
            return bt;
        }return null;
    }
*/
    public void OnDestroy(){
        deRegisterStateReceiver();
        BtInterface.getInstance().OnDestroy();
            BluetoothConnection.closeAll();
    }
}

