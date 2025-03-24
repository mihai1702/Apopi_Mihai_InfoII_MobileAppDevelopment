package com.example.safealertapp;

import static com.example.safealertapp.MainActivity.getFavoriteContacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class BatteryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        System.out.println("battery level"+level);
        if (level <= 10) {
            Toast.makeText(context, "Baterie scăzută! Trimitere mesaj...", Toast.LENGTH_SHORT).show();

            List<emergContact> emergContacts = MainActivity.getFavoriteContacts(context);
            System.out.println(emergContacts);
        }
    }
}
