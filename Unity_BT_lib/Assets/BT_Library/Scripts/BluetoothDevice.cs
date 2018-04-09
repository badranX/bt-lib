/*
 * Use of this asset and source code is governed by the ASSET STORE TERMS OF SERVICE AND EULA license
 * that can be found in the LICENSE file at https://unity3d.com/legal/as_terms
 */
using UnityEngine;
using System.Collections;
using System;

using TechTweaking.BtCore;
using System.Collections.Generic;

namespace TechTweaking.Bluetooth
{
	/// Represents a remote %Bluetooth device.
	/** A BluetoothDevice lets you create a connection with the respective 
	 * device or query information about it, such as the name, address, class, and bonding state.
	 */
	public class BluetoothDevice: IDisposable
	{
		/// <summary>
		/// Occurs when this BluetoothDevice instance get connected, and pass its reference.
		/// </summary>
		public  event Action<BluetoothDevice> OnConnected; 
		/// <summary>
		/// Occurs when this BluetoothDevice instance get disconnected, and pass its reference.
		/// </summary>
		public  event Action<BluetoothDevice> OnDisconnected;
		/// <summary>
		/// Occurs when this BluetoothDevice instance can't be found as a near by Device, and pass its reference.
		/// </summary>
		public  event Action<BluetoothDevice> OnDeviceNotFound;

		/// <summary>
		/// Occurs when this BluetoothDevice instance has been found but failed to connect to it, and pass its reference.
		/// </summary>
		public  event Action<BluetoothDevice> OnDeviceOFF;

		/// <summary>
		/// Occurs when this BluetoothDevice instance failed to connect, passes error message. Can replace OnDeviceOFF and OnDeviceNotFound
		/// </summary>
		public  event Action<BluetoothDevice, String> OnConnectionError;

		/// <summary>
		/// Occurs when on sending error. Passes this BluetoothDevice reference that has the error.
		/// </summary>
		public  event Action<BluetoothDevice> OnSendingError;
		/// <summary>
		/// Occurs when on reading error. Passes this BluetoothDevice reference that has the error.
		/// </summary>
		public  event Action<BluetoothDevice> OnReadingError;
		/// <summary>
		/// Occurs when on reading starts for this BluetoothDevice insance and Passes its reference.
		/// </summary>	
		public  event Action<BluetoothDevice> OnReadingStarted;
		/// <summary>
		/// Occurs when on reading stops for this BluetoothDevice insance and Passes its reference.
		/// </summary>	
		public  event Action<BluetoothDevice> OnReadingStoped;

		internal void RaiseOnConnected ()
		{
			if (this.OnConnected != null)
				this.OnConnected (this);
		}

		internal void RaiseOnDisconnected ()
		{
			if (this.OnDisconnected != null)
				this.OnDisconnected (this);
		}

		internal void RaiseOnDeviceNotFound ()
		{
			if (this.OnDeviceNotFound != null)
				this.OnDeviceNotFound (this);
		}

		internal void RaiseOnDeviceOFF ()
		{
			if (this.OnDeviceOFF != null)
				this.OnDeviceOFF (this);
		}

		internal void RaiseOnConnectionError (String e) {
			if (this.OnConnectionError != null)
				this.OnConnectionError (this, e);
		}

		internal void RaiseOnSendingError ()
		{
			if (this.OnSendingError != null)
				this.OnSendingError (this);
		}

		internal void RaiseOnReadingError ()
		{
			if (this.OnReadingError != null)
				this.OnReadingError (this);
		}

		internal void RaiseOnReadingStarted ()
		{
			if (this.OnReadingStarted != null)
				this.OnReadingStarted (this);
		}

		internal void RaiseOnReadingStoped ()
		{
			if (this.OnReadingStoped != null)
				this.OnReadingStoped (this);
		}

		internal enum READING_MODES
		{
			LENGTH_PACKET,
			END_BYTE_PACKET,
			NO_PACKETIZATION
			
		}
		private  READING_MODES reading_mode = READING_MODES.NO_PACKETIZATION;

		internal int Id{ private set; get; }

		private static Dictionary<int,BluetoothDevice> DevicesMap = new Dictionary<int,BluetoothDevice> ();

