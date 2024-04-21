package com.example.food_conservation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class profile_page extends AppCompatActivity {

    private EditText fullname, username, email, phoneNumber, password;
    private Button updateProfileButton;
    private Button donatedBTN;
    private LinearLayout cardsHolder;
    private DBHandler dbHandler;
    private String userPhoneNumber; // Assuming this is set and available

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        dbHandler = new DBHandler();
        setupUI();
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userPhoneNumber = prefs.getString("userPhoneNumber", null);

        if (userPhoneNumber == null) {
            Toast.makeText(this, "User phone number is not available.", Toast.LENGTH_SHORT).show();
            return; // Exit if phone number isn't available
        }

        fetchUserProfile(userPhoneNumber);
        fetchUserPosts();

        updateProfileButton.setOnClickListener(v -> updateUserProfile());
    }

    private void setupUI() {
        fullname = findViewById(R.id.fullname);
        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        phoneNumber = findViewById(R.id.phoneNumber);
        password = findViewById(R.id.password);
        updateProfileButton = findViewById(R.id.updateProfileBTN);
        cardsHolder = findViewById(R.id.cards_holder);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                startActivity(new Intent(getApplicationContext(), home_page.class));
            } else if (id == R.id.donate_add) {
                startActivity(new Intent(getApplicationContext(), add_product_page.class));
            } else if (id == R.id.account) {
                startActivity(new Intent(getApplicationContext(), profile_page.class));
            }
            return false;
        });
    }

    private void fetchUserProfile(String phoneNumber) {
        dbHandler.getUserByPhoneNumber(phoneNumber, user -> {
            fullname.setText(user.getFullName());
            username.setText(user.getUsername());
            email.setText(user.getEmail());
            this.phoneNumber.setText(user.getPhoneNumber());
            password.setText(user.getPassword()); // Note: Handling passwords like this is insecure
        }, error -> Toast.makeText(getApplicationContext(), "Failed to fetch user details: " + error, Toast.LENGTH_LONG).show());
    }

    private void updateUserProfile() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullname", fullname.getText().toString());
        updates.put("username", username.getText().toString());
        updates.put("email", email.getText().toString());
        updates.put("phoneNumber", phoneNumber.getText().toString());
        updates.put("password", password.getText().toString()); // Note: It's generally insecure to store passwords directly in Firestore.

        String userEmail = email.getText().toString();

        dbHandler.updateUser(userEmail, updates, successMessage -> {
            Toast.makeText(getApplicationContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
        }, errorMessage -> {
            Toast.makeText(getApplicationContext(), "Failed to update profile: " + errorMessage, Toast.LENGTH_LONG).show();
        });
    }


    private void fetchUserPosts() {
        dbHandler.getPostsByUserPhone(userPhoneNumber, posts -> {
            runOnUiThread(() -> {
                LayoutInflater inflater = LayoutInflater.from(this);
                cardsHolder.removeAllViews(); // Clear the holder before adding new views
                for (DBHandler.Post post : posts) {
                    View card = inflater.inflate(R.layout.history_card, null, false);
                    populatePostCard(card, post);
                    cardsHolder.addView(card);
                }
            });
        }, error -> Toast.makeText(getApplicationContext(), "Failed to fetch posts: " + error, Toast.LENGTH_LONG).show());
    }


    private void populatePostCard(View card, DBHandler.Post post) {
        TextView itemName = card.findViewById(R.id.item_name);
        TextView itemDescription = card.findViewById(R.id.item_description);
        TextView expiryDate = card.findViewById(R.id.expiry_date);
        TextView itemCity = card.findViewById(R.id.item_city);
        TextView contactDetails = card.findViewById(R.id.contact_details);
        ImageView postImage = card.findViewById(R.id.post_image);
        Button donatedBtn = card.findViewById(R.id.DonatedBTN);

        itemName.setText(post.getName());
        itemDescription.setText(post.getDescription());
        expiryDate.setText("Expires on: " + post.getExpireDate() + " at " + post.getExpireTime());
        itemCity.setText(post.getCity());
        contactDetails.setText(post.getDonorDetails());
        Glide.with(this).load(post.getImage()).into(postImage);

        if ("unavailable".equals(post.getAvailability())) {
            donatedBtn.setText("Donated");
            donatedBtn.setEnabled(false);
        } else {
            donatedBtn.setOnClickListener(v -> {
                updatePostAsDonated(post, donatedBtn);
            });
        }
    }

    private void updatePostAsDonated(DBHandler.Post post, Button donatedBtn) {
        // Assume DBHandler.Post has a getter for phoneNumber and name
        String phoneNumber = userPhoneNumber; // Make sure Post model has a phoneNumber field or fetch it appropriately
        String name = post.getName();

        // Update the post's availability to "unavailable" based on userPhoneNumber and post name
        dbHandler.updatePostAvailability(phoneNumber, name, "unavailable",
                unused -> runOnUiThread(() -> {
                    donatedBtn.setText("Donated");
                    donatedBtn.setEnabled(false);
                    Toast.makeText(getApplicationContext(), "Marked as donated", Toast.LENGTH_SHORT).show();
                }),
                error -> runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to mark as donated: " + error.getMessage(), Toast.LENGTH_LONG).show();
                })
        );
    }



}
