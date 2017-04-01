
/*
 * Use of this asset and source code is governed by the ASSET STORE TERMS OF SERVICE AND EULA license
 * that can be found in the LICENSE file at https://unity3d.com/legal/as_terms
 */
using System;
using UnityEngine;

using System.Collections;
using System.Collections.Generic;
using TechTweaking.BtCore.BtBridge;

namespace TechTweaking.Bluetooth
{

	/// Represents the local device %Bluetooth adapter.
	/** This class has all the general actions that aren't just specific to the remote device you are connecting to,
	 * like enabling or checking the status of
	 * the %Bluetooth Adapter or showing the nearby devices.
	 * Also it has all the events that can be raised by this library.
	 */
	public class BluetoothAdapter : MonoBehaviour
	{

		private const string GET_ID = "getID";
		private const string BLUETOOTH_STATE_ON = "ON";
		private const string BLUETOOTH_STATE_OFF = "OFF";
		private const string BLUETOOTH_STATE_TURNING_OFF = "TURNING_OFF";

		internal static BluetoothAdapter mono_BluetoothAdapter;
		/// <summary>
		/// Occurs when a BluetoothDevice instance get connected, and pass its reference.
		/// </summary>
		public static event Action<BluetoothDevice> OnConnected; 
		/// <summary>
		/// Occurs when a BluetoothDevice instance get disconnected, and pass its reference.
		/// </summary>
		public static event Action<BluetoothDevice> OnDisconnected;
		/// <summary>
		/// Occurs when a BluetoothDevice instance can't be found as a near by Device, and pass its reference.
		/// </summary>
		public static event Action<BluetoothDevice> OnDeviceNotFound;

		/// <summary>
		/// Occurs when a BluetoothDevice instance has been found but failed to connect to it, and pass its reference.
		/// </summary>
		public static event Action<BluetoothDevice> OnDeviceOFF;

		/// <summary>
		/// Occurs when a BluetoothDevice instance has been found but failed to connect to it, and pass its reference with error message.
		/// </summary>
		public static event Action<BluetoothDevice,String> OnConnectionError;

		/// <summary>
		/// Occurs when on sending error. Passes the BluetoothDevice reference that has the error.
		/// </summary>
		public static event Action<BluetoothDevice> OnSendingError;
		/// <summary>
		/// Occurs when on reading error. Passes the BluetoothDevice reference that has the error.
		/// </summary>
		public static event Action<BluetoothDevice> OnReadingError;
		/// <summary>
		/// Occurs when on a device picked by user from the default Android %Bluetooth devices list.
		/// </summary>
		/// <description>Passes the BluetoothDevice instance which the user picked</description>
		/// <remarks>You can show the list using showDevices()</remarks>
		public static event Action<BluetoothDevice> OnDevicePicked;
		/// <summary>
		/// Occurs when a client remote device request to connect to your server.
		/// </summary>
		/// <description>After calling one of the startServer(string) methods, you can listen
		/// to client requesting to connect using this event.</description>
		/// <remarks> The Event Passes a BluetoothDevice reference of the remote client device that is trying to connect</remarks>
		public static event Action<BluetoothDevice> OnClientRequest;

		/// <summary>
		/// Occurs when the BluetoothAdapter listening to other devices is finished.
		/// </summary>
		/// <description>After calling one of the startServer(string) methods, it will start a listening process and when finish it will send this event.
		/// Passes a boolean equals to 'TRUE' if user refuses to start server/discovery by clicking 'NO', 
		/// which means the user is the reason for Finishing the listening process. 'FALSE' otherwise.
		/// </description>
		public static event Action<Boolean> OnServerFinishedListening;

		/// <summary>
		/// Occurs when on reading starts for a BluetoothDevice insance and Passes its reference.
		/// </summary>	
		public static event Action<BluetoothDevice> OnReadingStarted;
		/// <summary>
		/// Occurs when on reading stops for a BluetoothDevice insance and Passes its reference.
		/// </summary>	
		public static event Action<BluetoothDevice> OnReadingStoped;
		/// <summary>
		/// Occurs when this Android device changes its Bluetoth Adapter state to <c>ON</c>.
		/// </summary>
		/// <description>You need to call listenToBluetoothState() inorder to rescieve this event.
		/// Call stopListenToBluetoothState() to stop rescieving this event.</description>
		public static event Action OnBluetoothON;
		/// <summary>
		/// Occurs when this Android device changes its Bluetoth Adapter state to <c>OFF</c>.
		/// </summary>
		/// <description>You need to call listenToBluetoothState() inorder to rescieve this event.
		/// Call stopListenToBluetoothState() to stop rescieving this event.</description>
		public static event Action OnBluetoothOFF;

