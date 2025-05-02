package com.example.axel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordingService extends Service implements SensorEventListener {

    private DataRecorder dataRecorder;
    private static final String CHANNEL_ID = "RecordingServiceChannel";
    private File tempCsvFile;
    private Intent intent;
    private Handler handler = new Handler();
    private boolean isFFT;
    private int duration;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private FFTProcessor fftProcessor;
    private static final int FFT_SIZE = 1024;
    private List<Float> bufferX = new ArrayList<>();
    private List<Float> bufferY = new ArrayList<>();
    private List<Float> bufferZ = new ArrayList<>();
    private float samplingRate = 100.0f;
    private long previousTimestamp = 0;
    private final ArrayList<Float> samplingRates = new ArrayList<>();
    private static final int AVERAGE_BUFFER_SIZE = 3;
    private final List<Float> averagedBufferX = new ArrayList<>();
    private final List<Float> averagedBufferY = new ArrayList<>();
    private final List<Float> averagedBufferZ = new ArrayList<>();
    private float[] fftX;
    private float[] fftY;
    private float[] fftZ;

    @Override
    public void onCreate() {
        super.onCreate();
        fftProcessor = new FFTProcessor(FFT_SIZE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        dataRecorder = new DataRecorder(this, null); // Не передаём DataListener, так как сервис не обновляет график
        createNotificationChannel();
    }

    private void saveToDatabase() {
        try {
            File tempFile = dataRecorder.getTempCsvFile();
            if (tempFile == null || !tempFile.exists()) {
                Log.e("RecordingService", "Temp file not found");
                return;
            }

            // Создаем постоянный файл во внешнем хранилище
            File recordsDir = new File(getExternalFilesDir(null), "records");
            if (!recordsDir.exists()) recordsDir.mkdirs();

            String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                    .format(new Date()) + (isFFT ? "_fft" : "") + ".csv";

            File permanentFile = new File(recordsDir, fileName);

            // Копируем временный файл в постоянное хранилище
            try (InputStream in = new FileInputStream(tempFile);
                 OutputStream out = new FileOutputStream(permanentFile)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }

            // Добавляем запись в БД
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            dbHelper.addRecord(
                    fileName,
                    permanentFile.getAbsolutePath(),
                    isFFT
            );

            // Удаляем временный файл после успешного копирования
            tempFile.delete();

        } catch (IOException e) {
            Log.e("RecordingService", "File save error", e);
            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }


        this.isFFT = intent.getBooleanExtra("isFFT", false);
        this.duration = intent.getIntExtra("duration", 1);
        int scheduleId = intent.getIntExtra("schedule_id", 0);
        boolean isFFT = intent != null && intent.getBooleanExtra("isFFT", false);
        int duration = intent.getIntExtra("duration", 1);

        new Handler().postDelayed(() -> {
            stopRecording();
            saveToDatabase();
        }, duration * 60 * 1000);

        // Создаём уведомление для Foreground Service
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Запись данных")
                .setContentText("Идёт запись данных с акселерометра")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        // Запускаем сервис в качестве Foreground Service
        startForeground(1, notification);


        dataRecorder.startRecording(isFFT);

        // Возвращаем путь к файлу через Intent
        Intent resultIntent = new Intent("com.example.axel.RECORDING_STARTED");
        resultIntent.putExtra("filePath", dataRecorder.getTempCsvFile().getAbsolutePath());
        sendBroadcast(resultIntent);

        if (isFFT) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }

        return START_STICKY;
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isFFT) {
            long currentTimestamp = System.nanoTime();
            if (previousTimestamp != 0) {
                float intervalNs = currentTimestamp - previousTimestamp;
                float frequencyHz = 1_000_000_000f / intervalNs;

                samplingRates.add(frequencyHz);
                if (samplingRates.size() > 10) {
                    samplingRates.remove(0);
                }

                float sum = 0;
                for (float f : samplingRates) sum += f;
                samplingRate = sum / samplingRates.size();
            }
            previousTimestamp = currentTimestamp;
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            bufferX.add(x);
            bufferY.add(y);
            bufferZ.add(z);

            if (bufferX.size() >= FFT_SIZE) {
                processFFT();
                bufferX.clear();
                bufferY.clear();
                bufferZ.clear();
            }
            updateAverageBuffers(x, y, z);
            updateRawChartsWithAveragedData();

        }
    }
    private void updateAverageBuffers(float x, float y, float z) {
        averagedBufferX.add(x);
        averagedBufferY.add(y);
        averagedBufferZ.add(z);

        if (averagedBufferX.size() > AVERAGE_BUFFER_SIZE) {
            averagedBufferX.remove(0);
            averagedBufferY.remove(0);
            averagedBufferZ.remove(0);
        }
    }
    private void updateRawChartsWithAveragedData() {
        if (averagedBufferX.size() >= AVERAGE_BUFFER_SIZE
                && fftX != null
                && fftY != null
                && fftZ != null) {
            float avgX = calculateAverage(averagedBufferX) / 9.8f;
            float avgY = calculateAverage(averagedBufferY) / 9.8f;
            float avgZ = calculateAverage(averagedBufferZ) / 9.8f;

            // Записываем усредненные данные
            dataRecorder.writeFFTData(
                    avgX,
                    avgY,
                    avgZ,
                    getMaxAmplitude(fftX),
                    getMaxAmplitude(fftY),
                    getMaxAmplitude(fftZ)
            );
        }
    }

    private void processFFT() {
        this.fftX = fftProcessor.computeFFT(toFloatArray(bufferX), samplingRate);
        this.fftY = fftProcessor.computeFFT(toFloatArray(bufferY), samplingRate);
        this.fftZ = fftProcessor.computeFFT(toFloatArray(bufferZ), samplingRate);

        float avgX = calculateAverage(bufferX) / 9.8f;
        float avgY = calculateAverage(bufferY) / 9.8f;
        float avgZ = calculateAverage(bufferZ) / 9.8f;

        float xfft = getMaxAmplitude(fftX);
        float yfft = getMaxAmplitude(fftY);
        float zfft = getMaxAmplitude(fftZ);

        dataRecorder.writeFFTData(avgX, avgY, avgZ, xfft, yfft, zfft);
    }
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    // Добавьте вспомогательные методы calculateAverage, getMaxAmplitude и toFloatArray
    private float calculateAverage(List<Float> list) {
        float sum = 0;
        for (float num : list) {
            sum += num;
        }
        return sum / list.size();
    }
    private float getMaxAmplitude(float[] data) {
        float max = 0;
        for (float val : data) if (val > max) max = val;
        return max;
    }

    private void stopRecording() {
        dataRecorder.stopRecording();
        saveToDatabase();
        stopForeground(true);
        stopSelf();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Можно оставить пустым, но метод должен присутствовать
    }
    @Override
    public void onDestroy() {
        dataRecorder.stopRecording();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recording Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


}
