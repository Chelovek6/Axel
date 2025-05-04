package com.example.axel;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataRecorder{

    private boolean isFFTRecording = false;
    private FileWriter csvWriter;
    private File tempCsvFile;
    private Context context;
    private long recordingStartTime;


    public DataRecorder(Context context) {
        this.context = context;
    }

    public void startRecording(boolean isFFT) {
        this.isFFTRecording = isFFT;
        recordingStartTime = System.currentTimeMillis();
        String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date()) + ".csv";
        tempCsvFile = new File(context.getCacheDir(), fileName);

        try {
            csvWriter = new FileWriter(tempCsvFile);
            csvWriter.append("Device: ").append(android.os.Build.MODEL).append("\n");
            String serial = Build.SERIAL.isEmpty() ? "unknown" : Build.SERIAL;
            csvWriter.append("Serial: ").append(serial).append("\n");
            csvWriter.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            if (isFFTRecording) {
                csvWriter.append("time;x;xfft;y;yfft;z;zfft\n");
            } else {
                csvWriter.append("time;x;y;z;totalAcceleration\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        try {
            if (csvWriter != null) {
                csvWriter.flush();
                csvWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeDataToCsv(float x, float y, float z, float totalAcceleration) {
        if (csvWriter != null && !isFFTRecording) {
            try {
                double timeSinceStart = (System.currentTimeMillis() - recordingStartTime) / 1000.0;
                csvWriter.append(String.format(Locale.getDefault(), "%.3f;%.6f;%.6f;%.6f;%.6f\n",
                        timeSinceStart, x, y, z, totalAcceleration));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeFFTData(float x, float y, float z, float xfft, float yfft, float zfft) {
        if (isFFTRecording && csvWriter != null) {
            try {
                double timeSinceStart = (System.currentTimeMillis() - recordingStartTime) / 1000.0;
                String line = String.format(Locale.getDefault(),
                        "%.3f;%.6f;%.6f;%.6f;%.6f;%.6f;%.6f\n",
                        timeSinceStart, x, xfft, y, yfft, z, zfft
                );
                csvWriter.append(line);
                csvWriter.flush();
            } catch (IOException e) {
                Log.e("DataRecorder", "Error writing FFT data", e);
            }
        }
    }

    public File getTempCsvFile() {
        return tempCsvFile;
    }
}