package com.techtweaking.libextra;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.Field;

public class IOUtils {
    private static final String TAG = "PLUGIN . UNITY";
    public static void flushQuietly(Flushable input) {
        try {
            if (input != null) {
                input.flush();
            }
        } catch (IOException e) {
            Log.w(TAG, "Failure to flush stream: ", e);
        }
    }
    public static void closeQuietly(Closeable input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Failure to close stream: ", e);
        }
    }

    //BUG_FIX: https://stackoverflow.com/questions/22022001/android-bluetooth-connection-fails-after-489-successful-connections/27873675#27873675
    public static synchronized void close_socket(BluetoothSocket socket) {
        if (socket != null) {
            try {
                clearFileDescriptor(socket);
                //clearLocalSocket();
                socket.close();
            }
            catch (IOException e) {
                Log.w(TAG, "Socket could not be cleanly closed.");
            }
        }
    }

    public static synchronized void close_serverSocket(BluetoothServerSocket socket) {
        if (socket != null) {
            try {
                clearFileDescriptor_ForServerSocket(socket);
                //clearLocalSocket();
                socket.close();
            }
            catch (IOException e) {
                Log.w(TAG, "Socket could not be cleanly closed.");
            }
        }
    }
    private static synchronized  void clearFileDescriptor_ForServerSocket(BluetoothServerSocket socket) {

        try {

            Field mSocketFld = socket.getClass().getDeclaredField("mSocket");
            mSocketFld.setAccessible(true);

            BluetoothSocket btsock = (BluetoothSocket) mSocketFld.get(socket);

            Field mPfdFld = btsock.getClass().getDeclaredField("mPfd");
            mPfdFld.setAccessible(true);

            ParcelFileDescriptor pfd = (ParcelFileDescriptor) mPfdFld.get(btsock);

            pfd.close();
        }catch (Exception e) {
            Log.w(TAG, "ParcelFileDescriptor could not be cleanly closed for ServerSocket.");

        }
    }

    private static synchronized void clearFileDescriptor(BluetoothSocket socket){
        try{

            Field field = BluetoothSocket.class.getDeclaredField("mPfd");
            field.setAccessible(true);
            ParcelFileDescriptor mPfd = (ParcelFileDescriptor)field.get(socket);
            if(null == mPfd){
                return;
            }
            mPfd.close();
        }catch(Exception e){
            Log.w(TAG, "ParcelFileDescriptor could not be cleanly closed.");
        }
    }



}