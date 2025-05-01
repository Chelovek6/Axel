package com.example.axel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
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
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {

    private DataRecorder dataRecorder;
    private static final String CHANNEL_ID = "RecordingServiceChannel";
    private File tempCsvFile;
    private Intent intent;
    private Handler handler = new Handler();
    private boolean isFFT;
    private int duration;
    @Override
    public void onCreate() {
        super.onCreate();
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

        return START_STICKY;
    }

    private void stopRecording() {
        dataRecorder.stopRecording();
        saveToDatabase();
        stopForeground(true);
        stopSelf();
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
