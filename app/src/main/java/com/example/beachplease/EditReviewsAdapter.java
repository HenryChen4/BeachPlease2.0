package com.example.beachplease;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class EditReviewsAdapter extends RecyclerView.Adapter<EditReviewsAdapter.ReviewViewHolder> {

    private List<Review> reviewsList;
    private OnEditClickListener editClickListener;


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

    // Listener interface for edit icon click
    public interface OnEditClickListener {
        void onEditClick(Review review);
    }
}
