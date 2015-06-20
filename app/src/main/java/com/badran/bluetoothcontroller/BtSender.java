package com.badran.bluetoothcontroller;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


class BtSender {

    private static BtSender instance = null;


    protected BtSender() {

        // Exists only to defeat instantiation.

    }

    public static BtSender getInstance() {
        if (instance == null) {
            instance = new BtSender();
        }
        return instance;
    }


    volatile boolean isSending = false;


    Queue<Job> outMessages = new ConcurrentLinkedQueue<Job>();
    private Object lock1 = new Object();


    class Job {
        byte[] msg;
        BufferedOutputStream bufferedOutputStream;

        public Job(BufferedOutputStream bufferedOutputStream, byte[] msg) {
            this.msg = msg;
            this.bufferedOutputStream = bufferedOutputStream;
        }
    }


    void addJob(BufferedOutputStream bufferedOutputStream, byte[] msg) {


        synchronized (lock1) {
            outMessages.add(new Job(bufferedOutputStream, msg));
            Log.v("unity", "Acquired Lock for Sending");
            if (!isSending) {
                Log.v("unity", "Started A thread for sending");
                isSending = true;
                (new Thread(new BtSenderThread())).start();

            }
        }


    }


    private class BtSenderThread implements Runnable {
        public void run() {

            Job job;
            while (true) {
                synchronized (lock1) {
                    if (outMessages.size() <= 0) {
                        isSending = false;
                        break;
                    } else job = outMessages.poll();
                }
                try {
                    job.bufferedOutputStream.write(job.msg);
                } catch (IOException e) {

                    PluginToUnity.ControlMessages.SENDING_ERROR.send(1);
                }


            }


        }
    }


}
