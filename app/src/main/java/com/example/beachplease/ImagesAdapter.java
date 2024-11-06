package com.example.beachplease;

import android.content.Context; // Correctly import the Context class
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageViewHolder> {
    private ArrayList<Uri> imageUris;
    private Context context; // Change to EditReviewActivity for more specific context handling

    public ImagesAdapter(ArrayList<Uri> imageUris, Context context) { // Use EditReviewActivity directly
        this.imageUris = imageUris;
        this.context = context;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        holder.imageView.setImageURI(imageUri);

        // Set up delete button
        holder.deleteButton.setOnClickListener(v -> {
            // Remove the image from the list and notify the adapter
            imageUris.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, imageUris.size()); // Update the position of remaining items
            // Check if context is an instance of EditReviewActivity to call updatePhotoCount()
            if (context instanceof EditReviewActivity) {
                ((EditReviewActivity) context).updatePhotoCount(); // Update the photo count in EditReviewActivity
            } else if (context instanceof AddReviewActivity) {
                ((AddReviewActivity) context).updatePhotoCount(); // Update the photo count in AddReviewActivity (if such method exists)
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        Button deleteButton;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
