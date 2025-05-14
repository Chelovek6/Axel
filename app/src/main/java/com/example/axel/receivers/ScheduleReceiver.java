package com.example.axel.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.axel.services.RecordingService;

public class ScheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("type");
        int duration = intent.getIntExtra("duration", 1);
        int scheduleId = intent.getIntExtra("schedule_id", 0);

        // Запускаем соответствующий тип записи
        Intent serviceIntent = new Intent(context, RecordingService.class);
        serviceIntent.putExtra("isFFT", "fft".equals(type));
        serviceIntent.putExtra("duration", duration);
        serviceIntent.putExtra("schedule_id", scheduleId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        if (isOrderedBroadcast()) {
            abortBroadcast();
        }
    }
}

