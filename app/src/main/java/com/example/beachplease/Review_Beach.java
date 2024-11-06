package com.example.beachplease;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Review_Beach extends AppCompatActivity {
    private String user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_review_beach);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // button listeners
        ImageView home_button = findViewById(R.id.home_button);
        home_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Review_Beach.this, MainActivity.class);
                startActivity(intent);
            }
        });

        ImageView profile_button = findViewById(R.id.profile_button);
        profile_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("string","user profile clicked");
                Intent intent = new Intent(Review_Beach.this, User_Profile.class);
                startActivity(intent);
            }
        });

        // get current logged in user id
        SharedPreferences local_storage = getSharedPreferences("user_data", MODE_PRIVATE);
        String current_user_id = local_storage.getString("user_id", "-1");
        if(!current_user_id.equals("-1")){
            user_id = current_user_id;
            Log.i("string", "Posting review for: " + user_id);
        }

        // TODO: post a review

    }
}