		//##############constants################
		private const string CONNECT = "connect";
		private const string NORMAL_CONNECT = "normal_connect";
		private const string SEND_BYTES = "sendBytes";
		private const string SEND_BYTES_BLOCKING = "sendBytes_Blocking";
		private const string READ = "read";
		private const string READ_ALL_PACKETS = "readAllPackets";
		private const string CLOSE = "close";
		private const string SET_NAME = "setName";
		private const string SET_MAC = "setMac";
		private const string SET_ID = "setID";
		private const string SET_PACKET_SIZE = "setPacketSize";
		private const string SET_END_BYTE = "setEndByte";
		private const string ENABLE_READING = "enableReading";
		private const string DISABLE_READING = "disableReading";
		private const string INSTANCE_REMOVED = "instanceRemoved";
		private const string GET_CONNECTION_MODE = "getConnectionMode";
		private const string SET_UUID = "setUUID";
		private const string GET_UUIDs = "getUUIDs";
		private const string IS_BLUETOOTH_SUPPORTED = "isBluetoothSupported";
		/// <summary>
		/// Initializes a new instance of this class.
		/// </summary>
		public BluetoothDevice ()
		{
			this.Id = generateID ();
			registerDevice (this, this.Id);
		}


		//If Not ready this method will try to initialize again
		private bool isDeviceReady ()
		{
			bool ready = this.JavaBtConnection != null && 
				this.JavaBtConnection.GetRawClass ().ToInt32 () != 0;
			if (!ready && BtBridge.Instance.isPluginReady ()) {
				this.JavaBtConnection = BtBridge.Instance.createJavaBtConnection (this.Id);
				ready = this.JavaBtConnection != null && 
					this.JavaBtConnection.GetRawClass ().ToInt32 () != 0;
			}
			return ready;
		}

		internal BluetoothDevice (bool dummy)//used for yet to be assigned JavaBtConnection // and JavaBtConnection has an ID
		{
			this.Id = generateID ();
			registerDevice (this, this.Id);
			
		}

		internal BluetoothDevice (AndroidJavaObject obj)//the AndroidJavaObject Have No ID yet
		{
			this.Id = generateID ();
			registerDevice (this, this.Id);
			this.JavaBtConnection = obj;
			this.javaBtConnection.Call (SET_ID, this.Id);
		}

		private static int generateID ()
		{
			int nextID;
			do {
				//0 means there's no ID
				int nextID1 = UnityEngine.Random.Range (int.MinValue, -1);
				int nextID2 = UnityEngine.Random.Range (1, int.MaxValue);

				nextID = UnityEngine.Random.Range (0, 2) == 1 ? nextID1 : nextID2;

			} while(DevicesMap.ContainsKey(nextID));

			return nextID;
		}
			   
		private static void registerDevice (BluetoothDevice device, int id)
		{
			if (DevicesMap.ContainsKey (id)) {
				DevicesMap.Remove (id);
			}
			DevicesMap.Add (id, device);
			
		}
		
		internal static BluetoothDevice GET_DEVICE_OF_ID (int id)
		{
			if (DevicesMap.ContainsKey (id)) {
				return DevicesMap [id];
			}
			return null;
		}

		private bool isReading = false;
		/// <summary>
		/// Gets a value indicating whether this device is reading.
		/// </summary>
		/// <value><c>true</c> if this instance is reading; otherwise, <c>false</c>.</value>
		public bool IsReading {
			get {
				return isReading;
			}
			internal set { isReading = value; }
		}

		private bool isDataAvailable = false;
		/// <summary>
		/// Gets a value indicating whether this device has data available to read.(It has one frame delay).
		/// </summary>
		/// <value><c>true</c> if it has data available; otherwise, <c>false</c>.</value>
		/// <description>Data mean bytes unless a method to packetize data has been called on this instance, then data would mean packets.
		/// It still has one frame delay, so if you expect "continious" data just just keep checking if BluetoothDevice.read() has data.
		/// </description>
		/// <remarks>Available packetization methods : setEndByte(), setPacketSize()</remarks>
		public bool IsDataAvailable {
			get { 

				return isDataAvailable;
			}
			internal set {
				isDataAvailable = value;
			}

		}

