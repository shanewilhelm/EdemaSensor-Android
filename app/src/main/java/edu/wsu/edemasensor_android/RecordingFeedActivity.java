package edu.wsu.edemasensor_android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class RecordingFeedActivity extends AppCompatActivity {

    int MAX_RECENT_DATA_ITEMS;
    int REFRESH_RATE;
    int STRETCH_MAX_VALUE;
    int ACCELEROMETER_MAX_VALUE;

    BluetoothDevice mSensor;
    BluetoothSocket bluetoothSocket;
    File tempFile;
    FileOutputStream fileOutputStream;
    String header;
    LinkedList<SensorData> recentData;
    boolean isRecording;
    UUID MY_UUID;

    BarChart stretchChart;
    RadarChart accelerometerChart;

    ArrayList<BarEntry> entries;
    BarDataSet entrySet;
    BarData barData;

    ArrayList<Entry> radarEntries;
    RadarDataSet radarDataSet;
    RadarData radarData;

    public RecordingFeedActivity() {
        MY_UUID = UUID.fromString("6B433250-4D32-4E15-AE2D-CB19B5FAC9CC");
        MAX_RECENT_DATA_ITEMS = 50;
        REFRESH_RATE = 10; //milliseconds
        ACCELEROMETER_MAX_VALUE = 100;
        STRETCH_MAX_VALUE = 1000;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_feed);

        // Initialize variables
        isRecording = true;
        recentData = new LinkedList<>();

        // Get data from intent
        if(getIntent().getExtras() != null) {
            header = getIntent().getStringExtra("header");
            mSensor = getIntent().getExtras().getParcelable("mSensor");
        }
        else {
            // Error with intent data. Restart at connect activity
            Intent intent = new Intent(this, ConnectActivity.class);
            startActivity(intent);
        }

        // Create file to hold data as it's recorded
        try {
            tempFile = File.createTempFile("TempFile", "", this.getCacheDir());
            fileOutputStream = new FileOutputStream(tempFile);
            fileOutputStream.write(header.getBytes());
        }
        catch (IOException e) {
            // TODO: Handle file failure
        }

        // Create Charts
        stretchChart = (BarChart) findViewById(R.id.barChart);
        accelerometerChart = (RadarChart) findViewById(R.id.radarChart);

        // TODO: Restore code after test
        /*
        // Open a Bluetooth socket
        try {
            bluetoothSocket = mSensor.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
        }
        catch (IOException e)
        {
            // TODO: Handle socket/connection error
        }
        */

        // Initialize BarChart
        stretchChart.setTouchEnabled(false);
        stretchChart.setDescription("Stretch Sensor");
        stretchChart.getXAxis().setEnabled(false);
        stretchChart.getLegend().setEnabled(false);
        stretchChart.getAxisLeft().setEnabled(false);
        // TODO: Set max y-range

        //Initialize RadarChart
        accelerometerChart.setTouchEnabled(false);
        accelerometerChart.setDescription("Accelerometer");
        accelerometerChart.getLegend().setEnabled(false);
        accelerometerChart.getYAxis().setAxisMaxValue(ACCELEROMETER_MAX_VALUE);
        accelerometerChart.getYAxis().setShowOnlyMinMax(true);
        accelerometerChart.getYAxis().setAxisMinValue(0);
        accelerometerChart.getYAxis().setDrawAxisLine(false);


        // Create an update loop
        final Handler handler = new Handler();
        handler.post(new Runnable(){
            @Override
            public void run() {
                if(isRecording){
                    SensorData data = getDataFrame();

                    recentData.add(data);
                    if (recentData.size() > MAX_RECENT_DATA_ITEMS)
                        recentData.remove();

                    try {
                        fileOutputStream.write(data.toCsv().getBytes());
                    }
                    catch (IOException e){
                        // TODO: Handle exception
                    }

                    // Update visuals
                    updateBarChart();
                    updateRadarChart();
                }
                handler.postDelayed(this, REFRESH_RATE);
            }

        });
    }

    protected void updateBarChart() {
        BarChart mBarChart = stretchChart;
        entries = new ArrayList<>();

        int i = 0;
        for (SensorData data : recentData) {
            BarEntry entry = new BarEntry(data.stretch, i);
            entries.add(entry);
            i++;
        }

        entrySet = new BarDataSet(entries, "Stretch Sensor");
        entrySet.setAxisDependency(YAxis.AxisDependency.LEFT);

        ArrayList<String> xVals = new ArrayList<>();
        for(int j = 0; j < entrySet.getEntryCount(); j++) {
            xVals.add((entrySet.getEntryForXIndex(j).toString()));
        }

        barData = new BarData(xVals, entrySet);

        mBarChart.setData(barData);
        mBarChart.invalidate();
    }

    protected void updateRadarChart() {
        RadarChart mRadarChart = accelerometerChart;
        radarEntries = new ArrayList<>();

        SensorData mostRecentData = recentData.peek();

        // Order: Y, X, Z
        radarEntries.add(new Entry(mostRecentData.accelerometerY, 0));
        radarEntries.add(new Entry(mostRecentData.accelerometerX, 1));
        radarEntries.add(new Entry(mostRecentData.accelerometerZ, 2));

        radarDataSet = new RadarDataSet(radarEntries, "Accelerometer");
        radarDataSet.setDrawFilled(true);

        ArrayList<String> xVals = new ArrayList<>();
        xVals.add("Y-Axis");
        xVals.add("X-Axis");
        xVals.add("Z-Axis");

        radarData = new RadarData(xVals, radarDataSet);

        mRadarChart.setData(radarData);
        mRadarChart.invalidate();
    }

    public void onClickToggleRecording(View view) {
        toggleRecording();
    }

    public void toggleRecording() {
        Button toggleButton = (Button) findViewById(R.id.toggleRecording);
        if(isRecording) {
            isRecording = false;
            toggleButton.setText("Resume Recording");
        }
        else {
            isRecording = true;
            toggleButton.setText("Stop/Pause Recording");
        }
    }

    public void onClickDeleteRecording(View view) {
        // Prompt for confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete all unsaved data?")
                .setTitle("Delete?")
                .setCancelable(false)
                .setPositiveButton("Yes, Delete it.", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteRecording();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // Deletes the current recording.
    // Creates a new fileOutputStream and adds the header information.
    public void deleteRecording() {
        if (isRecording) {
            // Stop recording
            toggleRecording();
        }

        try {
            // Close outputStream and create a new one.
            fileOutputStream.close();
            fileOutputStream = new FileOutputStream(tempFile, false);
            fileOutputStream.write(header.getBytes());
        }
        catch (IOException e) {
            simpleAlert("Failed to write to file.\n" +
                        "Recording cannot proceed.");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

        accelerometerChart.clear();
        stretchChart.clear();
        recentData.clear();
    }

    public void onClickSaveRecording(View view) {
        saveRecording();
    }

    public void saveRecording() {
        if (isRecording) {
            toggleRecording();
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String saveFileName = "Edema_Sensor_Recording_" + formatter.format(new Date());
        // First try saving to external storage, so access via computer is possible.
        if (isExternalStorageWritable()) {
            try {
                fileOutputStream.close();
                File saveFile = new File(Environment.getExternalStorageDirectory(), saveFileName);
                copyFile(tempFile, saveFile);
            }
            catch (IOException e) {
                // TODO: Handle
            }
        }
        else { // External storage not available. Internal it is.
            try {
                fileOutputStream.close();
                File saveFile = new File(getFilesDir(), saveFileName);
                copyFile(tempFile, saveFile);
            }
            catch (IOException e) {
                // TODO: Handle
            }
        }

        // Once the file is saved, clear out the temp file for another recording.
        deleteRecording();

        simpleAlert("File saved successfully!");
    }

    public void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
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

    // Returns SensorData object if able. Returns null if failure.
    protected SensorData getDataFrame() {
        // TODO: Remove test code
        // Begin Test code
        return testGetDataFrame();
        // End Test code
        /*
        SensorData data = null;
        String dataString = "";
        InputStream inputStream;
        do{
            // Read data from sensor
            byte[] buffer = new byte[1024];
            try {
                inputStream = bluetoothSocket.getInputStream();
                inputStream.read(buffer, 0, buffer.length);
            }
            catch (IOException e) {
                return null;
            }

            // Convert byteArray to human readable ASCII
            try {
                dataString = new String(buffer, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {};

            // Attempt to parse SensorData object from dataString
            data = stringToSensorData(dataString);
        } while (data == null);
        return data;
        */
    }

    protected SensorData testGetDataFrame() {
        Random rand = new Random();
        Date date = new Date();
        SensorData data = new SensorData(rand.nextInt()%100,
                rand.nextInt()%100,
                rand.nextInt()%100,
                rand.nextInt()%100,
                new java.sql.Timestamp(date.getTime()));
        return data;
    }

    // Returns a SensorData object created from the string if the string is correct.
    // Returns null if string is formatted incorrectly
    protected SensorData stringToSensorData(String string) {
        SensorData data = new SensorData();
        LinkedList<String> tokens = stringArrayToList(string.split("\\D+", -1));

        if (tokens.size() < 4){
            return null;
        }
        if (tokens.getFirst().equals("")) {
            tokens.removeFirst();
        }
        if (tokens.getLast().equals("")) {
            tokens.removeLast();
        }
        if(tokens.size() != 4) {
            return  null;
        }

        try {
            data.accelerometerX = Integer.parseInt(tokens.get(0));
            data.accelerometerY = Integer.parseInt(tokens.get(1));
            data.accelerometerZ = Integer.parseInt(tokens.get(2));
            data.stretch = Integer.parseInt(tokens.get(3));

            Date date = new Date();
            data.captureTime = new java.sql.Timestamp(date.getTime());
        }
        catch (NumberFormatException e){
            //Bad data
            return null;
        }

        // Success
        return data;
    }

    protected LinkedList<String> stringArrayToList(String[] arr) {
        LinkedList<String> list = new LinkedList();
        for (String str : arr){
            list.add(str);
        }
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recording_feed, menu);
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
    protected void onDestroy() {
        super.onDestroy();

        // Delete temp file
        try {
            fileOutputStream.close();
        }
        catch (IOException e) {
            // Hopefully it's already closed
        }
        tempFile.delete();
    }
}
