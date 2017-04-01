/*
 * Use of this asset and source code is governed by the ASSET STORE TERMS OF SERVICE AND EULA license
 * that can be found in the LICENSE file at https://unity3d.com/legal/as_terms
 */
using UnityEngine;

//TODO RELEASE DISCOVERY RESOURCES
namespace TechTweaking.BtCore.BtBridge
{
	public   class  BtBridge
	{

		private static BtBridge instance;
		private readonly AndroidJavaObject ajc;

		private readonly bool PluginReady;
		private const string BridgePackage = "com.techtweaking.bluetoothcontroller.Bridge";

		//###########constants############
		private const string CREATE_BT_CONNECTION_OBJ = "createBlutoothConnectionObject";
		private const string GET_PAIRED_DEVICES = "getPairedDevices";
		private const string GET_BONDED_DEVICES = "getBondedDevices";

		private const string GET_NEXT_DISCOVERED_DEVICES = "getNextDiscoveredDevice";
		//Not for server but regular discovery
		private const string ASK_ENABLE_BT = "askEnableBluetooth";
		private const string ENABLE_BT = "enableBluetooth";
		private const string DISABLE_BT = "disableBluetooth";
		private const string IS_BT_ENABLED = "isBluetoothEnabled";
		private const string GET_PICKED_DEVICE = "getPickedDevice";
		private const string REGISTER_STATE_RECEIVER = "registerStateReceiver";
		private const string DE_REGISTER_STATE_RECEIVER = "deRegisterStateReceiver";
		private const string GET_CLIENT_DEVICE = "getDiscoveredDeviceForServer";
		private const string SHOW_DEVICES = "showDevices";
		private const string ON_DESTROY = "OnDestroy";
		private const string RELEASE_DISCOVERY_RESOURCES = "releaseDiscoveryResources";
		private const string CANCEL_DISCOVERY = "cancelDiscovery";
		private const string START_DISCOVERY = "startDiscovery";
		private const string REFRESH_DISCOVERY = "refreshDiscovery";

		private const string MAKE_DISCOVERABLE = "makeDiscoverable";
		private const string INITE_SERVER = "initServer";
		private const string RENAME_UNITY_OBJECT = "renameUnityObject";

		private static string unity_game_object_name = "BtConnector";
		private static string MY_MAC_ADRESS = "MyMacAdress";
		private static string ABORT_SERVER = "abortServer";
		#if !UNITY_EDITOR && UNITY_ANDROID
		private static bool needCommitObjectName = false; 
		#endif

		private  BtBridge ()
		{

			ajc = null;
			PluginReady = false;


			#if !UNITY_EDITOR && UNITY_ANDROID

			if (Application.platform == RuntimePlatform.Android) {

			try {

				using (AndroidJavaClass ajcClazz = new AndroidJavaClass (BridgePackage)) {

				if(!IsAndroidJavaClassNull (ajcClazz)){
					ajc = ajcClazz.CallStatic<AndroidJavaObject> ("getInstance",unity_game_object_name);		
					PluginReady = !IsAndroidJavaObjectNull (ajc);
					if(needCommitObjectName && PluginReady)  {
						ajc.Call (RENAME_UNITY_OBJECT, unity_game_object_name);

					}
				}
			}

			} catch {
				Debug.LogError ("Bluetooth initialization failure. Probably classes.jar not present in directory (Assets->Plugins->classes.jar) ");
				throw;
			}
			}

			#endif	


		}

		public static BtBridge Instance {
			get {
				if (instance == null) {
					instance = new BtBridge ();
				}
				return instance;
			}
		}

		static internal void set_unity_game_object_name (string name)
		{
			if (name.Equals (unity_game_object_name))
				return;
			#if !UNITY_EDITOR && UNITY_ANDROID
			if(instance == null) {//Plugin is not ready yet. Instance.Plugin Ready will create instance. It's internal method and the user might not need an instance yet.
				needCommitObjectName = true;
				unity_game_object_name = name;
			}else {
				Instance.ajc.Call (RENAME_UNITY_OBJECT, unity_game_object_name);
			}
			#endif
		}


		public void test ()
		{

			if (!PluginReady)
				return;
			ajc.Call ("startActivity");

			/*
			using (AndroidJavaClass ass = new AndroidJavaClass ("com.techtweaking.bluetoothcontroller.ForwarderActivity1")) {

			


				ass.CallStatic ("startActivity");
			}
			*/
		}


		public bool isPluginReady ()
		{
			return PluginReady;
		}

		private bool IsAndroidJavaClassNull (AndroidJavaClass androidJavaClass)
		{
			return androidJavaClass == null ||
				androidJavaClass.GetRawClass ().ToInt32 () == 0;
		}