		private bool isConnected = false;
		/// <summary>
		/// Gets a value indicating whether this device is connected.
		/// </summary>
		/// <value><c>true</c> if this device is connected; otherwise, <c>false</c>.</value>
		public bool IsConnected {
			get{ return isConnected;}
			internal set{ isConnected = value;}
		}

		private Func<BluetoothDevice,IEnumerator> readingCoroutine;
		/// <summary>
		/// If it's not <c>Null</c> then the referenced <c>IEnumerator</c> will be started directly after a reading started.
		/// </summary>
		/// <example>
		/// <b>Example Code</b>
		/// <code>IEnumerator MyMethodName (BluetoothDevice device) { ... }
		/// 
		/// ...
		/// 
		/// yourDevice.readingCoroutine = MyMethodName;
		/// </code> 
		/// <c>MyMethodName(BluetoothDevice device)</c> will start directly after a reading channel has been istablished.
		/// the parameter device will be equal to yourDevice.
		/// </example>
		public  Func<BluetoothDevice,IEnumerator>  ReadingCoroutine {
			internal get{ return readingCoroutine;}
			set{ readingCoroutine = value;}
		}

		//Store the couroutine before starting by (MonoBehaviour.StartCouroutine(..)), so it can be StopCouroutine(it) if needed
		internal IEnumerator last_started_couroutine;

		/// <summary>
		/// Will stop the ongoing reading couroutine, referenced by <see cref="ReadingCoroutine">
		/// </summary>
		public void stopReadingCoroutine () {
			if(BluetoothAdapter.mono_BluetoothAdapter != null && this.last_started_couroutine != null) BluetoothAdapter.mono_BluetoothAdapter.StopCoroutine(last_started_couroutine);
		}
		private byte endByte;
		private bool isUsingUUID = false;
		private bool isNeedCommitData = false;

		/// <summary>
		/// Read all available <c>bytes</c>, or read the next available packet if packetization was used.
		/// </summary>
		/// <description>The default returned value would be all available <c>bytes</c>, but if data was packetized using a packetization method
		/// , then it would return the next available packet.
		/// </description>
		/// <remarks> Available packetization methods : setEndByte(), setPacketSize()</remarks>
		/// <returns><c>byte[]</c> contains all available bytes, or all available bytes in the next available packet if packetization was used</returns>
		public byte[] read ()
		{
			if (isDeviceReady ()) {
				if (reading_mode == READING_MODES.NO_PACKETIZATION) {
					this.IsDataAvailable = false;//Incase of No_packetization we will read the whole buffer so IsDataAvaialable will be false will be empty.
				}
				byte [] msg = javaBtConnection.Call<byte[]> (READ);
				return (msg != null  && msg.Length > 0) ? msg : null;
			}
			return null;
		}

		/// <summary>
		/// Read from available <c>bytes</c> up to a max <c>size</c> number of bytes, or read all <c>bytes</c> _Regarding its Size_ of the next available packet if packetization was used.
		/// </summary>
		/// <description>The default returned value would be at most a <c>size</c> number of available <c>bytes</c>, but if data was packetized using a packetization method
		/// , then it would return the next available packet regarding its size.
		/// </description>
		/// <example>Available packetization methods : setEndByte(), setPacketSize()</example>
		/// <returns><c>byte[]</c> contains all available bytes, or all available bytes in the next available packet if packetization was used</returns>
		/// <param name="size"> maximum number of bytes to read </param>
		public byte[] read (int size)
		{
			if (isDeviceReady ()) {
				return javaBtConnection.Call<byte[]> (READ, size);
				
			}
			return null;
		}


		/// <summary>
		/// Read all available <c>bytes</c>, or read the next available packet if packetization was used.
		/// </summary>
		/// <description>The default returned value would be all available <c>bytes</c>, but if data was packetized using a packetization method
		/// , then it would return the next available packet.
		/// </description>
		/// <remarks> Available packetization methods : setEndByte(), setPacketSize()</remarks>
		/// <returns><c>byte[]</c> contains all available bytes, or all available bytes in the next available packet if packetization was used</returns>
		public BtPackets readAllPackets ()
		{
			if (isDeviceReady () && reading_mode == READING_MODES.END_BYTE_PACKET) {
				byte[] msg = javaBtConnection.Call<byte[]> (READ_ALL_PACKETS);
				this.IsDataAvailable = false;
				if (msg != null && msg.Length > 0) {
					return new BtPackets (msg);
				}
			}

			return null;
		}

