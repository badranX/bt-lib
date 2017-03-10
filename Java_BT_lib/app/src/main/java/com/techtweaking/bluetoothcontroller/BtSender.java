package com.techtweaking.bluetoothcontroller;

import android.util.Log;

import com.techtweaking.library.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;

import java.util.LinkedList;
import java.util.Queue;


class BtSender {

    private static final String TAG = "PLUGIN . UNITY";

    private static BtSender instance = null;


    private BtSender() {

        // Exists only to defeat instantiation.

    }

    public static BtSender getInstance() {
        if (instance == null) {
            instance = new BtSender();
        }
        return instance;
    }


    private volatile boolean isSending = false;


    private final Queue<Job> outMessages = new LinkedList<Job>();
    private final Object lock1 = new Object();


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
    /*
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
    */

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
                    job = BtSender.this.outMessages.poll();
                }

                if (job.bufferedOutputStream != null) {
                    if (!job.isClose)
                    {
                        try {
                            job.bufferedOutputStream.write(job.msg);
                        }catch (IOException e)
                        {
                            if( job.btConnection != null) job.btConnection.RaiseSENDING_ERROR();
                            Log.e(TAG, "failed sending data while write/sending", e);
                        }

                        try {
                            job.bufferedOutputStream.flush();
                        }catch (IOException e)
                        {
                            Log.w(TAG, "failed flushing buffer while write/sending data", e);
                        }
                    }
                    else
                    {
                        IOUtils.closeQuietly(job.bufferedOutputStream);
                    }
                }


            }

        }
    }


}