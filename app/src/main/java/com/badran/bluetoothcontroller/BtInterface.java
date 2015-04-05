package com.badran.bluetoothcontroller;
/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 * 
 */

import android.bluetooth.BluetoothAdapter;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;



import java.util.Set;
import java.util.UUID;



import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;




import com.unity3d.player.UnityPlayer;


public class BtInterface extends Thread {
	
	
	


	
	private BluetoothDevice device = null;
	private BluetoothSocket socket ;
	private BluetoothAdapter mBluetoothAdapter = null;
	private InputStream receiveStream = null;
	private BufferedReader receiveReader = null;
	private OutputStream sendStream = null;	
	
	private PrintWriter writer = null;
	private BufferedOutputStream outBufferStream = null;
	
	private final String TAG = "PLUGIN";
	
	private BtReader RECEIVER = null;  /////must change

	
	
	private BtSender SENDER = null;
		private boolean isChineseMobile = false;
	
	
	private boolean isConnected = false;
	
	
	private int connectionTrials = 2;
	
	
	class ControlThread {


	   
	    public volatile boolean listening = false;
	    
	    public volatile boolean isSending = false;
	  }
	private boolean isDevicePicked = false;
	private final ControlThread control = new ControlThread();
	public BtInterface(BluetoothAdapter tempBluetoothAdapter) {
		this.mBluetoothAdapter =tempBluetoothAdapter;
		
		
	}
	public BtInterface(BluetoothAdapter tempBluetoothAdapter,BluetoothDevice bluetoothDevice) {
		this.mBluetoothAdapter =tempBluetoothAdapter;
		this.device = bluetoothDevice;
		this.isDevicePicked = true;
	}
	
	public void connect(int trialsNumber){
		this.connectionTrials = trialsNumber;
		socket = createSocket();


	}
	private BluetoothSocket createSocket(){
		
		final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
		boolean foundModule = false;
		if(!this.isDevicePicked){
		Set<BluetoothDevice> setPairedDevices;
            setPairedDevices = mBluetoothAdapter.getBondedDevices();

            BluetoothDevice[] pairedDevices = setPairedDevices.toArray(new BluetoothDevice[setPairedDevices.size()]);


            for (BluetoothDevice pairedDevice : pairedDevices) {


                if (Bridge.mac)
                    foundModule = pairedDevice.getAddress().equals(Bridge.ModuleMac);
                else
                    foundModule = pairedDevice.getName().equals(Bridge.ModuleName);

                if (foundModule) {
                    device = pairedDevice;
                    break;
                }
            }
            setPairedDevices = null;
		} else foundModule = true;
		
		
		BluetoothSocket tmpSocket = null;
			if(foundModule || this.isDevicePicked) {
				

			

			try {

				 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
					 tmpSocket = Level10.getInstance().createRfcommSocket(device, SPP_UUID);
					
				 } else if (isChineseMobile){

					 Method m;
					   try {
					    	m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class         });
					    	tmpSocket = (BluetoothSocket) m.invoke(device, 1);  
					        } catch (SecurityException e) {
					    	Log.v(TAG,  e.getMessage());
					    	} catch (NoSuchMethodException e) {
					    	Log.v(TAG, e.getMessage());
					    	} catch (IllegalArgumentException e) {
					    	Log.v(TAG, e.getMessage());
					    	} catch (IllegalAccessException e) {
					    	Log.v(TAG,  e.getMessage());
					    	} catch (InvocationTargetException e) {
					    	Log.v(TAG,  e.getMessage());
					    	}         
				 }
				 
