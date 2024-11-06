package com.example.beachplease;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private List<Review> reviewsList;
    String reviewID;

    public ReviewsAdapter(List<Review> reviewsList) {
        this.reviewsList = reviewsList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewsList.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviewsList.size();
    }

    public interface OnUsernameFind {
        void onUsernameFound(String username);
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewComment, userName;
        TextView numStars;
        RatingBar reviewStars;
        ChipGroup activityTags;
        RecyclerView imagesRecyclerView;
        ImageView editIcon;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewComment = itemView.findViewById(R.id.reviewComment);
            reviewStars = itemView.findViewById(R.id.reviewStars);
            numStars = itemView.findViewById(R.id.numStars);
            userName = itemView.findViewById(R.id.userName);
            activityTags = itemView.findViewById(R.id.activityTags);
            editIcon = itemView.findViewById(R.id.editIcon);
        }

        public void bind(Review review) {
            reviewComment.setText(review.getReviewComment());

            // Call fetchUsername asynchronously
            fetchUsername(review.getUserId(), new OnUsernameFind() {
                @Override
                public void onUsernameFound(String username) {
                    // Once the username is found, set it to the userName TextView
                    userName.setText(username);
                }
            });
//            userName.setText(fetchUserName(review.getUserId(), OnUsernameFind onUsernameFind);
//            userName.setText(review.getUserId()); // NOTE: CURRENTLY SETTING TO USER ID. SWITCH TO USERNAME LATER

            reviewStars.setRating(review.getNumStars());
            numStars.setText("(" + String.valueOf((int)review.getNumStars()) + "/5)");
            reviewID = review.getReviewId();

            setActivityTags(review.getActivityTags());
        }

        public void setActivityTags(List<String> tags) {
            if (tags == null) {
                tags = new ArrayList<String>(); // Set to an empty list to avoid NullPointerException
            }

            // Find the ChipGroup and each Chip by their ID
            Chip chipSwimming = activityTags.findViewById(R.id.chipSwimming);
            Chip chipSurfing = activityTags.findViewById(R.id.chipSurfing);
            Chip chipSunbathing = activityTags.findViewById(R.id.chipSunbathing);
            Chip chipVolleyball = activityTags.findViewById(R.id.chipVolleyball);
            Chip chipSunset = activityTags.findViewById(R.id.chipSunset);

            // Hide all chips initially
            chipSwimming.setVisibility(View.GONE);
            chipSurfing.setVisibility(View.GONE);
            chipSunbathing.setVisibility(View.GONE);
            chipVolleyball.setVisibility(View.GONE);
            chipSunset.setVisibility(View.GONE);

            // Loop through the tags and show corresponding chips
            for (String tag : tags) {
                switch (tag) {
                    case "Swim":
                        chipSwimming.setVisibility(View.VISIBLE);
                        break;
                    case "Surf":
                        chipSurfing.setVisibility(View.VISIBLE);
                        break;
                    case "Tan":
                        chipSunbathing.setVisibility(View.VISIBLE);
                        break;
                    case "Volleyball":
                        chipVolleyball.setVisibility(View.VISIBLE);
                        break;
                    case "Sunset":
                        chipSunset.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }

        private void fetchUsername(String user_id, OnUsernameFind onUsernameFind){
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
            DatabaseReference database_ref = database.getReference("User");

            database_ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot user_snapshot : snapshot.getChildren()) {
                        Log.i("st", user_id);
                        Log.i("st", "compared: " + user_snapshot.getKey().toString());
                        Log.i("st", "result: " + user_snapshot.getKey().toString().equals(user_id));
                        if (user_snapshot.getKey().toString().strip().equals(user_id.strip())) {
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
    }
}

