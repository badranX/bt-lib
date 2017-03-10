package com.techtweaking.bluetoothcontroller;


import android.bluetooth.BluetoothSocket;
import com.unity3d.player.UnityPlayer;




public class PluginToUnity {

    private PluginToUnity() {
        // restrict instantiation
    }

    //    public  final int CONNECTED = 1; //"Connected"
//    public  final int DISCONNECTED = 2; //"Disconnected"
//    public  final int UNABLE_TO_CONNECT = -1; //"found your Bluetooth Module but unable to connect to it"
//    public  final int NOT_FOUND = -2; //"Bluetooth module with the name or the MAC you provided can't be found"
//    public  final int MODULE_OFF = -3; //"Connection Failed, usually because your Bluetooth module is off "
//    public  final int CLOSING_ERROR = -4; //"error while closing"
//    public  final int SENDING_ERROR = -5; //"error while writing"
//    public  final int READING_ERROR = -6; //"error while reading"
    volatile static BluetoothSocket socket;

    private static String UNITY_GAME_OBJECT_NAME = "BtConnector";//TODO
    public static void change_UNITY_GAME_OBJECT_NAME(String name) {
        UNITY_GAME_OBJECT_NAME = name;
    }

    public enum ControlMessages {
        CONNECTED ("TrConnect"), //"Connected"
        DISCONNECTED ("TrDisconnect") , //"Disconnected"
        NOT_FOUND ("TrModuleNotFound"), //"Bluetooth module with the name or the MAC you provided can't be found"
        MODULE_OFF ("TrModuleOFF"),//"Connection Failed, usually because your Bluetooth module is off "
        SENDING_ERROR ("TrSendingError"), //"error while writing"
        READING_ERROR ("TrReadingError"),//error while reading
        CONNECTION_ERROR("TrConnectionError"),
        EMPTIED_DATA ("TrEmptiedData"),
        //added after version 3.6
        DATA_AVAILABLE ("TrDataAvailable"),

        READING_STOPPED ("TrReadingStopped"),
        READING_STARTED("TrReadingStarted"),
        DEVICE_PICKED ("TriggerPicked"),
        BLUETOOTH_OFF("TrBluetoothOFF"),
        BLUETOOTH_ON("TrBluetoothON"),
        SERVER_DISCOVERED_DEVICE("TrServerDiscoveredDevice"),
        SERVER_FINISHED_LISTENING("TrServerFinishedListening"),//"0" means finished by user, "-1" otherwise

        DISCOVERED_DEVICE("TrDiscoveredDevice"),
        ACTION_DISCOVERY_FINISHED("TrDiscoveryFinished");


        private final String value;

        private ControlMessages(final String newValue) {
            value = newValue;
        }


        //public String getValue() { return value; }

        public void send(int id){ //send Control Message in the Name of THE CONNECTION [id]
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT_NAME, value, Integer.toString(id));
        }

        //CAREFULL ANDROID USES ':' in macAddress
        public void send(){ //send Control Message in the Name of THE CONNECTION [id]
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT_NAME, value, "");
        }

        public void send(String msg){ //send Control Message in the Name of THE CONNECTION [id]
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT_NAME, value, msg);
        }

        public void send(int id,String val) {
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT_NAME, value, Integer.toString(id)+ "#$" + val);
        }


        //USED FOR ONLY TrDiscoveredDevice
        public void send(String Name,String macAddress, String RSSI) {
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT_NAME, value, Name + "#$" + macAddress + "#$" + RSSI);

        }

        //USED FOR ONLY ShowDevices
        public void send(String Name,String macAddress) {
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT_NAME, value, Name +"#$" + macAddress);
        }

    }


}