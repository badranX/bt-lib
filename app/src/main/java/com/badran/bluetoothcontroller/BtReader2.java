package com.badran.bluetoothcontroller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import android.bluetooth.BluetoothSocket;

//import android.util.Log;
import android.util.Log;
import android.util.SparseArray;


import com.badran.library.CircularArrayList;

class BtReader2 {

//    private BtReceiver RECEIVER;

    private static BtReader instance = null;
    protected BtReader2() {
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
        public Object ReadWriteBufferKey = new Object();
        int id;
        volatile boolean stopReading = false;
        private CircularArrayList buffer = new CircularArrayList(128);

        public boolean IsDataAvailable(){
            synchronized (ReadWriteBufferKey){
                return buffer.isDataAvailable();
            }
        }

        public byte[] PollArray (int size,int id){
            synchronized (ReadWriteBufferKey) {
                return buffer.pollArray(size, id);
            }
        }
        public byte[] PollPacket (int id){
            synchronized (ReadWriteBufferKey) {
                return buffer.pollPacket(id);
            }
        }

        public int Size(){
            return  buffer.size();
        }
        public int Capacity(){
            return  buffer.capacity();
        }
        public boolean AddByte(byte item){
            return  buffer.add(item);
        }

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
        public final int  ThreadID;
        public BtReceiver thread;
        private SparseArray< BtElement> map = new SparseArray< BtElement>();
        public Object key = new Object();

        public ReadingThreadData(int ThreadID){
            this.ThreadID = ThreadID;
        }
        public  void AddReader(int id, BtElement btElment){
            synchronized (key) {
                this.map.put(id, btElment);
            }
        }

        public  BtElement GetReader(int id){
            synchronized (key) {
                return this.map.get(id);
            }
        }
        public  BtElement GetReaderByIndex(int index){
            synchronized (key) {
                int id = this.map.keyAt(index);
                return this.map.get(id);
            }
        }
        public  int NumberOfReaders(){
            synchronized (key) {
                return this.map.size();
            }
        }

        public  void RemoveReader(int id){
            synchronized (key) {
                this.map.remove(id);
            }
        }
        public void Clear(){
            synchronized (key) {
                this.map.clear();
            }
        }
    }

    private static class ReadingThreads{
        private static SparseArray<ReadingThreadData> dictionary = new SparseArray <ReadingThreadData>();
        private static Object key = new Object();
        public static void Add(int ThreadID, ReadingThreadData rtd){
                synchronized (key ) {
                    dictionary.put(ThreadID, rtd);
                }
        }
        public static ReadingThreadData Get(int ThreadID){
            synchronized (key) {
                return dictionary.get(ThreadID);
            }
        }

        public static void Remove(int ThreadID){
            synchronized (key) {
                 dictionary.remove(ThreadID);
            }
        }

    }

    public void EnableReading(BluetoothConnection btConnection) {

        ReadingThreadData rtd;
        if(btConnection.readingThreadID == 0 && ReadingThreads.Get(btConnection.readingThreadID) == null ){
            rtd = new ReadingThreadData(btConnection.readingThreadID);
            BtElement element = new BtElement(btConnection.socket, btConnection.inputStream,btConnection.id);
            rtd.AddReader(btConnection.id, element);
            ReadingThreads.Add(btConnection.readingThreadID, rtd);
            new Thread(new SingleBtReceiver(element)).start();
            return;
        }else if (btConnection.readingThreadID == 0 && (rtd = ReadingThreads.Get(btConnection.readingThreadID)) != null) {

            BtElement element = new BtElement(btConnection.socket, btConnection.inputStream,btConnection.id);
            rtd.AddReader(btConnection.id, element);
            new Thread(new SingleBtReceiver(element)).start();
            return;

        }else if ((rtd = ReadingThreads.Get(btConnection.readingThreadID)) != null)

            rtd.AddReader(btConnection.id, new BtElement(btConnection.socket, btConnection.inputStream));


        else {
            rtd = new ReadingThreadData(btConnection.readingThreadID);
            rtd.AddReader(btConnection.id, new BtElement(btConnection.socket, btConnection.inputStream));
            ReadingThreads.Add(btConnection.readingThreadID, rtd);

        }

        if (!rtd.isReading) {
            rtd.thread = new BtReceiver(rtd);
            new Thread(rtd.thread).start();
        }

    }


