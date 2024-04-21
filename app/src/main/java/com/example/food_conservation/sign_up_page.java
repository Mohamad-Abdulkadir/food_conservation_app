package com.example.food_conservation;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class sign_up_page extends AppCompatActivity {

    private EditText editTextEmail, editTextUsername, editTextPassword, editTextFullName, editTextPhoneNumber;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_page);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);

        Button signUpButton = findViewById(R.id.buttonSignUp);
        signUpButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String fullName = editTextFullName.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password cannot be empty.", Toast.LENGTH_LONG).show();
            return;
        }

        // Firebase Authentication to create a user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Authentication success
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Store additional user information in Firestore
                            Map<String, Object> user = new HashMap<>();
                            user.put("username", username);
                            user.put("fullName", fullName);
                            user.put("email", email);
                            user.put("phoneNumber", phoneNumber);
                            user.put("password", password);

                            db.collection("Users").document(firebaseUser.getUid())
                                    .set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(sign_up_page.this, "User registration successful.", Toast.LENGTH_SHORT).show();
                                        Log.d("SignUpActivity", "User registration successful");
                                        // Navigate to LoginActivity
                                        Intent intent = new Intent(sign_up_page.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();  // Close the sign-up activity
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(sign_up_page.this, "Failed to save user details: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(sign_up_page.this, "Authentication failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        Log.d("SignUpActivity", "Auth Failed");
                    }
                });
    }
}
