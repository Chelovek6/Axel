package com.example.axel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

public class ScheduleReceiver extends BroadcastReceiver {
    private static final String TAG = "ScheduleReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered! Intent: " + intent.getExtras());

        boolean isFFT = intent.getStringExtra("type").equals("fft");
        int duration = intent.getIntExtra("duration", 1);
        int scheduleId = intent.getIntExtra("schedule_id", 0);

        Intent serviceIntent = new Intent(context, RecordingService.class);

        serviceIntent.putExtra("isFFT", isFFT);
        serviceIntent.putExtra("duration", duration);
        serviceIntent.putExtra("schedule_id", scheduleId);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RecordingService::WakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L); // 10 минут

        // Планируем уведомление за 5 минут
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        wakeLock.release();
    }
}