    public void Close(int id, int threadID) {//need adjusting
        ReadingThreadData rtd = ReadingThreads.Get(threadID);
        BtElement element;
        if(rtd != null)
            element = rtd.GetReader(id);
        else return;


        if (element != null) {
            element.stopReading = true;
            PluginToUnity.ControlMessages.READING_STOPPED.send(element.id);
        }
    }

    public boolean IsDataAvailable(int id, int threadID) {//need adjusting

        ReadingThreadData rtd = ReadingThreads.Get(threadID);
        BtElement e;

        if (rtd != null) {
                e = rtd.GetReader(id);
                if (e != null) {

                        return e.IsDataAvailable();

                }
        }
        return false;
    }


    public  byte[] ReadArray(int id, int threadId, int size) {

        ReadingThreadData rtd = ReadingThreads.Get(threadId);
        BtElement e;
        if(rtd != null)
            e = rtd.GetReader(id);
        else return null;


        if (e != null) {
            Log.v("unity", "test pollingArray");
            byte[] tempBytes = e.PollArray(size, id);
            Log.v("unity", "pollingArray test Passed");

            return tempBytes;
        }

        return null;
    }

    public  byte[] ReadPacket(int id, int threadId) {
        ReadingThreadData rtd = ReadingThreads.Get(threadId);
        BtElement e;
        if (rtd != null) {
            e = rtd.GetReader(id);
            if (e != null) {
                return e.PollPacket(id);
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
                synchronized (rtd.key) {
                    int size = rtd.NumberOfReaders();
                    if(size <= 0)
                        break;
                    if (i >= size)
                        i=0;

                    element = rtd.GetReaderByIndex(i);
                }
                if (element.socket != null) {
                    try {
                        if (element.inputStream.available() > 0) {


                            synchronized (element.ReadWriteBufferKey) {
                            byte ch;
                            while ((ch = (byte) element.inputStream.read()) >= 0) {

                                    if (element.Size() < element.Capacity()) {
                                        if (element.AddByte(ch)) {
                                            PluginToUnity.ControlMessages.DATA_AVAILABLE.send(element.id);
                                        } else break;
                                    }
                                }
                            }

                        }
                    } catch (IOException e) {
                        isListening = false;
                        PluginToUnity.ControlMessages.READING_ERROR.send(element.id);//-6
                    }


                }
                //perform closing for one element

                if (element.stopReading) {
                    try {
                        if (element.inputStream != null) element.inputStream.close();
                        if (element.socket != null) element.socket.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }

                    rtd.RemoveReader(element.id);
                    if(rtd.NumberOfReaders() <= 0) rtd.RemoveReader(rtd.ThreadID);
                    PluginToUnity.ControlMessages.READING_STOPPED.send(element.id);
                }
            }

            performStreamsClosing(rtd);

        }

        private  void performStreamsClosing(ReadingThreadData rtd) {
            for(int i = 0; i < rtd.NumberOfReaders(); i++) {
                BtElement element = rtd.GetReaderByIndex(i);
                try {
                    if (element.inputStream != null) element.inputStream.close();
                    if (element.socket != null) element.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            ReadingThreads.Remove(rtd.ThreadID);
            rtd.Clear();
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


                        synchronized (element.ReadWriteBufferKey) {
                        if (element.Size() < element.Capacity()){
                            byte ch;
                            if ((ch = (byte) element.inputStream.read()) >= 0) {

                                    if (element.AddByte(ch)) {
                                        PluginToUnity.ControlMessages.DATA_AVAILABLE.send(element.id);
                                    }
                                }
                            }
                        }
                    }

                } catch (IOException e) {
                    isListening = false;
                    PluginToUnity.ControlMessages.READING_ERROR.send(element.id);//-6
                }

            }

                try {
                    if (element.socket != null) element.socket.close();
                    if (element.inputStream != null) element.inputStream.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
                ReadingThreadData rtd = ReadingThreads.Get(0);
                if(rtd != null) rtd.RemoveReader(element.id);
                PluginToUnity.ControlMessages.READING_STOPPED.send(element.id);

        }
    }
}