		/// <summary>
		/// Get the UUIDs used by this device instance except a BluetoothDevice initialized by Name before connecting.
		/// </summary>
		/// <description> This method returns an array of String UUIDs. This will help you find what UUID is used by the remote device. 
		/// You can do multiple connection attempts with different UUIDs.
		/// This won't work with a new created BluetoothDevice instance with a name provided using the BluetoothDevice.Name property, because the actuall phisical device still isn't known.
		/// </description>
		public String[] getUUIDs () 
		{
			if(isDeviceReady()) {
				return javaBtConnection.Call<String[]>(GET_UUIDs);
			}
			return null;
		}

		public Boolean isBluetoothSupported ()
		{
			if(isDeviceReady()) {
				return javaBtConnection.Call<Boolean>(IS_BLUETOOTH_SUPPORTED);
			}
			return false;

		}


		


		/// <summary>
		/// Packetize data into packets of a <c>size</c> number of bytes per each.
		/// </summary>
		/// <description>Should be called before reading started, otherwize it might make no effect, so it's safer to call it before calling connect().<br>
		/// The method will override any previous packetization. Check what effect this method has on read() and read(int).</description>
		/// 
		/// <example><b>Example</b> 
		/// Calling setPacketSize(10), then read() or read(int) will return a <c>byte</c> array of size 10, if 10 or more bytes are available.
		/// </example>
		/// <param name="size">size of each packet</param>
		public void setPacketSize (int size)
		{
			if (isDeviceReady ()) {
				javaBtConnection.Call (SET_PACKET_SIZE, size);
				reading_mode = READING_MODES.LENGTH_PACKET;
			}
		}
		/// <summary>
		/// Packetize data into packets, each packets ends with the byte <c>byt</c>.
		/// </summary>
		/// <description>Should be called before reading started, otherwize it might make no effect, so it's safer to call it before calling connect().<br>
		/// The method will override any previous packetization. Check what effect this method has on read() and read(int).</description>
		/// <remarks>Packets won't contain the end Byte <c>byt</c></remarks>
		/// <example><b>Example</b> 
		/// Calling setEndByte(13), then read() or read(int) will return the next available bunch of bytes as a <c>byte</c> array that ends with the byte <c>13</c>,
		/// if there was no end Byte '13' in your sequence of bytes, they won't return anything.
		/// </example>
		/// <param name="byt">a <c>byte</c> that seperates each packate from the next one</param>
		public void setEndByte (byte byt)
		{
			if (isDeviceReady ()) {
				javaBtConnection.Call (SET_END_BYTE, byt);
				reading_mode = READING_MODES.END_BYTE_PACKET;
			} 
		}

		/// <summary>
		/// Sets the underlying buffer size, and stops it from changing size dynamically. This will save resources.
		/// </summary>
		///  <description>If you know what buffer size is good for you (you can find that by experimenting), then it's better to make the buffer static with a fixed size, because changing its size dynamically costs a lot of resources </description>
		public void setBufferSize (int size)
		{
			if (isDeviceReady ()) {
				javaBtConnection.Call ("setBufferSize", size);
			} 
		}

		//TODO this just should commit data
		private void commitData ()
		{


			if (isDeviceReady () && isNeedCommitData) {
				switch (btConnectionMode) {
				case BtConnectionMode.UsingMac:
					javaBtConnection.Call (SET_MAC, this.macAddress);
					break;
				case BtConnectionMode.UsingName:
					javaBtConnection.Call (SET_NAME, this.name);
					break;
				}

				if (isUsingUUID) {
					javaBtConnection.Call (SET_UUID, this.uuid);
				}
				if (this.willRead) 
					javaBtConnection.Call (ENABLE_READING, this.threadID);
				else
					javaBtConnection.Call (DISABLE_READING);

				isNeedCommitData = false;

			}

		}
			
				
			
			
		internal enum  BtConnectionMode
		{
			UsingMac,
			UsingName,
			UsingBluetoothDeviceReference,
			NotSet
		}
		internal BtConnectionMode btConnectionMode = BtConnectionMode.NotSet;
		private AndroidJavaObject javaBtConnection ;