		/// <summary>
		/// Occurs when this Android device changes its Bluetoth Adapter state.
		/// </summary>
		/// <description>You need to call listenToBluetoothState() inorder to rescieve this event.
		/// passes a boolean that is <c>True</c> if the state changed to <c>ON</c>, otherwise false.
		/// Call stopListenToBluetoothState() to stop rescieving this event.</description>
		public static event Action<bool> OnBluetoothStateChanged;

		/// <summary>
		/// Occurs when a nearby device has been discovered after calling startDiscovery() .
		/// </summary>
		/// <description>It passes a BluetoothDevice reference and its RSSI value</description>
		public static event Action<BluetoothDevice,short> OnDeviceDiscovered;
		/// <summary>
		/// Occurs when Discovery has finished.
		/// </summary>
		/// <description>It passes a BluetoothDevice reference and its RSSI value</description>
		public static event Action OnDiscoveryFinished;

		void Awake ()
		{
			mono_BluetoothAdapter = this;
			BtBridge.set_unity_game_object_name (this.gameObject.name);//AWAKE: CHANGE THE NAME OF ITS OBJECT
		}

		/// <summary>
		/// Display the default Android %Bluetooth devices list.
		/// </summary>
		/// 
		/// <description>Subscribe to <see cref="OnDevicePicked"/> Event in order to get a reference of the picked device.</description>
		public static void showDevices ()
		{
			BtBridge.Instance.showDevices ();
		}
		/// <summary>
		/// the %Bluetooth adapter state.
		/// </summary>
		/// <returns><c>true</c>, if %Bluetooth was enabled, <c>false</c> otherwise.</returns>
		public static bool isBluetoothEnabled ()
		{
			return BtBridge.Instance.isBluetoothEnabled ();
		}
		/// <summary>
		/// Force Enabling the bluetooth, without asking the user.
		/// </summary>
		/// <returns><c>true</c>, if %Bluetooth enabling process started, <c>false</c> otherwise.</returns>
		public static bool enableBluetooth ()
		{
			return BtBridge.Instance.enableBluetooth ();
		}

		/// <summary>
		/// Disables the bluetooth.
		/// </summary>
		/// <returns><c>true</c>, if %Bluetooth disabling process was started, <c>false</c> otherwise.</returns>
		public static bool disableBluetooth ()
		{
			return BtBridge.Instance.disableBluetooth ();
		}
		/// <summary>
		/// Asks the user to enable bluetooth.
		/// </summary>
		/// <description> A yes/no dialog box will appear to the user asking him to turn %Bluetooth ON</description>
		public static void askEnableBluetooth ()
		{
			BtBridge.Instance.askEnableBluetooth ();
		}
		/// <summary>
		/// Starts the server.
		/// </summary>
		/// <description>Listen to the Event <see cref="OnClientRequest"/> in order to get a refrence of
		/// the remote device is trying to connect</description>
		/// <remarks>This server will run for 100 seconds, and will close after the first client requests a connection
		/// which means after the first <see cref="OnClientRequest"/> event Broadcasted </remarks>
		/// <param name="UUID">a Unique identifier that other devices will use to connect and identefy your server</param>
		public static void startServer (string UUID)
		{
			BtBridge.Instance.startServer (UUID, 100, true);
		}
		/// <summary>
		/// Starts the server.
		/// </summary>
		/// <description>Listen to the Event <see cref="OnClientRequest"/> in order to get a refrence of
		/// the remote device that is trying to connect</description>
		/// <remarks>This server will close after the first client requests a connection
		/// which means after the first <see cref="OnClientRequest"/> event Broadcasted </remarks>
		/// <param name="UUID">a Unique identifier that other devices will use to connect and identefy your server</param>
		/// <param name="time">how long the server will be running in seconds</param>
		public static void startServer (string UUID, int time)
		{
			BtBridge.Instance.startServer (UUID, time, true);
		}
		/// <summary>
		/// Starts the server.
		/// </summary>
		/// <description>Listen to the Event <see cref="OnClientRequest"/> in order to get a refrence of
		/// the remote devices which are trying to connect</description>
		/// <param name="UUID">a Unique identifier that other devices will use to connect and identefy your server</param>
		/// <param name="time">how long the server will be running in seconds</param>
		/// <param name="connectOneDevice">If set to <c>true</c> it will close after the first client requests a connection
		/// which means <see cref="OnClientRequest"/> will be broadcasted once.<br>
		/// If set to <c>false</c> it will stay running and accepting connection attempts 
		/// for the whole time period of a <c>time</c> number of seconds</param>
		public static void startServer (string UUID, int time, bool connectOneDevice)
		{
			BtBridge.Instance.startServer (UUID, time, connectOneDevice);
		}

