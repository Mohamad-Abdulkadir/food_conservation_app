package com.example.food_conservation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class home_page extends AppCompatActivity {

    private static final String TAG = "HomePage";
    private LinearLayout cardsHolder;
    private Spinner citySpinner;
    private ImageButton filterButton;
    private DBHandler dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        dbHandler = new DBHandler();
        cardsHolder = findViewById(R.id.cards_holder);
        citySpinner = findViewById(R.id.city_spinner);
        filterButton = findViewById(R.id.filter_button);

        setupBottomNavigationView();
        fetchAndDisplayPosts();  // Fetch and display all posts initially

        filterButton.setOnClickListener(v -> {
            String selectedCity = citySpinner.getSelectedItem().toString();
            filterPostsByCity(selectedCity);  // Filter posts when filter button is clicked
        });
    }

    private void fetchAndDisplayPosts() {
        dbHandler.getAllPosts(this::displayPosts, error -> {
            Log.e(TAG, "Failed to fetch posts", error);
            showError("Failed to fetch posts: " + error.getMessage());
        });
    }

    private void filterPostsByCity(String city) {
        dbHandler.getPostsByLocation(city, this::displayPosts, error -> {
            Log.e(TAG, "Failed to fetch posts for city " + city, error);
            showError("Failed to fetch posts for city: " + error.getMessage());
        });
    }

    private void displayPosts(List<DBHandler.Post> posts) {
        LayoutInflater inflater = LayoutInflater.from(this);
        cardsHolder.removeAllViews();
        for (DBHandler.Post post : posts) {
            if(! post.getAvailability().equals("unavailable")) {
                View postCard = inflater.inflate(R.layout.post_card, cardsHolder, false);
                populatePostCard(postCard, post);
                cardsHolder.addView(postCard);
            }
        }
    }

    private void populatePostCard(View postCard, DBHandler.Post post) {
        TextView itemName = postCard.findViewById(R.id.item_name);
        TextView itemDescription = postCard.findViewById(R.id.item_description);
        ImageView postImage = postCard.findViewById(R.id.post_image);
        TextView contactDetails = postCard.findViewById(R.id.contact_details);
        TextView expiryDetails = postCard.findViewById(R.id.expiry_date);
        TextView city = postCard.findViewById(R.id.item_city);

        itemName.setText(post.getName());
        itemDescription.setText(post.getDescription());
        expiryDetails.setText("Expires on: " + post.getExpireDate() + " at " + post.getExpireTime());
        contactDetails.setText(post.getDonorDetails());
        city.setText(post.getCity());
        Glide.with(this).load(post.getImage()).into(postImage);
    }

    private void setupBottomNavigationView() {
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

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
