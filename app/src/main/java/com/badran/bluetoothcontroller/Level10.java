package com.badran.bluetoothcontroller;

import java.io.IOException;
import java.util.UUID;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class Level10 {
    //Log
    private final String TAG = "PLUGIN";
    private final String CHINESE_CONNECTION_FAILED ="Chinese connection failed";
    //log

    private static Level10 instance = null;

    protected Level10() {
        // Exists only to defeat instantiation.

    }

    public static Level10 getInstance() {
        if (instance == null) {
            instance = new Level10();
        }
        return instance;
    }

    @TargetApi(10)
    public BluetoothSocket createRfcommSocket(BluetoothDevice device, UUID uuid) {
        BluetoothSocket tmp = null;
        try {

            tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.v(TAG, CHINESE_CONNECTION_FAILED);
        }
        return tmp;
    }

}
