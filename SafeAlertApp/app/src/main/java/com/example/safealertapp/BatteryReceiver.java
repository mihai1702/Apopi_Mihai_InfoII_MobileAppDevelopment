package com.example.safealertapp;

import static com.example.safealertapp.MainActivity.getFavoriteContacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public class BatteryReceiver extends BroadcastReceiver {
    int ok=0;
    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        int batteryPct = (int) ((level / (float) scale) * 100);

        if (batteryPct <= 10 && batteryPct>5 && ok==0) {
            ok=1;
            Log.d("BATTERY", "Baterie sub 10%");
            List<emergContact> emergContacts;
            emergContacts = getFavoriteContacts(context);
            for (emergContact contact : emergContacts) {
                String message = "Atenție! Bateria mea este sub 10%";
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(contact.getPhoneNumber(), null, message, null, null);
            }
        }
        else if(batteryPct<=5 && ok==1){
            ok=2;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "battery_channel")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Baterie scăzută")
                    .setContentText("Activează modul de economisire!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
            try {
                NotificationManagerCompat.from(context).notify(2025, builder.build());
            } catch (SecurityException e) {
                Log.e("BATTERY", "Permisiunea pentru notificări nu e acordată: " + e.getMessage());
            }

        }
        else if(batteryPct > 10)
            ok=0;
    }
}