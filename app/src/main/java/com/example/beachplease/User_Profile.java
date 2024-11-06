package com.example.beachplease;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Firebase;
import com.google.firebase.database.*;

import org.w3c.dom.Text;

class User {
    public String username;
    public String email;
    public String password;

    public User(String username, String email, String password){
        this.username = username;
        this.email = email;
        this.password = password;
    }
}

interface OnUserLogin{
    void onUserLoginCallback(String msg, String user_id);
}

interface OnUserRegister{
    void onUserRegisterCallback(String msg, String user_id);
}

interface OnExistingUserFind {
    void onExistingUserFound(String msg);
}

public class User_Profile extends AppCompatActivity {
    private String user_id;
    private String login_or_register = "register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // check if user is logged in
        SharedPreferences local_storage = getSharedPreferences("user_data", MODE_PRIVATE);
        String current_user_id = local_storage.getString("user_id", "-1");
        Log.i("String", current_user_id+" current user");
        if(current_user_id.equals("-1")){
            LinearLayout register_login_form = findViewById(R.id.register_login_form);
            register_login_form.setVisibility(View.VISIBLE);
            LinearLayout validated_user_content = findViewById(R.id.validated_user_content);
            validated_user_content.setVisibility(View.GONE);
            user_id = current_user_id;
        } else {
            LinearLayout register_login_form = findViewById(R.id.register_login_form);
            register_login_form.setVisibility(View.GONE);
            LinearLayout validated_user_content = findViewById(R.id.validated_user_content);
            validated_user_content.setVisibility(View.VISIBLE);
            user_id = current_user_id;
        }

