package com.techtweaking.bluetoothcontroller;

import java.io.IOException;
import java.io.InputStream;

import android.bluetooth.BluetoothSocket;

import android.util.Log;
import android.util.SparseArray;


import com.techtweaking.libextra.CircularArrayList;
import com.techtweaking.libextra.IOUtils;

class BtReader {

    private static final String TAG = "PLUGIN . UNITY";

//    private BtReceiver RECEIVER;

    private final byte[] empty = new byte[0];
    private static BtReader instance = null;
    private BtReader() {
        // Exists only to defeat instantiation.
    }

    public static BtReader getInstance() {
        if (instance == null) {
            instance = new BtReader();
        }
        return instance;
    }


    private class BtElement {
        final boolean isDynamicSize;
        final BluetoothSocket socket;
        InputStream inputStream;
        final public Object ReadWriteBufferKey = new Object();
        final int id;
        volatile boolean stopReading = false;
        private final CircularArrayList buffer;
        /*
        Unity SendMessages instead
        public boolean IsDataAvailable(){
            synchronized (ReadWriteBufferKey){
                return buffer.isDataAvailable();
            }
        }
        */

        public byte[] PollArray (int size,int id){
            synchronized (ReadWriteBufferKey) {
                return buffer.pollArrayOfSize(size, id);
            }
        }
        public byte[] PollPacket (int id){
            synchronized (ReadWriteBufferKey) {
                return buffer.pollPacket(id);
            }
        }
        public byte[] PollAllPackets(){
            synchronized (ReadWriteBufferKey) {
                return buffer.pollAllPackets();
            }
        }

        public void flush (int id){
            synchronized (ReadWriteBufferKey) {
                buffer.flush(id);
            }
        }

        public void setPacketSize (int size){
            synchronized (ReadWriteBufferKey) {
                buffer.setPacketSize(size);
            }

        }

        public void setEndByte (byte byt){
            synchronized (ReadWriteBufferKey) {
                buffer.setEndByte(byt);
            }

        }

        public int Size(){
            return buffer.size();
        }
        public int Capacity(){
            return buffer.capacity();
        }
        public void Resize(){
            synchronized (ReadWriteBufferKey) {
                buffer.resize();
            }
        }
        public boolean AddByte(byte item){
            //Resize and AddByte are used on the same thread no need for synchronization
            //Also AddByte and Polls() methods dont affect each other! in a circular buffer.
            return buffer.add(item);

        }


        public BtElement(BluetoothSocket socket, InputStream inputStream, int id,int bufferSize,boolean isDynamicSize) {
            this.socket = socket;
            this.inputStream = inputStream;
            this.id = id;
            this.isDynamicSize = isDynamicSize;
            this.buffer = new CircularArrayList(bufferSize);
        }

    }

