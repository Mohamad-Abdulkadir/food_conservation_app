package com.example.food_conservation;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class add_product_page extends AppCompatActivity {
    private ActivityResultLauncher<Intent> selectImageLauncher;
    private EditText foodName, foodDescription;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private Spinner spinnerCities, spinnerDistricts;
    private Uri imageUri;
    private ImageView imageView;
    private DBHandler dbHandler;
    private StorageReference storageRef;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product_page);

        imageView = findViewById(R.id.selectedImageView);
        foodName = findViewById(R.id.food_name);
        foodDescription = findViewById(R.id.food_description);
        datePicker = findViewById(R.id.datePicker);
        timePicker = findViewById(R.id.timePicker);
        spinnerCities = findViewById(R.id.spinnerCountries);
        spinnerDistricts = findViewById(R.id.spinnerDistricts);
        Button selectImageButton = findViewById(R.id.selectImageButton);
        Button addButton = findViewById(R.id.add);

        dbHandler = new DBHandler(); // Assuming this initializes Firestore connection
        storageRef = FirebaseStorage.getInstance().getReference(); // Get Firebase Storage reference

        // Image selection setup
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        imageView.setImageURI(imageUri);
                        imageView.setVisibility(ImageView.VISIBLE);
                    }
                });

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            selectImageLauncher.launch(intent);
        });

        addButton.setOnClickListener(v -> uploadImageAndSaveData());

        setupBottomNavigationView();
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

    private void uploadImageAndSaveData() {
        if (imageUri != null) {
            final StorageReference fileRef = storageRef.child("images/" + UUID.randomUUID().toString());
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                savePostDetails(imageUrl);
                            }))
                    .addOnFailureListener(e -> Toast.makeText(add_product_page.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            savePostDetails(null);
        }
    }

    private void savePostDetails(String imageUrl) {
        String name = foodName.getText().toString().trim();
        String description = foodDescription.getText().toString().trim();
        String city = spinnerCities.getSelectedItem().toString();
        String district = spinnerDistricts.getSelectedItem().toString();

        // Formatting date and time
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        String expireDate = day + "/" + (month + 1) + "/" + year; // month + 1 because January is zero
        String expireTime = hour + ":" + minute;

        if (!name.isEmpty() && !description.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            String userPhoneNumber = prefs.getString("userPhoneNumber", null);
            String userFullName = prefs.getString("userFullName", null);
            Map<String, Object> post = new HashMap<>();
            post.put("name", name);
            post.put("description", description);
            post.put("expireDate", expireDate);
            post.put("expireTime", expireTime);
            post.put("city", city);
            post.put("district", district);
            post.put("image", imageUrl);
            String donorDetails = "Donor Name: " + userFullName + "\nDonor Phone Number:" + userPhoneNumber;
            post.put("donorDetails", donorDetails);
            post.put("availability", "available");
            post.put("userPhoneNumber", userPhoneNumber);// Can be null if no image was selected

            dbHandler.addPost(post)
                    .addOnSuccessListener(aVoid -> Toast.makeText(add_product_page.this, "Post added successfully", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(add_product_page.this, "Failed to add post: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Name and description cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }


}