				 else tmpSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
				 
				 
			} catch (IOException mainError) {
						Log.v(TAG, mainError.getMessage());
						close();     
						Bridge.controlMessage(-1);
						
						
					}      
			
			
				
				
				
				
						
				
			}else {
				
			Bridge.controlMessage(-2);
			
			
		}
			return tmpSocket;
	}
	
	void chineseMobile(){
		

		
			if(socket != null){
				try {
			socket.close();
				}catch(IOException ignored){}
                socket = null;
			}
			isConnected = false;
		isChineseMobile = !isChineseMobile;
		socket = null;
		socket = createSocket();
		
		
		
	}
 private void inetializeStreams(boolean input, boolean output){
	 if(input){
	 try{
		 
			 receiveStream = socket.getInputStream();
		
			 
	 }catch(IOException e){
		 
		 Log.v("yahya", "inFailed" + e.getMessage());
	 }
	        receiveReader = new BufferedReader(new InputStreamReader(receiveStream)); //it's not used

         RECEIVER = new BtReader(socket, receiveStream,receiveReader);
         RECEIVER.startListeningThread();
	 
	 }
	 if(output){
		 try{
			 
			 
		
			 sendStream = socket.getOutputStream();
	 }catch(IOException e){
		 Log.v("yahya", "outFailed" + e.getMessage());
	 
	 }
		 
		    outBufferStream =  new BufferedOutputStream(sendStream);
			writer = new PrintWriter(sendStream,true);
			SENDER = new BtSender(outBufferStream,writer);
	 }
				
				//receiveReader = new BufferedReader(new Reader(receiveStream));
				
				
				
				
 }
 
	public void run() {
        if(socket != null) {
            boolean tryAgain, failed;
            int counter = 1;
            do {
                tryAgain = false;
                failed = false;

                mBluetoothAdapter.cancelDiscovery();
                try {

                    socket.connect();
                } catch (IOException e) {
                    Log.v(TAG, "TRYING TO CONNECT AGAIN : " + String.valueOf(isChineseMobile) + " : " + e.getMessage());
                    if (counter < connectionTrials) {
                        chineseMobile();
                        tryAgain = true;
                        counter++;
                    } else {
                        close();
                        failed = true;
                        Bridge.controlMessage(-3);
                    }
                }


                if (tryAgain) try {
                    Thread.sleep(100);

                } catch (InterruptedException e) {

                    Log.v(TAG,"Sleep Thread Interrupt Exception");
                }
                if (!tryAgain && !failed) {

                    inetializeStreams(Bridge.enableReading, true);
                    isConnected = true;
                    UnityPlayer.UnitySendMessage("BtConnector", "connection", "1");

                    Bridge.controlMessage(1);



                }

            } while (tryAgain);
        }
	}
	
	
	public boolean isConnected() { return isConnected;}
	public boolean isSending() { return control.isSending;}
	public boolean isListening(){return control.listening;}
	
	
	public void close() {
	
		
            RECEIVER.stop();
            SENDER.stop();

			if(receiveStream != null) {try{receiveStream.close();}  catch (Exception e) {receiveStream = null;}}
			if(receiveReader != null) {try{receiveReader.close();}catch (Exception e) {receiveReader = null;}}
			if(sendStream != null)  {try{sendStream.flush();sendStream.close();}catch (Exception e) {sendStream = null;}}
			if(writer != null) { try{writer.flush();writer.close();}catch (Exception e) {writer = null;}}
			if(outBufferStream != null) {try{outBufferStream.flush();outBufferStream.close();}catch (Exception e3) {outBufferStream = null;}}

			try {
				if(socket != null){
				socket.close();
				socket = null;
				}
				isConnected = false;
			Bridge.controlMessage(2);
            
		} 
		catch (IOException e) {
			
			Bridge.controlMessage(-4);
		}finally{
			
			UnityPlayer.UnitySendMessage("BtConnector", "connection","");}
	}

		
		
		
		public  void sendString(String data) {
			SENDER.sendString(data);

		}
		
		
		
		
		public  void sendChar(char data) {
			
			SENDER.sendChar(data);
		}
		//////////////
		public  void sendBytes(byte [] data) {
			SENDER.sendBytes(data);
		}
		
		
		public void doneReading(){
			RECEIVER.doneReading();
		}
		
		public byte [] readBuffer() { 
			return RECEIVER.readBuffer();
			
		}
		 public  String readLine(){
				return RECEIVER.readLine();
				
				
			}
		    
		
		    


			
			
		   
			

			

	public void listen (){// read chars
			
				
		RECEIVER.listen();
			
			
			}


			
			public boolean available(){ return RECEIVER.available();}
		    
		
	//public static boolean TESTINGvariable = false;
	
	
	

	
	
}
