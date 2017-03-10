using UnityEngine;
using UnityEditor;
using System.Collections;
using System.Xml;
using System.Text;
using System.IO;
using System;
namespace TechTweaking.BtLibrary.Editor
{
	public class PluginsMerger 
	{

		private  const string ACCESS_LOCATION_PERMISSION = "android.permission.ACCESS_COARSE_LOCATION";
		private  const string BLUETOOTH_PERMISSION = "android.permission.BLUETOOTH";
		private  const string BLUETOOTH_ADMIN_PERMISSION = "android.permission.BLUETOOTH_ADMIN";
		private const string FORWARDING_ACITIVITY = "com.techtweaking.bluetoothcontroller.ForwardingActivity";

		private static string[][] Inside_PlayBackEngine = new string[][] {
			new string[] {  "AndroidPlayer","Apk","AndroidManifest.xml"},
			new string[] {  "AndroidPlayer","AndroidManifest.xml"}
		};
		private  static string[][] Navigate_To_PlayBackEngine = new string[][] {
			new string[] {"PlaybackEngines"},
			new string[] { "..", "..","PlaybackEngines"}
		};

		public static bool MergeManifest(string xmlPath) {
			XmlDocument doc = loadManifest(xmlPath);
			if(doc != null) {
				fillRequiredXML(doc,xmlPath);
				return true;
			}

			return false;

		}

		public static bool AddNewManifest (string targetPath) {
			string manifest = GetOriginalManifest();
			XmlDocument doc = loadManifestFromString(manifest);
			if(doc != null) {
				fillRequiredXML(doc,targetPath);
				return true;
			}

			return false;
		}
		private static XmlDocument loadManifest (string xmlPath)
		{
			XmlDocument doc = new XmlDocument ();
			doc.PreserveWhitespace = true;

			try {
				doc.Load (xmlPath);
			} catch {
				Debug.LogError ("Can't load AndroidManifest from : " + xmlPath);
				return null;
			}

			return doc;

		}

		private static XmlDocument loadManifestFromString (string manifest)
		{
			XmlDocument doc = new XmlDocument ();
			doc.PreserveWhitespace = true;

			try {
				doc.LoadXml(manifest);
			} catch {
				Debug.LogError ("Can't load AndroidManifest from");
				return null;
			}

			return doc;

		}

		private static bool addPermission(XmlDocument doc, string xmlPath, string Permission,bool first, bool last) {
			XmlNode manifest = doc.SelectSingleNode ("manifest");
			XmlNode applicationNode = manifest.SelectSingleNode ("application");

			if(manifest == null || applicationNode == null)
			{
				Debug.LogError("the AndroidManifest available in the 'Assets/Plugins/Android' folder lacks a <manifest> node or an <application> node");
				return false;
			}

			XmlElement permissionNode = doc.CreateElement ("uses-permission");
			permissionNode.SetAttribute ("name", "http://schemas.android.com/apk/res/android", Permission);

			manifest.InsertBefore (doc.CreateTextNode (System.Environment.NewLine + "  "), applicationNode);

			if(first)
			{
				XmlComment newComment = doc.CreateComment("Permissions added by the BT library");

				manifest.InsertBefore (newComment,applicationNode);
				manifest.InsertBefore (doc.CreateTextNode (System.Environment.NewLine + "  "), applicationNode);

			}

			manifest.InsertBefore (permissionNode, applicationNode);
			if(last) {
				
				manifest.InsertBefore (doc.CreateTextNode (System.Environment.NewLine + System.Environment.NewLine), applicationNode);

			}

			return true;

		}
		private static bool addActivity(XmlDocument doc, string xmlPath, string Activity_Name) {
			XmlNode manifest = doc.SelectSingleNode ("manifest");
			XmlNode applicationNode = manifest.SelectSingleNode ("application");
			if(manifest == null || applicationNode == null)
			{
				Debug.LogError("the AndroidManifest available in the 'Assets/Plugins/Android' folder lacks a <manifest> node or an <application> node");
				return false;
			}
			bool isActivityExists = false;

			XmlNodeList nList = applicationNode.SelectNodes("activity");

			foreach(XmlNode node in nList) {
				XmlAttribute atrp =  node.Attributes["android:name"];
				if(atrp != null && atrp.Value.Equals(Activity_Name) ) isActivityExists = true;
			}
			if(!isActivityExists) {
				XmlElement ActivityNode = doc.CreateElement ("activity");
				ActivityNode.InnerText = "";
				ActivityNode.SetAttribute ("name", "http://schemas.android.com/apk/res/android", Activity_Name);
				ActivityNode.SetAttribute ("background", "http://schemas.android.com/apk/res/android", "#00000000");

				applicationNode.AppendChild(ActivityNode);

				XmlComment newComment = doc.CreateComment("Activity added by the BT library");
				applicationNode.InsertBefore (newComment,ActivityNode);
				applicationNode.InsertBefore (doc.CreateTextNode (System.Environment.NewLine + "  "), ActivityNode);
				applicationNode.InsertAfter (doc.CreateTextNode (System.Environment.NewLine + "  "), ActivityNode);


			}

			return true;

		}

