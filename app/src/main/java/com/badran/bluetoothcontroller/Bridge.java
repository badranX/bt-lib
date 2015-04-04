package com.badran.bluetoothcontroller;

/* Android PlugIn for Unity Game Engine
 * By Tech Tweaking
 */

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.unity3d.player.UnityPlayer;

import android.content.BroadcastReceiver;
import android.content.Context;
//import android.content.Intent;
import android.content.IntentFilter;




public class Bridge  {
	
	
	private static BluetoothAdapter mBluetoothAdapter =  BluetoothAdapter.getDefaultAdapter();
	
	private static BtInterface B = null;
	private static ArrayList<BtInterface> deviceInterfaces = new ArrayList<BtInterface>();
	public static String ModuleName = "HC-05";
	public static boolean mac = false;
	public static String ModuleMac = "";
	
	
	private static int CMessage = 0;
	
	private static boolean mode0 = true;
	private static boolean mode2 = false;
	private static boolean mode3 = false;
	
	private static int lengthB = 0;
	private static byte stopByteB = 0;
	
	private static boolean stopListen = false;
	public static void askEnableBluetooth(){
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		
		UnityPlayer.currentActivity.startActivity(enableBtIntent);
		
	    

	}
	
	
	
    public static synchronized void controlMessage(int message){
    	
      CMessage = message;
        	
        	
    }
   
   
    public static void moduleName(String Module ){
    	mac = false;
    	ModuleName = Module;
    }
    public static void moduleMac (String Mac){
    	mac = true;
    	ModuleMac = Mac;
    }
    
  
    
  public static boolean enableBluetooth(){
	  if(mBluetoothAdapter != null) { return mBluetoothAdapter.enable(); } else return false;
	  
	  
  }
  
  
    
	 public static boolean  isBluetoothEnabled(){
		 
    	if(mBluetoothAdapter != null) {return mBluetoothAdapter.isEnabled();} else return false;
    }
  

    public static int  connect(int trialsNumber) {
        if(deviceInterfaces.size() < trialsNumber){
         //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
         if (mBluetoothAdapter == null) {
         	return -2;
         }
         
         	if (!mBluetoothAdapter.isEnabled()){
         		return -1;
         	} 
         	
         		
         		if(B != null) {
         			B.interrupt();
         			B.close();
         			
         			B=null;
         			
         		}	
         		
         		if(isDevicePicked)
         			B = new BtInterface(mBluetoothAdapter,BtDevice);
         		else
         			B = new BtInterface(mBluetoothAdapter);
         			
         			B.defaultValues(mode0,mode2,mode3,lengthB,stopByteB,stopListen);
         		
         			B.connect(trialsNumber);
         		    B.start();

         		 
         		
            
         }
        
        return 1;
     }
    
    public static int  connect(int trialsNumber, int deviceOrder) {
        if(!isConnected()){
         //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
         if (mBluetoothAdapter == null) {
         	return -2;
         }
         
         	if (!mBluetoothAdapter.isEnabled()){
         		return -1;
         	} 
         	
         		
         		if(B != null) {
         			B.interrupt();
         			B.close();
         			
         			B=null;
         			
         		}	
         		
         		if(isDevicePicked)
         			B = new BtInterface(mBluetoothAdapter,BtDevice);
         		else
         			B = new BtInterface(mBluetoothAdapter);
         			
         			B.defaultValues(mode0,mode2,mode3,lengthB,stopByteB,stopListen);
         		
         			B.connect(trialsNumber);
         		 B.start();
         		 
         		
            
         }
        
        return 1;
     }
	
		public static void close(){
			if(B != null) {
    			B.interrupt();
    			B.close();
    			B=null;
    		}
					
			
		
		}
		public static void sendString(String message){
			//CMessage("Move Forward");
			if( isConnected())
			B.sendString(message);
		}
		
		public static void sendChar(char message){
			if( isConnected())
			B.sendChar(message);
		}
		public static void sendBytes(byte [] message){
			if( isConnected()){
				
			B.sendBytes(message);
			
			}
		}
		
		public static String read(){
			
				listen(true);
			if(!isConnected()) return "";
			return B.readLine();
				
		}
		
