package com.example.beachplease;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Review {

    private FirebaseDatabase root;
    private DatabaseReference reference;

    FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");

    // Review Attributes
    private String Beach_Name;
    private float Num_Stars;
    private String Review_Comment;
    private List<String> Review_Pictures;
    private String User_ID;
    private List<String> Activity_Tags;

    // Default Constructor
    public Review() {
    }

    // Created Constructor
    public Review(String Beach_Name, float Num_Stars, String Review_Comment, List<String> Review_Pictures, String User_ID, List<String> Activity_Tags) {
        this.Beach_Name = Beach_Name;
        this.Num_Stars = Num_Stars;
        this.Review_Comment = Review_Comment;
        this.Review_Pictures = Review_Pictures;
        this.User_ID = User_ID;
        this.Activity_Tags = Activity_Tags;
    }

    // Getters and Setters
    public String getBeachName() {
        return Beach_Name;
    }
    public void setBeachName(String beachName) {
        this.Beach_Name = beachName;
    }

    public float getNumStars() {
        return Num_Stars;
    }
    public void setNumStars(float numStars) {
        this.Num_Stars = numStars;
    }

    public String getReviewComment() {
        return Review_Comment;
    }
    public void setReviewComment(String reviewComment) {
        this.Review_Comment = reviewComment;
    }

    public List<String> getReviewPictures() {
        return Review_Pictures;
    }
    public void setReviewPictures(List<String> reviewPictures) {
        this.Review_Pictures = reviewPictures;
    }

    public String getUserId() {
        return User_ID;
    }
    public void setUserId(String userId) {
        this.User_ID = userId;
    }

    public List<String> getActivityTags() {
        return Activity_Tags;
    }
    public void setActivityTags(List<String> activityTags) {
        this.Activity_Tags = activityTags;
    }

    // Calls the average stars to MainActivity
    public interface AverageStarsCallback {
        void onCallback(double averageStars);
    }

    public interface UploadCallback {
        void onSuccess();
        void onFailure(Exception e);

        void onFailure();
    }

    // Function that given the beachName, goes into the firebase database and calculates
    // average number of stars in all the reviews with that beachName
    public void getAverageReviewStars(String beachName, AverageStarsCallback callback) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/").getReference("Review");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int totalStars = 0;
                int reviewCount = 0;

                for (DataSnapshot reviewSnapshot : dataSnapshot.getChildren()) {
                    String currentBeachName = reviewSnapshot.child("Beach_Name").getValue(String.class);

                    if (beachName.equals(currentBeachName)) {
                        Long stars = reviewSnapshot.child("Num_Stars").getValue(Long.class);
                        if (stars != null) {
                            totalStars += stars;
                            reviewCount++;
                        }
                    }
                }

                double averageStars;
                if (reviewCount > 0) {
                    averageStars = (double) totalStars / reviewCount;
                } else {
                    averageStars = 0; // or return some other value to indicate no reviews
                }

                // Call the callback with the calculated average stars
                callback.onCallback(averageStars);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                System.err.println("Database error: " + databaseError.getMessage());
                callback.onCallback(0); // Handle error case by returning 0 or some error indicator
            }
        });
    }

    // Function to create a new review given the review attributes
    public void createNewReview(String beachName, float numStars, String reviewComment, List<String> reviewPictures, String userID, List<String> activityTags){
        DatabaseReference reviewsRef = database.getReference("Review");

        // Create a reviewID based off the database
        String reviewID = reviewsRef.push().getKey();

        Review newReview = new Review(
                beachName,
                numStars,
                reviewComment,
                reviewPictures,
                userID,
                activityTags
        );

        Log.d("ReviewUpload", "In Reviews, attempting to add review with stars: " + numStars);
        Log.d("ReviewUpload", "In Reviews, attempting to add review to: " + beachName);
        Log.d("ReviewUpload", "In Reviews, attempting to add review with comment: " + reviewComment);
        Log.d("ReviewUpload", "In Reviews, attempting to add review with tags: " + activityTags);
        Log.d("ReviewUpload", "In Reviews, attempting to add review with id: " + reviewID);

        if (reviewID != null && numStars > 0) {
            reviewsRef.child("Review").child(reviewID).setValue(newReview)
                    .addOnSuccessListener(aVoid -> {
                        // Successfully uploaded
                        System.out.println("SUCCESS: Review added with ID: " + reviewID);
                    })
                    .addOnFailureListener(e -> {
                        // Failed to upload
                        System.err.println("FAILURE: Failed to add review: " + e.getMessage());
                        Log.e("ReviewUpload", "Failed to add review: " + e.getMessage());
                    });
        }
    }

    public void createNewReview(String Beach_Name, float Num_Stars, String Review_Comment, List<String> Review_Picture, String User_ID, List<String> Activity_Tags, UploadCallback callback) {
        DatabaseReference reviewsRef = database.getReference("Review");

        // Create a reviewID based off the database
        String reviewID = reviewsRef.push().getKey();

        Review newReview = new Review(
                Beach_Name,
                Num_Stars,
                Review_Comment,
                Review_Picture,
                User_ID,
                Activity_Tags
        );

        if (reviewID != null && Num_Stars > 0) {
            Log.i("str", "Review id: " + reviewID);
            reviewsRef.child(reviewID).setValue(newReview)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            Log.i("str", "SUCCESS: Review uploaded");
                            callback.onSuccess();
                        } else {
                            Log.e("str", "FAILURE: Review not uploaded", task.getException());
                            callback.onFailure();
                        }
                    });
        } else {
            Log.e("str", "Invalid data: reviewID is null or numStars is <= 0");
        }

    }


    // Function to edit a review
    // Parameters will be null if there is nothing to change
    public void editReview(String reviewID, String newBeachName, float newNumStars, String newReviewComment, List<String> newReviewPictures, List<String> newActivityTags){
        DatabaseReference editReviewRef = database.getReference("Review").child(reviewID);

        // Check if each parameter is not null and update accordingly
        if (newBeachName != null) {
            editReviewRef.child("Beach_Name").setValue(newBeachName);
        }
        if (newNumStars > 0) {
            editReviewRef.child("Num_Stars").setValue(newNumStars);
        }
        if (newReviewComment != null) {
            editReviewRef.child("Review_Comment").setValue(newReviewComment);
        }
        if (newReviewPictures != null) {
            editReviewRef.child("Review_Pictures").setValue(newReviewPictures);
        }
        if (newActivityTags != null) {
            editReviewRef.child("Activity_Tags").setValue(newActivityTags);
        }

        // Adding a listener for success/failure of the updates
        editReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Review successfully updated
                System.out.println("Review updated successfully with ID: " + reviewID);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to update
                System.err.println("Failed to update review: " + databaseError.getMessage());
            }
        });


    }

    public void editReview(String reviewID, String newBeachName, float newNumStars, String newReviewComment, List<String> newReviewPictures, List<String> newActivityTags, UploadCallback callback) {
        DatabaseReference editReviewRef = database.getReference("Review").child(reviewID);

        // Check if each parameter is not null and update accordingly
        // Track if each update succeeds
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger expectedUpdates = new AtomicInteger(0);
        AtomicBoolean failureOccurred = new AtomicBoolean(false);

        // Helper method to track success/failure for each update
        DatabaseReference.CompletionListener listener = new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error != null) {
                    failureOccurred.set(true);
                    if (callback != null) {
                        callback.onFailure(new Exception(error.getMessage())); // Pass an Exception here
                    }
                } else {
                    successCount.incrementAndGet();
                    if (successCount.get() == expectedUpdates.get() && !failureOccurred.get()) {
                        if (callback != null) {
                            callback.onSuccess(); // Call onSuccess when all updates are successful
                        }
                    }
                }
            }
        };

        if (newBeachName != null) {
            expectedUpdates.incrementAndGet();
            editReviewRef.child("Beach_Name").setValue(newBeachName, listener);
        }
        if (newNumStars > 0) {
            expectedUpdates.incrementAndGet();
            editReviewRef.child("Num_Stars").setValue(newNumStars, listener);
        }
        if (newReviewComment != null) {
            expectedUpdates.incrementAndGet();
            editReviewRef.child("Review_Comment").setValue(newReviewComment, listener);
        }
        if (newReviewPictures != null) {
            expectedUpdates.incrementAndGet();
            editReviewRef.child("Review_Pictures").setValue(newReviewPictures, listener);
        }
        if (newActivityTags != null) {
            expectedUpdates.incrementAndGet();
            editReviewRef.child("Activity_Tags").setValue(newActivityTags, listener);
        }

        // If there were no updates to make, call success immediately
        if (expectedUpdates.get() == 0 && callback != null) {
            callback.onSuccess();
        }
    }


    // Function to delete a review
    public void deleteReview(String reviewID) {
        DatabaseReference deleteReviewRef = database.getReference("Review").child(reviewID);

        // Delete the review
        deleteReviewRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Successfully deleted
                    System.out.println("Review deleted successfully with ID: " + reviewID);
                })
                .addOnFailureListener(e -> {
                    // Failed to delete
                    System.err.println("Failed to delete review: " + e.getMessage());
                });
    }
}
