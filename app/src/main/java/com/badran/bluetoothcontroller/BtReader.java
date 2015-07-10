package com.badran.bluetoothcontroller;

import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


import android.bluetooth.BluetoothSocket;

//import android.util.Log;
import android.util.Log;




import com.badran.library.CircularArrayList;

class BtReader {

//    private BtReceiver RECEIVER;


    private static BtReader instance = null;


    protected BtReader() {
        Byte[] x;

        // Exists only to defeat instantiation.

    }

    public static BtReader getInstance() {
        if (instance == null) {
            instance = new BtReader();
        }
        return instance;
    }

    public volatile boolean isListening = false;







    private class BtElement {
        BluetoothSocket socket;
        InputStream inputStream;

        int id;
        boolean stopReading = false;
        CircularArrayList buffer = new CircularArrayList(1024);

        public BtElement(BluetoothSocket socket, InputStream inputStream) {

            this.socket = socket;
            this.inputStream = inputStream;

        }

        public BtElement(BluetoothSocket socket, InputStream inputStream, int id) {

            this.socket = socket;
            this.inputStream = inputStream;
            this.id = id;
        }

    }

    private class ReadingThreadData {
        volatile boolean isReading = false;
        public BtReceiver thread;
        public Map<Integer, BtElement> map = new HashMap<Integer, BtElement>();

    }


    Map<Integer, ReadingThreadData> dictionary = new Hashtable<Integer, ReadingThreadData>();

    public void enableReading(BluetoothConnection btConnection) {

        ReadingThreadData rtd;
        if(btConnection.readingThreadID == 0){
            rtd = new ReadingThreadData();
            BtElement element = new BtElement(btConnection.socket, btConnection.inputStream);
            rtd.map.put(btConnection.id, element);
            dictionary.put(btConnection.readingThreadID, rtd);
            new Thread(new SingleBtReceiver(element)).start();
            return;
        }
        else if ((rtd = dictionary.get(btConnection.readingThreadID)) != null)
            synchronized (this) {
                rtd.map.put(btConnection.id, new BtElement(btConnection.socket, btConnection.inputStream));
            }

        else {
            rtd = new ReadingThreadData();
            rtd.map.put(btConnection.id, new BtElement(btConnection.socket, btConnection.inputStream));
            dictionary.put(btConnection.readingThreadID, rtd);

        }

        if (!rtd.isReading) {
            rtd.thread = new BtReceiver(rtd);
            new Thread(rtd.thread).start();
        }

    }


    public void close(int id, int threadID) {//need adjusting

        if (dictionary.containsKey(threadID))//removing key
            if (dictionary.get(threadID).map.containsKey(id)) {
                synchronized (this) {
                    BtElement btElement = dictionary.get(threadID).map.get(id);
                    try {
                        if (btElement.inputStream != null) btElement.inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dictionary.get(threadID).map.get(id).stopReading = true;
                    dictionary.get(threadID).map.remove(id);

                }
            }
    }

    public boolean isDataAvailable(int id, int threadID) {//need adjusting

        ReadingThreadData rtd = dictionary.get(threadID);

        BtElement e;

        if (rtd != null) {
            synchronized (this) {
                e = rtd.map.get(id);
                if (e != null) {
                    return e.buffer.isDataAvailable();
                }
            }
        }
        return false;
    }


    public byte[] readArray(int id, int threadId, int size) {

        ReadingThreadData rtd = dictionary.get(threadId);

        BtElement e;

        if (rtd != null) {
            synchronized (this) {
                e = rtd.map.get(id);
                if (e != null) {
                    Log.v("unity", "test pollingArray");
                    byte[] tempBytes = e.buffer.pollArray(size, id);
                    Log.v("unity", "pollingArray test Passed");

                    return tempBytes;
                }
            }
        }
        return null;
    }

    public synchronized byte[] readPacket(int id, int threadId) {
        ReadingThreadData rtd = dictionary.get(threadId);
        BtElement e;
        if (rtd != null) {
            synchronized (this) {
                e = rtd.map.get(id);
                if (e != null) {
                    byte[] tempBytes = e.buffer.pollPacket(id);
                    return tempBytes;
                }
            }
        }
        return null;
    }


    private class BtReceiver implements Runnable {
        private ReadingThreadData rtd;

        public BtReceiver(ReadingThreadData rtd) {
            this.rtd = rtd;

        }

        /////////////////////////////////////////String dataToSend = "";
        @Override
        public void run() {


            BtElement element;
            Iterator it;
            int id;
            Map.Entry pair;
            synchronized (BtReader.this) {
                it = rtd.map.entrySet().iterator();
            }


            while (true) {

                synchronized (BtReader.this) {
                    if (!it.hasNext()) {
                        it = rtd.map.entrySet().iterator();
                        if (!it.hasNext()) break;
                    }
                }

                pair = (Map.Entry) it.next();
                element = (BtElement) pair.getValue();
                id = (Integer) pair.getKey();

                if (element.socket != null) {
                    try {
                        if (element.inputStream.available() > 0) {

                            byte ch;
                            while (true) {

                                if ((ch = (byte) element.inputStream.read()) >= 0) {
                                    synchronized (BtReader.this) {
                                        if (element.buffer.size() < element.buffer.capacity())
                                            if (element.buffer.add(ch)) {

                                                PluginToUnity.ControlMessages.DATA_AVAILABLE.send(id);
                                            } else break;
                                    }
                                } else break;
                            }
                        }
                    } catch (IOException e) {
                        isListening = false;
                        PluginToUnity.ControlMessages.SENDING_ERROR.send(id);//-6
                    }


                }
            }
            performClosing(rtd);
        }


    }

    private void performClosing(ReadingThreadData rtd) {

        Iterator it = rtd.map.entrySet().iterator();
        BtElement element;

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            element = (BtElement) pair.getValue();

            try {
                if (element.inputStream != null) element.inputStream.close();


            } catch (IOException e) {
            }
        }
    }

    private class SingleBtReceiver implements Runnable {
        BtElement element;

        public SingleBtReceiver(BtElement element) {
            this.element = element;
        }

        @Override
        public void run() {

            Log.v("unity","single Thread running");
            while (element.socket != null && !element.stopReading) {
                try {
                    if (element.inputStream.available() > 0) {

                        byte ch;

                         if ((ch = (byte) element.inputStream.read()) >= 0) {
                            synchronized (BtReader.this) {
                                if (element.buffer.size() < element.buffer.capacity())
                                    if (element.buffer.add(ch)) {

                                        PluginToUnity.ControlMessages.DATA_AVAILABLE.send(element.id);
                                    }
                            }
                        }
                    }

                } catch (IOException e) {
                    isListening = false;
                    PluginToUnity.ControlMessages.SENDING_ERROR.send(element.id);//-6
                }

            }
            try {
                if (element.inputStream != null) element.inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}





