package com.badran.bluetoothcontroller;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.bluetooth.BluetoothSocket;

//import android.util.Log;
import android.util.Log;
import android.widget.Switch;

import com.unity3d.player.UnityPlayer;

import com.badran.library.CircularArrayList;
class BtReader {

//    private BtReceiver RECEIVER;



    private static BtReader instance = null;


    protected BtReader() {

        // Exists only to defeat instantiation.

    }

    public static BtReader getInstance() {
        if (instance == null) {
            instance = new BtReader();
        }
        return instance;
    }

    public volatile boolean isListening = false;
    volatile int modeIndex2 = 0;


    private String stringData = "";
    private boolean lineAvailable = false;
    private boolean dataAvailable = false;
    private boolean bufferDataAvailable = false;
    private boolean stopReading = false;
    private boolean closeReading = false;


    private byte[] tempBuffer = {};



    private BtReceiver readThread;


    private class BtElement {
        BluetoothSocket socket;
        InputStream inputStream;

        int id;

        CircularArrayList buffer = new CircularArrayList (1024);
        public BtElement(BluetoothSocket socket, InputStream inputStream){

            this.socket = socket;
            this.inputStream = inputStream;
            this.id = id;
        }

    }

    private class ReadingThreadData {
        volatile boolean isReading = false;
        public BtReceiver thread;
        public Map<Integer,BtElement> map = new HashMap<Integer, BtElement>();



    }



     Dictionary<Integer,ReadingThreadData> dictionary = new Hashtable<Integer, ReadingThreadData>();

    public void enableReading(BluetoothConnection btConnection) {

        ReadingThreadData rtd;

        if((rtd = dictionary.get(btConnection.readingThreadID)) != null )

            rtd.map.put(btConnection.id, new BtElement(btConnection.socket, btConnection.inputStream));


        else {


            rtd = new ReadingThreadData();

            rtd.map.put(btConnection.id,new  BtElement(btConnection.socket,btConnection.inputStream));

            dictionary.put(btConnection.readingThreadID,rtd);

        }

        if(!rtd.isReading) {
            rtd.thread = new BtReceiver(rtd);
            Log.v("unity","starting A thread reading");
            new Thread(rtd.thread).start();
        }

    }


    void close(int id,int threadID) {//need adjusting
        dictionary.get(threadID).map.remove(id);
        this.closeReading = true;//close when finish loop
        this.stopReading = true;//stop loop

    }




    public byte [] readArray(int id,int threadId,int size) {
        ReadingThreadData rtd = dictionary.get(threadId);
        BtElement e;
        if(rtd != null) {
            if(rtd.map != null) {
                e = rtd.map.get(id);
                if (e != null) {
                    byte [] tempBytes =  e.buffer.pollArray(size,id);


                    return tempBytes;
                }

            }
        }
       return  null;
    }

    public byte [] readPacket(int id,int threadId) {
        ReadingThreadData rtd = dictionary.get(threadId);
        BtElement e;
        if(rtd != null) {
            if(rtd.map != null) {
                e = rtd.map.get(id);
                if (e != null) {
                    byte [] tempBytes =  e.buffer.pollPacket(id);



                    return tempBytes;
                }

            }
        }
        return  null;
    }





    private class BtReceiver implements Runnable {
        private ReadingThreadData rtd;
        public BtReceiver(ReadingThreadData rtd){
            this.rtd = rtd;

        }
        /////////////////////////////////////////String dataToSend = "";
        @Override
        public void run() {


            int firstIndex = 0;

            Iterator it = rtd.map.entrySet().iterator();
            BtElement element;
            int id;
            Map.Entry pair;
            Log.v("unity", "start reading");
            while(it.hasNext()) {

                pair = (Map.Entry) it.next();
                element = (BtElement)pair.getValue();
                id = (Integer)pair.getKey();

                if (element.socket != null ) {
                    try {
                        if (element.inputStream.available() > 0) {

                            byte ch;
                            while (element.buffer.size() < element.buffer.capacity()) {

                                if ((ch = (byte) element.inputStream.read()) >= 0) {

                                    if( element.buffer.add(ch)) {

                                        PluginToUnity.ControlMessages.DATA_AVAILABLE.send(id);
                                    }
                                } else break;
                            }
                        }
                    } catch (IOException e) {
                        isListening = false;
                        PluginToUnity.ControlMessages.SENDING_ERROR.send(id);//-6
                    }
                    if (!it.hasNext())
                        it = rtd.map.entrySet().iterator();
                    }
            }
            if(closeReading) performClosing(rtd);
    }


        }
        void performClosing(ReadingThreadData rtd){

            Iterator it = rtd.map.entrySet().iterator();
            BtElement element;
            while(it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                element = (BtElement)pair.getValue();

                try {
                    if (element.inputStream != null)element.inputStream.close();;

                } catch (IOException e) {
                }
            }
        }

}