		private bool IsAndroidJavaObjectNull (AndroidJavaObject androidJavaObject)
		{
			return androidJavaObject == null ||
				androidJavaObject.GetRawClass ().ToInt32 () == 0;
		}

		public  AndroidJavaObject createJavaBtConnection (int id)
		{

			if (!PluginReady)
				return null;
			return ajc.Call<AndroidJavaObject> (CREATE_BT_CONNECTION_OBJ, id);
		}

		public AndroidJavaObject[] getPairedDevices ()
		{
			if (!PluginReady)
				return null;
			return ajc.Call<AndroidJavaObject[]> (GET_PAIRED_DEVICES);
		}

		//returns array of strings {NAME1,MAC1,NAME2,MAC2,....,NAMEn,MACn}
		public string[] getBondedDevices ()
		{
			if (!PluginReady)
				return null;
			return ajc.Call<string[]> (GET_BONDED_DEVICES);
		}

		public void startServer (string UUID, int time, bool connectOneDevice)
		{
			if (!PluginReady)
				return;
			ajc.Call (INITE_SERVER, UUID, time, connectOneDevice);
		}
		public void abortServer() {
			if (!PluginReady)
				return;
			ajc.Call (ABORT_SERVER);
		}

		public void askEnableBluetooth ()
		{
			if (!PluginReady)
				return;
			ajc.Call (ASK_ENABLE_BT);
		}

		public bool enableBluetooth ()
		{
			if (!PluginReady)
				return false;
			return ajc.Call<bool> (ENABLE_BT);
		}

		public bool disableBluetooth ()
		{
			if (!PluginReady)
				return false;
			return ajc.Call<bool> (DISABLE_BT);
		}

		public bool isBluetoothEnabled ()
		{
			if (!PluginReady)
				return false;
			return ajc.Call<bool> (IS_BT_ENABLED);
		}

		public AndroidJavaObject getPickedDevice (int id)
		{
			if (!PluginReady)
				return null;

			return ajc.Call<AndroidJavaObject> (GET_PICKED_DEVICE, id);
		}


		public string MyMacAdress ()
		{
			if (!PluginReady)
				return null;

			return ajc.Call<string> (MY_MAC_ADRESS);
		}

		public void registerStateReceiver ()
		{
			if (!PluginReady)
				return;

			ajc.Call (REGISTER_STATE_RECEIVER);

		}

		public void deRegisterStateReceiver ()
		{
			if (!PluginReady)
				return;

			ajc.Call (DE_REGISTER_STATE_RECEIVER);

		}

		internal AndroidJavaObject getClientDeviceForServer (int id)
		{
			if (!PluginReady)
				return null;

			return ajc.Call<AndroidJavaObject> (GET_CLIENT_DEVICE, id);


		}

		//General Discovery devices... not for server
		internal AndroidJavaObject getNextDiscoveredDevice (int id)
		{
			if (!PluginReady)
				return null;

			return ajc.Call<AndroidJavaObject> (GET_NEXT_DISCOVERED_DEVICES, id);

		}

		public bool startDiscovery ()
		{
			if (!PluginReady)
				return false;

			return ajc.Call<bool> (START_DISCOVERY);
		}

		public bool refreshDiscovery ()
		{
			if (!PluginReady)
				return false;

			return ajc.Call<bool> (REFRESH_DISCOVERY);
		}

		public bool cancelDiscovery ()
		{
			if (!PluginReady)
				return false;

			return ajc.Call<bool> (CANCEL_DISCOVERY);
		}

		public void releaseDiscoveryResources ()
		{
			if (!PluginReady)
				return;

			ajc.Call (RELEASE_DISCOVERY_RESOURCES);
		}

		public void makeDiscoverable (int time)
		{
			if (!PluginReady)
				return;

			ajc.Call (MAKE_DISCOVERABLE, time);
		}

		public void showDevices ()
		{
			if (!PluginReady)
				return;

			ajc.Call (SHOW_DEVICES);
		}

		/* TEST
		public byte[] test (byte[] x)
		{

			if (!PluginReady)
				return new byte[]{};
			
			return ajc.Call<byte[]> ("TEST", x);
		}
		*/


		//THIS METHOD SHOULD BE CALLED AFTER EVERYTHING RELATED TO BLUETOOTH NEEDS TO BE ENDED
		public void OnDestroy ()
		{


			if (PluginReady) {
				ajc.Call (ON_DESTROY);
			}

			try {
				if (ajc != null) {
					ajc.Dispose ();
				}
			} catch (UnityException) {

			}

			instance = null;//This will dispose this instance. And will create a new one if needed next time.


		}

		~BtBridge ()
		{
			try {
				if (ajc != null) {
					ajc.Dispose ();
				}
			} catch (UnityException) {

			}
			instance = null;
		}

	}
}

