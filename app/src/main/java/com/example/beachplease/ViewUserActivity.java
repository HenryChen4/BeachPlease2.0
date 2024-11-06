package com.example.beachplease;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


// Connects with activity_view_user_review.xml
// Backend code for displaying past user reviews
public class ViewUserActivity  extends AppCompatActivity {

    private RecyclerView reviewsRecyclerView;
    private EditReviewsAdapter editReviewsAdapter;  // Custom adapter to display reviews
    private List<Review> reviewsList;
    private DatabaseReference databaseReference;
    private FirebaseDatabase database;
    private String username;
    private String userId;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_user_review);
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
        userId = current_user_id;

        // listeners for button clicks
        ImageView home_button = findViewById(R.id.home_button);
        home_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewUserActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        ImageView profile_button = findViewById(R.id.profile_button);
        profile_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("str", "user profile clicked from view user activity");
                Intent intent = new Intent(ViewUserActivity.this, User_Profile.class);
                startActivity(intent);
            }
        });

        // update beach name
        Intent intent = getIntent();
        String beach_name = intent.getStringExtra("beach_name");


        // TODO: run database code to view reviews
        // Initialize Firebase database reference
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        databaseReference = database.getReference("Review");

        // Initialize RecyclerView
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize list and adapter
        reviewsList = new ArrayList<>();
        editReviewsAdapter = new EditReviewsAdapter(reviewsList);
        reviewsRecyclerView.setAdapter(editReviewsAdapter);

        // Load reviews matching the user
        loadUserReviews(userId);

        RecyclerView recyclerView = findViewById(R.id.reviewsRecyclerView);
        EditReviewsAdapter adapter = new EditReviewsAdapter(reviewsList, new EditReviewsAdapter.OnEditClickListener() {
            @Override
            public void onEditClick(Review review) {
                // Create the Intent to edit the review
                Intent intent = new Intent(ViewUserActivity.this, EditReviewActivity.class);

                // Pass the review attributes through the Intent
//                intent.putExtra("Review_ID", review.getReviewText());
                intent.putExtra("Beach_Name", review.getBeachName());
                intent.putExtra("Num_Stars", review.getNumStars());
                intent.putExtra("Review_Comment", review.getReviewComment());
                ArrayList<String> reviewPictures = (ArrayList<String>) review.getReviewPictures();
                intent.putStringArrayListExtra("Review_Pictures", reviewPictures);
                ArrayList<String> reviewActivities = (ArrayList<String>) review.getActivityTags();
                intent.putStringArrayListExtra("Activity_Tags", reviewActivities);

                // Start the EditReviewActivity
                startActivity(intent);
            }
        });

// Set the adapter for the RecyclerView
        recyclerView.setAdapter(adapter);
    }

    // LOAD BEACH REVIEWS
    private void loadUserReviews(String username) {
        database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        databaseReference = database.getReference("Review");
        Log.i("ViewUserActivity", "User_Id: " +  username);
        Query query = databaseReference.orderByChild("userId").equalTo(username);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reviewsList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Review review = snapshot.getValue(Review.class);
                    if (review != null) {
                        reviewsList.add(review);
                    }
                }
                // Update the TextView with the number of reviews
                TextView numReviews = findViewById(R.id.numTotalReviews); // Make sure the TextView has this ID in your XML
                numReviews.setText(reviewsList.size() + " Review(s)");

                editReviewsAdapter.notifyDataSetChanged();  // Notify adapter of data change
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewUserActivity.this,
                        "Failed to load reviews: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
