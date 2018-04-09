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
		 *  Note: The library will fill the properties device.Name and device.MacAdress with the right data after succesfully connecting.
		 * 
		 *  Moreover, any BluetoothDevice instance returned by a method or event of this library will have both properties (Name & MacAdress) filled with the right data
		 */


		//You might need th following:
		//this.device.UUID = UUID; //This is not required for HC-05/06 devices and many other electronic bluetooth modules.
		/*
		 * Quoting docs: A uuid is a Universally Unique Identifier (UUID) standardized 128-bit format for a string ID used to uniquely identify information. 
		 * It's used to uniquely identify your application's Bluetooth service.
		 * Check out getUUIDs(), if you don't know what UUID to use.
		 */
	}
	
	public void connect() {
		statusText.text = "Status : ...";

		/*
		 * Notice that there're more than one connect() method, check out the docs to read about them.
		 * a simple device.connect() is equivalent to connect(3, 1000, false) which will make 3 connection attempts
		 * before failing completly, each attempt will cost at least 1 second = 1000 ms.
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

	public void sendHello() {
		if (device != null) {
			/*
			 * Send and Read works only with bytes. You need to convert everything to bytes.
			 * Different devices with different encoding is the reason for this. You should know what encoding you're using.
			 * In the method call below I'm using the ASCII encoding to send "Hello" + a new line.
			 */
			device.send (System.Text.Encoding.ASCII.GetBytes ("Hello\n"));
		}
	}


	//############### Reading Data  #####################
	/* Please note that this way of reading is only used in this demo. All other demos use Coroutines(Unity offers many tutorials on Coroutines).
	 * Just to make things simple
	 */
	void Update() {
		if (device.IsReading) {

			byte [] msg = device.read ();


			if (msg != null ) {

				/* Send and read in this library use bytes. So you have to choose your own encoding.
				 * The reason is that different Systems (Android, Arduino for example) use different encoding.
				 */
				string content = System.Text.ASCIIEncoding.ASCII.GetString (msg);

				statusText.text = "MSG : " + content;

			} 
		}
	}
}
