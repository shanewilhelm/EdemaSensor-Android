package edu.wsu.edemasensor_android;

import java.sql.Timestamp;

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
        return Integer.toString(accelerometerX) + ","
                + Integer.toString(accelerometerY) + ","
                + Integer.toString(accelerometerZ) + ","
                + Integer.toString(stretch) + ","
                + captureTime.toString() + "\n";
    }

    public static String createCsvHeader() {
        return "Accelerometer X-axis, Accelerometer Y-axis, Accelerometer Z-axis,"
                + "Stretch, Capture Time";
    }
}
