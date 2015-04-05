package com.badran.bluetoothcontroller;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;





 class BtSender  {
	private BufferedOutputStream outBufferStream = null;
	private PrintWriter writer = null;


     private volatile ArrayList<Object []> outMessages;



	

	
	public volatile boolean isSending = false;
	private boolean stopSending = false;
	
	public BtSender (BufferedOutputStream outBufferStream, PrintWriter writer){

		this.outBufferStream = outBufferStream;
		this.writer = writer;
        outMessages = new ArrayList<Object []>();
    }
     void stop(){
         this.stopSending = true;
         outMessages.clear();

     }
	private class BtSenderThread extends Thread { 
	public    void  run(){

		isSending = true;
		while(!stopSending && outMessages.size() > 0){

			try{
				switch ((Integer)outMessages.get(0)[0]){
					case 0 : 
					outBufferStream.write((Integer)outMessages.get(0)[1]);
			        outBufferStream.flush();
			        break;
			        
					case 1 :writer.print((char [])outMessages.get(0)[1] );
							
							if(writer.checkError()) Bridge.controlMessage(-5);
					break;
					case 2 : 
					outBufferStream.write((byte [])outMessages.get(0)[1]);
			        outBufferStream.flush();
			        break;
			        default : break;
				}
				if(outMessages.size() >0)
				outMessages.remove(0);
				
				
			}catch (IOException e) {
				if(outMessages.size() >0)
					outMessages.remove(0);
				Bridge.controlMessage(-5);
				
			}
		}
		isSending =false;
		
	
	}
	}
	
	private void sendingThread(){
		if(!isSending   ){
			isSending = true;
            BtSenderThread SENDER;
            SENDER = new BtSenderThread();
            SENDER.start();
			
		} 
			
					}
	
	public  void sendChar(char data) {
		
		outMessages.add(new Object [] {(Integer)0,(int)data});
		sendingThread();
	}
	//////////////
	public  void sendBytes(byte [] data) {
		outMessages.add(new Object [] {(Integer)2,data});
		sendingThread();
	}
	
	public  void sendString(String data) {
		char [] temp3 = data.toCharArray();
		
		char [] temp4 = Arrays.copyOf(temp3, temp3.length+1);
		temp4[temp3.length] = '\n';


         outMessages.add(new Object[]{ (Integer)1, temp4});

        sendingThread();

	}
	

	

}