        // listeners for footer button clicks
        ImageView home_button = findViewById(R.id.home_button);
        home_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(User_Profile.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // listener for login or register
        TextView register_option = (TextView) findViewById(R.id.register_option);
        TextView login_option = (TextView) findViewById(R.id.login_option);

        // register option
        register_option.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register_option.setTextColor(Color.WHITE);
                register_option.setBackgroundColor(Color.parseColor("#2a9d8f"));
                login_option.setTextColor(Color.BLACK);
                login_option.setBackgroundColor(Color.parseColor("#bee3db"));

                EditText email_field = (EditText) findViewById(R.id.email_field);
                email_field.setVisibility(View.VISIBLE);
                EditText username_field = (EditText) findViewById(R.id.username_field);
                username_field.setHint("Enter username");
                Button button = (Button) findViewById(R.id.register_login_button);
                button.setText("Register");

                login_or_register = "register";
            }
        });

        // login option
        login_option.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login_option.setTextColor(Color.WHITE);
                login_option.setBackgroundColor(Color.parseColor("#2a9d8f"));
                register_option.setTextColor(Color.BLACK);
                register_option.setBackgroundColor(Color.parseColor("#bee3db"));

                EditText email_field = (EditText) findViewById(R.id.email_field);
                email_field.setVisibility(View.GONE);
                EditText username_field = (EditText) findViewById(R.id.username_field);
                username_field.setHint("Enter username or email");
                Button button = (Button) findViewById(R.id.register_login_button);
                button.setText("Login");

                login_or_register = "login";
            }
        });

        // listen for login button click
        Button register_login_button = (Button) findViewById(R.id.register_login_button);
        register_login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(login_or_register.equals("register")){
                    registration_routine();
                } else {
                    login_routine();
                }
            }
        });

        // listen for logout button click
        Button logout_button = (Button) findViewById(R.id.logout_button);
        logout_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor local_storage_editor = local_storage.edit();
                local_storage_editor.putString("user_id", "-1");
                local_storage_editor.commit();
                user_id = "-1";
                LinearLayout validated_user_content = findViewById(R.id.validated_user_content);
                validated_user_content.setVisibility(View.GONE);
                LinearLayout register_login_form = findViewById(R.id.register_login_form);
                register_login_form.setVisibility(View.VISIBLE);
            }
        });

        Button viewPastReviewsButton = findViewById(R.id.view_past_reviews_button);
        viewPastReviewsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start ViewUserActivity
                Intent intent = new Intent(User_Profile.this, ViewUserActivity.class);
                startActivity(intent);
            }
        });

        // display user info if already logged in
        if(!user_id.equals("-1")){
            display_user_info(current_user_id);
        }
    }

    private void display_user_info(String user_id){
        // clear the login
        LinearLayout register_login_form = (LinearLayout) findViewById(R.id.register_login_form);
        register_login_form.setVisibility(View.GONE);
        LinearLayout validated_user_content = findViewById(R.id.validated_user_content);
        validated_user_content.setVisibility(View.VISIBLE);

        // fetch user object
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        DatabaseReference database_user_ref = database.getReference("User").child(user_id);

        // display username
        database_user_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User with this ID exists, check further if needed
                    String username = dataSnapshot.child("username").getValue(String.class);
                    String welcomeUser = "Hello " + username + "!";
                    runOnUiThread(() -> {
                        TextView tv = (TextView) findViewById(R.id.user_content_username);
                        tv.setVisibility(View.VISIBLE);
                        tv.setText(welcomeUser);
                    });
                } else {
                    Log.i("umm", "user error");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("umm", "db error");
            }
        });

        // display all user reviews

    }

    private void login_routine(){
        Log.i("str", "log");
        EditText username_email_field = (EditText) findViewById(R.id.username_field);
        EditText password_field = (EditText) findViewById(R.id.password_field);

        String username_email_input = username_email_field.getText().toString().strip();
        String password_input = password_field.getText().toString().strip();

        if(username_email_input.strip().equals("") || password_input.strip().equals("")){
            runOnUiThread(() -> {
                TextView error_msg = (TextView) findViewById(R.id.register_login_error_msg);
                error_msg.setText("All fields are required");
            });
        } else {
            validate_user(username_email_input, password_input, new OnUserLogin() {
                @Override
                public void onUserLoginCallback(String msg, String user_id) {
                    if(msg.equals("valid")){
                        runOnUiThread(() -> {
                            TextView error_msg = (TextView) findViewById(R.id.register_login_error_msg);
                            error_msg.setVisibility(View.GONE);

                            hideKeyboard(username_email_field);
                            hideKeyboard(password_field);

                            SharedPreferences local_storage = getSharedPreferences("user_data", MODE_PRIVATE);
                            SharedPreferences.Editor local_storage_editor = local_storage.edit();
                            local_storage_editor.putString("user_id", user_id);
                            local_storage_editor.commit();

                            display_user_info(user_id);
                        });
                    } else if(msg.equals("invalid")){
                        runOnUiThread(() -> {
                            TextView error_msg = (TextView) findViewById(R.id.register_login_error_msg);
                            error_msg.setText("Invalid credentials");
                        });
                    } else {
                        runOnUiThread(() -> {
                            TextView error_msg = (TextView) findViewById(R.id.register_login_error_msg);
                            error_msg.setText("Database error");
                        });
                    }
                }
            });
        }
    }

    private void registration_routine(){
        Log.i("str", "reg");
        // register
        EditText username_field = (EditText) findViewById(R.id.username_field);
        EditText email_field = (EditText) findViewById(R.id.email_field);
        EditText password_field = (EditText) findViewById(R.id.password_field);

        String username_input = username_field.getText().toString().strip();
        String email_input = email_field.getText().toString().strip();
        String password_input = password_field.getText().toString().strip();

        // check if all fields are good to go
        if(username_input.strip().equals("") || email_input.strip().equals("") || password_input.strip().equals("")){
            runOnUiThread(() -> {
                TextView error_msg = (TextView) findViewById(R.id.register_login_error_msg);
                error_msg.setText("All fields are required");
            });
        } else {
            check_user_exists(username_input, email_input, new OnExistingUserFind() {
                @Override
                public void onExistingUserFound(String msg) {
                    Log.i("str", msg);

                    if(msg.equals("taken")){
                        Log.i("reg", "taken");
                        runOnUiThread(() -> {
                            TextView error_msg = (TextView) findViewById(R.id.register_login_error_msg);
                            error_msg.setText("Username or email has already been taken.");
                        });
                    } else if(msg.equals("error")){
                        Log.i("reg", "error with user find");
                        runOnUiThread(() -> {
                            TextView error_msg = (TextView) findViewById(R.id.register_login_error_msg);
                            error_msg.setText("Database error");
                        });
                    } else {
                        Log.i("reg", "good to reg");
                        register_user(username_input, email_input, password_input, new OnUserRegister() {
                            @Override
                            public void onUserRegisterCallback(String msg, String user_id) {
                                Log.i("msg", msg);

                                if(msg.equals("error")){
                                    runOnUiThread(() -> {
                                        hideKeyboard(username_field);
                                        hideKeyboard(email_field);
                                        hideKeyboard(password_field);

                                        TextView error_msg = (TextView) findViewById(R.id.register_login_error_msg);
                                        error_msg.setText("Registration error");
                                    });
                                } else if(msg.equals("success")){
                                    runOnUiThread(() -> {
                                        TextView error_msg = (TextView) findViewById(R.id.register_login_error_msg);
                                        error_msg.setVisibility(View.GONE);

                                        hideKeyboard(username_field);
                                        hideKeyboard(email_field);
                                        hideKeyboard(password_field);

                                        SharedPreferences local_storage = getSharedPreferences("user_data", MODE_PRIVATE);
                                        SharedPreferences.Editor local_storage_editor = local_storage.edit();
                                        local_storage_editor.putString("user_id", user_id);
                                        local_storage_editor.commit();

                                        display_user_info(user_id);
                                    });
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void register_user(String new_username, String new_email, String password, OnUserRegister onUserRegister){
        // initialize database
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        DatabaseReference database_ref = database.getReference("User");

        // register user
        String user_id = database_ref.push().getKey();
        Log.i("str", user_id);
        User new_user = new User(new_username, new_email, password);
        if (user_id != null) {
            database_ref.child(user_id).setValue(new_user)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.i("str", "reg-success");
                            onUserRegister.onUserRegisterCallback("success", user_id);
                        } else {
                            Log.i("str", "reg-not-success");
                            onUserRegister.onUserRegisterCallback("error", "-1");
                        }
                    });
        }
    }

    private void check_user_exists(String new_username, String new_email, OnExistingUserFind onExistingUserFind){
        // initialize database
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        DatabaseReference database_ref = database.getReference("User");

        Log.i("str", "yayay");

        // see if user already exists
        database_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            boolean existing_user_found = false;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String username = userSnapshot.child("username").getValue(String.class);
                    String email = userSnapshot.child("email").getValue(String.class);
                    if (username.equals(new_username) || email.equals(new_email)) {
                        existing_user_found = true;
                    }
                }
                if(existing_user_found){
                    onExistingUserFind.onExistingUserFound("taken");
                } else {
                    onExistingUserFind.onExistingUserFound("success");
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                onExistingUserFind.onExistingUserFound("error");
            }
        });
    }

    private void validate_user(String username_email_input, String password_input, OnUserLogin onUserLogin){
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://beachplease-database-default-rtdb.firebaseio.com/");
        DatabaseReference database_ref = database.getReference("User");

        database_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean user_validated = false;
                String user_id = "-1";
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String username = userSnapshot.child("username").getValue(String.class);
                    String email = userSnapshot.child("email").getValue(String.class);
                    String password = userSnapshot.child("password").getValue(String.class);

                    Log.i("s", username);
                    Log.i("s", email);
                    Log.i("s", password);

                    Log.i("s", "u_in"+username_email_input);
                    Log.i("s", "p_in"+password_input);

                    if ((username.equals(username_email_input) || email.equals(username_email_input)) && password.equals(password_input)) {
                        user_validated = true;
                        user_id = userSnapshot.getKey();
                    }
                }
                if(user_validated){
                    onUserLogin.onUserLoginCallback("valid", user_id);
                } else {
                    onUserLogin.onUserLoginCallback("invalid", "-1");
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                onUserLogin.onUserLoginCallback("error", "-1");
            }
        });
    }
}