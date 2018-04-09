using UnityEngine;
using System.Collections;
using System.Collections.Generic;

using TechTweaking.Bluetooth;
using UnityEngine.UI;

public class ServerClient : MonoBehaviour
{
	private const string UUID = "0acc9c7c-48e1-41d2-acaa-610d1a7b085e";
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
	
	//############### Handle Events #####################
	
	void HandleOnDeviceOff (BluetoothDevice dev)
	{
		if (!string.IsNullOrEmpty (dev.Name))
			devicNameText.text = "Can't connect to " + dev.Name + ", device maybe OFF";
		else if (!string.IsNullOrEmpty (dev.Name)) {
			devicNameText.text = "Can't connect to " + dev.MacAddress + ", device maybe OFF";
		}
	}
	
	void HandleOnClientRequest (BluetoothDevice device)
	{
		this.device = device;
		

		//Assign the 'Coroutine' that will handle your reading Functionality, this will improve your code style
		//Other way would be listening to the event Bt.OnReadingStarted, and starting the courotine from there
		this.device.ReadingCoroutine = ManageConnection;

		//Any device passed by the event OnClientRequest to this handler will has the same UUID, so we will connect to it directly.
		this.device.connect ();
		
	}
	
	void HandleOnDevicePicked (BluetoothDevice device)//Called when device is Picked by user
	{
		
		this.device = device;//save a global reference to the device
		

		//We didn't use this when connecting to Bluetooth modules
		//if you didn't provide a UUID, the plugin has a default one that works with most classic Bluetooth modules
		this.device.UUID = UUID;
		

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
	
	public void connect ()//Connect to the  global variable "device" if it's not null.
	{
		if (device != null) {
			device.connect ();
		}
	}
	
	public void disconnect ()//Disconnect the  global variable "device" if it's not null.
	{
		if (device != null)
			device.close ();
	}
	
	public void send ()
	{		
		if (device != null && !string.IsNullOrEmpty (dataToSend.text)) {
			device.send_Blocking  (System.Text.Encoding.ASCII.GetBytes (dataToSend.text + (char)10));
		}
	}
	
	public void initServer ()
	{
		BluetoothAdapter.OnClientRequest += HandleOnClientRequest;//listen to client remote devices trying to connect to your device
		
		//start a server for 100 second, and close after the first connection attempt by a device that has the same UUID. [One call to this.HandleOnClientRequest]
		//see Bt.startServer(UUID,int,bool) to change the default behaviour.
		//####If you want to connect to this server you need to have a similar UUID. otherwise it won't connect###
		BluetoothAdapter.startServer (UUID);

	}

	//############### Reading Data  #####################
	IEnumerator  ManageConnection (BluetoothDevice device)
	{//Manage Reading Coroutine
		
		//Switch to Terminal View
		InfoCanvas.SetActive (false);
		DataCanvas.SetActive (true);

		while (device.IsReading) {

			byte [] msg = device.read ();
			
			if (msg != null) {

				/* Send and read in this library use bytes. So you have to choose your own encoding.
				 * The reason is that different Systems (Android, Arduino for example) use different encoding.
				 */
				string content = System.Text.ASCIIEncoding.ASCII.GetString (msg);

				//here we split the string into lines. '\n','\r' are charachter used to represent new line.
				readDataText.add (device.Name, content.Split(new char[]{'\n','\r'}));
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
		BluetoothAdapter.OnClientRequest -= HandleOnClientRequest;
		BluetoothAdapter.OnDevicePicked -= HandleOnDevicePicked; 
		
	}
	
}
