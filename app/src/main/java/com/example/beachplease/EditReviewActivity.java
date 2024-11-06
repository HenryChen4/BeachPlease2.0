package com.example.beachplease;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

// USERS CAN EDIT OR DELETE REVIEW
public class EditReviewActivity extends AppCompatActivity {
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
    private Button deleteButton;
    private DatabaseReference databaseReference;

    private String public_review_id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_review);

        cancelButton = findViewById(R.id.cancelButton);
        postButton = findViewById(R.id.postButton);
        databaseReference = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/").getReference("Review");


        Intent intent = getIntent();
        String review_id = intent.getStringExtra("Review_ID");
        String beach_name = intent.getStringExtra("Beach_Name");
        float old_numStars = intent.getFloatExtra("Num_Stars", 0);
        String old_comment = intent.getStringExtra("Review_Comment");
        ArrayList<String> old_pictures = intent.getStringArrayListExtra("Review_Pictures");
        ArrayList<String> old_activityTags = intent.getStringArrayListExtra("Activity_Tags");
        System.out.println("Print activity msg");
        Log.d("EditReviewActivity", "Received Activity Tags: " + old_activityTags);

        public_review_id = review_id;

        String user_id = "-1";
        SharedPreferences local_storage = getSharedPreferences("user_info", MODE_PRIVATE);
        if (!local_storage.getString("current_user", "-1").equals("-1")) { // checks if current user_id
            user_id = local_storage.getString("current_user", "-1");
        }

        TextView beachReviewDisplayStringView = findViewById(R.id.beachReviewText);
        String beachReviewString = "Edit a review: " + beach_name;
        beachReviewDisplayStringView.setText(beachReviewString);

        // Attributes of a review: BeachName, NumStars, ReviewComment, ReviewPicture, UserID, ActivityTags
        // Reads in the fields of a review: NumStars, ReviewComment, ReviewPicture, ActivityTags
        // BeachName is taken from intent
        // UserID taken from intent?
        numStars = findViewById(R.id.starRatingNum);
        numStars.setRating(old_numStars);
        reviewActivityTags = findViewById(R.id.activityTagsChipGroup);
        // Check each predefined tag in reviewActivityTags and set checked state if it matches
        if (old_activityTags != null && !old_activityTags.isEmpty()) {
            for (int i = 0; i < reviewActivityTags.getChildCount(); i++) {
                View view = reviewActivityTags.getChildAt(i);
                if (view instanceof Chip) {
                    Chip chip = (Chip) view;
                    // Check if the chip's text is in the old_activityTags list
                    if (old_activityTags.contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    } else {
                        chip.setChecked(false);  // Uncheck if it's not in the list
                    }
                }
            }
        } else {
            Log.e("EditReviewActivity", "No activity tags received to update predefined tags.");
        }
        reviewComment = findViewById(R.id.reviewCommentText);
        reviewComment.setText(old_comment);

        selectImagesButton = findViewById(R.id.selectImagesButton);

        numPicturesText = findViewById(R.id.numTotalPhotos);

        imageUriList = new ArrayList<>();
        selectedImageUris = new ArrayList<>();
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView);
        imagesAdapter = new ImagesAdapter(imageUriList, this);

        // Set up RecyclerView to display images horizontally or in a grid
        imagesRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        imagesRecyclerView.setAdapter(imagesAdapter);
        // Sets current pictures to old pictures
        if (old_pictures != null) {
            // Set the number of pictures text
            numPicturesText.setText("Photos (" + old_pictures.size() + ")");

            // Convert each URI string to a Uri object and add to imageUriList
            for (String uriString : old_pictures) {
                Uri uri = Uri.parse(uriString);
                imageUriList.add(uri);
                selectedImageUris.add(uriString); // Keep a string representation if needed
            }

            // Notify the adapter of the new data
            imagesAdapter.notifyDataSetChanged();
        }

        if (ContextCompat.checkSelfPermission(EditReviewActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(EditReviewActivity.this,
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
                                        Toast.makeText(EditReviewActivity.this, "Not allowed to pick more than 4 images", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(EditReviewActivity.this, "Not allowed to pick more than 4 images", Toast.LENGTH_SHORT).show();
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
                Log.i("ReviewEdited", "Review id: " + review_id);
                Log.i("ReviewEdited", "Stars: " + stars);
                Log.i("ReviewEdited", "Comment: " + comment);
                Log.i("ReviewEdited", "Selected Activity Tags: " + selectedActivityTags.toString());
                Log.i("ReviewEdited", "Selected Photos: " + selectedPhotos.toString());

                if (stars > 0){
                    Review reviewInstance = new Review();

                    // Set up a success listener
                    reviewInstance.editReview(review_id, beach_name, stars, comment, selectedPhotos, selectedActivityTags, new Review.UploadCallback() {
                        @Override
                        public void onSuccess() {
                            System.out.println("Submission success");
                            // Show a success message
                            Toast.makeText(EditReviewActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                            // Optionally, close the activity after submitting the review
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            System.out.println("Submission failed");
                            // Show a failure message
                            Toast.makeText(EditReviewActivity.this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure() {
                            System.out.println("Submission failed");
                            // Show a failure message
                            Toast.makeText(EditReviewActivity.this, "Failed to submit review: ", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else{
                    Toast.makeText(EditReviewActivity.this, "Failed to edit review. Please leave a rating", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // DELETE REVIEW
        deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Log the values to Logcat
                Log.i("Review Deletion", "Review id: " + public_review_id);

                // Create an AlertDialog to confirm deletion
                new AlertDialog.Builder(EditReviewActivity.this)
                        .setMessage("Are you sure you want to delete this review?")
                        .setCancelable(false) // Make sure the user can't dismiss the dialog without confirming
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Proceed with deletion if user confirms
                                Review reviewInstance = new Review();

                                // Set up a success listener
                                reviewInstance.deleteReview(public_review_id, new Review.UploadCallback() {
                                    @Override
                                    public void onSuccess() {
                                        System.out.println("Deletion success");
                                        // Show a success message
                                        Toast.makeText(EditReviewActivity.this, "Review deleted successfully!", Toast.LENGTH_SHORT).show();
                                        // Optionally, close the activity after deleting the review
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        System.out.println("Submission failed");
                                        // Show a failure message
                                        Toast.makeText(EditReviewActivity.this, "Failed to delete review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure() {
                                        System.out.println("Submission failed");
                                        // Show a failure message
                                        Toast.makeText(EditReviewActivity.this, "Failed to submit review: ", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("No", null) // Cancel the deletion if the user selects "No"
                        .show();

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
