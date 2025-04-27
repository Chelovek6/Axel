package com.example.axel;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.AlarmManager;
import androidx.core.app.NotificationCompat;

import java.util.List;

public class ScheduleManager {
    private Context context;
    private DatabaseHelper dbHelper;

    public ScheduleManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    public void scheduleAllRecordings() {
        List<Schedule> mainSchedules = dbHelper.getActiveSchedules("main");
        List<Schedule> fftSchedules = dbHelper.getActiveSchedules("fft");

        scheduleRecordings(mainSchedules);
        scheduleRecordings(fftSchedules);
    }

    private void scheduleRecordings(List<Schedule> schedules) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (Schedule schedule : schedules) {
            Intent intent = new Intent(context, ScheduleReceiver.class);
            intent.putExtra("trigger_time", schedule.getStartTime());
            intent.putExtra("type", schedule.getType());
            intent.putExtra("duration", schedule.getDuration());
            intent.putExtra("schedule_id", schedule.getId());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    schedule.getId(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerTime = schedule.getStartTime() - 5 * 60 * 1000;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }
        }
    }

    public void showNotification(String message) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Создаем канал для Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "schedule_channel",
                    "Расписание записей",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Уведомления о расписании записей");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "schedule_channel")
                .setContentTitle("Запись по расписанию")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    public void scheduleRecording(long triggerAt, int scheduleId, boolean isFFT, int duration) {
        Intent recordingIntent = new Intent(context, ScheduleReceiver.class);
        recordingIntent.putExtra("type", isFFT ? "fft" : "main");
        recordingIntent.putExtra("duration", duration);
        recordingIntent.putExtra("schedule_id", scheduleId);
        recordingIntent.putExtra("trigger_time", triggerAt);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId * 2 + 1,
                recordingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
            );
        }
    }

    public void scheduleNotification(long triggerAt, int scheduleId) {
        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        notificationIntent.putExtra("schedule_id", scheduleId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId * 2,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
            );
        }
    }

    public void startScheduledRecording(boolean isFFT, int duration) {
        Intent serviceIntent = new Intent(context, RecordingService.class);
        serviceIntent.putExtra("isFFT", isFFT);
        serviceIntent.putExtra("duration", duration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}

