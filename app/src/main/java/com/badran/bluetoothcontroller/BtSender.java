package com.badran.bluetoothcontroller;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;





public class BtSender  {
	private BufferedOutputStream outBufferStream = null;
	private PrintWriter writer = null;
	private BtSenderThread SENDER;
	
	class MessagesThread {
		public volatile ArrayList<Object []> outMessages = new ArrayList<Object []>();
		
		public volatile char charMessage = (char)0;
		public volatile String stringMessage = "";
		public volatile byte[] bytesMessage  = new byte []{};
	  }
	
	private final  MessagesThread messagesThread = new MessagesThread();
	
	public volatile boolean isSending = false;
	private boolean sending = true;
	
	public BtSender (BufferedOutputStream outBufferStream, PrintWriter writer){
		this.outBufferStream = outBufferStream;
		this.writer = writer;
	}
	
	private class BtSenderThread extends Thread { 
	public    void  run(){
		
		isSending = true;
		while(sending && messagesThread.outMessages.size() > 0){
			
			try{
				switch ((Integer)messagesThread.outMessages.get(0)[0]){
					case 0 : 
					outBufferStream.write((Integer)messagesThread.outMessages.get(0)[1]);
			        outBufferStream.flush();
			        break;
			        
					case 1 :writer.print((char [])messagesThread.outMessages.get(0)[1] );
							
							if(writer.checkError()) Bridge.controlMessage(-5);
					break;
					case 2 : 
					outBufferStream.write((byte [])messagesThread.outMessages.get(0)[1]);
			        outBufferStream.flush();
			        break;
			        default : break;
				}
				if(messagesThread.outMessages.size() >0)
				messagesThread.outMessages.remove(0);
				
				
			}catch (IOException e) {
				if(messagesThread.outMessages.size() >0)
					messagesThread.outMessages.remove(0);
				Bridge.controlMessage(-5);
				
			}
		}
		isSending =false;
		
	
	}
	}
	
	private void sendingThread(){
		if(!isSending   ){
			isSending = true;
			SENDER = new BtSenderThread(); 
			SENDER.start();
			
		} 
			
					}
	
	public  void sendChar(char data) {
		
		messagesThread.outMessages.add(new Object [] {(Integer)0,(int)data});
		sendingThread();
	}
	//////////////
	public  void sendBytes(byte [] data) {
		messagesThread.outMessages.add(new Object [] {(Integer)2,data});
		sendingThread();
	}
	
	public  void sendString(String data) {
		char [] temp3 = data.toCharArray();
		
		char [] temp4 = Arrays.copyOf(temp3, temp3.length+1);
		temp4[temp3.length] = '\n';
		
		
		
		
		messagesThread.outMessages.add(new Object [] {(Integer)1,temp4});
		
		sendingThread();

	}
	
	public void stopSending(){
		
		sending = false;
	}
	

}
