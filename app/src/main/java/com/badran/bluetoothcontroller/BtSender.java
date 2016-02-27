package com.badran.bluetoothcontroller;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;

import java.util.LinkedList;
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


    Queue<Job> outMessages = new LinkedList<Job>();
    private Object lock1 = new Object();


    class Job {
        boolean isClose = false;
        byte[] msg;
        BufferedOutputStream bufferedOutputStream;
        final BluetoothConnection btConnection;
        public Job(BufferedOutputStream bufferedOutputStream, byte[] msg,BluetoothConnection btConnection) {
            this.msg = msg;
            this.bufferedOutputStream = bufferedOutputStream;
            this.btConnection = btConnection;
        }

        public Job(BufferedOutputStream bufferedOutputStream)
        {
            this.bufferedOutputStream = bufferedOutputStream;
            this.isClose = true;
            this.btConnection = null;
        }
    }


    void addJob(BufferedOutputStream bufferedOutputStream, byte[] msg,BluetoothConnection btConnection) {

        synchronized (lock1) {
            outMessages.add(new Job(bufferedOutputStream, msg,btConnection));
            if (!isSending) {
                isSending = true;
                (new Thread(new BtSenderThread())).start();

            }
        }


    }

    void addCloseJob(BufferedOutputStream bufferedOutputStream)
    {
        synchronized (this.lock1)
        {
            if (this.isSending) {
                this.outMessages.add(new Job(bufferedOutputStream));
            } else {
                try
                {
                    if (bufferedOutputStream != null)
                    {
                        bufferedOutputStream.flush();
                        bufferedOutputStream.close();
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }


    private class BtSenderThread implements Runnable {
        public void run() {


            while (true) {

                BtSender.Job job;
                synchronized (BtSender.this.lock1)
                {
                    if (BtSender.this.outMessages.size() <= 0)
                    {
                        BtSender.this.isSending = false;
                        break;
                    }
                    job = (BtSender.Job)BtSender.this.outMessages.poll();
                }
                try
                {
                    if (job.bufferedOutputStream != null) {
                        if (!job.isClose)
                        {
                            job.bufferedOutputStream.write(job.msg);
                            job.bufferedOutputStream.flush();
                        }
                        else
                        {
                            try
                            {
                                if (job.bufferedOutputStream != null)
                                {
                                    job.bufferedOutputStream.flush();
                                    job.bufferedOutputStream.close();
                                }
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    Log.v("PLUGIN . UNITY", "failed while write/sending data");
                    job.btConnection.RaiseSENDING_ERROR();
                }

            }
            
        }
    }


}
