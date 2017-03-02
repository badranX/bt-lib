using UnityEngine;
using UnityEditor;
using System.IO;
using System;

namespace TechTweaking.BtLibrary.Editor
{
	//TODO CHECK IF CLASSES.JAR COPIED IF THE SAME NAM OR NOT
	//TODO LAMP::: CHANGE CLASSES.JAR TO CLASSES.TXT SO YOU CAN GENERATE AS MUCH CLASSES AS U WANT
	public class BtEditor  : EditorWindow
	{

		private  static bool isInitialized;
		BtEditorLib editorLib;

		[MenuItem ("Tools/TechTweaking/Bluetooth Classic/Setup the BT library")]
		public static void initializeBT_Lib ()
		{

			while (true) {
				bool allow = EditorUtility.DisplayDialog ("Autmatic BT library setup?",
					             "The BT library will do the followings : " + Environment.NewLine + Environment.NewLine
					             + " 1. Add a Jar file to 'Assets/Plugins/Android'." + Environment.NewLine + Environment.NewLine
					             + " 2. Add its own 'AndroidManifest.xml' or combine itself with any available Plugin."
					, "Ok", "Cancel");


				if (allow) {
					BtEditorLib.Instance.Initialize ();
					break;
				} else {
					
					if (EditorUtility.DisplayDialog ("Warning", "Are you sure you don't want an Automatic Setup?", "Yes", "No"))
						break;
				}
			}

		}

		/*
		static BtEditor ()
		{

			bool isInitialized = BtEditorLib.Instance.IsJarFileInitialized() && BtEditorLib.Instance.IsManifestFileInitialized();
			while (!isInitialized) {
				bool allow = EditorUtility.DisplayDialog ("Autmatic BT library setup?",
					            "The BT library will do the followings : " + Environment.NewLine + Environment.NewLine
					            + " 1. Add a Jar file to 'Assets/Plugins/Android'." + Environment.NewLine + Environment.NewLine
					            + " 2. Add its own 'AndroidManifest.xml' or combine itself with any available Plugin."
				, "Ok", "Cancel");
			

				if (allow) {
					BtEditorLib.Instance.Initialize ();
					break;
				} else {

					if (EditorUtility.DisplayDialog ("Warning", "Are you sure you don't want an Automatic Setup?", "Yes", "No"))
						break;
				}
			}

		}
		*/


	
	}
}