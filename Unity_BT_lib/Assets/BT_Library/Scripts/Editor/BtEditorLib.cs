using UnityEngine;
using UnityEditor;
using System.Collections;
using System.IO;
using System.IO.Compression;
using System;
namespace TechTweaking.BtLibrary.Editor {
	//TODO CHECK IF THE BT_LIBRARY PLUGIN FOLDER IS REMOVED.
	internal class BtEditorLib : ScriptableObject{

		private static BtEditorLib instance;
		private  const string JAR_FILE = "BT_Library_Bluetooth_Classic.jar.txt";
		private const string MANIFEST_FILE = "AndroidManifest.txt";
		private  const string JAR_FILE_2 = "BT_Library_Bluetooth_Classic.jar";
		private const string MANIFEST_FILE_2 = "AndroidManifest.xml";

		private BtEditorLib() {}

		internal static BtEditorLib Instance
		{
			get 
			{
				if (instance == null)
				{
					instance = ScriptableObject.CreateInstance("BtEditorLib") as BtEditorLib;
				}
				return instance;
			}
		}

		internal static void Remove_Instance ()
		{
				instance = null;
		}

		internal void Initialize() {
			if(!isBtJarAvailable()){
				EditorUtility.DisplayDialog(
					"Error",
					"Can't find the needed 'Plugins' files for the BT library. They should be available in 'NativeLibraries/Android/' folder ",
					"Ok");
				return;
			}

			InitializeFiles();
		}

		private string InitializeFolders() {
			string path = Application.dataPath;
		
			path = Path.Combine(path,"Plugins");
			if(Directory.Exists(path)){
				path = Path.Combine(path,"Android");
				if(!Directory.Exists(path)) {
					string relativPath = Path.Combine("Assets","Plugins");
					AssetDatabase.CreateFolder(relativPath,"Android");
				}
			}else {
				path = Path.Combine(path,"Android");
				AssetDatabase.CreateFolder("Assets","Plugins");
				AssetDatabase.CreateFolder(Path.Combine("Assets","Plugins"),"Android");
			}

			return path;
		}

		//Require ready Plugins/Android folder
		private void InitializeFiles() {


			string path = InitializeFolders();

			string []  subdirectoryEntries = Directory.GetFiles(path,MANIFEST_FILE_2);
			if(subdirectoryEntries.Length == 1) {



				if( 
					EditorUtility.DisplayDialog ("Warning", "AndroidManifest.xml file exists in 'Assets/Plugins/Android' folder. " +
						" The wizard might add a couple of permissions to the file (if needed)."
					+ " Do you want to proceed?","Yes","No")
				) PluginsMerger.MergeManifest(subdirectoryEntries[0]);


				
			}else if (subdirectoryEntries.Length >1) {
				EditorUtility.DisplayDialog(
					"BT_Library setup Error",
					"You have more than one AndroidManifest.xml file in the 'Assets/Plugins/Android' folder. " +
					"One AndroidManifest.xml file should be avialble.",
					"Ok");
				Debug.LogError("You have more than one AndroidManifest.xml file in the Assets/Plugins/Android folder. " +
					"One AndroidManifest.xml file should be avialble.");
				return;
			}else {


				PluginsMerger.AddNewManifest(Path.Combine(path,MANIFEST_FILE_2));
			}
			if(!IsJarFileInitialized()) {

				moveClassesJarFile();
			}
			AssetDatabase.Refresh();
		}

		private void moveClassesJarFile() {
			string relativePath_Dest =  combine (new string[] {"Assets","Plugins", "Android",JAR_FILE_2});
			string BtLibAndroidPath = getBtLibAndroidPath();
			string relativeBtLibJar = Path.Combine(BtLibAndroidPath,JAR_FILE);


			//Moving Plugins file to the right position


			AssetDatabase.CopyAsset(relativeBtLibJar, relativePath_Dest);

		}
		private void moveManifestFile() {
			string relativePath =  combine (new string[] {"Assets","Plugins", "Android",MANIFEST_FILE_2});
			string BtLibAndroidPath = getBtLibAndroidPath();
			string relativeBtLibJar = Path.Combine(BtLibAndroidPath,MANIFEST_FILE);


			//Moving Plugins file to the right position


			AssetDatabase.CopyAsset(relativeBtLibJar, relativePath);
		}

		//absolute path
		private string getBtLibAndroidPath() {
			MonoScript scriptAsset =  MonoScript.FromScriptableObject(BtEditorLib.Instance);
			string path = AssetDatabase.GetAssetPath(scriptAsset);
			path = Path.GetDirectoryName(path);//Moving up to the Editor folder
			path = Path.GetDirectoryName(path);//Moving up to the Script folder
			path = Path.GetDirectoryName(path);//Moving up to the BT Library folder

			path = Path.Combine(path,"NativeLibraries");
			path = Path.Combine(path,"Android");
			return path;
		}

	


		private string combine (string [] folders) {
			
			string returned_path = folders[0];
			for (int i = 1; i < folders.Length; i++) {
				returned_path = Path.Combine(returned_path, folders[i]);
			}

			return returned_path;
		}

		/*
		public string toRelativPath(string path) {
			string [] s = path.Split(new [] {"Assets"}, StringSplitOptions.None);
			return "Assets" + s[s.Length -1];

		}
		*/


		internal bool isBtJarAvailable() {
			string path = getBtLibAndroidPath();
			if(Directory.Exists(path)) {
				string jar_path = Path.Combine(path,JAR_FILE);

				if(File.Exists(jar_path)) {
					return true;
				}
			}
			return false;
		}


		internal bool IsJarFileInitialized() {
			string path = Application.dataPath;
			path = System.IO.Path.Combine(path, "Plugins");
			path = System.IO.Path.Combine(path, "Android");
			if(Directory.Exists(path)) {
				string jar_path = Path.Combine(path,JAR_FILE_2);

				if(File.Exists(jar_path)) {
					return true;
				}

			}
			return false;
		}

		internal bool IsManifestFileInitialized() {
			string path = Application.dataPath;
			path = System.IO.Path.Combine(path, "Plugins");
			path = System.IO.Path.Combine(path, "Android");
			if(Directory.Exists(path)) {
				string jar_path = Path.Combine(path,MANIFEST_FILE_2);

				if(File.Exists(jar_path)) {
					return true;
				}

			}
			return false;
		}
	}
}
