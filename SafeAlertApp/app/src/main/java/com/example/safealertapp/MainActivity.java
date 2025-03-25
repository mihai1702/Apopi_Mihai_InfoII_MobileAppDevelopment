package com.example.safealertapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 1;
    private static final int LOCATION_PERMISSION_CODE = 2;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_CALL_PERMISSION = 1;
    private static final String EMERGENCY_NUMBER = "0787596450";
    private Handler handler = new Handler();
    private Runnable callRunnable;
    private Button stopEmergButton;
    private TextView EmergencyTextView;
    private static final int VOICE_REQUEST_CODE = 123;
    List<emergContact>emergContacts;
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private BatteryReceiver batteryReceiver;


    private Handler inactivityHandler = new Handler();
    private Runnable inactivityRunnable;
    private long inactivityDelay = 30 * 1000;///30 secunde
    private final float MOVEMENT_THRESHOLD = 2.0f;///20.0f pentru testing



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        permissions();

        emergContacts=getFavoriteContacts(this);
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button EmergContact = findViewById(R.id.EmergButton);

        stopEmergButton=findViewById(R.id.stopEmergButton);
        stopEmergButton.setVisibility(View.GONE);

        EmergencyTextView=findViewById(R.id.EmergencyTextView);
        EmergencyTextView.setVisibility(View.GONE);

        stopEmergButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopEmergencyCall();
            }
        });
        ///SHORTCUT POWERBUTTON
        if (getIntent().getBooleanExtra("sos_triggered", false)) {
            for (emergContact contact : emergContacts) {
                sendLocationAndMessage(contact);

            }
            startEmergencyCountDown();
        }
        ///BUTON EMERGENCY
        EmergContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (emergContact contact : emergContacts) {
                    sendLocationAndMessage(contact);

                }
                startEmergencyCountDown();
            }
        });

        ///VOICE RECOGN
        findViewById(R.id.VoiceRecognButton).setOnClickListener(v -> startVoiceRecognition());

        ///Inactivity
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        ///weather notifications
        checkWeatherAndNotify();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("weather_channel", "Alerte Meteo",
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        ///Baterry channel
        createNotificationChannel();

    }
    ///EMERGENCY CALL AFTER 10 SECONDS
    public void startEmergencyCountDown(){
        EmergencyTextView.setVisibility(View.VISIBLE);
        stopEmergButton.setVisibility(View.VISIBLE);
        callRunnable=this::makeEmergencyCall;
        handler.postDelayed(callRunnable, 10000);
    }
    public void stopEmergencyCall(){
        handler.removeCallbacks(callRunnable);
        Toast.makeText(this, "Apelul de urgență a fost anulat!", Toast.LENGTH_SHORT).show();

        stopEmergButton.setVisibility(View.GONE);
        EmergencyTextView.setVisibility(View.GONE);

    }
    public void makeEmergencyCall(){
        stopEmergButton.setVisibility(View.GONE);
        EmergencyTextView.setVisibility(View.GONE);
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + EMERGENCY_NUMBER));
        startActivity(intent);

    }
    ///PERMISIUNI
    public void permissions(){
        ///permisiuni SMS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
        ///perimisiuni locatie
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
        ///permisiuni contacte
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 1);
        }
        ///permisiuni apel
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
        }
        ///permisiuni notificari
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }
    ///Extragere contacte favorite
    public static List<emergContact> getFavoriteContacts(Context context) {
        List<emergContact> Contacts = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_CONTACTS}, 1);
            return Contacts;
        }
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = ContactsContract.Contacts.CONTENT_URI;

        String selection = ContactsContract.Contacts.STARRED + "=?";
        String[] selectionArgs = new String[]{"1"};

        Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, null);

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                Cursor phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                );

                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    @SuppressLint("Range") String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    Contacts.add(new emergContact(name, phoneNumber));
                    phoneCursor.close();
                }
            }
            cursor.close();
        }

        return Contacts;
    }
    ///Metoda trimitere mesaj la apasarea butonului de emergency
    private void sendLocationAndMessage(emergContact contact){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String locationLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;
                    System.out.println(locationLink);
                    contact.sendEmergMessage(MainActivity.this, locationLink);
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission accepted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission needed", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisiune locație acordată!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permisiunea pentru locație este necesară!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    ///VOICE RECOGNITION
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null) {
                for (String command : results) {
                    if (command.toLowerCase().contains("help")) {
                        emergContacts=getFavoriteContacts(this);
                        for (emergContact contact : emergContacts) {
                            sendLocationAndMessage(contact);

                        }
                        startEmergencyCountDown();
                        break;
                    }
                }
            }
        }
    }
    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Spune ceva...");
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Recunoaștere vocală indisponibilă", Toast.LENGTH_SHORT).show();
        }
    }
    ///TESTARE INACTIVITATE
    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double acceleration = Math.sqrt(x * x + y * y + z * z);

            if (Math.abs(acceleration - SensorManager.GRAVITY_EARTH) > MOVEMENT_THRESHOLD) {
                resetInactivityTimer();
                System.out.println("salut");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
    private void startInactivityTimer() {
        inactivityRunnable = () -> {
            sendInactivityAlert();
        };
        inactivityHandler.postDelayed(inactivityRunnable, inactivityDelay);
    }

    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable);
        inactivityHandler.postDelayed(inactivityRunnable, inactivityDelay);
    }
    private void sendInactivityAlert() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                String message = "Nu am mai fost activ de " + (inactivityDelay / 60000) + " minute. Locația mea curentă este: https://maps.google.com/?q=" +
                        location.getLatitude() + "," + location.getLongitude();

                emergContacts = getFavoriteContacts(this);
                for (emergContact contact : emergContacts) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(contact.getPhoneNumber(), null, message, null, null);
                    Toast.makeText(this, "Mesaj de inactivitate trimis către: " + contact.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null)
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (gyroscope != null)
            sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        startInactivityTimer();

        ///battery receiver
        batteryReceiver = new BatteryReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
        inactivityHandler.removeCallbacks(inactivityRunnable);

        unregisterReceiver(batteryReceiver);

    }
    ///Check weather
    private void checkWeatherAndNotify() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                WeatherChecker.checkWeather(this, location.getLatitude(), location.getLongitude(),
                        new WeatherChecker.WeatherCallback() {
                            @Override
                            public void onDangerDetected(String condition, double temp) {
                                showNotification("Atenție!", "Vreme periculoasă: " + condition + " (" + temp + "°C)");

                                // (opțional) trimite SMS
                                SmsManager sms = SmsManager.getDefault();
                                sms.sendTextMessage("07xxxxxxxx", null,
                                        "Alertă meteo: " + condition + " (" + temp + "°C)", null, null);
                            }

                            @Override
                            public void onSafe() {
                                Log.d("WEATHER", "Vreme ok");
                            }
                        });
            }
        });
    }
    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "weather_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1001, builder.build());
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "battery_channel",
                    "Alerte Baterie",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Canal pentru notificări de baterie scăzută");

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String locationLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;
                    System.out.println(locationLink);
                    for (emergContact contact : emergContacts) {
                        String message="Atentie! Bateria mea este sub 5%! Locatia mea este: "+locationLink;
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(contact.getPhoneNumber(), null, message, null, null);
                    }
                }
                });

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}