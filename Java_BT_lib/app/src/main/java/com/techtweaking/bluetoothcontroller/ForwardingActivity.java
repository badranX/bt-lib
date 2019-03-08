package com.techtweaking.bluetoothcontroller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;

public class ForwardingActivity extends Activity {

    private static final int REQUEST_DISCOVERABLE_CODE = 42;
    private static final int REQUEST_COARSE_LOCATION_PERMISSIONS = 103;

    private static boolean serverIntent = false;
    private static int serverTime = 0;
    private static boolean RSSI_Discovery_intent = false;
    //TODO is it worth it to check with server timing when resultCode?
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        if (serverIntent) {
            serverIntent = false;
            initServer();
        } else if(RSSI_Discovery_intent) {

            RSSI_Discovery_intent = false;
            ask_RSSI_permissions();
        }else finish();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    BtInterface.getInstance().initDiscovery();
                } else {
                    PluginToUnity.ControlMessages.USER_REJECT_LOCATION_PERMISSION.send();
                    //TODO cancelOperation();ActivityCompat
                }

            }
        }
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_DISCOVERABLE_CODE) {

            if (resultCode == Activity.RESULT_CANCELED) {
                PluginToUnity.ControlMessages.SERVER_FINISHED_LISTENING.send("0");//"0" means server finished by User
            } else {

                BtInterface.getInstance().startServer();//it will start if was asked
            }

        }
        finish();
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

    private void  ask_RSSI_permissions(){

        ActivityCompat.requestPermissions(this,
                new String[]{
                        android.Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_COARSE_LOCATION_PERMISSIONS);

    }

    static void makeDiscoverable(int time) {

        serverTime = time;
        serverIntent = true;
        startActivity();
    }

    static void askLocationPermission(){
        RSSI_Discovery_intent = true;
        startActivity();
    }


}
