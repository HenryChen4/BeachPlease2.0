package com.example.beachplease;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class AddReviewActivity extends AppCompatActivity {

    private static final int READ_PERMISSION = 101;

    ActivityResultLauncher<Intent> activityResultLauncher;


    private RatingBar numStars;
    private ChipGroup reviewActivityTags;
    private EditText reviewComment;

    private TextView numPicturesText;
    private ImageView reviewPicture;
    private ArrayList<Uri> imageUriList;
    private ArrayList<String> selectedImageUris;
    private RecyclerView imagesRecyclerView;
    private ImagesAdapter imagesAdapter;
    private Button selectImagesButton;

    private Button cancelButton;
    private Button postButton;
    private DatabaseReference databaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_review);

        cancelButton = findViewById(R.id.cancelButton);
        postButton = findViewById(R.id.postButton);
        databaseReference = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/").getReference("Review");

        ImageView closeButton = findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        String beach_name = intent.getStringExtra("beach_name");
        String user_id = intent.getStringExtra("User_ID");

        TextView beachReviewDisplayStringView = findViewById(R.id.beachReviewText);
        String beachReviewString = "Add a review: " + beach_name;
        beachReviewDisplayStringView.setText(beachReviewString);

        // Attributes of a review: BeachName, NumStars, ReviewComment, ReviewPicture, UserID, ActivityTags
        // Reads in the fields of a review: NumStars, ReviewComment, ReviewPicture, ActivityTags
        // BeachName is taken from intent
        // UserID taken from intent?
        numStars = findViewById(R.id.starRatingNum);
        reviewActivityTags = findViewById(R.id.activityTagsChipGroup);
        reviewComment = findViewById(R.id.reviewCommentText);

        selectImagesButton = findViewById(R.id.selectImagesButton);

        numPicturesText = findViewById(R.id.numTotalPhotos);

        imageUriList = new ArrayList<>();
        selectedImageUris = new ArrayList<>();
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView);
        imagesAdapter = new ImagesAdapter(imageUriList, this);

        // Set up RecyclerView to display images horizontally or in a grid
        imagesRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        imagesRecyclerView.setAdapter(imagesAdapter);

        if (ContextCompat.checkSelfPermission(AddReviewActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(AddReviewActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION);
        }

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK && null != result.getData()){
                            if(result.getData().getClipData() != null) {
                                // this part is to get multiple images
                                int countOfImages = result.getData().getClipData().getItemCount();
                                for (int i = 0; i < countOfImages; i++){
                                    // limiting the number of images picked up from gallery
                                    if (imageUriList.size() < 4) {
                                        Uri imageUri = result.getData().getClipData().getItemAt(i).getUri(); // Correctly get each URI
                                        imageUriList.add(imageUri);
                                        selectedImageUris.add(imageUri.toString());
                                    } else {
                                        Toast.makeText(AddReviewActivity.this, "Not allowed to pick more than 4 images", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                imagesAdapter.notifyDataSetChanged();
                                numPicturesText.setText("Photos (" + imageUriList.size() + ")");
                            } else {
                                //limiting the number of images picked up from gallery even when single image picked
                                if (imageUriList.size() < 4) {
                                    Uri imageuri = result.getData().getData();
                                    imageUriList.add(imageuri);
                                    selectedImageUris.add(imageuri.toString());
                                } else {
                                    Toast.makeText(AddReviewActivity.this, "Not allowed to pick more than 4 images", Toast.LENGTH_SHORT).show();
                                }

                                imagesAdapter.notifyDataSetChanged();
                                numPicturesText.setText("Photos (" + imageUriList.size() + ")");
                            }
                        }
                    }
                });

        selectImagesButton.setOnClickListener(v -> openImagePicker());

        // Cancel button click listener
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close the activity and go back to the previous page
            }
        });

        // Post button click listener
        String finalUser_id = user_id;
        postButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                float stars = numStars.getRating();
                String comment = reviewComment.getText().toString();
                ArrayList<String> selectedActivityTags = getSelectedActivityTags();
                ArrayList<String> selectedPhotos = selectedImageUris;

                // Log the values to Logcat
                Log.i("ReviewUpload", "Stars: " + stars);
                Log.i("ReviewUpload", "Comment: " + comment);
                Log.i("ReviewUpload", "Selected Activity Tags: " + selectedActivityTags.toString());
                Log.i("ReviewUpload", "Selected Photos: " + selectedPhotos.toString());

                if (stars > 0){
                    System.out.println("Trying to submit review");
                    Review reviewInstance = new Review();

                    // Set up a success listener
                    reviewInstance.createNewReview(beach_name, stars, comment, selectedPhotos, finalUser_id, selectedActivityTags, new Review.UploadCallback() {
                        @Override
                        public void onSuccess() {
                            System.out.println("Submission success");
                            // Show a success message
                            Toast.makeText(AddReviewActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                            // Optionally, close the activity after submitting the review
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            System.out.println("Submission failed");
                            // Show a failure message
                            Toast.makeText(AddReviewActivity.this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure() {
                            System.out.println("Submission failed");
                            // Show a failure message
                            Toast.makeText(AddReviewActivity.this, "Failed to submit review: ", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else{
                    Toast.makeText(AddReviewActivity.this, "Failed to add review. Please leave a rating", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple images
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activityResultLauncher.launch(intent);
    }

    // Method to get selected activity tags
    private ArrayList<String> getSelectedActivityTags() {
        ArrayList<String> selectedTags = new ArrayList<>();
        for (int i = 0; i < reviewActivityTags.getChildCount(); i++) {
            Chip chip = (Chip) reviewActivityTags.getChildAt(i);
            if (chip.isChecked()) {
                selectedTags.add(chip.getText().toString());
            }
        }
        return selectedTags;
    }

    // Method to update photo count
    public void updatePhotoCount() {
        numPicturesText.setText("Photos (" + imageUriList.size() + ")");
    }
}


