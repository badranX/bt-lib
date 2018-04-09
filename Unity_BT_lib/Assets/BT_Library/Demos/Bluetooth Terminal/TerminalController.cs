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
			status.text = "Couldn't connect to " + dev.Name + ", device might be OFF";
		else if (!string.IsNullOrEmpty (dev.MacAddress)) {
			status.text = "Couldn't connect to " + dev.MacAddress + ", device might be OFF";
		} 
	}

	void HandleOnDevicePicked (BluetoothDevice device)//Called when device is Picked by user
	{
		this.device = device;//save a global reference to the device


		//this.device.UUID = UUID; //This is not required for HC-05/06 devices and many other electronic bluetooth modules.


		//Here we assign the 'Coroutine' that will handle your reading Functionality, this will improve your code style
		//Another way to achieve this would be listening to the event Bt.OnReadingStarted, and starting the courotine from there by yourself.
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
			status.text = "Trying to connect...";
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
	//Please note that you don't have to use Couroutienes, you can just put your code in the Update() method. Like what we did in the BasicDemo
	IEnumerator  ManageConnection (BluetoothDevice device)
	{//Manage Reading Coroutine
		
		//Switch to Terminal View
		InfoCanvas.SetActive (false);
		DataCanvas.SetActive (true);
		
		
		while (device.IsReading) {

				byte [] msg = device.read ();

				if (msg != null) {
					
					//converting byte array to string.
					string content = System.Text.ASCIIEncoding.ASCII.GetString (msg);

					//here we split the string into lines. '\n','\r' are charachter used to represent new line.
					string [] lines = content.Split(new char[]{'\n','\r'});

					//Add those lines to the scrollText
					readDataText.add (device.Name, lines);

					/* Note: You should notice the possiblity that at the time of calling read() a whole line has not yet completly recieved.
					 * This will split a line into two or more lines between consecutive read() calls. This is not hard to fix, but the goal here is to keep the code simple.
					 * To see a solution using methods of this library check out the "High Bit Rate demo". 
					 */
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
