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
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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

interface OnAverageRatingFind{
    void onAverageRatingFound(double average_rating);
}

interface OnUsernameFind{
    void onUsernameFound(String username);
}

interface OnReviewFind{
    void onReviewFound(BeachReview beachReview);
}

class BeachReview{
    public String username;
    public double beach_rating;
    public List<String> tags;
    public String review_body;

    public BeachReview(String username, double beach_rating, List<String> tags, String review_body){
        this.username = username;
        this.beach_rating = beach_rating;
        this.tags = tags;
        this.review_body = review_body;
    }
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

        // post overview of beach
        fetchOverviewRoutine();

        // run database code to view reviews
        fetchAllUserReviews(global_beach_name, new OnReviewFind() {
            @Override
            public void onReviewFound(BeachReview beachReview) {
                runOnUiThread(() -> {
                    // review wrapper
                    LinearLayout review_wrapper = new LinearLayout(Beach_Info.this);
                    LinearLayout.LayoutParams review_wrapper_params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    review_wrapper.setOrientation(LinearLayout.VERTICAL);
                    review_wrapper_params.setMargins(0, 0, 0, 16);
                    review_wrapper.setPadding(16, 16, 16, 16);

                    // rating and username
                    LinearLayout user_rating_wrapper = new LinearLayout(Beach_Info.this);
                    user_rating_wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    user_rating_wrapper.setOrientation(LinearLayout.HORIZONTAL);
                    user_rating_wrapper.setPadding(0, 16, 0, 16);
                    user_rating_wrapper.setGravity(Gravity.CENTER);

                    // username
                    TextView username_tv = new TextView(Beach_Info.this);
                    username_tv.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    username_tv.setText(beachReview.username);
                    username_tv.setTextSize(20);
                    username_tv.setTextColor(Color.BLACK);
                    username_tv.setGravity(Gravity.CENTER_VERTICAL);
                    username_tv.setTypeface(null, Typeface.BOLD);

                    // rating bar
                    RatingBar ratingBar = new RatingBar(Beach_Info.this, null, android.R.attr.ratingBarStyleSmall);
                    LinearLayout.LayoutParams ratingLayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    ratingLayoutParams.setMarginStart(120);
                    ratingBar.setLayoutParams(ratingLayoutParams);
                    ratingBar.setRating((float) beachReview.beach_rating);
                    ratingBar.setStepSize(1);

                    user_rating_wrapper.addView(username_tv);
                    user_rating_wrapper.addView(ratingBar);

                    // tags
                    ChipGroup chipGroup = new ChipGroup(Beach_Info.this);
                    chipGroup.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));

                    for (String chipText : beachReview.tags) {
                        Chip chip = new Chip(Beach_Info.this);
                        chip.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        ));
                        chip.setText(chipText);
                        chip.setTextSize(12);
                        chip.setTextColor(Color.parseColor("#f7fff7"));
                        chip.setChipBackgroundColorResource(android.R.color.holo_red_light);
                        chip.setChipCornerRadius(100f);
                        chipGroup.addView(chip);
                    }

                    // review body
                    TextView commentTextView = new TextView(Beach_Info.this);
                    commentTextView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    commentTextView.setText(beachReview.review_body);
                    commentTextView.setTextSize(16);
                    LinearLayout.LayoutParams commentLayoutParams = (LinearLayout.LayoutParams) commentTextView.getLayoutParams();
                    commentLayoutParams.setMargins(0, 0, 0, 10);
                    commentTextView.setLayoutParams(commentLayoutParams);

                    // add everything
                    review_wrapper.addView(user_rating_wrapper);
                    review_wrapper.addView(chipGroup);
                    review_wrapper.addView(commentTextView);

                    LinearLayout beach_review_wrapper = (LinearLayout) findViewById(R.id.beach_review_wrapper);
                    beach_review_wrapper.addView(review_wrapper, review_wrapper_params);
                });
            }
        });
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

    private void fetchOverviewRoutine(){
        mostPopularTags(global_beach_name, new OnMostTagsFind() {
            @Override
            public void onMostTagsFound(String[] most_used_tags) {
                if(most_used_tags.length == 0){
                    runOnUiThread(() -> {
                        TextView no_tags = (TextView) findViewById(R.id.no_tags_header);
                        no_tags.setVisibility(View.VISIBLE);
                        ChipGroup most_pop_tags = (ChipGroup) findViewById(R.id.most_pop_tags);
                        most_pop_tags.setVisibility(View.GONE);
                    });
                } else {
                    runOnUiThread(() -> {
                        TextView no_tags = (TextView) findViewById(R.id.no_tags_header);
                        no_tags.setVisibility(View.GONE);
                        ChipGroup most_pop_tags = (ChipGroup) findViewById(R.id.most_pop_tags);
                        most_pop_tags.setVisibility(View.VISIBLE);

                        if(most_used_tags.length == 1){
                            Chip most_pop_tag_1 = (Chip) findViewById(R.id.most_pop_tag_1);
                            most_pop_tag_1.setText(most_used_tags[0]);
                            most_pop_tag_1.setVisibility(View.VISIBLE);
                        } else {
                            Chip most_pop_tag_1 = (Chip) findViewById(R.id.most_pop_tag_1);
                            most_pop_tag_1.setText(most_used_tags[0]);
                            most_pop_tag_1.setVisibility(View.VISIBLE);
                            Chip most_pop_tag_2 = (Chip) findViewById(R.id.most_pop_tag_2);
                            most_pop_tag_2.setText(most_used_tags[1]);
                            most_pop_tag_2.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
        averageRating(global_beach_name, new OnAverageRatingFind() {
            @Override
            public void onAverageRatingFound(double average_rating) {
                runOnUiThread(() -> {
                    RatingBar overview_rating_bar = (RatingBar) findViewById(R.id.overview_rating);
                    overview_rating_bar.setRating((float) average_rating);
                });
            }
        });
    }

    private void averageRating(String beach_name, OnAverageRatingFind onAverageRatingFind){
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        DatabaseReference database_ref = database.getReference("Review");

        database_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double average_rating = 0;
                double rating_count = 0;
                for(DataSnapshot review_snapshot : snapshot.getChildren()){
                    String beachName = review_snapshot.child("beachName").getValue(String.class);
                    if(beachName != null){
                        if(beachName.equals(beach_name)){
                            rating_count += 1;
                            double num_stars = review_snapshot.child("numStars").getValue(Double.class);
                            average_rating += num_stars;
                        }
                    }
                }
                onAverageRatingFind.onAverageRatingFound(average_rating/rating_count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i("str", "error retrieving average rating");
            }
        });
    }

    private void fetchUsername(String user_id, OnUsernameFind onUsernameFind){
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        DatabaseReference database_ref = database.getReference("User");

        database_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot user_snapshot : snapshot.getChildren()){
                    Log.i("st", user_id);
                    Log.i("st", "compared: " + user_snapshot.getKey().toString());
                    Log.i("st", "result: " + user_snapshot.getKey().toString().equals(user_id));
                    if(user_snapshot.getKey().toString().strip().equals(user_id.strip())){
                        String username = user_snapshot.child("username").getValue(String.class);
                        onUsernameFind.onUsernameFound(username);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i("str", "cant find username");
            }
        });
    }

    private void fetchAllUserReviews(String beach_name, OnReviewFind onReviewFind){
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        DatabaseReference database_ref = database.getReference("Review");

        database_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot review_snapshot : snapshot.getChildren()){
                    String review_beach_name = review_snapshot.child("beachName").getValue(String.class);
                    if(review_beach_name != null){
                        if(review_beach_name.equals(beach_name)){
                            int num_stars = review_snapshot.child("numStars").getValue(Integer.class);
                            List<String> activity_tags = new ArrayList<>();
                            for(DataSnapshot tagSnapshot : review_snapshot.child("activityTags").getChildren()) {
                                String tag = tagSnapshot.getValue(String.class);
                                activity_tags.add(tag);
                            }
                            String review_body = review_snapshot.child("reviewComment").getValue(String.class);
                            fetchUsername(review_snapshot.child("userId").getValue(String.class), new OnUsernameFind() {
                                @Override
                                public void onUsernameFound(String username) {
                                    BeachReview beach_review = new BeachReview(username, num_stars, activity_tags, review_body);
                                    onReviewFind.onReviewFound(beach_review);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i("str", "username not found");
            }
        });
    }

    private void mostPopularTags(String beach_name, OnMostTagsFind onMostTagsFind){
        // return 2 most popular tags
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        DatabaseReference database_ref = database.getReference("Review");

        String[] available_tags = {"Swim", "Surf", "Tan", "Volleyball", "Sunset"};

        database_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int[] tag_count = {0, 0, 0, 0, 0};
                for(DataSnapshot reviewSnapShot : snapshot.getChildren()){
                    String beachName = reviewSnapShot.child("beachName").getValue(String.class);
                    if(beachName != null){
                        if(beachName.equals(beach_name)){
                            ArrayList<String> activity_tags = new ArrayList<>();
                            for(DataSnapshot tagSnapshot : reviewSnapShot.child("activityTags").getChildren()) {
                                String tag = tagSnapshot.getValue(String.class);
                                activity_tags.add(tag);
                            }
                            for(String tag : activity_tags){
                                for(int i = 0; i < available_tags.length; i++){
                                    if(tag.equals(available_tags[i])){
                                        tag_count[i]++;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                int first_idx = -1;
                int second_idx = -1;
                for(int i = 0; i < tag_count.length-1; i++){
                    int first = tag_count[i];
                    int second = tag_count[i + 1];
                    if(second > first){
                        first_idx = i + 1;
                        second_idx = i;
                    }
                }

                String first_tagged = "";
                String second_tagged = "";

                if(first_idx != -1 && second_idx != -1){
                    first_tagged = available_tags[first_idx];
                    second_tagged = available_tags[second_idx];
                } else if(first_idx != -1){
                    first_tagged = available_tags[first_idx];
                }

                String[] most_tagged = {first_tagged, second_tagged};
                onMostTagsFind.onMostTagsFound(most_tagged);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i("str", "error when calculating tags");
            }
        });
    }
}