    private class ReadingThreadData {
        private boolean isReading = false;
        public final int  ThreadID;
        //public BtReceiver thread;
        private final SparseArray< BtElement> map = new SparseArray< BtElement>();
        final public Object key = new Object();

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
        private static final SparseArray<ReadingThreadData> dictionary = new SparseArray <ReadingThreadData>();
        private static final Object key = new Object();
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
                return rtd.GetReader(btConnection.getID()) != null;
            }
        }
        return false;
    }
    public void EnableReading(BluetoothConnection btConnection) {

        ReadingThreadData rtd;
        BtElement element;
        if(btConnection.readingThreadID == 0 && ReadingThreads.Get(btConnection.readingThreadID) == null ){
            rtd = new ReadingThreadData(btConnection.readingThreadID);
            element = new BtElement(btConnection.socket, btConnection.inputStream,btConnection.getID(),btConnection.bufferSize,btConnection.isBufferDynamic);
            rtd.AddReader(btConnection.getID(), element);
            ReadingThreads.Add(btConnection.readingThreadID, rtd);
            packetize(btConnection, element);
            rtd.StartSingletonThread(element);

        }else if (btConnection.readingThreadID == 0 && (rtd = ReadingThreads.Get(btConnection.readingThreadID)) != null) {
            element = new BtElement(btConnection.socket, btConnection.inputStream,btConnection.getID(),btConnection.bufferSize,btConnection.isBufferDynamic);
            rtd.AddReader(btConnection.getID(), element);
            packetize(btConnection,element);
            rtd.StartSingletonThread(element);
        }else if ((rtd = ReadingThreads.Get(btConnection.readingThreadID)) != null) {
            element =  new BtElement(btConnection.socket, btConnection.inputStream,btConnection.getID(),btConnection.bufferSize,btConnection.isBufferDynamic);
            rtd.AddReader(btConnection.getID(), element);
            packetize(btConnection,element);
            rtd.StartThread();
        }else {
            rtd = new ReadingThreadData(btConnection.readingThreadID);
            element = new BtElement(btConnection.socket, btConnection.inputStream,btConnection.getID(),btConnection.bufferSize,btConnection.isBufferDynamic);
            rtd.AddReader(btConnection.getID(), element);
            ReadingThreads.Add(btConnection.readingThreadID, rtd);
            packetize(btConnection,element);
            rtd.StartThread();
        }

        PluginToUnity.ControlMessages.READING_STARTED.send(btConnection.getID());

    }

    private void packetize(BluetoothConnection btConnection,BtElement element){
        if(btConnection.isSizePacketized){
            element.setPacketSize(btConnection.packetSize);
        }else if(btConnection.isEndBytePacketized){
            element.setEndByte(btConnection.packetEndByte);
        }
    }
    // CALLED BY UNITY
    public boolean Close(int id, int threadID) {//need adjusting
        ReadingThreadData rtd = ReadingThreads.Get(threadID);
        if (rtd != null) {
            synchronized (rtd.key)
            {
                BtElement element;
                if ((element = rtd.GetReader(id)) != null)
                {
                    element.stopReading = true;
                    return true;
                }
            }
        }
        return false;
    }

    Object getReadLock(int id, int threadID) {
        ReadingThreadData rtd = ReadingThreads.Get(threadID);
        if (rtd != null) {
            synchronized (rtd.key)
            {
                BtElement element;
                if ((element = rtd.GetReader(id)) != null)
                {
                    return element.ReadWriteBufferKey;
                }
            }
        }
        return null;
    }

    /* We're using Unity SendMessages to check data availablity
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
        */
    public void setPacketSize(int id, int threadId, int size){
        ReadingThreadData rtd = ReadingThreads.Get(threadId);
        BtElement e;
        if(rtd != null)
            e = rtd.GetReader(id);
        else return;


        if (e != null) {
            e.setPacketSize(size);
        }
    }

    public void setEndByte(int id, int threadId, byte byt){
        ReadingThreadData rtd = ReadingThreads.Get(threadId);
        BtElement e;
        if(rtd != null)
            e = rtd.GetReader(id);
        else return;


        if (e != null) {
            e.setEndByte(byt);
        }
    }

    public  byte[] ReadArray(int id, int threadId, int size) {

        ReadingThreadData rtd = ReadingThreads.Get(threadId);
        BtElement e;
        if(rtd != null)
            e = rtd.GetReader(id);
        else return empty;


        if (e != null) {

            return e.PollArray(size, id);
        }

        return empty;
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
        return empty;
    }

    public  byte[] ReadAllPackets(int id, int threadId) {
        ReadingThreadData rtd = ReadingThreads.Get(threadId);
        BtElement e;
        if (rtd != null) {
            e = rtd.GetReader(id);
            if (e != null) {
                return e.PollAllPackets();
            }
        }
        return empty;
    }

    private class BtReceiver implements Runnable {
        private final BtReader.ReadingThreadData rtd;

        public BtReceiver(BtReader.ReadingThreadData rtd)
        {
            this.rtd = rtd;
        }

        public void run()
        {
            for (int i = 0;; i++)
            {
                BtReader.BtElement element;
                synchronized (this.rtd.key)
                {
                    int size = this.rtd.NumberOfReaders();
                    if (size <= 0)
                    {
                        this.rtd.DoneReading();
                        break;
                    }
                    if (i >= size) {
                        i = 0;
                    }
                    element = this.rtd.GetReaderByIndex(i);
                }

                if (element.socket != null) {
                    try
                    {
                        if (element.inputStream.available() > 0) {
                            synchronized (element.ReadWriteBufferKey)
                            {
                                if (element.Size() < element.Capacity())
                                {
                                    int ch;

                                    if ((ch = element.inputStream.read()) >= 0)
                                    {
                                        if (element.AddByte((byte)ch)) {
                                            PluginToUnity.ControlMessages.DATA_AVAILABLE.send(element.id);
                                        }
                                    }
                                }else if(element.isDynamicSize) {
                                    element.Resize();
                                }
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        Log.w(TAG, "failed while reading/receiving data", e);
                        if(!element.stopReading) {
                            PluginToUnity.ControlMessages.READING_ERROR.send(element.id, e.getMessage());
                        }
                    }
                }
                if (element.stopReading)
                {
                    this.rtd.RemoveReader(element.id);

                    PluginToUnity.ControlMessages.READING_STOPPED.send(element.id);
                }
            }
            performStreamsClosing(this.rtd);
        }

        private void performStreamsClosing(BtReader.ReadingThreadData rtd)
        {
            //closing INPUTSTREAM will close socket, let the BluetoothConnection.close() method do the job

            /*
            for (int i = 0; i < rtd.NumberOfReaders(); i++)
            {
                BtReader.BtElement element = rtd.GetReaderByIndex(i);

                if (element.inputStream != null) {
                    IOUtils.closeQuietly(element.inputStream);
                    element.inputStream = null;
                }
            }
            */

            BtReader.ReadingThreads.Remove(rtd.ThreadID);
            rtd.Clear();
        }
    }




    private class SingleBtReceiver implements Runnable {
        final BtElement element;

        public SingleBtReceiver(BtElement element) {
            this.element = element;

        }

        @Override
        public void run() {

            while ( !element.stopReading) {
                try {
                    if (element.inputStream.available() > 0) {
                        synchronized (element.ReadWriteBufferKey) {
                            if (element.Size() < element.Capacity()){
                                int ch;
                                if ((ch = element.inputStream.read()) >= 0)
                                {
                                    if (element.AddByte((byte)ch)) {
                                        PluginToUnity.ControlMessages.DATA_AVAILABLE.send(element.id);
                                    }
                                }
                            } else if(element.isDynamicSize) {
                                element.Resize();
                            }
                        }
                    }

                } catch (IOException e) {
                    Log.w(TAG, "failed while reading/receiving data", e);
                    if(!element.stopReading)
                        PluginToUnity.ControlMessages.READING_ERROR.send(element.id, e.getClass().getName() + ": " + e.getMessage());//-6
                }
            }

            //Let BluetoothConnection do all the closing required. InputStream closing causes the socket to close
            /*
            if (element.inputStream != null) {
                IOUtils.closeQuietly(element.inputStream);
                element.inputStream = null;
            }
            */

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


