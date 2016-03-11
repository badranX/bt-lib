package com.badran.library;


import com.badran.bluetoothcontroller.PluginToUnity;

import java.util.*;



public class CircularArrayList {

    private volatile int n; // buffer length and it's volatile as it would be changed from a thread.

    private final byte[] empty = new byte[0];

    private volatile int  head = 0;
    private volatile int  tail = 0;

    private int lengthPacketsCounter = 0;
    private int counter = 0;
    private int packetSize = 0;

    private byte [] buf;

    private enum MODES {
        LENGTH_PACKET,END_BYTE_PACKET,NO_PACKETIZATION

    } private  MODES mode = MODES.NO_PACKETIZATION;


    private LinkedList<Integer> marks ;

    private Byte endByte;


    public CircularArrayList(int capacity) {
        n = capacity + 1;
        buf = new byte[n];

    }





    public int capacity() {
        return n - 1 ;
    }



    private int wrapIndex(int i) {
        int m = i % n;
        if (m < 0) { // java modulus can be negative
            m += n;
        }
        return m;
    }
    public boolean isDataAvailable(){
        switch (mode){
            case LENGTH_PACKET :
                return lengthPacketsCounter > 0;

            case END_BYTE_PACKET :
                return !marks.isEmpty();
            case NO_PACKETIZATION: return size() > 0;
            default: return false;
        }
    }


    public int size() {
        //tail never equals capacity because wrapindex() doesn't allow that. so size never equals capacity
        return tail - head + (tail < head ? n : 0);
    }


    public void setEndByte(byte byt) {
        if(size() <= 0) {
            marks = new LinkedList<Integer>();

            endByte = byt;
            mode = MODES.END_BYTE_PACKET;
        }
    }

    public void setPacketSize(int size) {
        if(size() <= 0) {
            packetSize = size;
            mode = MODES.LENGTH_PACKET;
        }
    }

//    public void erasePackets(int size) {
//        marks.clear();
//        endBytes.clear();
//        packetSize = 0;
//        lengthPacketsCounter = 0;
//        counter = 0;
//
//    }

    public   int getDataSize(){
        switch (mode){
            case NO_PACKETIZATION: size();
            case LENGTH_PACKET : return lengthPacketsCounter;
            case END_BYTE_PACKET : return marks.size();
            default: return size();
        }

    }

    public void resize(){//Assumes that the buffer is full. size == capacity
        int tmpN = (int)(n * 1.5);
        byte[] tmp;
       try {
           tmp = new byte[tmpN];
       }catch (OutOfMemoryError e) {
            return;
        }


        if(head == 0) {//The case that the head is still at 0 index. so the tail is at last index
            //as the last index is where the tail points out and it's empty.
            //copy all elements to the new buffer and that's it.
            System.arraycopy(buf, 0, tmp, 0, n -1  );//n-1 is the last index, and since the last index isn't included (n-1)
            n = tmpN;
            //the array structure is still the same , heads and marks
            return;

        }else {
            int len = n - head;//From the Head (included) up to the last index(included). this is there length.
            System.arraycopy(buf, head, tmp, 0, len  );//n-1 index is included here
            System.arraycopy(buf, 0, tmp, len, head );//head won't be included so from 0 to head -1

            int markSize = marks.size();
            for (int i = 0 ; i < markSize ; i++) {
                int mark = marks.poll();
                //It's not possible that mark == head, as it would be already removed //
                // ( a mark marks the end of a packet head could only be a start of a packet.
                if(mark > head){
                    mark = mark - head;
                }else {
                    mark = mark + len;
                }

                marks.addLast(mark);
            }

            head = 0;
            tail = n -1;
            n = tmpN;
        }



    }

    public   boolean add(byte e) {//returns true if packet/data available for the first time after was no packets

        int s = size();
        if (s == n - 1) {
            //TODO this should never be reached, should throw exception
            return false;//No Adding will be done
        }


        boolean isFirstTimeData = false;
        switch (mode){
            case LENGTH_PACKET :
                if (counter < packetSize) {
                    counter++;

                } else {
                    counter = 0;
                    if(lengthPacketsCounter == 0)
                        isFirstTimeData = true;

                    lengthPacketsCounter++;

                }break;

            case END_BYTE_PACKET :

                if (endByte == e) {

                    if( size() == 0 || ( marks.peek()!= null && marks.peek() == tail))//endByte at the start of new packet
                        return false;

                    if(marks.isEmpty()) isFirstTimeData = true;
                    marks.add(tail);//index excluded

                    return isFirstTimeData;//shouldn't add endByte

                }


                break;
            case NO_PACKETIZATION: if(s == 0) isFirstTimeData = true;


        }



        buf[tail] = e;
        tail = wrapIndex(tail + 1);

        return isFirstTimeData;
    }


    public   Byte poll() {

        if (size() <= 0) return null;


        byte e = buf[head];
        head = wrapIndex(head + 1);

        return e;
    }

    private byte[] pollArray (int size){//Doesn't tollerate errors in inputs (size),expect to check them before calling

        //same As pollArraySize() but used for packetization, so it doesn't send to unity


        int end = wrapIndex(head + size );

        byte[] e;
        if(end >= head) {
            e = Arrays.copyOfRange(buf, head, end);
        }else {
            e = new byte[size];
            int len = n - head;
            System.arraycopy(buf, head, e, 0, len  );//n-1 is the actual capacity
            System.arraycopy(buf, 0, e, len, end  );
        }
        head = end; // this end had excluded from copying _still hasn't been read


        return e;
    }

    public byte[] pollArrayOfSize(int size, int id) {//endIndex or Size of Array
        if(mode != MODES.NO_PACKETIZATION) return pollPacket(id);


        boolean readAllData = false;

        int s = size();

        if (s <= 0 || size <=0) {
            return empty;
        }

        //endIndex - startIndex = size ;;; endIndex = startIndex + size;
        if (size >= s ) {
            size = s;
            readAllData = true;
        }

        byte[] e = pollArray(size);

        if(readAllData){
            PluginToUnity.ControlMessages.EMPTIED_DATA.send(id);
        }
        return e;

    }

    public void flush(int id){
        marks.clear();
        lengthPacketsCounter = 0;
        head = 0;
        tail = 0;

        PluginToUnity.ControlMessages.EMPTIED_DATA.send(id);

    }
    public byte[] pollAll(int id){
        marks.clear();
        lengthPacketsCounter = 0;
        byte[] temp = pollArray(size());
        PluginToUnity.ControlMessages.EMPTIED_DATA.send(id);
        return temp;
    }

    public byte[] pollPacket(int id) {
        switch (mode){
            case LENGTH_PACKET :
                if(lengthPacketsCounter > 0) {
                    byte[] temp = pollArray(packetSize);
                    --lengthPacketsCounter;
                    if(lengthPacketsCounter <= 0)
                        PluginToUnity.ControlMessages.EMPTIED_DATA.send(id);
                    return temp;
                }
                break;

            case END_BYTE_PACKET :
                if (!marks.isEmpty()) {
                    int bytTail = marks.poll();
                    byte[] temp = pollArray(bytTail - head + (bytTail < head ? n : 0));//size between marks.poll and head
                    if(marks.isEmpty())
                        PluginToUnity.ControlMessages.EMPTIED_DATA.send(id);

                    return temp;
                }
                break;
            case NO_PACKETIZATION:
                byte[] temp = pollArray(size());
                PluginToUnity.ControlMessages.EMPTIED_DATA.send(id);
                return temp;

        }
        return empty;
    }


}