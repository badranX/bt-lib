package com.badran.bluetoothcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by a on 6/12/15.
 */
public class ConnectionSetupData {
    private final String UUID_SERIAL = "00001101-0000-1000-8000-00805F9B34FB";


    static enum MODES {
        mode0, mode1, mode2, mode3

    }public MODES mode;


    public  String name;
    public  String mac;

    public boolean isUsingMac;

    public String SPP_UUID = UUID_SERIAL;


    public boolean isDevicePicked;
    public BluetoothDevice device;






    public int maxBufferLength;
    public byte stopByte ;


    public boolean enableSending = true;
    public boolean enableReading = false;





}
