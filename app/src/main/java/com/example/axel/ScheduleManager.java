package com.example.axel;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
            if (!schedule.isActive()) continue;

            if (schedule.getDaysOfWeek().isEmpty()) {
                scheduleSingleRecording(alarmManager, schedule);
            } else {
                String[] days = schedule.getDaysOfWeek().split(",");
                for (String dayStr : days) {
                    try {
                        int day = Integer.parseInt(dayStr.trim());
                        scheduleWeeklyRecording(alarmManager, schedule, day);
                    } catch (NumberFormatException e) {
                        Log.e("ScheduleManager", "Invalid day format: " + dayStr);
                    }
                }
            }
        }
    }
    public void scheduleRecording(Schedule schedule) {
        if (!schedule.isActive()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (schedule.getDaysOfWeek().isEmpty()) {
            scheduleSingleRecording(alarmManager, schedule);
        } else {
            String[] days = schedule.getDaysOfWeek().split(",");
            for (String dayStr : days) {
                try {
                    int day = Integer.parseInt(dayStr.trim());
                    scheduleWeeklyRecording(alarmManager, schedule, day);
                } catch (NumberFormatException e) {
                    Log.e("ScheduleManager", "Invalid day format: " + dayStr);
                }
            }
        }
    }
    private void scheduleWeeklyRecording(AlarmManager alarmManager, Schedule schedule, int day) {
        Calendar nextTrigger = calculateNextTrigger(schedule, day);
        int uniqueId = schedule.getId() * 100 + day;

        Intent intent = new Intent(context, ScheduleReceiver.class)
                .putExtra("type", schedule.getType())
                .putExtra("duration", schedule.getDuration())
                .putExtra("schedule_id", schedule.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTrigger.getTimeInMillis(),
                pendingIntent
        );

        Log.d("ScheduleDebug", "Scheduled: " + nextTrigger.getTime() + " Day: " + day);
    }

    private Calendar calculateNextTrigger(Schedule schedule, int targetDay) {
        Calendar now = Calendar.getInstance();
        Calendar nextTrigger = Calendar.getInstance();

        Calendar scheduleCal = Calendar.getInstance();
        scheduleCal.setTimeInMillis(schedule.getStartTime());
        int hour = scheduleCal.get(Calendar.HOUR_OF_DAY);
        int minute = scheduleCal.get(Calendar.MINUTE);
        nextTrigger.setTimeInMillis(schedule.getStartTime());

        nextTrigger.set(Calendar.HOUR_OF_DAY, hour);
        nextTrigger.set(Calendar.MINUTE, minute);
        nextTrigger.set(Calendar.SECOND, 0);
        nextTrigger.set(Calendar.MILLISECOND, 0);

        // Adjust day
        int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int daysToAdd = (convertToCalendarDay(targetDay) - currentDayOfWeek);

        if (daysToAdd < 0 ||
                (daysToAdd == 0 && nextTrigger.getTimeInMillis() <= System.currentTimeMillis())) {
            daysToAdd += 7;
        }

        nextTrigger.add(Calendar.DAY_OF_YEAR, daysToAdd);
        while (nextTrigger.before(now)) {
            nextTrigger.add(Calendar.DAY_OF_YEAR, 7);
        }
        return nextTrigger;
    }

    public void cancelAlarmsForSchedule(Schedule schedule) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (schedule.getDaysOfWeek().isEmpty()) {
            cancelAlarm(schedule.getId());
        } else {
            String[] days = schedule.getDaysOfWeek().split(",");
            for (String dayStr : days) {
                try {
                    int day = Integer.parseInt(dayStr.trim());
                    int uniqueId = schedule.getId() * 100 + day;
                    cancelAlarm(uniqueId);
                } catch (NumberFormatException e) {
                    Log.e("ScheduleManager", "Invalid day format: " + dayStr);
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

    private long calculateTriggerTime(Schedule schedule) {
        Calendar now = Calendar.getInstance();
        Calendar cal = Calendar.getInstance();

        Calendar scheduleCal = Calendar.getInstance();
        scheduleCal.setTimeInMillis(schedule.getStartTime());

        int hour = scheduleCal.get(Calendar.HOUR_OF_DAY);
        int minute = scheduleCal.get(Calendar.MINUTE);

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (!schedule.getDaysOfWeek().isEmpty()) {
            String[] days = schedule.getDaysOfWeek().split(",");
            int firstDay = Integer.parseInt(days[0].trim());
            int targetDay = convertToCalendarDay(firstDay);

            // Находим ближайший день
            cal.set(Calendar.DAY_OF_WEEK, targetDay);
            if (cal.before(now)) {
                cal.add(Calendar.DAY_OF_YEAR, 7);
            }
        } while (cal.before(now)) {
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
}

