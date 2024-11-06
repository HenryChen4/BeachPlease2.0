package com.example.beachplease;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import android.os.SharedMemory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

class Beach{
    private String name;
    private double longitude;
    private double latitude;

    public Beach(String name, double longitude, double latitude) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getName(){
        return name;
    }
    public double getLongitude(){
        return longitude;
    }
    public double getLatitude(){
        return latitude;
    }
}

interface OnBeachFind {
    void onBeachesFetched(String beachData);
}

interface OnUserLocationFind {
    void onLocationFound(Location user_location);
}

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap googleMap;

    private final int LOCATION_PERMISSION_CODE = 1;
    FusedLocationProviderClient fusedLocationClient;

    double USC_LONGITUDE = -118.285530;
    double USC_LATITUDE = 34.022415;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Initialize class wide google map object
        this.googleMap = googleMap;

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                String beach_name = marker.getTitle();
                String beach_coords = marker.getTag().toString();

                Log.i("str", beach_name +" clicked");

                Intent intent = new Intent(MainActivity.this, Beach_Info.class);
                intent.putExtra("beach_name", beach_name);
                intent.putExtra("beach_coords", beach_coords);
                startActivity(intent);

                return true;
            }
        });
    }

    private void getLastLocation(OnUserLocationFind onUserLocationFind){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null){
                    onUserLocationFind.onLocationFound(location);
                }
            }
        });
    }

    private void beachMapRoutine(){
        getLastLocation(new OnUserLocationFind() {
            @Override
            public void onLocationFound(Location user_location) {
                LatLng current_latlng = new LatLng(USC_LATITUDE, USC_LONGITUDE);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_latlng, 15));

                googleMap.addMarker(new MarkerOptions()
                        .position(current_latlng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        .title("You are here"));

                Log.i("String", current_latlng.latitude + "," +current_latlng.longitude);

                getNearestBeachesString(USC_LONGITUDE, USC_LATITUDE, new OnBeachFind() {
                    @Override
                    public void onBeachesFetched(String beach_data) {
                        List<Beach> nearest_beaches = stringToBeaches(beach_data);

                        LatLngBounds.Builder bounds_builder = new LatLngBounds.Builder();
                        LatLng current_user_latlng = new LatLng(user_location.getLatitude(), user_location.getLongitude());
                        bounds_builder.include(new LatLng(USC_LATITUDE, USC_LONGITUDE));

                        for (Beach beach : nearest_beaches) {
                            runOnUiThread(() -> {
                                LatLng beach_pin = new LatLng(beach.getLatitude(), beach.getLongitude());
                                Log.i("string", "name:"+beach.getName());
                                Marker beach_marker = googleMap.addMarker(new MarkerOptions()
                                                               .position(beach_pin)
                                                               .title(beach.getName()));
                                beach_marker.setTag(beach_pin.longitude + "," + beach_pin.latitude);
                                bounds_builder.include(beach_pin);
                            });
                        }

                        runOnUiThread(() -> {
                            int padding = 100;
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds_builder.build(), padding));
                        });
                    }
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == LOCATION_PERMISSION_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                beachMapRoutine();
            } else {
                Toast.makeText(this, "Location permission is denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialization code
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // DEBUG LOCAL STORAGE
//        SharedPreferences local_storage = getSharedPreferences("user_data", MODE_PRIVATE);
//        SharedPreferences.Editor editor = local_storage.edit();
//        editor.remove("user_id");
//        editor.commit();

        // listeners for button clicks
        ImageView profile_button = findViewById(R.id.profile_button);
        profile_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, User_Profile.class);
                startActivity(intent);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Main code
        beachMapRoutine();
    }

    // Nearest beach helper functions
    private void getNearestBeachesString(double longitude, double latitude, OnBeachFind callback){
        String base_url = "https://api.geoapify.com/v2/places?";
        String categories = "categories=beach&conditions=named&";
        String filter = "filter=circle:" + longitude + "," + latitude + ",100000&";
        String bias = "bias=proximity:" + longitude + "," + latitude + "&";
        String limit = "limit=5&";
        String API_KEY = "apiKey=36927a5a88e242bda82d2893f4067118";

        String full_url = base_url + categories + filter + bias + limit + API_KEY;

        String[] beach_results = {""};

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
                        callback.onBeachesFetched(unparsed_beaches_string);
                    }
                } catch (Exception e) {
                    Log.i("String", "Exception: " + e);
                }
            }
        });
        thread.start();
    }

    private List<Beach> stringToBeaches(String beach_string){
        Gson gson = new Gson();
        GeoapifyResponse api_response = gson.fromJson(beach_string, GeoapifyResponse.class);
        List<Beach> beach_list = new ArrayList<Beach>();
        for(GeoapifyResponse.Feature feature : api_response.getFeatures()){
            String beach_name = feature.getProperties().getName();
            double beach_long = feature.getProperties().getLongitude();
            double beach_lat = feature.getProperties().getLatitude();
            Beach beach = new Beach(beach_name, beach_long, beach_lat);
            beach_list.add(beach);
        }
        return beach_list;
    }
}