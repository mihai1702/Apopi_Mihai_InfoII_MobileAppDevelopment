package com.example.safealertapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event.hasError()) {
            Log.e("GEOFENCE", "Eroare la geofencing!");
            return;
        }

        if (event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d("GEOFENCE", "Ai ieșit din zona sigură!");
            Toast.makeText(context, "iabadabadoo", Toast.LENGTH_SHORT).show();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "geo_channel")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Alertă Geofencing")
                    .setContentText("Ai ieșit din zona sigură!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            try {
                NotificationManagerCompat.from(context).notify(3001, builder.build());
            } catch (SecurityException e) {
                Log.e("NOTIF", "Permisiunea pentru notificări nu e acordată: " + e.getMessage());
            }
        }
    }
}
