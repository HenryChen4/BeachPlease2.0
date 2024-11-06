package com.example.beachplease;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private List<Review> reviewsList;

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

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewText, reviewStars;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewText = itemView.findViewById(R.id.reviewText);
            reviewStars = itemView.findViewById(R.id.reviewStars);
        }

        public void bind(Review review) {
            reviewText.setText(review.getReviewComment());
            reviewStars.setText(String.valueOf(review.getNumStars()) + " stars");
        }
    }
}