		internal  AndroidJavaObject JavaBtConnection {
			get {
				return this.javaBtConnection;
			}
			set {
				this.javaBtConnection = value;

			}
		}

		private string name = "";
		private bool isNameGrabed = false;
		/// <summary>
		/// Gets or sets the name of this device.
		/// </summary>
		/// <description>If you assigned a value to this Property, then the connect() method will try to find a device with the <c>the device Name</c> assigned and connect to it.<br>
		/// You can either use this property to identfy the device and connect or the <see cref="MacAddress"> property, using both of them isn't possible simply the last call will override everything.<br>
		/// <value>if this Library has recognized the device after connecting, or you're using this property to connect, then its value will be a string of the <c>device name</c>, otherwise empty string</value>

		public  string Name {
			get {
				if (isDeviceReady () && !isNameGrabed) {
					isNameGrabed = true;
					this.name = javaBtConnection.Call<string> ("getName");
				}
				return this.name;
			}
			set {
				if (this.IsConnected) {
					this.close ();
				}
				this.btConnectionMode = BtConnectionMode.UsingName;
				this.isNameGrabed = true;
				this.isMacGrabed = false;
				this.name = value;
				isNeedCommitData = true;

			}
		}

		internal void initDeviceAsStruct_withNoJava (string mac, string name)
		{
			this.btConnectionMode = BtConnectionMode.UsingMac;
			this.isNameGrabed = true;
			this.isMacGrabed = true;
			this.macAddress = mac;
			this.name = name;
			isNeedCommitData = true;
		}

		private string macAddress = "";
		private bool isMacGrabed = false;
		/// <summary>
		/// Gets or sets the mac address of this device.
		/// </summary>
		/// <description>If you assigned a value to this Property, then the connect() method will try to find a device with the <c>Mac Address</c> assigned and connect to it.<br>
		/// You can either use this property to identfy the device and connect or the <see cref="MacAddress"/> property, using both of them isn't possible simply the last call will override everything.<br>
		/// Most of the time its value is the name of the device, if this Library has recognized the device after connecting, or you're using this property to connect.</description>
		/// <value>The mac address.</value>
		public  string MacAddress {
			get {
				if (isDeviceReady () && !isMacGrabed) {
					isNameGrabed = true;
					macAddress = javaBtConnection.Call<string> ("getAddress");
				}
				return macAddress;
			}
			set {
				this.btConnectionMode = BtConnectionMode.UsingMac;
				this.isMacGrabed = true;
				this.isNameGrabed = false;
				macAddress = value;
				isNeedCommitData = true;
			}
		}

		private string uuid = "";
		/// <summary>
		/// Gets or sets the UUI.
		/// </summary>
		/// <description>A Universally Unique Identifier (UUID) is a standardized 128-bit format for a string ID used to uniquely 
		/// identify information. The point of a UUID is that it's big enough that you can select any random and it won't clash. 
		/// In this case, it's used to uniquely identify your application's %Bluetooth service.
		/// To get a UUID to use with your application, you can use one of the many random UUID generators on the web</description>
		/// 
		/// <remarks>The Default UUID is the one mostly used with Electronic classic bluetooth prepherals (SPP)
		/// which is "00001101-0000-1000-8000-00805F9B34FB"
		/// </remarks>
		public string UUID {
			get {
				return uuid;
			}
			set {
				isUsingUUID = true;
				uuid = value;
				isNeedCommitData = true;
			}
		}

		private bool willRead = true;

		/// <summary>
		/// Gets or sets a value indicating whether this device will read.
		/// </summary>
		/// <description>If WillRead is false before calling connect() the device won't reads</description>
		/// <value><c>true</c> if will read; otherwise, <c>false</c>. Default is <c>true</c></value>
		public bool WillRead {
			get {
				return this.willRead;
			}
			set {
				this.willRead = value;
				isNeedCommitData = true;
			}
		}
        
