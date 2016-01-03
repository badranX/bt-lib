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
        final int deviceID;
        public Job(BufferedOutputStream bufferedOutputStream, byte[] msg,int deviceID) {
            this.msg = msg;
            this.bufferedOutputStream = bufferedOutputStream;
            this.deviceID = deviceID;
        }
    }


    void addJob(BufferedOutputStream bufferedOutputStream, byte[] msg,int deviceID) {

        synchronized (lock1) {
            outMessages.add(new Job(bufferedOutputStream, msg,deviceID));
            if (!isSending) {
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
                    job.bufferedOutputStream.flush();
                } catch (IOException e) {
                    Log.v("PLUGIN . UNITY","failed while write/sending data");
                    PluginToUnity.ControlMessages.SENDING_ERROR.send(job.deviceID);
                }

            }
            
        }
    }


}
