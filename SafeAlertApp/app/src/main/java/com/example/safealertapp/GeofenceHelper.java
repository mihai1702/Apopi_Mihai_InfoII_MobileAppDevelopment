package com.example.safealertapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;

public class GeofenceHelper {

    private Context context;

    public GeofenceHelper(Context context) {
        this.context = context;
    }

    public GeofencingRequest getGeofencingRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofence(geofence)
                .build();
    }

    public Geofence getGeofence(String id, double lat, double lon, float radius) {
        return new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(lat, lon, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    public PendingIntent getPendingIntent() {
        Intent intent = new Intent(context, GeofenceReceiver.class);
        return PendingIntent.getBroadcast(context, 2607, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }
}
