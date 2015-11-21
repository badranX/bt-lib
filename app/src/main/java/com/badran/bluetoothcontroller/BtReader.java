package com.badran.bluetoothcontroller;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.concurrent.SynchronousQueue;

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


    private class BtElement {
        BluetoothSocket socket;
        InputStream inputStream;
        final public Object ReadWriteBufferKey = new Object();
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
            return buffer.add(item);
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
        private boolean isReading = false;
        public final int  ThreadID;
        //public BtReceiver thread;
        private SparseArray< BtElement> map = new SparseArray< BtElement>();
        final public Object key = new Object();
        final public Object StartDoneKey = new Object();

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
                if(map.get(id) != null)
                    this.map.remove(id);
            }
        }
        public void Clear(){
            synchronized (key) {
                this.map.clear();
            }
        }

        public void StartSingletonThread(BtElement btElment){

            synchronized (key){
                isReading= true;
                new Thread(new SingleBtReceiver(btElment)).start();
            }
        }

        public void StartThread(){
            synchronized (key){
                if(!IsReading()) {
                    isReading = true;
                    new Thread(new BtReceiver(this)).start();
                }
            }
        }

        public void DoneReading(){
            synchronized (key){
                    isReading = false;
            }
        }

        public boolean IsReading(){
            synchronized (key) {
                return this.map.size() > 0 && isReading;
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

    public boolean IsReading(BluetoothConnection btConnection){
        ReadingThreadData rtd = ReadingThreads.Get(btConnection.readingThreadID);
        if(rtd != null) {
            synchronized (rtd.key) {
                return rtd.GetReader(btConnection.getID()) != null ? true : false;
            }
        }
        return false;
    }
    public void EnableReading(BluetoothConnection btConnection) {

        ReadingThreadData rtd;
        if(btConnection.readingThreadID == 0 && ReadingThreads.Get(btConnection.readingThreadID) == null ){
            rtd = new ReadingThreadData(btConnection.readingThreadID);
            BtElement element = new BtElement(btConnection.socket, btConnection.inputStream,btConnection.getID());
            rtd.AddReader(btConnection.getID(), element);
            ReadingThreads.Add(btConnection.readingThreadID, rtd);
            rtd.StartSingletonThread(element);

        }else if (btConnection.readingThreadID == 0 && (rtd = ReadingThreads.Get(btConnection.readingThreadID)) != null) {

            BtElement element = new BtElement(btConnection.socket, btConnection.inputStream,btConnection.getID());
            rtd.AddReader(btConnection.getID(), element);
            rtd.StartSingletonThread(element);


        }else if ((rtd = ReadingThreads.Get(btConnection.readingThreadID)) != null) {
            rtd.AddReader(btConnection.getID(), new BtElement(btConnection.socket, btConnection.inputStream));
            rtd.StartThread();
        }else {
            rtd = new ReadingThreadData(btConnection.readingThreadID);
            rtd.AddReader(btConnection.getID(), new BtElement(btConnection.socket, btConnection.inputStream));
            ReadingThreads.Add(btConnection.readingThreadID, rtd);
            rtd.StartThread();

        }

        PluginToUnity.ControlMessages.READING_STARTED.send(btConnection.getID());

    }


    public void Close(int id, int threadID) {//need adjusting
        ReadingThreadData rtd = ReadingThreads.Get(threadID);

        if(rtd != null) {
            BtElement element;
            if ((element = rtd.GetReader(id)) != null) {
                    element.stopReading = true;
                    rtd.RemoveReader(id);
            }
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
                    if(size <= 0) {
                        rtd.DoneReading();
                        break;
                    }if (i >= size)
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
                    //if(rtd.NumberOfReaders() <= 0) rtd.RemoveReader(rtd.ThreadID);
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
            rtd.isReading = false;
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
            if(rtd != null) {
                synchronized (rtd.key) {
                rtd.RemoveReader(element.id);//removing only the element//every element has its own thread
                    if (rtd.NumberOfReaders() == 0) rtd.DoneReading();
                }
            }

            PluginToUnity.ControlMessages.READING_STOPPED.send(element.id);
        }
    }
}





