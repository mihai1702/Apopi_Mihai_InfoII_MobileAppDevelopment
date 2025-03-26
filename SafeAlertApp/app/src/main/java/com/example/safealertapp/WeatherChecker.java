package com.example.safealertapp;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

///https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid={API key}
public class WeatherChecker {
    public interface WeatherCallback {
        void onDangerDetected(String condition, double temp);
        void onSafe();
    }

    public static void checkWeather(Context context, double lat, double lon, WeatherCallback callback) {
        String apiKey = "26dbcc0a400d5d81bdb235049169c05e";
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                "&lon=" + lon + "&appid=" + apiKey + "&units=metric";

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray weatherArray = response.getJSONArray("weather");
                        JSONObject weatherObject = weatherArray.getJSONObject(0);
                        String condition = weatherObject.getString("main").toLowerCase();

                        double temp = response.getJSONObject("main").getDouble("temp");

                        if (condition.contains("storm") || condition.contains("thunderstorm") ||
                                condition.contains("snow") || condition.contains("extreme") ||
                                temp < -10 || temp > 38) {
                            callback.onDangerDetected(condition, temp);

                        } else {
                            callback.onSafe();
                        }
                        ///se decomenteaza pentru testing
                        callback.onDangerDetected("simulated storm", 42);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("WEATHER", "Eroare la parsarea JSON: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e("WEATHER", "Eroare API meteo: " + error.getMessage());
                });

        queue.add(request);

    }
}
