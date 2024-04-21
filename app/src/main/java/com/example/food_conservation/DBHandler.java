package com.example.food_conservation;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DBHandler {

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    public DBHandler() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public void getUserByPhoneNumber(String phoneNumber, Consumer<User> onSuccess, Consumer<String> onFailure) {
        db.collection("Users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        User user = document.toObject(User.class);
                        onSuccess.accept(user);
                    } else {
                        onFailure.accept("No user found with the phone number: " + phoneNumber);
                    }
                })
                .addOnFailureListener(e -> onFailure.accept("Failed to fetch user: " + e.getMessage()));
    }

    public void fetchUserDetails(String phoneNumber, Consumer<User> onSuccess, Consumer<String> onFailure) {
        db.collection("Users").whereEqualTo("phone_number", phoneNumber)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        onSuccess.accept(user);
                    } else {
                        onFailure.accept("No user found with the phone number.");
                    }
                })
                .addOnFailureListener(e -> onFailure.accept(e.getMessage()));
    }

//    public void updateUser(String email, Map<String, Object> updates, Runnable onSuccess, Consumer<String> onFailure) {
//        db.collection("Users").document(email).update(updates)
//                .addOnSuccessListener(aVoid -> onSuccess.run())
//                .addOnFailureListener(e -> onFailure.accept(e.getMessage()));
//    }

    public void getPostsByUserPhone(String phoneNumber, Consumer<List<Post>> onSuccess, Consumer<String> onFailure) {
//        // First, get the user ID associated with the phone number
//        getUserByPhoneNumber(phoneNumber, user -> {
//            String userId = user.getId(); // Assuming User class has getUserId method
//            getPostsByUserId(userId, onSuccess, onFailure);
//        }, onFailure);
        db.collection("Posts")
                .whereEqualTo("userPhoneNumber", phoneNumber)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> posts = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        posts.add(document.toObject(Post.class));
                    }
                    onSuccess.accept(posts);
                })
                .addOnFailureListener(e -> onFailure.accept("Failed to fetch posts: " + e.getMessage()));
    }

    public void getPostsByUserId(String userId, Consumer<List<Post>> onSuccess, Consumer<String> onFailure) {
        db.collection("Posts")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> posts = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        posts.add(document.toObject(Post.class));
                    }
                    onSuccess.accept(posts);
                })
                .addOnFailureListener(e -> onFailure.accept("Failed to fetch posts: " + e.getMessage()));
    }

    public void updatePostAvailability(String phoneNumber, String name, String availability, Consumer<Void> onSuccess, Consumer<Exception> onError) {
        // Query posts by userPhoneNumber and name
        db.collection("Posts")
                .whereEqualTo("userPhoneNumber", phoneNumber)
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if query returned any documents
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Loop through the documents and update each one
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            DocumentReference postRef = document.getReference();
                            postRef.update("availability", availability)
                                    .addOnSuccessListener(aVoid -> onSuccess.accept(null))
                                    .addOnFailureListener(e -> {
                                        // Handle any exceptions during the update
                                        onError.accept(e);
                                    });
                        }
                    } else {
                        // No documents found
                        onError.accept(new Exception("No matching posts found"));
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any exceptions during the query
                    onError.accept(e);
                });
    }


    public void addUser(String email, String password, String username, String phoneNumber, Consumer<String> onSuccess, Consumer<Exception> onFailure) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("password", password);  // Consider using Firebase Authentication for better security
        user.put("username", username);
        user.put("phone_number", phoneNumber);

        db.collection("Users").document(email).set(user)
                .addOnSuccessListener(aVoid -> onSuccess.accept("User added successfully"))
                .addOnFailureListener(onFailure::accept);
    }

    public void updateUser(String email, Map<String, Object> updates, Consumer<String> onSuccess, Consumer<String> onFailure) {
        // Query the Users collection for documents where the "email" field matches the provided email
        db.collection("Users").whereEqualTo("email", email).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                if (!documents.isEmpty()) {
                    // Assuming email is unique and there's only one matching document
                    DocumentSnapshot document = documents.get(0);
                    DocumentReference userDocRef = document.getReference();

                    // Proceed to update the document
                    userDocRef.update(updates)
                            .addOnSuccessListener(aVoid -> onSuccess.accept("User details updated successfully"))
                            .addOnFailureListener(e -> onFailure.accept("Failed to update user details: " + e.getMessage()));
                } else {
                    onFailure.accept("No user found with email: " + email);
                }
            } else {
                onFailure.accept("Failed to retrieve user data: " + task.getException().getMessage());
            }
        });
    }



    public void addPost(String userId, String itemName, String availability, String itemDescription, String itemExpiryDate, byte[] imageData, String donorDetails, Consumer<String> onSuccess, Consumer<Exception> onFailure) {
        StorageReference imageRef = storage.getReference().child("post_images/" + userId + "/" + System.currentTimeMillis() + ".jpg");
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Map<String, Object> post = new HashMap<>();
            post.put("user_id", userId);
            post.put("item_name", itemName);
            post.put("item_description", itemDescription);
            post.put("item_expiry_date", itemExpiryDate);
            post.put("item_image", uri.toString());
            post.put("donor_details", donorDetails);
            post.put("availability", availability);// Update donor details
// Include donor details in the post

            db.collection("Posts").add(post)
                    .addOnSuccessListener(documentReference -> onSuccess.accept("Post added successfully"))
                    .addOnFailureListener(onFailure::accept);
        })).addOnFailureListener(onFailure::accept);
    }


    public Task<Void> addPost(Map<String, Object> post) {
        return db.collection("Posts").add(post).continueWith(task -> null);
    }

    public void updatePost(String postId, String itemName, String itemDescription, String itemExpiryDate, byte[] imageData, String donorDetails, String availability, Consumer<String> onSuccess, Consumer<Exception> onFailure) {
        StorageReference imageRef = storage.getReference().child("post_images/" + postId + "/" + System.currentTimeMillis() + ".jpg");
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("item_name", itemName);
            updates.put("item_description", itemDescription);
            updates.put("item_expiry_date", itemExpiryDate);
            updates.put("item_image", uri.toString());
            updates.put("donor_details", donorDetails);
            updates.put("availability", availability);// Update donor details

            db.collection("Posts").document(postId).update(updates)
                    .addOnSuccessListener(aVoid -> onSuccess.accept("Post updated successfully"))
                    .addOnFailureListener(onFailure::accept);
        })).addOnFailureListener(onFailure::accept);
    }
    public void getAllPosts(Consumer<List<Post>> onSuccess, Consumer<Exception> onFailure) {
        db.collection("Posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> posts = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        posts.add(document.toObject(Post.class));
                    }
                    onSuccess.accept(posts);
                })
                .addOnFailureListener(onFailure::accept);
    }

    public void getPostsByLocation(String location, Consumer<List<Post>> onSuccess, Consumer<Exception> onFailure) {
        db.collection("Posts")
                .whereEqualTo("city", location)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> posts = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        posts.add(document.toObject(Post.class));
                    }
                    onSuccess.accept(posts);
                })
                .addOnFailureListener(onFailure::accept);
    }

    // Define a Post class to handle Firestore data


    public static class Post {
        private String id; // Document ID from Firestore
        private String name;
        private String description;
        private String image;
        private String city;
        private String district;
        private String expireDate;
        private String expireTime;
        private String donorDetails;
        private String availability;

        public Post() {}

        @PropertyName("id")
        public String getId() { return id; }
        @PropertyName("id")
        public void setId(String id) { this.id = id; }

        @PropertyName("name")
        public String getName() {
            return name;
        }

        @PropertyName("name")
        public void setName(String name) {
            this.name = name;
        }

        @PropertyName("description")
        public String getDescription() {
            return description;
        }

        @PropertyName("description")
        public void setDescription(String description) {
            this.description = description;
        }

        @PropertyName("image")
        public String getImage() {
            return image;
        }

        @PropertyName("image")
        public void setImage(String image) {
            this.image = image;
        }

        @PropertyName("city")
        public String getCity() {
            return city;
        }

        @PropertyName("city")
        public void setCity(String city) {
            this.city = city;
        }

        @PropertyName("district")
        public String getDistrict() {
            return district;
        }

        @PropertyName("district")
        public void setDistrict(String district) {
            this.district = district;
        }

        @PropertyName("expireDate")
        public String getExpireDate() {
            return expireDate;
        }

        @PropertyName("expireDate")
        public void setExpireDate(String expireDate) {
            this.expireDate = expireDate;
        }

        @PropertyName("expireTime")
        public String getExpireTime() {
            return expireTime;
        }

        @PropertyName("expireTime")
        public void setExpireTime(String expireTime) {
            this.expireTime = expireTime;
        }

        @PropertyName("donorDetails")
        public String getDonorDetails() {
            return donorDetails;
        }

        @PropertyName("donorDetails")
        public void setDonorDetails(String donorDetails) {
            this.donorDetails = donorDetails;
        }
        @PropertyName("availability")
        public String getAvailability() {
            return availability;
        }

        @PropertyName("availability")
        public void setAvailability(String availability) {
            this.availability = availability;
        }
    }

    public static class User {
        private String id;
        private String fullName;
        private String username;
        private String email;
        private String phoneNumber;
        private String password;

        public String getId(){return id;}

        // Add constructors, getters, and setters here
        public String getFullName() {
            return fullName;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getPassword() {
            return password;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }


}
