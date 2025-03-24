package com.example.safealertapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class powerButtonReceiver extends BroadcastReceiver {
    private static int powerPressCount = 0;
    private static long lastPressTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()) || Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            long now = System.currentTimeMillis();
            if (now - lastPressTime <= 3000) {
                powerPressCount++;
            } else {
                powerPressCount = 1;
            }
            lastPressTime = now;

            if (powerPressCount >= 3) {
                powerPressCount = 0;
                Intent sosIntent = new Intent(context, MainActivity.class);
                sosIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sosIntent.putExtra("sos_triggered", true);
                context.startActivity(sosIntent);
            }
        }
    }
}
