package com.badran.library;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * If you use this code, please consider notifying isak at du-preez dot com
 *  with a brief description of your application.
 *
 * This is free and unencumbered software released into the public domain.
 *  Anyone is free to copy, modify, publish, use, compile, sell, or
 *  distribute this software, either in source code form or as a compiled
 *  binary, for any purpose, commercial or non-commercial, and by any
 *  means.
 */

public class CircularArrayList {

    private final int n; // buffer length
    private final byte [] buf; // a List implementing RandomAccess
    private final ByteBuffer buffer;
    private int head = 0;
    private int tail = 0;
    private Queue<Integer> marks;

    public CircularArrayList(int capacity) {
        n = capacity + 1;
        buf = new byte[capacity];
        buffer = ByteBuffer.wrap(buf);
        marks = new LinkedList<Integer>();
    }

    public int capacity() {
        return n - 1;
    }

    private int wrapIndex(int i) {
        int m = i % n;
        if (m < 0) { // java modulus can be negative
            m += n;
        }
        return m;
    }

    // This method is O(n) but will never be called if the
    // CircularArrayList is used in its typical/intended role.
    private void shiftBlock(int startIndex, int endIndex) {
        assert (endIndex > startIndex);
        for (int i = endIndex - 1; i >= startIndex; i--) {
            set(i + 1, get(i));
        }
    }


    public int size() {
        return tail - head + (tail < head ? n : 0);
    }



    public byte get(int i) {
        if (i < 0 || i >= size()) {
            throw new IndexOutOfBoundsException();
        }
        return buf[wrapIndex(head + i)];
    }

    public byte[] getArray (int startIndex , int endIndex){
        if (startIndex < 0 || startIndex >= size()) {
            throw new IndexOutOfBoundsException();
        }
        if (endIndex < 0 || endIndex >= size()) {
            throw new IndexOutOfBoundsException();
        }

        return Arrays.copyOfRange(buf, wrapIndex(head + startIndex), wrapIndex(head + endIndex));
    }

    public void set(int i, byte e) {
        if (i < 0 || i >= size()) {
            throw new IndexOutOfBoundsException();
        }

         buf[wrapIndex(head + i)] = e;
    }


    public void add(int i, byte e) {
        int s = size();
        if (s == n - 1) {
            throw new IllegalStateException("Cannot add element."
                    + " CircularArrayList is filled to capacity.");
        }
        if (i < 0 || i > s) {
            throw new IndexOutOfBoundsException();
        }
        tail = wrapIndex(tail + 1);
        if (i < s) {
            shiftBlock(i, s);
        }
        set(i, e);
    }

    public void add( byte e) {
        int s = size();
        if (s == n - 1) {
            throw new IllegalStateException("Cannot add element."
                    + " CircularArrayList is filled to capacity.");
        }

        buf[tail] = e;
        tail = wrapIndex(tail + 1);



    }


    public byte remove(int i) {
        int s = size();
        if (i < 0 || i >= s) {
            throw new IndexOutOfBoundsException();
        }
        byte e = get(i);
        if (i > 0) {
            shiftBlock(0, i);
        }
        head = wrapIndex(head + 1);
        return e;
    }

    public byte[] removeArray ( int endIndex){
        int s = size();

        if (endIndex < 0 || endIndex >= s) {
            Log.v("unity", "index Out of Range from CIRCULAR BUFFER");
            throw new IndexOutOfBoundsException();

        }

        byte [] e = getArray(0 ,endIndex);
        Log.v("unity", "buffer content : " + new String(e));

        head = wrapIndex(head + endIndex + 1);
        return e;

    }
}