package com.example.beachplease;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.pwittchen.weathericonview.WeatherIconView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.gms.tasks.Task;
import com.github.pwittchen.weathericonview.WeatherIconView;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Calendar;

import org.json.JSONObject;
import org.json.JSONArray;
import org.w3c.dom.Text;

interface OnBeachIDFind{
    void onBeachIDFound(String beachID);
}

interface OnBeachHoursFind {
    void onBeachHoursFound(String[] beach_hours);
}

interface OnWaveHeightFind {
    void onWaveHeightFound(double wave_height);
}

interface OnCloudCoverFind {
    void onCloudCoverFound(double coverage);
}

interface OnCurrentWeatherFind {
    void onCurrentWeatherFound(String weather);
}

interface OnDayForecastFind {
    void onDayForecastFound(double[] daily_temps, int[] weather_codes);
}

public class Beach_Info extends AppCompatActivity {
    String global_beach_name;
    String google_api_key = "AIzaSyAKKuEVLnbLp9TVkzTNdzDStZzxhp_DOBo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_beach_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // check if user is logged in
        SharedPreferences local_storage = getSharedPreferences("user_data", MODE_PRIVATE);
        String current_user_id = local_storage.getString("user_id", "-1");
        if(!current_user_id.equals("-1")){
            ImageView post_review_button = findViewById(R.id.post_review_button);
            post_review_button.setVisibility(View.VISIBLE);
        }

