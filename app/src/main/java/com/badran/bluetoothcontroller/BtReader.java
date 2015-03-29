package com.badran.bluetoothcontroller;

import java.io.IOException;
import java.io.InputStream;

import android.bluetooth.BluetoothSocket;


import com.unity3d.player.UnityPlayer;

public class BtReader {
	
private BtReceiver RECEIVER;
	
	
	private BluetoothSocket socket ;
	private InputStream receiveStream = null;
	 public volatile boolean MODE = true;
	    public volatile boolean MODE2 = false;
	    public volatile boolean MODE3 = false;
	    public volatile boolean listening = false;
	    volatile int modeIndex2 = 0;
	    
	    public volatile byte stopByte ;
	    private String stringData = "";
	    private boolean lineAvailable = false;
	    private boolean dataAvailable = false;
	    private boolean bufferDataAvailable = false;
	    public volatile boolean startListen = true;
	    public volatile int BufferLength = 0;
	    
	    private byte [] tempBuffer = {};
	    private byte [] buffer = {};
	    
		
	
	
	public void defaultValues(boolean tempMode0,boolean tempMode2,boolean tempMode3,int tempLength,byte tempStopByte,boolean stop){
		MODE = tempMode0;
		MODE2 = tempMode2;
		MODE3 = tempMode3;
		stopByte = tempStopByte;
		startListen = stop;
		
		if(tempLength != BufferLength){
			tempBuffer = new byte [tempLength];
			BufferLength = tempLength;
		}
	
	}
	
	    
	    public byte [] readBuffer() { 
			dataAvailable = false;
			if (buffer == null || !bufferDataAvailable) return new byte [] {};
			if(MODE2) modeIndex2 = 0;
			bufferDataAvailable = false;
			
			//byte [] tempBuffer = Arrays.copyOf(buffer, buffer.length);
			//Arrays.fill(buffer, (byte)0);
			return buffer;
			
		}
	    
	    
	    public  String readLine(){
			dataAvailable = false;
			if(stringData.length() > 0){
				String tempMessage = stringData;
				stringData = "";
				lineAvailable = false;
				return tempMessage;
				
				}else  return  "";
			
			
		}
	    
	    public void listen (boolean start){ // read lines
			startListen = start;
			MODE2 = false;
			MODE = true;
			MODE3 = false;
				
		if(!listening && start){
			
			//RECEIVER.start(); //read lines
			RECEIVER = new BtReceiver();
			RECEIVER.start();
			
				
				}
				
			

		}
	    
	    ///must change
	    public void startListeningThread(BluetoothSocket socket,InputStream receiveStream ){
			this.socket = socket;
			this.receiveStream = receiveStream;
			
	    	if(!listening ){
			
			//RECEIVER.start(); //read lines
			RECEIVER = new BtReceiver();
			RECEIVER.start();
			
				
				}
	    }
	    ///must change
	    
	    
		public void stopListen(){
			
			startListen = false;
		}
		public void doneReading(){
			dataAvailable = false;
			
			
			
			
			lineAvailable = false;
		}
		
		
		
		
		public boolean mode(){return MODE;}
		
		public void listen (boolean start, int length, boolean byteLimit){// read chars
		
			
			startListen = start;
			MODE2 = false;
			MODE = false;
			MODE3 = byteLimit;
			
			
		if(start){
			if(length != BufferLength){
			tempBuffer = new byte [length];
			BufferLength = length;
			}
		if(!listening ){
			
			
			//RECEIVER.start();//read chars
			RECEIVER = new BtReceiver();
			RECEIVER.start();
				}
		
		}
		
		
		}
public void listen (boolean start, int length,byte stopByte){// read chars
		
			
			this.startListen = start;
			this.stopByte = stopByte;
			this.MODE2 = true;
			this.MODE = false;
			this.MODE3 = false;
		if(start){
			if(length != BufferLength){
			this.tempBuffer = new byte [length];
			BufferLength = length;
			}
		if(!listening ){
			
			
			//RECEIVER.start();//read chars
			RECEIVER = new BtReceiver();
			RECEIVER.start();
				}
		
		}
		
		
		}


		
		public boolean available(){ return dataAvailable;}
	    
	    
	private class BtReceiver extends Thread {
		
		
		
		/////////////////////////////////////////String dataToSend = ""; 
		@Override public    void run() {
			
			listening = true;
			int firstIndex = 0;
			
		 String dataToSend;
			while(socket != null && startListen ) {
				listening = true;
				
				
				try {
					if(receiveStream.available() > 0) {
						
						
								
						
						if(MODE){
							if(!lineAvailable){
								
							dataToSend = readLine();
						
						
						
							stringData = dataToSend;
							UnityPlayer.UnitySendMessage("BtConnector", "reciever", dataToSend);
							lineAvailable = true;
							dataAvailable = true;
			                dataToSend = "";
							}
						}
						else{
						
						if(MODE2 || MODE3){if(!bufferDataAvailable) {
							int tempByte ;
							int newLength = tempBuffer.length - firstIndex  ;
							int i = firstIndex;
							boolean notFound = true;
							for(; i< newLength;i++){
								tempByte =  receiveStream.read();
								//tempByte = receiveBuffer.read();
								
								if(tempByte >=0){
									
									if((tempByte == stopByte) && !MODE3) {
										firstIndex = 0;
										buffer = java.util.Arrays.copyOf(tempBuffer,i );
										
										UnityPlayer.UnitySendMessage("BtConnector", "startReading", "");
										bufferDataAvailable = true;
										dataAvailable = true;
										notFound = false;
										
										
										  
										break;}
									tempBuffer[i] = (byte)tempByte;
								
								}
								
								else {firstIndex = i;notFound = false;break;}
								
								
							}
							if(notFound){
								firstIndex = 0;
								buffer = java.util.Arrays.copyOf(tempBuffer,tempBuffer.length);
								UnityPlayer.UnitySendMessage("BtConnector", "startReading", "");
							bufferDataAvailable = true;
							  dataAvailable = true;
							  //Arrays.fill(tempBuffer, (byte)0);
							}}
						continue;}else{	
							
							if(!bufferDataAvailable)  {

							int tempByte2 ;
							int newLength2 = tempBuffer.length - modeIndex2  ;
							//int i = modeIndex2;
							
							boolean read = false;
							int i = 0;
							for(; i< newLength2;i++){
							if(receiveStream.available() > 0) {
								//tempByte2 = receiveBuffer.read();
								tempByte2 =  receiveStream.read();
								
								if(tempByte2 >=0){
									
									tempBuffer[i] = (byte)tempByte2;
									read = true;
									//TESTINGvariable = true;
								}else {break;}
							}
								else { modeIndex2 = i;break;}
								
								
							}
							
							
							if(read){
								//modeIndex2 = 0;
								//TESTINGvariable = true;
								buffer = java.util.Arrays.copyOf(tempBuffer,i );
								UnityPlayer.UnitySendMessage("BtConnector", "startReading","");
								
								//notFound = false;
								read = false;
								bufferDataAvailable = true;
								dataAvailable = true;
							}
								
						}
							continue;}
							  
							 /* else{
								//Arrays.fill(buffer, (byte)0);
								buffer = java.util.Arrays.copyOf(tempBuffer,modeIndex2+1);
								bufferDataAvailable = true;
								dataAvailable = true;
							}*/
							 
							  
							
							 
							 
						
							 
						////////////////////}
			           
					}
				}}
				catch (IOException e) {
					listening = false;
					Bridge.controlMessage(-6);
				}
					
				
				
				
			} listening = false;
			
		}
	}
}
