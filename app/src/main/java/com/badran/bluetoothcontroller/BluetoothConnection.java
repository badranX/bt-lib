package com.badran.bluetoothcontroller;


import android.bluetooth.BluetoothSocket;
import android.util.Log;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;


public class BluetoothConnection {

    public final ConnectionSetupData setupData = new ConnectionSetupData();




    //control data
    public boolean isConnected;
    public boolean isSending;
    public boolean isReading;


    public BtReader READER;



    public BluetoothSocket socket;

    public OutputStream outStream;

    public BufferedOutputStream bufferedOutputStream = null;

    public InputStream inputStream = null;
    public BufferedReader bufferReadder = null;






//    public void close() {
//
//
//        READER.close();
//        SENDER.close();
//
//        READER = null;
//        SENDER = null;
//
//        //
//        if (outStream != null) {
//            try {
//                outStream.flush();
//                outStream.close();
//            } catch (Exception e) {
//                outStream = null;
//            }
//        }
//
//
//        try {
//            if (socket != null) {
//                socket.close();
//                socket = null;
//            }
//            isConnected = false;
//            Bridge.controlMessage(2);
//
//        } catch (IOException e) {
//
//            Bridge.controlMessage(-4);
//        } finally {
//            Bridge.UnityEvents.connectionClosed(setupData.id);
//
//
//        }
//    }

    public  int connect( int trialsNumber) {

        Log.v("unity","Connect called");
        BtInterface.getInstance().connect(this,trialsNumber);
        return 1;

    }

    public void close() {

        try {
            if (socket != null) {
                Log.v("unity","socket is not null");
                socket.close();
                socket = null;
            }

            if(bufferedOutputStream != null) {
                bufferedOutputStream.flush();
                bufferedOutputStream.close();

            }
            isConnected = false;


        } catch (IOException e) {


        } finally {



        }
    }

    public void setUUID (String SPP_UUID){

        setupData.SPP_UUID = SPP_UUID;
    }



    public void sendString(String msg) {

        if(this.socket != null && this.bufferedOutputStream != null)
        BtSender.getInstance().addJob(bufferedOutputStream,msg.getBytes());

    }


    public String sendChar(byte msg) {

        if(this.socket != null && this.bufferedOutputStream != null) {
            Log.v("unity","ready to sendChar");
            BtSender.getInstance().addJob(bufferedOutputStream, new byte[]{msg});
        }
        return this.setupData.name;
    }



    public void sendBytes(byte[] msg) {
        if(this.socket != null && this.bufferedOutputStream != null)
        BtSender.getInstance().addJob(bufferedOutputStream, msg);

    }



    public boolean isConnected() {
        return isConnected;
    }

    public boolean isSending() {
        return isSending;
    }

    public boolean isListening() {
        return isReading;
    }
}
