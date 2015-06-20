package com.badran.bluetoothcontroller;

import com.unity3d.player.UnityPlayer;

/**
 * Created by a on 6/12/15.
 */
public final class PluginToUnity {

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


    public enum ControlMessages {
        CONNECTED (1), //"Connected"
        DISCONNECTED (2) , //"Disconnected"
        UNABLE_TO_CONNECT (-1), //"found your Bluetooth Module but unable to connect to it"
        NOT_FOUND (-2), //"Bluetooth module with the name or the MAC you provided can't be found"
        MODULE_OFF (-3),//"Connection Failed, usually because your Bluetooth module is off "
        CLOSING_ERROR (-4), //"error while closing"
        SENDING_ERROR (-5), //"error while writing"
        READING_ERROR (-6),//error while reading

        //added after version 3.6
        DATA_AVAILABLE (3);



        private final int value;

        private static final String UNITY_GAME_OBJECT_NAME = "BtConnector";
        private static final String UNITY_CONNECTION_STATUS = "connectionStatus";
        private static final String UNITY_DATA_AVAILABLE = "dataAvailable";
        private static final String EMPTY_STRING = "";
        private static final String UNITY_METHOD = "controlMessagesReceiver";
        private ControlMessages(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }

        public void send(int id){ //send Control Message in the Name of THE CONNECTION [id]
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT_NAME, UNITY_METHOD, Integer.toString(value));

        }
    }










}
