using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using TechTweaking.Bluetooth;

public class JoystickHandler : MonoBehaviour {
	private  BluetoothDevice device;
	public Text statusText;
	public Text logsOnScreen; 

	public Transform cube;


	public float speed = 1.0f;

	void Awake () {
		statusText.text = "Status : ...";
		
		BluetoothAdapter.enableBluetooth();//Force Enabling Bluetooth
		
		
		device = new BluetoothDevice();
		
		/*
		 * We need to identefy the device either by its MAC Adress or Name (NOT BOTH! it will use only one of them to identefy your device).
		 */
		device.Name = "HC-05";
		//device.MacAddress = "XX:XX:XX:XX:XX:XX";

		
		
		/*
		 * 10 equals the char '\n' which is a "new Line" in Ascci representation, 
		 * so the read() method will retun a packet that was ended by the byte 10. simply read() will read lines.
		 */
		device.setEndByte (255);
		
		
		/*
		 * The ManageConnection Coroutine will start when the device is ready for reading.
		 */
		device.ReadingCoroutine = ManageConnection;
		
		
	}
	
	public void connect() {
		device.connect();
	}
	
	public void disconnect() {
		device.close();
	}

	IEnumerator  ManageConnection (BluetoothDevice device)
	{
		statusText.text = "Status : Connected & Can read";
		while (device.IsConnected && device.IsReading) {
			
			//polll all available packets
			BtPackets packets = device.readAllPackets();
			
			if (packets != null) {
				
				/*
				 * parse packets, packets are ordered by indecies (0,1,2,3 ... N),
				 * where Nth packet is the latest packet and 0th is the oldest/first arrived packet.
				 * 
				 * Since this while loop is looping one time per frame, we only need the Nth(the latest potentiometer/joystick position in this frame).
				 * 
				 */
				int N = packets.Count - 1; 
				//packets.Buffer contains all the needed packets plus a header of meta data (indecies and sizes) 
				//To get a packet we need the INDEX and SIZE of that packet.
				int indx = packets.get_packet_offset_index(N);
				int size = packets.get_packet_size(N);
					
				if(size == 4){
					// packets.Buffer[indx] equals lowByte(x1) and packets.Buffer[indx+1] equals highByte(x2)
					int val1 =  (packets.Buffer[indx+1] << 8) | packets.Buffer[indx];
					//Shift back 3 bits, because there was << 3 in Arduino
					val1 = val1 >> 3;
					int val2 =  (packets.Buffer[indx+3] << 8) | packets.Buffer[indx+2];
					//Shift back 3 bits, because there was << 3 in Arduino
					val2 = val2 >> 3;
					
					//#########Converting val1, val2 into something similar to Input.GetAxis (Which is from -1 to 1)#########
					//since any val is from 0 to 1023
					float Axis1 = ((float)val1/1023f)*2f - 1f;
					float Axis2 = ((float)val2/1023f)*2f - 1f;

					logsOnScreen.text =  Axis1 + "," + Axis2;
					MoveCube(Axis1,Axis2);
					/*
					 * 
					 * Now Axis1 or Axis2  value will be in the range -1...1. Similar to Input.GetAxis
					 * Check out :
					 * 
					 * https://docs.unity3d.com/ScriptReference/Input.GetAxis.html
					 * 
					 * https://unity3d.com/learn/tutorials/topics/scripting/getaxis
					 */
					
					}
					
					
				}
			
			yield return null;
		}

		statusText.text = "Status : Done Reading";

	}

	private void MoveCube(float horizantal, float vertical) {
		//Here we create a vector in the direction of your joystick.
		Vector3 move = new Vector3(horizantal, vertical, 0);



		/*
		 * We add that vector to the current position, and remember that horizantal and vertical values are between (-1 and 1).
		 * 
		 * For Time.deltaTime, I qoute Unity docs :
		 * "If you add or subtract to a value every frame chances are you should multiply with Time.deltaTime. 
		 * When you multiply with Time.deltaTime you essentially express: I want to move this object 10 meters per second instead of 10 meters per frame."
		 * 
		 */
		cube.position += move * speed * Time.deltaTime;

		/*
		 * 
		 * Also please note that we have rigidbodys to prevent the cube from moving outside the screen. 
		 * Using the update method ( the while loop of the "function!" ManageConnection, works like the update method) to 
		 * alter the position is wrong when physics is a concern. This is just a simple example. 
		 * To understand more read about the FixedUpdate and physics in Unity.
		 * The while loop of the "function!" ManageConnection can work as FixedUpdate 
		 * using  "new WaitForFixedUpdate()" also check out https://docs.unity3d.com/ScriptReference/WaitForFixedUpdate.html
		 * 
		 */
	}


}
