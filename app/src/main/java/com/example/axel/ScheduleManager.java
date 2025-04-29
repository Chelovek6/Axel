package com.example.axel;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.AlarmManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import java.util.Calendar;
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
            if (schedule.getDaysOfWeek().isEmpty()) {
                scheduleSingleRecording(alarmManager, schedule);
            } else {
                String[] days = schedule.getDaysOfWeek().split(",");
                for (String day : days) {
                    scheduleRepeatingRecording(alarmManager, schedule, Integer.parseInt(day.trim()));
                }
            }
        }
    }

    private void scheduleSingleRecording(AlarmManager alarmManager, Schedule schedule) {
        Intent intent = new Intent(context, ScheduleReceiver.class);
        intent.putExtra("type", schedule.getType());
        intent.putExtra("duration", schedule.getDuration());
        intent.putExtra("schedule_id", schedule.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = calculateTriggerTime(schedule);

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

    private void scheduleRepeatingRecording(AlarmManager alarmManager, Schedule schedule, int day) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(schedule.getStartTime());
        cal.set(Calendar.DAY_OF_WEEK, convertToCalendarDay(day));

        // Если время уже прошло, добавляем неделю
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, ScheduleReceiver.class);
        intent.putExtra("type", schedule.getType());
        intent.putExtra("duration", schedule.getDuration());
        intent.putExtra("schedule_id", schedule.getId() * 10 + day); // Уникальный ID

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.getId() * 10 + day,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Установка повторяющегося будильника каждую неделю
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
        );
    }

    private long calculateTriggerTime(Schedule schedule) {
        Calendar now = Calendar.getInstance();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(schedule.getStartTime());

        // Устанавливаем время из расписания
        cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (!schedule.getDaysOfWeek().isEmpty()) {
            String[] days = schedule.getDaysOfWeek().split(",");
            int firstDay = Integer.parseInt(days[0].trim());

            // Конвертируем в Calendar.DAY_OF_WEEK
            int targetDay = convertToCalendarDay(firstDay);

            // Находим ближайший день
            cal.set(Calendar.DAY_OF_WEEK, targetDay);
            if (cal.before(now)) {
                cal.add(Calendar.DAY_OF_YEAR, 7);
            }
        } else if (cal.before(now)) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return cal.getTimeInMillis();
    }
    private int convertToCalendarDay(int day) {
        switch(day) {
            case 1: return Calendar.MONDAY;
            case 2: return Calendar.TUESDAY;
            case 3: return Calendar.WEDNESDAY;
            case 4: return Calendar.THURSDAY;
            case 5: return Calendar.FRIDAY;
            case 6: return Calendar.SATURDAY;
            case 7: return Calendar.SUNDAY;
            default: throw new IllegalArgumentException("Invalid day: " + day);
        }
    }

    public void cancelAllAlarms() {
        List<Schedule> schedules = dbHelper.getAllSchedules();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (Schedule schedule : schedules) {
            Intent intent = new Intent(context, ScheduleReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    schedule.getId(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }


    public void cancelAlarm(int scheduleId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduleReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
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

