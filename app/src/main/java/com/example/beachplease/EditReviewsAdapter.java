package com.example.beachplease;


import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;


public class EditReviewsAdapter extends RecyclerView.Adapter<EditReviewsAdapter.ReviewViewHolder> {

    private List<Review> reviewsList;
    private OnEditClickListener editClickListener;
    String reviewID;


    public EditReviewsAdapter(List<Review> reviewsList) {
        this.reviewsList = reviewsList;
    }

    public EditReviewsAdapter(List<Review> reviewList, OnEditClickListener editClickListener) {
        this.reviewsList = reviewList;
        this.editClickListener = editClickListener;
    }
    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_edit_review, parent, false);
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

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewComment, beachName;
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
            beachName = itemView.findViewById(R.id.beachName);
            activityTags = itemView.findViewById(R.id.activityTags);
            editIcon = itemView.findViewById(R.id.editIcon);
        }

        public void bind(Review review) {
            reviewComment.setText(review.getReviewComment());
            beachName.setText(review.getBeachName());
            reviewStars.setRating(review.getNumStars());
            numStars.setText("(" + String.valueOf((int)review.getNumStars()) + "/5)");
            reviewID = review.getReviewId();

            setActivityTags(review.getActivityTags());

            // Set the click listener for the edit icon
            editIcon.setOnClickListener(v -> {
                // Redirect to a new page (e.g., EditReviewActivity)
                Intent intent = new Intent(v.getContext(), EditReviewActivity.class);

                intent.putExtra("Review_ID", reviewID);
                intent.putExtra("Beach_Name", review.getBeachName());
                intent.putExtra("Num_Stars", review.getNumStars());
                intent.putExtra("Review_Comment", review.getReviewComment());
                intent.putStringArrayListExtra("Activity_Tags", (ArrayList<String>) review.getActivityTags());
                intent.putStringArrayListExtra("Review_Pictures", (ArrayList<String>) review.getReviewPictures());

                v.getContext().startActivity(intent);
            });
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

    }

    // Listener interface for edit icon click
    public interface OnEditClickListener {
        void onEditClick(Review review);
    }
}
