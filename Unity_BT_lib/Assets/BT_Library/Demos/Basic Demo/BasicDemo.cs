using UnityEngine;
using System.Collections;
using UnityEngine.UI;

using TechTweaking.Bluetooth;

public class BasicDemo : MonoBehaviour {

	private  BluetoothDevice device;
	public Text statusText;
	// Use this for initialization
	void Awake () {
		
		BluetoothAdapter.enableBluetooth();//Force Enabling Bluetooth


		device = new BluetoothDevice();

		/*
		 * We need to identefy the device either by its MAC Adress or Name (NOT BOTH! it will use only one of them to identefy your device).
		 *
		 *---------- MacAdress property
		 * Using the MAC adress is the best choice because the device doesn't have to be paired/bonded!
		 * 
		 * ----------Name property
		 * Identefy a device by its name using the Property 'BluetoothDevice.Name' require the remote device to be paired
		 * but you can try to alter the parameter 'allowDiscovery' of the Connect(int attempts, int time, bool allowDiscovery) method. 
		 * allowDiscovery will start a heavy discovery process (if the remote device weren't paired). This will take time 12 to 25 seconds.
		 * So it's better to use the 'BluetoothDevice.MacAdress' property. It doesn't need previuos pairing/bonding.
		 */


		device.Name = "HC-05";
		//device.MacAddress = "XX:XX:XX:XX:XX:XX";



		/*
		 * 10 equals the char '\n' which is a "new Line" in Ascci representation, 
		 * so the read() method will retun a packet that was ended by the byte 10. simply read() will read lines.
		 * If you don't use the setEndByte() method, device.read() will return any available data (line or not), then you can order them as you want.
		 */
		device.setEndByte (10);


		/*
		 * The ManageConnection Coroutine will start when the device is ready for reading.
		 */
		device.ReadingCoroutine = ManageConnection;


		/*
		 *  Note: The library will fill the properties device.Name and device.MacAdress with the right data after succesfully connecting.
		 * 
		 *  Moreover, any BluetoothDevice instance returned by a method or event of this library will have both properties (Name & MacAdress) filled with the right data
		 */
	}
	
	public void connect() {
		statusText.text = "Status : ...";

		/*
		 * Notice that there're more than one connect() method, check out the docs to read about them.
		 * a simple device.connect() is equivalent to connect(10, 1000, false) which will make 10 connection attempts
		 * before failing completly, each attempt will cost at least 1 second.
		 * -----------
		 * To alter that  check out the following methods in the docs :
		 * connect (int attempts, int time, bool allowDiscovery) 
		 * normal_connect (bool isBrutal, bool isSecure)
		 */
		device.connect();

	}

	public void disconnect() {
		device.close();
	}


	//############### Reading Data  #####################
	//Please note that you don't have to use this Couroutienes/IEnumerator, you can just put your code in the Update() method
	IEnumerator  ManageConnection (BluetoothDevice device)
	{
		statusText.text = "Status : Connected & Can read";
		
		while (device.IsReading) {
			if (device.IsDataAvailable) {
				byte [] msg = device.read ();//because we called setEndByte(10)..read will always return a packet excluding the last byte 10.
				
				if (msg != null && msg.Length > 0) {
					string content = System.Text.ASCIIEncoding.ASCII.GetString (msg);
					statusText.text = "MSG : " + content;
				}
			}
			
			yield return null;
		}
		
		statusText.text = "Status : Done Reading";
	}


}