		/// <summary>
		/// Listens the state of the %Bluetooth adapter of this device.
		/// </summary>
		/// <description>After calling this method register for <see cref="OnBluetoothOFF"/> or <see cref="OnBluetoothON"/>,
		/// they won't be broadcasted unless you called this methods</description>
		public static void listenToBluetoothState ()
		{
			BtBridge.Instance.registerStateReceiver ();
		}
		/// <summary>
		/// Stop listening to the state of the %Bluetooth adapter of this device.
		/// </summary>
		/// <description>After calling this method unregister for <see cref="OnBluetoothOFF"/> and <see cref="OnBluetoothON"/> events,
		/// because they won't be broadcasted after calling this method</description>
		public static void stopListenToBluetoothState ()
		{
			BtBridge.Instance.deRegisterStateReceiver ();
		}



		/*Old method
		public static BluetoothDevice[] getBondedDevices ()
		{
			AndroidJavaObject[] paired = BtBridge.Instance.getPairedDevices ();
			if (paired == null || paired.Length == 0)
				return null;
			
			BluetoothDevice[] devices = new BluetoothDevice[paired.Length];
			for (int i=0; i<paired.Length; i++) {
				devices [i] = new BluetoothDevice (paired [i]);
			}
			return devices;
		}
		*/

		/// <summary>
		/// Returns array of paired/bonded devices.
		/// </summary>
		/// <returns><c>BluetoothDevice</c> array of the paired devices on your Android.</returns>
		public static BluetoothDevice[] getPairedDevices ()
		{
			String[] bonded = BtBridge.Instance.getBondedDevices ();
			if (bonded == null || bonded.Length == 0)
				return null;
			
			BluetoothDevice[] devices = new BluetoothDevice[bonded.Length / 2];
			for (int i=0; i<bonded.Length; i+=2) {
				BluetoothDevice tmp = new BluetoothDevice (true);
				tmp.initDeviceAsStruct_withNoJava (bonded [i + 1], bonded [i]);//Connection will change to by Name

				devices [i / 2] = tmp;
			}
			return devices;
		}


//		The following commented getPairedDevices doesn't allow dublicate instances of the same device
//		public static BluetoothDevice[] getPairedDevices (){
//			AndroidJavaObject[] paired = BtBridge.Instance.getPairedDevices();
//			if(paired == null || paired.Length == 0) return null;
//
//			BluetoothDevice[] devices = new BluetoothDevice[paired.Length];
//
//			for(int i=0;i<paired.Length;i++){
//
//				int id = paired[i].Call<int>(GET_ID);
//				BluetoothDevice tmpDev =  BluetoothDevice.GET_DEVICE_OF_ID(id);
//				if(id != 0 && tmpDev != null){
//					devices[i] = tmpDev;
//				}else{
//					devices[i] = new BluetoothDevice(paired[i]);
//				}
//			}
//			return devices;
//		}


		/// <summary>
		/// Start Discovery for nearby devices, get those discovered devices by listening to the event <see cref="OnDeviceDiscovered"/>
		/// </summary>
		/// <returns><c>true</c>, if Discovery has started, <c>false</c> otherwise.</returns>
		public static bool startDiscovery ()
		{
			return BtBridge.Instance.startDiscovery ();
		}

		/// <summary>
		/// 
		/// It's the same as calling cancelDiscovery() then startDiscovery(). This will cancel any ongoing discovery and start a new one.
		/// It will create a referesh effect. But, it's more obtemized than calling those methods.
		/// </summary>
		/// <returns><c>true</c>, if Discovery has started, <c>false</c> otherwise.</returns>
		public static bool refreshDiscovery ()
		{
			return BtBridge.Instance.refreshDiscovery ();
		}