		private int threadID = 0;
		/// <summary>
		/// Gets and Sets the reading thread/process ID number of this device instance.
		/// </summary>
		/// <description> making two devices has the same threadID and not equal to 0, will remove the overhead of creating a thread/process for each device by sharing the same thread/process.
		/// this of course will introduce a delay for both devices.</description>
		/// <value><c>0</c> is the default value and means that it will has a single thread/process for itself, otherwise it will share a thread/process with other devices that has a similar ID </value>
		public int ThreadID {
			get {
				return threadID;
			}
			set {
				WillRead = true;
				threadID = value;
				isNeedCommitData = true;
			}
		}
		/// <summary>
		/// Connect to this device. Unlike connect(int, int, bool) it's just a one attempt connection.
		/// <param name="isBrutal">If set to <c>true</c> it will attempt to do a hacked connection, 
		/// in some devices speciall fake/cloned devices a hacked connection works better than normal connection.</param>
		/// <param name="isSecure">If set to <c>true</c> it will istablish a secure/encoded communication channel, 
		/// "Secure Connection" will add extra security matters that aren't useful for most users, so it's better to leave it as <c>false</c> </param>
		/// @remarks {
		/// 
		/// Brutal Connection (it's a madeup name, you might call it connecting by reflection) : is a hacked connection that have a higher chances of returning a succesful connection
		/// (it bypasses some steps that checks the overall connection status, which sometimes fail on some Android devices).
		/// It is not documented by Google so I can't gurantee that it will work on all devices,
		/// but the hack has been used a lot and tested by many users for years.
		/// <a href="https://redacacia.me/2012/07/17/overcoming-android-bluetooth-blues-with-reflection-method/">checkout this Blog post for example</a> 
		/// 
		/// }
		/// 
		public void normal_connect (bool isBrutal, bool isSecure)
		{
			if (!isDeviceReady ())
				return;
			commitData ();
			javaBtConnection.Call (NORMAL_CONNECT, isBrutal, isSecure);
		}
		/// <summary>
		/// Equivalent to connect(10, 1000, false) .
		/// </summary>

		public void connect ()
		{

			connect (3, 1000, true);
		}

		/// <summary>
		///  Equivalent to connect(tries, 1000, false) .
		/// </summary>
		public  void connect (int attempts)
		{


			//after closing ConnectionMode isn't right most of the time.
			//because the device might already has a reference to the socket or the actual BluetoothDevice of Android API

			connect (attempts, 1000, true);

		}

		/// <summary>
		/// Equivalent to connect(attempts, time, false) .
		/// </summary>
		public void connect (int attempts, int time)
		{
			connect (attempts, time, true);
		}


		/// <summary>
		/// Connect to this device.
		/// </summary>
		/// <remarks><b>Asynchronous Method</b></remarks>
		/// <description> If this instance has initialize proberly, either by providing enough data to recognize the device like its Name or MAC address, or it was a reference returned by a method/event 
		/// of this Library, then this method will try to connet to the remote device without blocking</description>
		/// <param name="attempts">Number of connection attempts before it fails, each connection try might differ than the other internally so it's not like calling this method multiple times</param>
		/// <param name="time">The time in melisecond to wait between two connection attempts, it's better to set it to 1000 which means 1 second</param>
		/// <param name="allowDiscovery"> When you use the <see cref="Name"/> property to identefy the device, the Library needs to search for a match for that name within 
		/// the paired devices, setting this parameters to <c>true</c> will start a discovery process for 12 seconds if the device hasn't been found as a Paired device.

		/// <br> <b> it's not a good idea to use it, because you'll block any further connection for 12 seconds.
		/// <br>
		/// By just calling startDiscover() , it will enlist any nearby found device in the Paired devices list so there will be no need for this parameter to be <c> </c>
		/// </b>
		/// </param>
		/// @remarks {
		/// If you called any <c>connect(...)</c> method multiple times, it will save the order of those calls and execute them in order, 
		///
		/// @code
		/// device1.connect(30)//30 trials, and lets assume they all fails
		/// device2.connect(10)//It might take around 30 seconds to start executing this operation [In the background, becaus it's asynchronous]
		/// @endcode
		/// }
		public void connect (int attempts, int time, bool allowDiscovery)
		{


			connect (attempts, time, allowDiscovery, true, false);
		}


