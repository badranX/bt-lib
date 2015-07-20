package com.badran.bluetoothcontroller;

import java.io.IOException;
import java.io.InputStream;

import android.bluetooth.BluetoothSocket;

//import android.util.Log;
import android.util.Log;
import android.util.SparseArray;


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


    private class BtElement {
        BluetoothSocket socket;
        InputStream inputStream;

        int id;
        boolean stopReading = false;
        CircularArrayList buffer = new CircularArrayList(128);

        public BtElement(BluetoothSocket socket, InputStream inputStream) {

            this.socket = socket;
            this.inputStream = inputStream;

        }

        public BtElement(BluetoothSocket socket, InputStream inputStream, int id) {
            this(socket,inputStream);
            this.id = id;
        }

    }

    private class ReadingThreadData {
        volatile boolean isReading = false;
        public BtReceiver thread;
        public SparseArray< BtElement> map = new SparseArray< BtElement>();

    }


    SparseArray<ReadingThreadData> dictionary = new SparseArray <ReadingThreadData>();

    public synchronized void enableReading(BluetoothConnection btConnection) {

        ReadingThreadData rtd;
        if(btConnection.readingThreadID == 0){
            rtd = new ReadingThreadData();
            BtElement element = new BtElement(btConnection.socket, btConnection.inputStream,btConnection.id);
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


    public synchronized void close(int id, int threadID) {//need adjusting
        ReadingThreadData rtd = dictionary.get(threadID);
        BtElement element;
        if(rtd != null)
            element = rtd.map.get(id);
        else return;


            if (element != null) {
                    PluginToUnity.ControlMessages.DISCONNECTED.send(element.id);
                    element.stopReading = true;

            }
    }

    public synchronized boolean isDataAvailable(int id, int threadID) {//need adjusting

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


    public synchronized byte[] readArray(int id, int threadId, int size) {

        ReadingThreadData rtd = dictionary.get(threadId);
        BtElement e;
        if(rtd != null)
            e = rtd.map.get(id);
        else return null;


        if (e != null) {
            Log.v("unity", "test pollingArray");
            byte[] tempBytes = e.buffer.pollArray(size, id);
            Log.v("unity", "pollingArray test Passed");

            return tempBytes;
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

                    return e.buffer.pollPacket(id);
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
                for (int i = 0;;i++) {
                    synchronized (BtReader.this) {
                        int size = rtd.map.size();
                        if(size <= 0)
                            break;
                        if (i >= size)
                            i=0;

                        int key = rtd.map.keyAt(i);
                        element = rtd.map.get(key);
                    }
                    if (element.socket != null) {
                        try {
                            if (element.inputStream.available() > 0) {

                                byte ch;
                                    while ((ch = (byte) element.inputStream.read()) >= 0) {
                                        synchronized (BtReader.this) {
                                            if (element.buffer.size() < element.buffer.capacity())
                                                if (element.buffer.add(ch)) {

                                                    PluginToUnity.ControlMessages.DATA_AVAILABLE.send(element.id);
                                                } else break;
                                        }
                                    }

                            }
                        } catch (IOException e) {
                            isListening = false;
                            PluginToUnity.ControlMessages.SENDING_ERROR.send(element.id);//-6
                        }


                    }
                    //perform closing for one element

                        if (element.stopReading) {
                            try {
                                if (element.socket != null) element.socket.close();
                                if (element.inputStream != null) element.inputStream.close();
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                            synchronized (BtReader.this) {
                                rtd.map.remove(element.id);
                                dictionary.remove(element.id);
                            }
                        }
            }

            performStreamsClosing(rtd);

        }

        private synchronized void performStreamsClosing(ReadingThreadData rtd) {

            for(int i = 0; i < rtd.map.size(); i++) {
                int key = rtd.map.keyAt(i);
                // get the object by the key.
                BtElement element = rtd.map.get(key);
                try {
                    if (element.inputStream != null) element.inputStream.close();

                    dictionary.remove(element.id);


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }




    private class SingleBtReceiver implements Runnable {
        BtElement element;
        ReadingThreadData rtd;
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
            if (element.stopReading) {
                try {
                    if (element.socket != null) element.socket.close();
                    if (element.inputStream != null) element.inputStream.close();
                }catch(IOException e){
                    e.printStackTrace();
                }

                synchronized (BtReader.this) {
                    rtd.map.remove(0);
                    dictionary.remove(element.id);
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





