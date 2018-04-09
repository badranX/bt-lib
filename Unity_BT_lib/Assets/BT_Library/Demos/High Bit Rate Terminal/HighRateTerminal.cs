using UnityEngine;
using System.Collections;
using System.Collections.Generic;

using TechTweaking.Bluetooth;
using UnityEngine.UI;

public class HighRateTerminal : MonoBehaviour
{
	
	public Text devicNameText;
	public ScrollTerminalUI readDataText;//ScrollTerminalUI is a script used to control the ScrollView text
	
	public GameObject InfoCanvas;
	public GameObject DataCanvas;
	private  BluetoothDevice device;
	public Text dataToSend;
	
	void Awake ()
	{
		BluetoothAdapter.askEnableBluetooth ();//Ask user to enable Bluetooth
		
		BluetoothAdapter.OnDeviceOFF += HandleOnDeviceOff;
		BluetoothAdapter.OnDevicePicked += HandleOnDevicePicked; //To get what device the user picked out of the devices list
		
	}
	
	void HandleOnDeviceOff (BluetoothDevice dev)
	{
		if (!string.IsNullOrEmpty (dev.Name))
			devicNameText.text = "Can't connect to " + dev.Name + ", device is OFF";
		else if (!string.IsNullOrEmpty (dev.Name)) {
			devicNameText.text = "Can't connect to " + dev.MacAddress + ", device is OFF";
		}
	}
	
	//############### UI BUTTONS RELATED METHODS #####################
	public void showDevices ()
	{
		BluetoothAdapter.showDevices ();//show a list of all devices//any picked device will be sent to this.HandleOnDevicePicked()
	}
	
	public void connect ()//Connect to the public global variable "device" if it's not null.
	{
		if (device != null) {
			device.connect ();
		}
	}
	
	public void disconnect ()//Disconnect the public global variable "device" if it's not null.
	{
		if (device != null)
			device.close ();
	}
	
	public void send ()
	{		
		if (device != null && !string.IsNullOrEmpty (dataToSend.text)) {
			device.send (System.Text.Encoding.ASCII.GetBytes (dataToSend.text + (char)10));//10 is our seperator Byte (sepration between packets)
		}
	}
	
	void HandleOnDevicePicked (BluetoothDevice device)//Called when device is Picked by user
	{
		
		this.device = device;//save a global reference to the device
		
		//this.device.UUID = UUID; //This is only required for Android to Android connection
		
		/* 
		 * setEndByte(10) will change how the read() method works.
		 * 10 equals the char '\n' which is a "new Line" in Ascci representation, 
		 * so the read() method will retun a packet that was ended by the byte 10, without including 10.
		 * Which means read() will read lines while excluding the '\n' new line charachter.
		 * If you don't use the setEndByte() method, device.read() will return any available data (line or not), then you can order/packatize them as you want.
		 * 
		 * Note: setEndByte will make reading lines or packest easier.
		 */
		device.setEndByte (10);
		
		
		//Assign the 'Coroutine' that will handle your reading Functionality, this will improve your code style
		//Other way would be listening to the event Bt.OnReadingStarted, and starting the courotine from there
		device.ReadingCoroutine = ManageConnection;
		
		devicNameText.text = device.Name;
		
	}
	
	
	//############### Reading Data  #####################
	//Please note that you don't have to use Couroutienes, you can just put your code in the Update() method
	//If you want to achieve a minimum delay please check the "High Bit Rate Terminal" demo
	IEnumerator  ManageConnection (BluetoothDevice device)
	{//Manage Reading Coroutine
		
		//Switch to Terminal View
		InfoCanvas.SetActive (false);
		DataCanvas.SetActive (true);
		
		
		while (device.IsReading) {
			
			//polll all available packets
			BtPackets packets = device.readAllPackets ();
			
			if (packets != null) {
				
				/*
				 * parse packets, packets are ordered by indecies (0,1,2,3 ... N),
				 * where Nth packet is the latest packet and 0th is the oldest/first arrived packet.
				 * 
				 */
				
				for (int i=0; i<packets.Count; i++) {
					
					//packets.Buffer contains all the needed packets plus a header of meta data (indecies and sizes) 
					//To parse a packet we need the INDEX and SIZE of that packet.
					int indx = packets.get_packet_offset_index (i);
					int size = packets.get_packet_size (i);
					
					string content = System.Text.ASCIIEncoding.ASCII.GetString (packets.Buffer, indx, size);
					readDataText.add (device.Name, content);
				}
			}
			
			
			yield return null;
		}
		
		//Switch to Menue View after reading stoped
		DataCanvas.SetActive (false);
		InfoCanvas.SetActive (true);	
	}
	
	
	//############### UnRegister Events  #####################
	void OnDestroy ()
	{
		BluetoothAdapter.OnDevicePicked -= HandleOnDevicePicked; 
		BluetoothAdapter.OnDeviceOFF -= HandleOnDeviceOff;
	}
	
}