		/// <summary>
		/// Connect to this device.
		/// </summary>
		/// <remarks><b>Asynchronous Method</b></remarks>
		/// <description> If this instance has initialize proberly, either by providing enough data to recognize the device like its Name or MAC address, or it was a reference returned by a method/event 
		/// of this Library, then this method will try to connet to the remote device without blocking</description>
		/// <param name="attempts">Number of connection attempts before it fails, each connection try might differ than the other internally so it's not like calling this method multiple times</param>
		/// <param name="time">The time in melisecond to wait between two connection attempts, it's better to set it to 1000 which means 1 second</param>
		/// <param name="allowDiscovery"> When you use the <see cref="Name"/> property to identefy the device, the Library needs to search for a match for that name within 
		/// the paired devices, setting this parameters to <c>true</c> will start a discovery process for more than 12 seconds if the device hasn't been found as a Paired device.
		///  <br> <b> it's not a good idea to use it, because you'll block any further connection for more than 12 seconds, if it couldn't find it.
		/// <br>
		/// By just calling startDiscover() , it will enlist any nearby found device in the Paired devices list so there will be no need for this parameter to be <c> </c>
		/// </b>
		/// </param>
		/// 
		/// <param name="startNormalConnection">If this parmeter set to <c>true</c> The first attempt will be a normal connection attempt to your remote device
		/// , otherwise the first attempt will be a brutal connection.
		/// Brutal Connection (it's a madeup name, you might call it connecting by reflection) : is a hacked connection that have a higher chances of returning a succesful connection
		/// (it bypasses some steps that checks the overall connection status, which sometimes fail on some Android devices).
		/// It is not documented by Google so I can't gurantee that it will work on all devices,
		/// but the hack has been used a lot and tested by many users for years.
		/// <a href="https://redacacia.me/2012/07/17/overcoming-android-bluetooth-blues-with-reflection-method/">checkout this Blog post for example</a> 
		///  </param>
		/// 
		/// <param name="swtitchBetweenNormal_Brutal"> After the first attempt of Brutal or Normal connection, if it is <c>true</c>, connection will
		/// switch between Normal and Brutal in every attempt after the first one. if it is set to <c>false</c>, 
		/// it will use the same connection mode (Normal/Brutal) through out all attempts.</param>
		///

		/// @remarks {
		/// If you called any <c>connect(...)</c> method multiple times, it will save the order of those calls and execute them in order, 
		///
		/// @code
		/// device1.connect(30)//30 trials, and lets assume they all fails
		/// device2.connect(10)//It might take around 30 seconds to start executing this operation [In the background, becaus it's asynchronous]
		/// @endcode
		/// }
		public void connect (int attempts, int time, bool allowDiscovery, bool startNormalConnection, bool swtitchBetweenNormal_Brutal)
		{
			if (!isDeviceReady ())
				return;
			commitData ();
			javaBtConnection.Call (CONNECT, attempts, time, allowDiscovery, startNormalConnection, swtitchBetweenNormal_Brutal);

		}

		public  void close ()
		{
			if (isDeviceReady ()) {
				javaBtConnection.Call (CLOSE);

			}
		}


		/// <summary>
		/// Sends a <c>byte[]<c/> array to this device.
		/// </summary>
		/// @b{Asynchronous Method}
		/// @remark{
		/// Asynchronous method, which means that after the method returns sending might even hasn't started yet. It's optemized to start at the most appropriate time, and it saves the order of mu.
		/// so for example the following code snippest has the same result:
		/// @code 
		/// device.send(new byte[]{1 , 2};
		/// device.send(new byte[]{3 , 4};
		/// device.send(new byte[]{5 , 6};
		/// @endcode
		/// it has the same effect as the following :
		/// @code 
		/// device.send(new byte[]{1 , 2 , 3 , 4 , 5 , 6};
		/// @endcode
		/// 
		/// Notice the last code with a one call is more efficient, since it doesn't need to create a multiple messages to send one after the other.	
		/// 
		/// }
		/// <param name="msg">Array of bytes to send</param>
		public void send (byte[] msg)
		{
			if (!isDeviceReady () || msg.Length == 0) {
				return;
				
			}

		

			javaBtConnection.Call (SEND_BYTES, msg);
		}

