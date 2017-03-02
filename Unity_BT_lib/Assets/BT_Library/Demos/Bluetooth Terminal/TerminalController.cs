using UnityEngine;
using System.Collections;
using System.Collections.Generic;

using TechTweaking.Bluetooth;
using UnityEngine.UI;

public class TerminalController : MonoBehaviour
{

	public Text devicNameText;
	public Text status;
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
			status.text = "Couldn't connect to " + dev.Name + ", device is OFF";
		else if (!string.IsNullOrEmpty (dev.Name)) {
			status.text = "Couldn't connect to " + dev.MacAddress + ", device is OFF";
		}
	}

	void HandleOnDevicePicked (BluetoothDevice device)//Called when device is Picked by user
	{
		this.device = device;//save a global reference to the device


		//this.device.UUID = UUID; //This is only required for Android to Android connection
		
		/*
		 * 10 equals the char '\n' which is a "new Line" in Ascci representation, 
		 * so the read() method will retun a packet that was ended by the byte 10. simply read() will read lines.
		 */
		device.setEndByte (10);
		
		
		//Assign the 'Coroutine' that will handle your reading Functionality, this will improve your code style
		//Other way would be listening to the event Bt.OnReadingStarted, and starting the courotine from there
		device.ReadingCoroutine = ManageConnection;
		
		devicNameText.text = "Remote Device : " + device.Name;
		
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
			status.text = "Trying to connect";
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
	

	

	

	
	//############### Reading Data  #####################
	//Please note that you don't have to use Couroutienes, you can just put your code in the Update() method
	IEnumerator  ManageConnection (BluetoothDevice device)
	{//Manage Reading Coroutine
		
		//Switch to Terminal View
		InfoCanvas.SetActive (false);
		DataCanvas.SetActive (true);
		
		
		while (device.IsReading) {
			
			
			if (device.IsDataAvailable) {
				//because we called setEndByte(10)..read will always return a packet excluding the last byte 10. 10 equals '\n' so it will return lines. 
				byte [] msg = device.read ();
				
				if (msg != null && msg.Length > 0) {
					string content = System.Text.ASCIIEncoding.ASCII.GetString (msg);
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
