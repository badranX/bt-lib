package com.techtweaking.bluetoothcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

import com.unity3d.player.UnityPlayer;

public class ForwardingActivity extends Activity {

    private static final int REQUEST_DISCOVERABLE_CODE = 42;

    private static boolean serverIntent = false;
    private static int serverTime = 0;

    //TODO is it worth it to check with server timing when resultCode?
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        if (serverIntent) {
            serverIntent = false;
            initServer();
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_DISCOVERABLE_CODE) {

            if (resultCode == Activity.RESULT_CANCELED) {
                PluginToUnity.ControlMessages.SERVER_FINISHED_LISTENING.send("0");//"0" means server finished by User
            } else {

                BtInterface.getInstance().startServer();//it will start if was asked
            }
            finish();
        }
    }


    private static void startActivity() {
        Activity _this = UnityPlayer.currentActivity;
        Intent i = new Intent(_this, ForwardingActivity.class);
        _this.startActivity(i);
    }


    private void initServer() {
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, serverTime);

        this.startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_CODE);
    }


    static void makeDiscoverable(int time) {

        serverTime = time;
        serverIntent = true;
        startActivity();
    }


}