		/// <summary>
		/// Releases any resources that have been allocated by startDiscovery()
		/// </summary>
		public static void releaseDiscoveryResources ()
		{
			BtBridge.Instance.releaseDiscoveryResources ();
		}

		/// <summary>
		/// Cancel Discovery
		/// </summary>
		/// <returns><c>true</c>, if Success, <c>false</c> otherwise.</returns>
		public static bool cancelDiscovery ()
		{
			return BtBridge.Instance.cancelDiscovery ();
		}
		/// <summary>
		/// Simply it makes the Android device discoverable. If you want other devices to connect to your device, use startServer (string UUID).
		/// </summary>
		public static void makeDiscoverable (int time)
		{
			BtBridge.Instance.makeDiscoverable (time);
		}


		private  void  TrDisconnect (string m)
		{
			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}
			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);
			
			//making sure that OnDisconnected called once
			if (device != null) {
				if (device.IsConnected) {
					device.IsConnected = false;
					if (OnDisconnected != null) {
						OnDisconnected (device);
					}
					device.RaiseOnDisconnected ();
					
					if (device.IsReading) {
						TrReadingStopped (m);
						device.IsReading = false;
					}
				}
				
			}
		}

		private  void  TrConnect (string m)
		{

			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}
			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);

			//making sure that OnConnected called once
			if (device != null) {
				if (!device.IsConnected) {
					device.IsConnected = true;
					if (OnConnected != null) {
						OnConnected (device);
					}
					device.RaiseOnConnected ();
				}
			}


		}

		private  void  TrModuleNotFound (string m)
		{
			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}

			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);
			if (device != null) {
				if (OnDeviceNotFound != null) {
					OnDeviceNotFound (device);
				}
				device.RaiseOnDeviceNotFound ();
			}
		}

		//Replacement for TrModuleOFF and TrModuleNotFound
		private void TrConnectionError (string m2) {

			string m; //regular first message
			string e; //error message
			string[] split = m2.Split (new string[] { "#$" }, StringSplitOptions.None);

			if (split.Length != 2)
			{
				Debug.LogError ("Argument 'm2' should only contains one '#$' seperator");
				return;
			}

			m = split[0];
			e = split [1];

			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}

			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);
			if (device != null) {
				if (OnConnectionError != null) {
					OnConnectionError (device, e);
				}
				device.RaiseOnConnectionError (e);
			}
		}


		private  void  TrModuleOFF (string m)
		{
			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}

			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);
			if (device != null) {
				if (OnDeviceOFF != null) {
					OnDeviceOFF (device);
				}
				device.RaiseOnDeviceOFF ();
			}
		}

		private  void  TrSendingError (string m)
		{
			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}

			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);
			if (device != null) {
				if (OnSendingError != null) {
					OnSendingError (device);
				
				}
				device.RaiseOnSendingError ();
			}
		}

		private  void  TrReadingError (string m)
		{
			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}

			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);
			if (device != null) {
				if (OnReadingError != null) {
					OnReadingError (device);
				}
				device.RaiseOnReadingError ();
			}
		}

		private void TrDataAvailable (string m)
		{
			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}

			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);


			if (device != null) {
				device.IsDataAvailable = true;
			}
		}

		private void TrEmptiedData (string m)
		{

			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}
			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);

			if (device != null)
				device.IsDataAvailable = false;
		}
	    
		private void TrReadingStopped (string m)
		{
			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}
			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);

			if (device != null) {
				device.IsReading = false;
				if (OnReadingStoped != null) {
					OnReadingStoped (device);
				}

				device.RaiseOnReadingStoped ();

			}
		}

		private void TrReadingStarted (string m)
		{
			int deviceID;
			if (!int.TryParse (m, out deviceID)) {
				return;
			}
			BluetoothDevice device = BluetoothDevice.GET_DEVICE_OF_ID (deviceID);
			
			//TODO HAS CHANGED
			if (device != null) {
				device.IsReading = true;
				if (OnReadingStarted != null) {
					OnReadingStarted (device);
				}
				device.RaiseOnReadingStarted ();
				
				
				if (device.ReadingCoroutine != null && device.WillRead) {
					device.last_started_couroutine = device.ReadingCoroutine (device);
					StartCoroutine (device.last_started_couroutine);
				}
			}
		}

		private  void  TriggerPicked_Old (string u)
		{
			getPickedDevice ();
		}

		private void TriggerPicked (string m)
		{
			if (OnDevicePicked != null) {
				string[] split = m.Split (new string[] { "#$" }, StringSplitOptions.None);
				
				if (split.Length < 2)
					return;
				
				string name;
				
				if (split.Length > 2) {//As the Name migh contain '#$' so we need to take the last three
					name = split [0];
					//i=split.Length -1 is the last split for name
					for (int i=1; i<split.Length -1; i++) {
						name += "#$" + split [i];
					}
				} else {
					name = split [0];
				}

				string macAddress = split [split.Length - 1];

				BluetoothDevice tmp = new BluetoothDevice (true);
				tmp.initDeviceAsStruct_withNoJava (macAddress, name);

				OnDevicePicked (tmp);
			}
		}

		private  void  TrServerDiscoveredDevice (string u)
		{
			getDiscoveredDeviceForServer ();
		}

		private  void  TrServerFinishedListening (string u)
		{
			if (OnServerFinishedListening != null) {
				bool user_refuses_to_run_discovery = u == "0" ? true : false;
				OnServerFinishedListening (user_refuses_to_run_discovery);
			}
		}

		private  void TrDiscoveredDevice (string m)
		{
			if (OnDeviceDiscovered != null) {
				string[] split = m.Split (new string[] { "#$" }, StringSplitOptions.None);
				
				if (split.Length < 3)
					return;
				
				string name;
				
				if (split.Length > 3) {//As the Name migh contain '#$' so we need to take the last three
					name = split [0];
					//i=split.Length -3 is the last split for name
					for (int i=1; i<split.Length -2; i++) {
						name += "#$" + split [i];
					}
				} else {
					name = split [0];
				}
				
				
				
				string macAddress = split [split.Length - 2];
				short rssi;
				if (!short.TryParse (split [split.Length - 1], out rssi)) {//The last element is RSSI
					rssi = 0;
				}
				BluetoothDevice device = new BluetoothDevice ();
				device.initDeviceAsStruct_withNoJava (macAddress, name);//Connection will change to by Name
				OnDeviceDiscovered (device, rssi);
			}

		}

		private void TrDiscoveryFinished (string u)
		{
			if (OnDiscoveryFinished != null) 
				OnDiscoveryFinished ();
		}

		private void TrBluetoothOFF (string u)
		{
			if (OnBluetoothOFF != null) 
				OnBluetoothOFF ();
			if (OnBluetoothStateChanged != null)
				OnBluetoothStateChanged (false);
		}

		private void TrBluetoothON (string u)
		{
			if (OnBluetoothON != null) 
				OnBluetoothON ();
			if (OnBluetoothStateChanged != null)
				OnBluetoothStateChanged (true);
		}

		private  void getPickedDevice ()
		{
			if (OnDevicePicked != null) {
				BluetoothDevice bt = new BluetoothDevice (true);

				bt.JavaBtConnection = BtBridge.Instance.getPickedDevice (bt.Id);
				bt.btConnectionMode = BluetoothDevice.BtConnectionMode.UsingBluetoothDeviceReference;
				if (bt.JavaBtConnection != null && bt.JavaBtConnection.GetRawClass ().ToInt32 () != 0) {//must change this check
					OnDevicePicked (bt);
				}
			}
		}

		private  void getDiscoveredDeviceForServer ()
		{
			if (OnClientRequest != null) {
				BluetoothDevice bt = new BluetoothDevice (true);
		        
				bt.JavaBtConnection = BtBridge.Instance.getClientDeviceForServer (bt.Id);
				bt.btConnectionMode = BluetoothDevice.BtConnectionMode.UsingBluetoothDeviceReference;

				if (bt.JavaBtConnection != null) {//TODO check
					
					OnClientRequest (bt);
				}
			}
		}
	
		void OnDestroy ()
		{
			BtBridge.Instance.OnDestroy ();
			BluetoothDevice.DisposeAllDevices ();
		}

		//TODO not Always called
		void OnApplicationQuit ()
		{
			BtBridge.Instance.OnDestroy ();
			BluetoothDevice.DisposeAllDevices ();
		}
	}

}