        // listeners for button clicks
        ImageView home_button = findViewById(R.id.home_button);
        home_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Beach_Info.this, MainActivity.class);
                startActivity(intent);
            }
        });

        ImageView profile_button = findViewById(R.id.profile_button);
        profile_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("str", "user prifle clicked from beach info");
                Intent intent = new Intent(Beach_Info.this, User_Profile.class);
                startActivity(intent);
            }
        });

        ImageView review_button = findViewById(R.id.post_review_button);
        // Add a review
        review_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Beach_Info.this, AddReviewActivity.class);
                intent.putExtra("beach_name", global_beach_name);
                intent.putExtra("User_ID", current_user_id);
                startActivity(intent);
            }
        });

        // update beach name
        Intent intent = getIntent();
        String beach_name = intent.getStringExtra("beach_name");
        String beach_coords = intent.getStringExtra("beach_coords");
        TextView beach_name_tv = findViewById(R.id.beach_name);
        beach_name_tv.setText(beach_name.toUpperCase());
        global_beach_name = beach_name;

        // get operating hours of beach
        fetchBeachHoursRoutine();

        // get weather data of beach
        fetchWeatherRoutine(beach_coords);

        // TODO: run database code to view reviews

    }

    private void fetchWeatherRoutine(String beach_coords){
        // get beach wave height
        fetchBeachWaveHeight(beach_coords, new OnWaveHeightFind() {
            @Override
            public void onWaveHeightFound(double wave_height) {
                runOnUiThread(() -> {
                    TextView wave_height_tv = findViewById(R.id.wave_height);
                    wave_height_tv.setText(wave_height + "m");
                });
            }
        });

        // get beach weather
        fetchBeachWeather(beach_coords, new OnCurrentWeatherFind() {
            @Override
            public void onCurrentWeatherFound(String weather) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);

                runOnUiThread(() -> {
                    WeatherIconView weatherIconView = (WeatherIconView) findViewById(R.id.current_weather);
                    if(weather.equals("clear")){
                        if (hour >= 6 && hour < 18) {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_sunny));
                        } else {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_clear));
                        }
                    } else if(weather.equals("partly_cloudy")){
                        if (hour >= 6 && hour < 18) {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_cloudy));
                        } else {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_cloudy));
                        }
                    } else if(weather.equals("fog")){
                        if (hour >= 6 && hour < 18) {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_fog));
                        } else {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_fog));
                        }
                    } else if(weather.equals("rain")){
                        if (hour >= 6 && hour < 18) {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_rain));
                        } else {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_rain));
                        }
                    } else if(weather.equals("thunderstorm")){
                        if (hour >= 6 && hour < 18) {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_thunderstorm));
                        } else {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_thunderstorm));
                        }
                    } else if(weather.equals("snow")){
                        if (hour >= 6 && hour < 18) {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_snow));
                        } else {
                            weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_snow));
                        }
                    }
                    weatherIconView.setIconSize(36);
                    weatherIconView.setIconColor(Color.BLACK);
                });
            }
        });

        // get cloud cover
        fetchBeachCloudCover(beach_coords, new OnCloudCoverFind() {
            @Override
            public void onCloudCoverFound(double coverage) {
                runOnUiThread(() -> {
                    TextView cloud_cover_tv = findViewById(R.id.cloud_cover);
                    cloud_cover_tv.setText(coverage + "%");
                });
            }
        });

        // get forecast
        fetchHourlyForecast(beach_coords, new OnDayForecastFind() {
            @Override
            public void onDayForecastFound(double[] daily_temps, int[] weather_codes) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);

                String[] hours = {"12AM", "1AM", "2AM", "3AM", "4AM", "5AM", "6AM", "7AM", "8AM", "9AM", "10AM",
                        "11AM", "12PM", "1PM", "2PM", "3PM", "4PM", "5PM", "6PM", "7PM", "8PM", "9PM", "10PM",
                        "11PM", "12AM", "1AM", "2AM", "3AM", "4AM", "5AM", "6AM", "7AM", "8AM", "9AM", "10AM",
                        "11AM", "12PM", "1PM", "2PM", "3PM", "4PM", "5PM", "6PM", "7PM", "8PM", "9PM", "10PM", "11PM"};

                runOnUiThread(() -> {
                    Log.i("string", ""+(hour+5));
                    Log.i("string", hours[hour+5]);
                    Log.i("string", weather_codes[hour+5]+"");
                    Log.i("string", daily_temps[hour+5]+"");
                    for(int i = hour; i < hour + 24; i++){
                        double hour_i_temp = daily_temps[i];
                        int hour_i_code = weather_codes[i];
                        String hour_i_string = hours[i];

                        String weather_desc;

                        if(hour_i_code == 0){
                            weather_desc = "clear";
                        } else if (hour_i_code <= 3){
                            weather_desc = "partly_cloudy";
                        } else if(hour_i_code == 45 || hour_i_code == 48){
                            weather_desc = "fog";
                        } else if(hour_i_code >= 51 && hour_i_code <= 67 || hour_i_code >= 80 && hour_i_code <= 82) {
                            weather_desc = "rain";
                        } else if(hour_i_code >= 95 && hour_i_code <= 99) {
                            weather_desc = "thunderstorm";
                        } else {
                            weather_desc = "snow";
                        }

                        WeatherIconView weatherIconView = new WeatherIconView(Beach_Info.this);
                        if(weather_desc.equals("clear")){
                            if (i >= 6 && i < 18 || i >= 30 && i <= 42) {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_sunny));
                            } else {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_clear));
                            }
                        } else if(weather_desc.equals("partly_cloudy")){
                            if (i >= 6 && i < 18 || i >= 30 && i <= 42) {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_cloudy));
                            } else {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_alt_cloudy));
                            }
                        } else if(weather_desc.equals("fog")){
                            if (i >= 6 && i < 18 || i >= 30 && i <= 42) {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_fog));
                            } else {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_fog));
                            }
                        } else if(weather_desc.equals("rain")){
                            if (i >= 6 && i < 18 || i >= 30 && i <= 42) {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_rain));
                            } else {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_alt_rain));
                            }
                        } else if(weather_desc.equals("thunderstorm")){
                            if (i >= 6 && i < 18 || i >= 30 && i <= 42) {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_thunderstorm));
                            } else {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_alt_thunderstorm));
                            }
                        } else if(weather_desc.equals("snow")) {
                            if (i >= 6 && i < 18 || i >= 30 && i <= 42) {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_day_snow));
                            } else {
                                weatherIconView.setIconResource(getString(com.github.pwittchen.weathericonview.library.R.string.wi_night_alt_snow));
                            }
                        }
                        weatherIconView.setIconSize(36);
                        if(i == hour){
                            weatherIconView.setIconColor(Color.parseColor("#028090"));
                        } else {
                            weatherIconView.setIconColor(Color.BLACK);
                        }
                        weatherIconView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                        LinearLayout forecast_block = new LinearLayout(Beach_Info.this);
                        forecast_block.setOrientation(LinearLayout.VERTICAL);
                        forecast_block.setGravity(Gravity.CENTER);

                        LinearLayout.LayoutParams forecast_block_params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        forecast_block_params.setMargins(0, 0, 100, 0);
                        forecast_block.setLayoutParams(forecast_block_params);

                        forecast_block.addView(weatherIconView);

                        LinearLayout.LayoutParams temp_tv_params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        temp_tv_params.setMargins(0,0,0,40);
                        TextView temp_tv = new TextView(Beach_Info.this);
                        temp_tv.setLayoutParams(temp_tv_params);
                        temp_tv.setText(Double.toString(hour_i_temp) + "°F");
                        temp_tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        temp_tv.setTypeface(null, Typeface.BOLD);
                        if(i == hour){
                            temp_tv.setTextColor(Color.parseColor("#028090"));
                        }
                        forecast_block.addView(temp_tv);

                        TextView hour_tv = new TextView(Beach_Info.this);
                        hour_tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        hour_tv.setTypeface(null, Typeface.BOLD);
                        if(i == hour){
                            hour_tv.setTextColor(Color.parseColor("#028090"));
                            hour_tv.setText("Now");
                        } else {
                            hour_tv.setText(hour_i_string);
                        }
                        forecast_block.addView(hour_tv);

                        LinearLayout parent_forecast_block = findViewById(R.id.day_forecast);
                        parent_forecast_block.addView(forecast_block);
                    }
                });
            }
        });
    }

    private void fetchHourlyForecast(String beach_coords, OnDayForecastFind onDayForecastFind){
        String[] coord = beach_coords.split(",");
        double latitude_double = Double.parseDouble(coord[1].trim());
        double longitude_double = Double.parseDouble(coord[0].trim());

        String base_url = "https://api.open-meteo.com/v1/forecast?";
        String latitude = "latitude=" + latitude_double + "&";
        String longitude = "longitude=" + longitude_double + "&";
        String current = "current=weather_code&";
        String settings = "hourly=temperature_2m,weather_code&timezone=America%2FLos_Angeles&temperature_unit=fahrenheit&forecast_days=2";

        String full_url = base_url + latitude + longitude + current + settings;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("String", full_url);
                try {
                    URL url = new URL(full_url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder unparsed_beaches_builder = new StringBuilder();
                        String input_line;

                        while((input_line = in.readLine()) != null){
                            unparsed_beaches_builder.append(input_line);
                        }

                        String unparsed_beach_details_string = unparsed_beaches_builder.toString();

                        Log.i("string", unparsed_beach_details_string);

                        JSONObject json_object = new JSONObject(unparsed_beach_details_string);
                        JSONObject hourly = json_object.getJSONObject("hourly");
                        JSONArray temps = hourly.getJSONArray("temperature_2m");
                        JSONArray weather_codes = hourly.getJSONArray("weather_code");

                        double[] temps_double = new double[temps.length()];
                        for (int i = 0; i < temps.length(); i++) {
                            temps_double[i] = temps.getDouble(i);
                        }

                        int[] weather_codes_int = new int[weather_codes.length()];
                        for (int i = 0; i < temps.length(); i++) {
                            weather_codes_int[i] = weather_codes.getInt(i);
                        }

                        onDayForecastFind.onDayForecastFound(temps_double, weather_codes_int);
                    }
                } catch (Exception e) {
                    Log.i("String", "Exception in Find Wave Height: " + e);
                }
            }
        });
        thread.start();
    }

    private void fetchBeachWaveHeight(String beach_coords, OnWaveHeightFind callback){
        String[] coord = beach_coords.split(",");
        double latitude_double = Double.parseDouble(coord[1].trim());
        double longitude_double = Double.parseDouble(coord[0].trim());

        String base_url = "https://marine-api.open-meteo.com/v1/marine?";
        String latitude = "latitude=" + latitude_double + "&";
        String longitude = "longitude=" + longitude_double + "&";
        String current = "current=wave_height";

        String full_url = base_url + latitude + longitude + current;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("String", "enterring run");
                Log.i("String", full_url);
                try {
                    URL url = new URL(full_url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder unparsed_beaches_builder = new StringBuilder();
                        String input_line;

                        while((input_line = in.readLine()) != null){
                            unparsed_beaches_builder.append(input_line);
                        }

                        String unparsed_beach_details_string = unparsed_beaches_builder.toString();

                        Log.i("string", unparsed_beach_details_string);

                        JSONObject json_object = new JSONObject(unparsed_beach_details_string);
                        JSONObject results = json_object.getJSONObject("current");

                        double wave_height = results.getDouble("wave_height");
                        Log.i("String", "wave height: " + wave_height);
                        callback.onWaveHeightFound(wave_height);
                    }
                } catch (Exception e) {
                    Log.i("String", "Exception in Find Wave Height: " + e);
                }
            }
        });
        thread.start();
    }

    private void fetchBeachCloudCover(String beach_coords, OnCloudCoverFind callback){
        String[] coord = beach_coords.split(",");
        double latitude_double = Double.parseDouble(coord[1].trim());
        double longitude_double = Double.parseDouble(coord[0].trim());

        //https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current=cloud_cover&temperature_unit=fahrenheit&forecast_days=1

        String base_url = "https://api.open-meteo.com/v1/forecast?";
        String latitude = "latitude=" + latitude_double + "&";
        String longitude = "longitude=" + longitude_double + "&";
        String current = "current=cloud_cover&forecast_days=1";

        String full_url = base_url + latitude + longitude + current;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("String", "enterring run");
                Log.i("String", full_url);
                try {
                    URL url = new URL(full_url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder unparsed_beaches_builder = new StringBuilder();
                        String input_line;

                        while((input_line = in.readLine()) != null){
                            unparsed_beaches_builder.append(input_line);
                        }

                        String unparsed_beach_details_string = unparsed_beaches_builder.toString();

                        Log.i("string", unparsed_beach_details_string);

                        JSONObject json_object = new JSONObject(unparsed_beach_details_string);
                        JSONObject results = json_object.getJSONObject("current");

                        double cloud_cover = results.getDouble("cloud_cover");
                        Log.i("String", "cloud cover: " + cloud_cover);
                        callback.onCloudCoverFound(cloud_cover);
                    }
                } catch (Exception e) {
                    Log.i("String", "Exception in Find Wave Height: " + e);
                }
            }
        });
        thread.start();
    }

    private void fetchBeachWeather(String beach_coords, OnCurrentWeatherFind callback){
        String[] coord = beach_coords.split(",");
        double latitude_double = Double.parseDouble(coord[1].trim());
        double longitude_double = Double.parseDouble(coord[0].trim());

        String base_url = "https://api.open-meteo.com/v1/forecast?";
        String latitude = "latitude=" + latitude_double + "&";
        String longitude = "longitude=" + longitude_double + "&";
        String current = "current=weather_code";

        String full_url = base_url + latitude + longitude + current;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("String", "enterring run");
                Log.i("String", full_url);
                try {
                    URL url = new URL(full_url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder unparsed_beaches_builder = new StringBuilder();
                        String input_line;

                        while((input_line = in.readLine()) != null){
                            unparsed_beaches_builder.append(input_line);
                        }

                        String unparsed_beach_details_string = unparsed_beaches_builder.toString();

                        Log.i("string", unparsed_beach_details_string);

                        JSONObject json_object = new JSONObject(unparsed_beach_details_string);
                        JSONObject results = json_object.getJSONObject("current");

                        int weather_code = results.getInt("weather_code");
                        String weather_desc;

                        if(weather_code == 0){
                            weather_desc = "clear";
                        } else if (weather_code <= 3){
                            weather_desc = "partly_cloudy";
                        } else if(weather_code == 45 || weather_code == 48){
                            weather_desc = "fog";
                        } else if(weather_code >= 51 && weather_code <= 67 || weather_code >= 80 && weather_code <= 82) {
                            weather_desc = "rain";
                        } else if(weather_code >= 95 && weather_code <= 99) {
                            weather_desc = "thunderstorm";
                        } else {
                            weather_desc = "snow";
                        }

                        callback.onCurrentWeatherFound(weather_desc);
                    }
                } catch (Exception e) {
                    Log.i("String", "Exception in Find Wave Height: " + e);
                }
            }
        });
        thread.start();
    }

    private void fetchBeachHoursRoutine(){
        fetchBeachIDbyName(global_beach_name, new OnBeachIDFind() {
            @Override
            public void onBeachIDFound(String beachID) {
                fetchBeachHours(beachID, new OnBeachHoursFind() {
                    @Override
                    public void onBeachHoursFound(String[] beach_hours) {
                        StringBuilder operating_hours_string_builder = new StringBuilder();
                        String operating_hours_string = "";
                        if(beach_hours != null){
                            for(int i = 0; i < beach_hours.length; i++){
                                operating_hours_string_builder.append(i == beach_hours.length - 1 ? beach_hours[i] : beach_hours[i] + "\n");
                            }
                            operating_hours_string = operating_hours_string_builder.toString();
                        } else {
                            operating_hours_string = "Sunday-Saturday: 12:00AM - 12:00AM";
                        }

                        final String final_op_hours_string = operating_hours_string;

                        runOnUiThread(() -> {
                            TextView operating_hours_tv = findViewById(R.id.operating_hours);
                            operating_hours_tv.setText(final_op_hours_string.toUpperCase());
                        });
                    }
                });
            }
        });
    }

    private void fetchBeachHours(String beach_id, OnBeachHoursFind hours_found_callback){
        String base_url = "https://maps.googleapis.com/maps/api/place/details/json?";
        String place_id = "place_id=" + beach_id + "&";
        String fields = "fields=name,opening_hours&";
        String api_key = "key=" + google_api_key;

        String full_url = base_url + place_id + fields + api_key;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(full_url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder unparsed_beaches_builder = new StringBuilder();
                        String input_line;

                        while((input_line = in.readLine()) != null){
                            unparsed_beaches_builder.append(input_line);
                        }

                        String unparsed_beach_details_string = unparsed_beaches_builder.toString();
                        JSONObject json_object = new JSONObject(unparsed_beach_details_string);
                        JSONObject results = json_object.getJSONObject("result");
                        JSONObject opening_hours;
                        try {
                            opening_hours = results.getJSONObject("opening_hours");
                        } catch (Exception e){
                            hours_found_callback.onBeachHoursFound(null);
                            return;
                        }
                        JSONArray weekday_text = opening_hours.getJSONArray("weekday_text");

                        String[] weekday_text_string = new String[weekday_text.length()];
                        for(int i = 0; i < weekday_text.length(); i++){
                            weekday_text_string[i] = weekday_text.getString(i);
                        }

                        hours_found_callback.onBeachHoursFound(weekday_text_string);
                    }
                } catch (Exception e) {
                    Log.i("String", "Exception in Find Hours: " + e);
                }
            }
        });
        thread.start();
    }

    private void fetchBeachIDbyName(String name, OnBeachIDFind id_found_callback){
        String base_url = "https://maps.googleapis.com/maps/api/place/textsearch/json?";
        String query = "query=" + name.replace(" ", "+") + "&";
        String api_key = "key=" + google_api_key;

        String full_url = base_url + query + api_key;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(full_url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder unparsed_beaches_builder = new StringBuilder();
                        String input_line;

                        while((input_line = in.readLine()) != null){
                            unparsed_beaches_builder.append(input_line);
                        }

                        String unparsed_beaches_string = unparsed_beaches_builder.toString();
                        JSONObject json_object = new JSONObject(unparsed_beaches_string);
                        JSONArray results = json_object.getJSONArray("results");
                        String place_id = results.getJSONObject(0).getString("place_id");
                        id_found_callback.onBeachIDFound(place_id);
                    }
                } catch (Exception e) {
                    Log.i("String", "Exception in Find ID: " + e);
                }
            }
        });
        thread.start();
    }
}