		public static byte [] readBuffer(int length){
				
				listen(true,length,false);
			if( !isConnected()) return new byte [] {};
			
			
			return B.readBuffer();
			
		}
		
		public static byte [] readBuffer(){
			
			
		if( !isConnected()) return new byte [] {};
		
		
		return B.readBuffer();
		
	}
		public static byte [] readBuffer(int length,byte stopByte){
			listen(true,length,stopByte);
			if( !isConnected()) return new byte [] {};
			
			
			return B.readBuffer();
			
		}
		
		public static boolean available(){
			if(!isConnected()) return false;
			else return B.available();
		
		}
		
		public static int controlData(){
			return CMessage;
		}
		
		public static boolean isSending(){
			if( B == null) return false;
		return B.isSending();
		}
		public static boolean isConnected(){
			if(B == null) return false;
			return B.isConnected();
		}
		
		//is there a bytelimit
		public static void listen(boolean start,int length,boolean byteLimit){
			
			if(B != null) 
			B.listen(start,length,byteLimit);
			
				
				
				lengthB = length;
				mode0 = false;
				mode2 = false;
				mode3 = byteLimit;
				stopListen = start;
				
			
		}
public static void listen(boolean start,int length,byte stopByte){
			
			if(B != null) 
			B.listen(start,length,stopByte);
			
				
				
				lengthB = length;
				stopByteB = stopByte;
				stopListen = start;
				mode0 = false;
				mode2 = true;
				mode3 = false;
		}
		
		public static void listen(boolean start){
			if(B != null) 
			B.listen(start);
			
				
				
				stopListen = start;
				
				mode0 = true;
				mode2 = false;
				mode3 = false;
			
			
		}
		
		public static void stopListen(){
			
			if(B != null){
				B.stopListen();
			} 
				
				stopListen = false;
				
			
		}
		public static boolean isListening(){
			if(B == null) return false;
			else return B.isListening();
				
		}
		public static void doneReading(){
			
			if(B != null) 
			  B.doneReading();
		}
	/*
		public static boolean TESTING (){
			return BtInterface.TESTINGvariable;
		}
		*/
		
		// show devices
		static BluetoothDevicePickerReceiver mBluetoothPickerReceiver = new BluetoothDevicePickerReceiver();
	    public static void showDevices () {
	    	
			IntentFilter deviceSelectedFilter = new IntentFilter();
			deviceSelectedFilter.addAction(BluetoothDevicePicker.ACTION_DEVICE_SELECTED);

			UnityPlayer.currentActivity.registerReceiver(mBluetoothPickerReceiver, deviceSelectedFilter);
			
	        UnityPlayer.currentActivity.startActivity(new Intent(BluetoothDevicePicker.ACTION_LAUNCH)
	                .putExtra(BluetoothDevicePicker.EXTRA_NEED_AUTH, false)
	                .putExtra(BluetoothDevicePicker.EXTRA_FILTER_TYPE, BluetoothDevicePicker.FILTER_TYPE_ALL)
	                .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
	



	    }

	   static private class BluetoothDevicePickerReceiver extends BroadcastReceiver implements  BluetoothDevicePicker  {

	        /*
	         * (non-Javadoc)
	         * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	         * android.content.Intent)
	         */
	        @Override
	        public void onReceive(Context context, Intent intent) {
	            if (ACTION_DEVICE_SELECTED.equals(intent.getAction())) {
	                // context.unregisterReceiver(this);
	                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	                BtDevice= device;
	                UnityPlayer.UnitySendMessage("BtConnector", "devicePicked","");
	            }

	            }
	        }



	    public static BluetoothDevice  BtDevice;
	    public static BluetoothDevice getPickedDevice (){
	        if(BtDevice != null)
	        return BtDevice;
	        return  null;
	    }
	    
	    private static boolean isDevicePicked = false;
	    public static boolean setBluetoothDevice(BluetoothDevice B){
	    	if(B == null || !(B instanceof BluetoothDevice))
	    		return false;
	    	BtDevice = B;
	    	isDevicePicked = true;
	    	return true;
	    	
	    	
	    }
	    public static String BluetoothDeviceName (BluetoothDevice B) {
	        return B.getName();

	    }
	    
	    public static String BluetoothDeviceMac (BluetoothDevice B) {
	        return B.getAddress();

	    }


	    //end show devices
		
}

