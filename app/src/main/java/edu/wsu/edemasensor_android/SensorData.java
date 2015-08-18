package edu.wsu.edemasensor_android;

import java.sql.Timestamp;

/**
 * Created by Shane on 8/12/2015.
 */
public class SensorData {
    public int accelerometerX;
    public int accelerometerY;
    public int accelerometerZ;
    public int stretch;
    public Timestamp captureTime;

    SensorData(){

    }
    SensorData(int accX, int accY, int accZ, int stretch, Timestamp capture_time){
        this.accelerometerX = accX;
        this.accelerometerY = accY;
        this.accelerometerZ = accZ;
        this.stretch = stretch;
        this.captureTime = capture_time;
    }

    public String toCsv(){
        String csv = Integer.toString(accelerometerX) + ","
                + Integer.toString(accelerometerY) + ","
                + Integer.toString(accelerometerZ) + ","
                + Integer.toString(stretch) + ","
                + captureTime.toString();
        return csv;
    }

    public static String createCsvHeader() {
        return "Accelerometer X-axis, Accelerometer Y-axis, Accelerometer Z-axis,"
                + "Stretch, Capture Time";
    }
}
