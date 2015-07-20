package com.badran.bluetoothcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by a on 6/12/15.
 */
public class ConnectionSetupData {
    private final String UUID_SERIAL = "00001101-0000-1000-8000-00805F9B34FB";

//    private final int id;




    private static Map<BluetoothDevice,Integer> map = new HashMap<BluetoothDevice, Integer>();

    public static Integer getIdFromDevice(BluetoothDevice device){
        if(map.containsKey(device))
            return map.get(device);
        return null;

    }
    public void setDevice(BluetoothDevice device,int id){

        this.device = device;
        this.connectionMode = ConnectionMode.UsingBluetoothDeviceReference;
        map.put(device,id);
    }
    public void setSucket(BluetoothSocket socket,int id){

        this.device = socket.getRemoteDevice();
        this.connectionMode = ConnectionMode.UsingSocket;
        map.put(device,id);
    }
    public BluetoothDevice getDevice(){
        return this.device;
    }
    public  String name;
    public  String mac;


    public int bufferLength;
    public boolean isUsingMac;

    public String SPP_UUID = UUID_SERIAL;


    public boolean isDevicePicked;
    private BluetoothDevice device;






    public int maxBufferLength;
    public byte stopByte ;


    public boolean enableSending = true;
    public boolean enableReading = false;


    public enum ConnectionMode {
        UsingMac , UsingName, UsingBluetoothDeviceReference,UsingSocket,NotSet
    } public ConnectionMode connectionMode= ConnectionMode.NotSet;

    public enum ReadingMode {
        STRINGS , ENDBYTE, LENGTH
    } public ReadingMode readMode;



}