		private static bool saveXML (XmlDocument doc,string xmlPath) {
			XmlWriterSettings settings = new XmlWriterSettings {
				Indent = true,
				IndentChars = "  ",
				NewLineChars = System.Environment.NewLine,
				NewLineHandling = NewLineHandling.Replace
			};
			using (XmlWriter writer = XmlWriter.Create (xmlPath, settings)) {
				doc.Save (writer);
			}

			AssetDatabase.Refresh ();
			return true;
		}
		private static bool fillRequiredXML (XmlDocument doc,string xmlPath)
		{
			
			XmlNode node = doc.SelectSingleNode ("manifest");

			if(node == null) {
				Debug.LogError("the AndroidManifest available lacks a <manifest> node ");
				return false;
			}

			XmlNodeList list = node.SelectNodes ("uses-permission");
			bool isAccesLocationPermited = false;
			bool isBluetoothPermited = false;
			bool isBluetoothAdminPermited = false;

			foreach (XmlNode chNode in list) {
				if (chNode.Attributes != null ){
					XmlAttribute atrb= chNode.Attributes ["android:name"];
					if(atrb == null) continue;

					switch( atrb.Value) {
						case ACCESS_LOCATION_PERMISSION : 
							isAccesLocationPermited = true;
							break;
						case BLUETOOTH_PERMISSION : 
							isBluetoothPermited = true;
							break;
						case BLUETOOTH_ADMIN_PERMISSION : 
							isBluetoothAdminPermited = true;
							break;
						}
					}
			}



			if(!isAccesLocationPermited) {
				addPermission(doc,xmlPath,ACCESS_LOCATION_PERMISSION,true,false);
			}
			if(!isBluetoothPermited) {
				addPermission(doc,xmlPath,BLUETOOTH_PERMISSION,isAccesLocationPermited, isBluetoothAdminPermited );
			}
			if(!isBluetoothAdminPermited) {
				addPermission(doc,xmlPath,BLUETOOTH_ADMIN_PERMISSION,isAccesLocationPermited && isBluetoothPermited, !isAccesLocationPermited || !isBluetoothPermited);
			}

			addActivity(doc,xmlPath,FORWARDING_ACITIVITY);

			return saveXML(doc,xmlPath);

		}


		private static string GetOriginalManifest() {
			try {
				
				string unityPath = EditorApplication.applicationContentsPath;

				string manifest_path = navigateToAndroidManifest(unityPath);

				string manifest = File.ReadAllText(manifest_path);


				return manifest;


			} catch (Exception) {
 				throw new Exception("Couldn't get the original Unity AndroidManifest!.  " +
					"  You can provide an AndroidManifest.xml in the 'Assets/Plugins/Android' folder so the wizard can use it" +
					"You can find the original AndroidManifest.xml for you Unity version in the installation folder." +
					"It is in the 'PlaybackEngines' folder. Something like 'PlaybackEngines/AndroidPlayer/../AndroidManifest.xml'");
			}
		}


				
		private static string navigateFromPlayEngine_To_XML(string unityPath) {
			string manifest_path;
			for(int i=0; i<Inside_PlayBackEngine.Length;i++) {
				manifest_path = unityPath;
				for(int j=0; j<Inside_PlayBackEngine[i].Length;j++) {
					manifest_path = Path.Combine(manifest_path,Inside_PlayBackEngine[i][j]);
				}

				if(File.Exists(manifest_path)) {					
					 return manifest_path;
				}
			}
			return "";
		}

		private static string navigateToAndroidManifest(string applicationContentsPath) {
			string playEngine_path;
			for(int i=0; i<Navigate_To_PlayBackEngine.Length;i++) {
				playEngine_path = applicationContentsPath;
				for(int j=0; j<Navigate_To_PlayBackEngine[i].Length;j++) {
					playEngine_path = Path.Combine(playEngine_path,Navigate_To_PlayBackEngine[i][j]);
				}
				if(Directory.Exists(playEngine_path)) 
				{

					string manifest_path = navigateFromPlayEngine_To_XML(playEngine_path);
					if(String.IsNullOrEmpty(manifest_path)) continue;

					return manifest_path;
				}
			}
			return "";
		}
	}
}