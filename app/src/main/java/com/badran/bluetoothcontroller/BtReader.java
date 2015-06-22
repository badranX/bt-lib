package com.badran.bluetoothcontroller;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;

import android.bluetooth.BluetoothSocket;

//import android.util.Log;
import android.util.Log;
import android.widget.Switch;

import com.unity3d.player.UnityPlayer;

import com.badran.library.CircularArrayList;
class BtReader {

//    private BtReceiver RECEIVER;


    private BluetoothSocket socket;
    private InputStream receiveStream = null;
    private BufferedReader receiveReader = null;

    public volatile boolean isListening = false;
    volatile int modeIndex2 = 0;


    private String stringData = "";
    private boolean lineAvailable = false;
    private boolean dataAvailable = false;
    private boolean bufferDataAvailable = false;
    private boolean stopReading = false;
    private boolean closeReading = false;


    private byte[] tempBuffer = {};


    private BluetoothConnection btConnection;
    private BtReceiver readThread;


    public BtReader(BluetoothConnection btConnection) {
        this.btConnection = btConnection;
        readThread = new BtReceiver();
        new Thread(readThread).start();
    }


    void close() {
        this.closeReading = true;//close when finish loop
        this.stopReading = true;//stop loop

    }


    public boolean available() {
        return dataAvailable;
    }

    public byte [] readBuffer() {
        dataAvailable = false;
        return buffer.removeArray(2);
    }





    CircularArrayList buffer = new CircularArrayList (1024);



    private class BtReceiver implements Runnable {


        /////////////////////////////////////////String dataToSend = "";
        @Override
        public void run() {


            int firstIndex = 0;
            Log.v("unity", "started reading thread");

            while (btConnection.socket != null  && !stopReading) {


                try {
                    if (btConnection.inputStream.available() > 0) {
                        Log.v("unity", "data available");
                        String msg = "";
                        byte ch;
                        while( buffer.size() < buffer.capacity() ) {
                            Log.v("unity", "reading Data");
                            if ((ch = (byte) btConnection.inputStream.read()) >= 0) {

                                buffer.add(ch);
                                dataAvailable = true;
                                msg += ch; // if read string
                                Log.v("unity"," msg : " + msg);
                            } else break;
                        }
                    }
                } catch (IOException e) {
                            isListening = false;
                            PluginToUnity.ControlMessages.SENDING_ERROR.send(btConnection.id);//-6
                        }

                        }
            if(closeReading) performClosing();
                    }








        }
        void performClosing(){
try {
    if (btConnection.inputStream != null) btConnection.inputStream.close();

}catch (IOException e) {}

        }

}
