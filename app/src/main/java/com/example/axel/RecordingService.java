package com.example.axel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class RecordingService extends Service {

    private DataRecorder dataRecorder;
    private static final String CHANNEL_ID = "RecordingServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        dataRecorder = new DataRecorder(this, null); // Не передаём DataListener, так как сервис не обновляет график
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        // Создаём уведомление для Foreground Service
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Запись данных")
                .setContentText("Идёт запись данных с акселерометра")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        // Запускаем сервис в качестве Foreground Service
        startForeground(1, notification);

        dataRecorder.startRecording();

        // Возвращаем путь к файлу через Intent
        Intent resultIntent = new Intent("com.example.axel.RECORDING_STARTED");
        resultIntent.putExtra("filePath", dataRecorder.getTempCsvFile().getAbsolutePath());
        sendBroadcast(resultIntent);

        return START_STICKY;
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
