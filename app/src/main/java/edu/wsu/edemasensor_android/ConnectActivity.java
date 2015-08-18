package edu.wsu.edemasensor_android;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.*;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class ConnectActivity extends AppCompatActivity{

    int REQUEST_ENABLE_BT;
    String DEVICE_NAME;
    String DEVICE_PIN;
    UUID MY_UUID;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mSensor;
    ArrayList<BluetoothDevice> foundBluetoothDevices;
    ArrayList<String> bluetoothDeviceNames;
    ArrayAdapter<String> mAdapter;

    public ConnectActivity() {
        DEVICE_PIN = "1234";
        // TODO: Insert device name
        DEVICE_NAME = "";
        REQUEST_ENABLE_BT = 1;
        MY_UUID = UUID.fromString("6B433250-4D32-4E15-AE2D-CB19B5FAC9CC");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        foundBluetoothDevices = new ArrayList<>();
        bluetoothDeviceNames = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        // Exit if there is no bluetooth adapter
        if(mBluetoothAdapter == null) {
            simpleAlert("Error! Bluetooth is not supported\n"+
                        "on this device.");
            this.finish();
            System.exit(0);
        }

        // Create adapter to display list of found BT devices
        mAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, bluetoothDeviceNames);

        // Ensure Bluetooth adapter is enabled
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        // Set ListView onClickListener and Adapter
        ListView mListView = (ListView) findViewById(R.id.deviceListView);
        mListView.setOnItemClickListener(mMessageClickedHandler);
        mListView.setAdapter(mAdapter);
    }

    // Handle click events for the list of BT devices
    private AdapterView.OnItemClickListener mMessageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            // Get the name of the clicked item and attempt to pair with it.
            String deviceName = mAdapter.getItem(position).toString();
            if (setSensorByName(deviceName)) {
                // Connection established. Pass to next activity
                nextActivity();
            }
        }
    };

    private void nextActivity(){
        Intent intent = new Intent(this, RecordingSetupActivity.class);
        intent.putExtra("mSensor", mSensor);
        startActivity(intent);
    }

    // Sets mSensor to the foundBluetoothDevice with entered name
    private boolean setSensorByName(String name) {
        for (BluetoothDevice device : foundBluetoothDevices) {
            if (device.getName().equals(name)) {
                if (checkBluetoothSocket(device)){
                    mSensor = device;
                    return true;
                }
                return false;
            }
        }

        // Couldn't find BT device with that name
        simpleAlert("Error: Device can no longer be reached");
        return false;
    }

    void simpleAlert(String message_body) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message_body)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(tryPairedDevices()){
            // Automatic pairing was successful
            nextActivity();
        }
        else {
            mBluetoothAdapter.startDiscovery();
        }
    }

    //Create BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            // When a BT device is found
            if (action == BluetoothDevice.ACTION_FOUND){
                // Add the found device to our list of found devices (if not already there)
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!foundBluetoothDevices.contains(device)) {
                    foundBluetoothDevices.add(device);
                    bluetoothDeviceNames.add(device.getName());
                    // Refresh the listView
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    // Checks the paired devices and sets mSensor to our target device, if found.
    // Returns true if target device found.
    protected boolean tryPairedDevices(){
        // Check for pre-paired devices in range
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            // See if any devices match our target sensor device
            for (BluetoothDevice device : pairedDevices){
                if (device.getName().equals(DEVICE_NAME)){
                    // If the device name matches, this is our target sensor
                    if (checkBluetoothSocket(device)) {
                        // Device can be paired
                        mSensor = device;
                        return true;
                    }
                    return false;
                }
            }
        }
        // Our target sensor has not been pre-paired
        return false;
    }

    protected boolean checkBluetoothSocket(BluetoothDevice device){
        mBluetoothAdapter.cancelDiscovery();
        BluetoothSocket tmpSocket = null;
        try{
           tmpSocket  = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (IOException e) {
            return false;
        }
        try{
            tmpSocket.connect();
        }
        catch (IOException e) {
            // Can't connect. Ensure unused socket is closed.
            try {
                tmpSocket.close();
            }
            catch (IOException ex){}
            return false;
        }

        // Connection to device is possible
        try {
            tmpSocket.close();
        }
        catch (IOException e){}
        return true;
    }

    public void onClickRescan(View view) {
        // Clear previous devices and search again.
        mBluetoothAdapter.cancelDiscovery();
        foundBluetoothDevices.clear();
        bluetoothDeviceNames.clear();
        mAdapter.notifyDataSetChanged();
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }
}
