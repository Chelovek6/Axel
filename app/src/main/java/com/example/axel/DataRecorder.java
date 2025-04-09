package com.example.axel;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataRecorder implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private FileWriter csvWriter;
    private File tempCsvFile;
    private long recordingStartTime;
    private Context context;
    private DataListener dataListener;
    private boolean isFFTEnabled = false;

    public interface DataListener {
        void onDataUpdated(float x, float y, float z, float totalAcceleration);
    }

    public DataRecorder(Context context, DataListener dataListener) {
        this.context = context;
        this.dataListener = dataListener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }


    public void startRecording() {
        recordingStartTime = System.currentTimeMillis();
        String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date()) + ".csv";
        tempCsvFile = new File(context.getCacheDir(), fileName);

        try {
            csvWriter = new FileWriter(tempCsvFile);
            csvWriter.append("time;x;y;z;totalAcceleration\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stopRecording() {
        sensorManager.unregisterListener(this);

        try {
            if (csvWriter != null) {
                csvWriter.flush();
                csvWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void setFFTEnabled(boolean enabled) {
        this.isFFTEnabled = enabled;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0] / 9.8f;
            float y = event.values[1] / 9.8f;
            float z = event.values[2] / 9.8f;

            float totalAcceleration = (float) Math.sqrt(x * x + y * y + z * z);

            writeDataToCsv(x, y, z, totalAcceleration);

            if (dataListener != null) {
                dataListener.onDataUpdated(x, y, z, totalAcceleration);
            }
            if (!isFFTEnabled) { // Записываем только если фильтр выключен
                writeDataToCsv(x, y, z, totalAcceleration);
            }
        }
    }

    private void writeDataToCsv(float x, float y, float z, float totalAcceleration) {
        if (csvWriter != null) {
            try {
                double timeSinceStart = (System.currentTimeMillis() - recordingStartTime) / 1000.0;
                csvWriter.append(String.format(Locale.getDefault(), "%.3f;%.6f;%.6f;%.6f;%.6f\n",
                        timeSinceStart, x, y, z, totalAcceleration));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public File getTempCsvFile() {
        return tempCsvFile;
    }
}