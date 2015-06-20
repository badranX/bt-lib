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
//import android.content.Intent;
import android.content.IntentFilter;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static Set<BluetoothConnection> noUse = new HashSet<BluetoothConnection>();

    public static BluetoothConnection createBlutoothConnectionObject( String name, boolean isUsingMac) {


        BluetoothConnection btConnection = new BluetoothConnection();
        noUse.add(btConnection);

        if (isUsingMac)
            btConnection.setupData.mac = name;
        else btConnection.setupData.name = name;

        btConnection.setupData.isUsingMac = isUsingMac;

        return  btConnection;
    }








    //FROM UNITY

    public static void askEnableBluetooth() {

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

        UnityPlayer.currentActivity.startActivity(enableBtIntent);


    }

    public static boolean enableBluetooth() {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.enable();
        } else return false;


    }


    public static boolean disableBluetooth() {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.disable();
        } else return false;


    }

    public static boolean isBluetoothEnabled() {

        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.isEnabled();
        } else return false;
    }


    //connection setup and control methods





}