		/// <summary>
		/// Similar to send(byte[]) , but it's a synchronouse/blocking method. send(byte[]) will raise exception if it was called from a C# thread, so if you're 
		/// using threads in Unity, it's better to use this method.
		/// </summary>
		public bool send_Blocking (byte[] msg)
		{
			if (!isDeviceReady ()) {
				return false;
				
			}
			
			if (msg.Length == 0)
				return false;
			
			return javaBtConnection.Call<bool> (SEND_BYTES_BLOCKING, msg);
		}

		/*
		//TODO this method check if this instance represent the same physical device
		private bool isEqual (BluetoothDevice dev)
		{

			if ((object)dev == null) {
				return false;
			}

			bool equalMAC = false;

			string mac1 = this.MacAddress;
			string mac2 = dev.macAddress;

			if (mac1 != null) {
				equalMAC = mac1.Equals (mac2);
			} else {//this should never be reached
				equalMAC = mac2 == null;
			}


			bool equalUUID = false;
			string uuid1 = this.UUID;
			string uuid2 = dev.UUID;

			if (uuid1 != null) {
				equalUUID = uuid1.Equals (mac2);
			} else {//this should never be reached
				equalUUID = uuid2 == null;
			}

			bool equalName = false;
			string name1 = this.Name;
			string name2 = dev.Name;

			if (name1 != null) {
				equalName = uuid1.Equals (mac2);
			} else {//this should never be reached
				equalName = name2 == null;
			}

			//bool equalConnectionMode = this.btConnectionMode == dev.btConnectionMode;//ConnectionMode in unity isn't important, Java might alter it.

			bool equalJavaConnectionMode = false;

			if (isReadyWithoutAttemptToGetReady ()) {
				int conMode1 = this.JavaBtConnection.Call<int> (GET_CONNECTION_MODE);
				int conMode2 = dev.JavaBtConnection.Call<int> (GET_CONNECTION_MODE);
				equalJavaConnectionMode = conMode1 == conMode2;
			} else {
				equalJavaConnectionMode = true;
			}
			return equalMAC && equalUUID && equalName && equalJavaConnectionMode;
		}
		*/

		private bool isReadyWithoutAttemptToGetReady ()
		{
			return this.JavaBtConnection != null && 
				this.JavaBtConnection.GetRawClass ().ToInt32 () != 0;
		}

		private bool disposed = false;

		//Implement IDisposable.
		public void Dispose ()
		{
			Dispose (true);
			GC.SuppressFinalize (this);
		}

		internal static void DisposeAllDevices ()
		{
			List<int> keys = new List<int> (BluetoothDevice.DevicesMap.Keys);
			foreach (int i in keys) {
				BluetoothDevice.DevicesMap [i].stopReadingCoroutine();
				BluetoothDevice.DevicesMap [i].Dispose ();
			}
			
			BluetoothDevice.DevicesMap.Clear ();
		}

		protected virtual void Dispose (bool disposing)
		{
			if (!disposed) {
				if (disposing) {
					//the Bt.cs class will free resources when game exit, but it's better to call close() when you done using this reference to free all resources.
					if (BluetoothDevice.DevicesMap.ContainsKey (this.Id))
						BluetoothDevice.DevicesMap.Remove (this.Id);
				}
				if (isReadyWithoutAttemptToGetReady ()) {
					Debug.Log ("JAVA Instance Removed");

					this.JavaBtConnection.Call (INSTANCE_REMOVED);
				}
				if (this.javaBtConnection != null) {
					this.javaBtConnection.Dispose ();
					this.javaBtConnection = null;
				}
				// Free your own state (unmanaged objects).
				// Set large fields to null.
				disposed = true;
			}
		}



		//###Destructor###
		~BluetoothDevice ()
		{
			// Simply call Dispose(false).
			Dispose (false);

		}
	}
}
