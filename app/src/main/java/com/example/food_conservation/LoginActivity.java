package com.example.food_conservation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public static String userFullName; // Public variable for user's full name
    public static String userPhoneNumber; // Public variable for user's phone number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        Log.d("LoginActivity", "Firebase Auth and Firestore initialized");

        // Setup UI elements
        emailField = findViewById(R.id.login_email);
        passwordField = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login);
        TextView signUpPageButton = findViewById(R.id.signUpPageButton);
        Log.d("LoginActivity", "UI elements bound");

        // Login button click listener
        loginButton.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            loginUser(email, password);
        });

        // Sign up button click listener
        signUpPageButton.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, sign_up_page.class);
            startActivity(intent);
        });
    }

    private void loginUser(String email, String password) {
        if (!email.isEmpty() && !password.isEmpty()) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d("LoginActivity", "Authentication successful");
                            fetchUserDetails(email);
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                            Log.d("LoginActivity", "Authentication Failed");
                        }
                    });
        } else {
            Toast.makeText(this, "Email and password cannot be empty.", Toast.LENGTH_LONG).show();
        }
    }

    private void fetchUserDetails(String email) {
        db.collection("Users")
                .whereEqualTo("email", email)  // Assuming there's an 'email' field in the documents
                .limit(1)  // Assuming email is unique, limit to the first result
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);  // Get the first document
                        if (document.exists()) {
                            String userPhoneNumber = document.getString("phoneNumber");
                            String name = document.getString("fullName");
                            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("userPhoneNumber", userPhoneNumber);
                            editor.putString("userFullName", name);
                            editor.apply();

                            Intent intent = new Intent(LoginActivity.this, home_page.class);
                            startActivity(intent);
                            finish();  // Close the login activity
                        } else {
                            Toast.makeText(LoginActivity.this, "No user found with that email", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Failed to fetch user data";
                        Toast.makeText(LoginActivity.this, "Failed to fetch user details: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
