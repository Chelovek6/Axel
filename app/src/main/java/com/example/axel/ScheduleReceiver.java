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
    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("type");
        int duration = intent.getIntExtra("duration", 1);

        Intent serviceIntent = new Intent(context, RecordingService.class);
        serviceIntent.putExtra("isFFT", "fft".equals(type));
        serviceIntent.putExtra("duration", duration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Явно указываем, что событие обработано
        if (isOrderedBroadcast()) {
            abortBroadcast();
        }
    }
}

