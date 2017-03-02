using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using System.Collections.Generic;
using System.Text;
public class ScrollTerminalUI : MonoBehaviour {

	private string newLine ;
	public int maxLength = 200;
	public int maxNumberOfMessages = 400;
	private StringBuilder SB = new StringBuilder();
	private Text mainText;
	private int newLineLen;
	Queue<int> lengths = new Queue<int>();

	void Awake(){
		mainText = GetComponent<Text>();
		newLine =  System.Environment.NewLine;
		newLineLen = newLine.Length;
	}

/*
 * We will add functionality to aloow automatic scrolling, in next release
//	private bool isScrollUsed = false;
//
//	public void onDown(){
//		isScrollUsed = true;
//	}
//
//	public void onUp(){
//		isScrollUsed = false;
//	}

*/


	public void set(string txt) {
		mainText.text = txt;
	}
	public void add(string deviceName , string text){

		if (string.IsNullOrEmpty(text)) return;


		deviceName = string.IsNullOrEmpty(deviceName) ? "Unkown : " : deviceName + " : ";


		int nameLen = deviceName.Length;
		int txtLen = text.Length;


		if(text.Length <= maxLength ){
			SB.Append(deviceName);
			SB.Append(text);
			SB.Append(newLine);

			lengths.Enqueue(nameLen + txtLen + newLineLen);
		}else {
			do { 
				SB.Append(deviceName);
				SB.Append(text.Substring(0,maxLength));
				SB.Append(newLine);

				lengths.Enqueue(nameLen + maxLength + newLineLen);

				text = text.Substring(maxLength);

			} while (text.Length > maxLength );

			if(text.Length != 0){
				SB.Append(deviceName);
				SB.Append(text);
				SB.Append(newLine);
				lengths.Enqueue(nameLen + text.Length + newLineLen);
			}
		}

		int lengthToCut = 0;
		while(lengths.Count > maxNumberOfMessages){
			lengthToCut += lengths.Dequeue();
		}
        

		if( lengthToCut > 0) {
			SB.Remove (0,lengthToCut);
		}
      
		mainText.text = SB.ToString();

	}
}
