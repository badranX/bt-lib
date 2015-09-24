package com.badran.bluetoothcontroller;

/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 */


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.unity3d.player.UnityPlayer;

import android.content.BroadcastReceiver;
import android.content.Context;
//import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;


import java.sql.Connection;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Bridge {


    private static BluetoothAdapter mBluetoothAdapter;

    private static Map<Integer, BluetoothConnection> map = new HashMap<Integer, BluetoothConnection>();

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
    public static BluetoothConnection createBlutoothConnectionObject( int id) {
        BluetoothConnection btConnection = new BluetoothConnection(id);
        return  btConnection;
    }


    //FROM UNITY


    public  void askEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        UnityPlayer.currentActivity.startActivity(enableBtIntent);

    }

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
     BluetoothDevicePickerReceiver mBluetoothPickerReceiver = new BluetoothDevicePickerReceiver();
    public  void showDevices () {

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
                BtDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                PluginToUnity.ControlMessages.DEVICE_PICKED.send();
                //PluginToUnity.ControlMessages.DEVICE_DISCOVERED.send();
                UnityPlayer.currentActivity.unregisterReceiver(this);
            }


        }
    }




    public  void startServer( String unityUUID) {

        Log.v("unity","Server Starting Called");

        BtInterface.getInstance().initServer(unityUUID);


    }

    private  BluetoothDevice  BtDevice;
    public  BluetoothConnection getPickedDevice (int id){
        if(BtDevice != null) {
            BluetoothConnection btConnection = new BluetoothConnection(id);
            btConnection.setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingBluetoothDeviceReference;
            btConnection.setupData.setDevice(BtDevice, id);
            return btConnection;
        }
        return  null;
    }

    public  BluetoothConnection getDiscoveredDevice (int id){
        Log.v("Accepting","get DiscoveredDevice Called");
        if(PluginToUnity.ControlMessages.socket != null) {

            BluetoothConnection btConnection = new BluetoothConnection(id);
            btConnection.socket = PluginToUnity.ControlMessages.socket;
            btConnection.setupData.connectionMode = ConnectionSetupData.ConnectionMode.UsingSocket;
            btConnection.setupData.setSucket(PluginToUnity.ControlMessages.socket, id);


            Boolean s = btConnection == null;
            Log.v("Accepting",s.toString());
            return btConnection;
        }return null;
    }

}

