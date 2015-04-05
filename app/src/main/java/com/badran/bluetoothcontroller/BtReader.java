package com.badran.bluetoothcontroller;

import java.io.IOException;
import java.io.InputStream;
import 	java.io.BufferedReader;
import android.bluetooth.BluetoothSocket;

//import android.util.Log;
import android.widget.Switch;

import com.unity3d.player.UnityPlayer;

 class BtReader {
	
    private BtReceiver RECEIVER;
	
	
	private BluetoothSocket socket ;
	private InputStream receiveStream = null;
    private BufferedReader receiveReader = null;

	    public volatile boolean listening = false;
	    volatile int modeIndex2 = 0;
	    

	    private String stringData = "";
	    private boolean lineAvailable = false;
	    private boolean dataAvailable = false;
	    private boolean bufferDataAvailable = false;
        private boolean stopReading = false;


	    private byte [] tempBuffer = {};
	    private byte [] buffer = {};
	    
		
	public BtReader (BluetoothSocket socket,InputStream receiveStream, BufferedReader receiveReader){
        this.socket = socket;
        this.receiveStream = receiveStream;
        this.receiveReader = receiveReader;
        tempBuffer = new byte [Bridge.lengthB];
    }
	

	
	    
	    public byte [] readBuffer() { 
			dataAvailable = false;
			if (buffer == null || !bufferDataAvailable) return new byte [] {};
			if(Bridge.MODE == Bridge.MODES.mode1) modeIndex2 = 0;
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
	    


        public void listen (){ // read lines



            if(Bridge.enableReading){
                boolean temp = Bridge.MODE == Bridge.MODES.mode0;
                if(!temp && tempBuffer.length != Bridge.lengthB){
                    tempBuffer = new byte [Bridge.lengthB];

                }
                if(!listening ){


                    //RECEIVER.start();//read chars
                    RECEIVER = new BtReceiver();
                    RECEIVER.start();
                }

            }


        }
	    
	    ///must change
	    public void startListeningThread(){

	    	if(!listening ){
			
			//RECEIVER.start(); //read lines
			RECEIVER = new BtReceiver();
			RECEIVER.start();
			
				
				}
	    }
	    ///must change
	    
	    

		public void doneReading(){
			dataAvailable = false;
			
			
			
			
			lineAvailable = false;
		}
		
		
		
		

		void stop(){
            this.stopReading = true;

        }




		
	public boolean available(){ return dataAvailable;}
	    
	    
	private class BtReceiver extends Thread {
		
		
		
		/////////////////////////////////////////String dataToSend = ""; 
		@Override public    void run() {
			
			listening = true;
			int firstIndex = 0;


			while(socket != null && Bridge.enableReading && !stopReading) {
                //Log.v("PLUGIN","YEEESSSSSS");
                //Log.v("PLUGIN",Bridge.MODE.toString());
                try {
                if (receiveStream.available() > 0) {
                    switch (Bridge.MODE) {
                        case mode0:
                            if (!lineAvailable) {


                                stringData = receiveReader.readLine();

                                //Log.v("PLUGIN", "DOODODODODODODD");
                                //changed METHOD NAME::
                                UnityPlayer.UnitySendMessage("BtConnector", "receiver", stringData);
                                lineAvailable = true;
                                dataAvailable = true;

                            }
                            break;

                        case mode1:
                        case mode2:
                            if (!bufferDataAvailable) {
                                int tempByte;
                                int newLength = tempBuffer.length - firstIndex;
                                int i = firstIndex;
                                boolean notFound = true;
                                for (; i < newLength; i++) {
                                    tempByte = receiveStream.read();
                                    //tempByte = receiveBuffer.read();

                                    if (tempByte >= 0) {

                                        if ((tempByte == Bridge.stopByteB) && Bridge.MODE != Bridge.MODES.mode2) {
                                            firstIndex = 0;
                                            buffer = java.util.Arrays.copyOf(tempBuffer, i);

                                            UnityPlayer.UnitySendMessage("BtConnector", "startReading", "");
                                            bufferDataAvailable = true;
                                            dataAvailable = true;
                                            notFound = false;


                                            break;
                                        }
                                        tempBuffer[i] = (byte) tempByte;

                                    } else {
                                        firstIndex = i;
                                        notFound = false;
                                        break;
                                    }


                                }
                                if (notFound) {
                                    firstIndex = 0;
                                    buffer = java.util.Arrays.copyOf(tempBuffer, tempBuffer.length);
                                    UnityPlayer.UnitySendMessage("BtConnector", "startReading", "");
                                    bufferDataAvailable = true;
                                    dataAvailable = true;
                                    //Arrays.fill(tempBuffer, (byte)0);
                                }
                            }
                            break;

                        case mode3:
                            if (!bufferDataAvailable) {

                                int tempByte2;
                                int newLength2 = tempBuffer.length - modeIndex2;
                                //int i = modeIndex2;

                                boolean read = false;
                                int i = 0;
                                for (; i < newLength2; i++) {
                                    if (receiveStream.available() > 0) {
                                        //tempByte2 = receiveBuffer.read();
                                        tempByte2 = receiveStream.read();

                                        if (tempByte2 >= 0) {

                                            tempBuffer[i] = (byte) tempByte2;
                                            read = true;
                                            //TESTINGvariable = true;
                                        } else {
                                            break;
                                        }
                                    } else {
                                        modeIndex2 = i;
                                        break;
                                    }


                                }


                                if (read) {
                                    //modeIndex2 = 0;
                                    //TESTINGvariable = true;
                                    buffer = java.util.Arrays.copyOf(tempBuffer, i);
                                    UnityPlayer.UnitySendMessage("BtConnector", "startReading", "");

                                    //notFound = false;

                                    bufferDataAvailable = true;
                                    dataAvailable = true;
                                }

                            }
                            break;

                    }
                }
                }catch (IOException e) {
                        listening = false;
                        Bridge.controlMessage(-6);
                    }



					
				
				
				
			} listening = false;
			
		}
	}
}
