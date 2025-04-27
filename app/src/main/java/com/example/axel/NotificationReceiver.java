package com.example.axel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ScheduleManager manager = new ScheduleManager(context);
        manager.showNotification("Через менее чем 5 минут начнется запись, положите телефон в спокойное место");
    }
}
