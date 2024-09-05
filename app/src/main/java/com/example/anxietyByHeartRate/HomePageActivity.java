package com.example.anxietyByHeartRate;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
/**
 * HomePageActivity is the main screen displayed after the user signs in.
 * It presents various options such as viewing user information, reports, and (if applicable) accessing a parent dashboard.
 * The activity also handles user authentication, retrieves data from Firebase Firestore, and manages user-specific features.
 * Additionally, it requests location permissions and starts a location service after user authentication.
 *
 * Key Components:
 * - FirebaseAuth (mAuth): Handles authentication, allowing access to the current user's information.
 * - FirebaseFirestore (db): Firestore database instance used to retrieve user data like the first name and user type.
 * - ProgressBar (loadingIndicator): Indicates loading when retrieving data from Firestore.
 * - SharedPreferences (prefs): Used to cache user data (e.g., first name and user type) for faster access in future sessions.
 *
 * Main Features:
 * - Fetches user data (first name, user type) from Firestore on sign-in and updates the UI accordingly.
 * - Provides buttons for navigation to the user's information page, reports, and parent dashboard (if applicable).
 * - Manages sign-out functionality and redirects to the login screen upon logout.
 * - Starts a location service after authentication and permission requests.
 *
 * How it works:
 * - On creation, the activity fetches the user's data from Firestore and populates the UI, displaying their first name and enabling relevant features based on their user type (e.g., showing the parent dashboard button for parents).
 * - The activity also handles location permissions and starts a foreground location service.
 * - Listeners are set for navigation buttons to handle moving to other activities like ReportActivity and UserInfoActivity.
 * - A custom back button press behavior is implemented using OnBackPressedCallback.
 */
public class HomePageActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FirebaseAuth mAuth; // Firebase authentication instance
    private FirebaseFirestore db; // Firestore database instance
    private ProgressBar loadingIndicator; // Loading indicator for async operations

    /**
     * Called when the activity is first created.
     * It sets up UI elements and initializes Firebase instances for authentication and Firestore.
     * Also handles fetching and displaying user data, starting the location service, and managing navigation.
     *
     * @param savedInstanceState The saved state of the activity if it was previously paused or stopped.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase authentication
        db = FirebaseFirestore.getInstance(); // Initialize Firestore instance

        // UI elements
        TextView usernameTextView = findViewById(R.id.usernameTextView);
        Button myInfoButton = findViewById(R.id.myInfoButton);
        Button reportsButton = findViewById(R.id.reportsButton);
        Button myKidsButton = findViewById(R.id.myKidsButton);
        ImageButton logoutButton = findViewById(R.id.logoutButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this); // SharedPreferences to store cached data

        // Retrieve cached first name and user type if available
        String cachedFirstName = prefs.getString("firstName", null);
        String cachedUserType = prefs.getString("userType", null);

        // Display cached first name in the usernameTextView if available
        if (cachedFirstName != null) {
            usernameTextView.setText("Welcome, " + cachedFirstName + "!");
        }

        FirebaseUser currentUser = mAuth.getCurrentUser(); // Get the current logged-in user
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            loadingIndicator.setVisibility(View.VISIBLE); // Show loading indicator while fetching data

            // Fetch user data from Firestore
            db.collection("users")
                    .document(userEmail)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            loadingIndicator.setVisibility(View.GONE); // Hide loading indicator once data is fetched
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String firstName = document.getString("firstName");
                                    String userType = document.getString("userType");
                                    if (firstName != null) {
                                        usernameTextView.setText("Welcome, " + firstName + "!");
                                        prefs.edit().putString("firstName", firstName).apply(); // Cache first name
                                        startLocationService(); // Start location service after successful sign-in
                                    }
                                    if ("parent".equals(userType)) {
                                        myKidsButton.setVisibility(View.VISIBLE); // Show "My Kids" button if user is a parent
                                        prefs.edit().putString("userType", userType).apply(); // Cache user type
                                    }
                                } else {
                                    Log.d("HomePageActivity", "No such document found for user with email: " + userEmail);
                                }
                            } else {
                                Log.e("HomePageActivity", "Error getting user document", task.getException());
                            }
                        }
                    });
        } else {
            Log.d("HomePageActivity", "No user logged in.");
        }

        // Set up button listeners for navigation
        reportsButton.setOnClickListener(v -> {
            Intent reportIntent = new Intent(HomePageActivity.this, ReportActivity.class);
            startActivity(reportIntent);
        });

        myInfoButton.setOnClickListener(v -> {
            Intent myInfoIntent = new Intent(HomePageActivity.this, UserInfoActivity.class);
            startActivity(myInfoIntent);
        });

        myKidsButton.setOnClickListener(v -> {
            Intent myKidsIntent = new Intent(HomePageActivity.this, ParentDashboardActivity.class);
            startActivity(myKidsIntent);
        });

        // Set up logout button listener
        logoutButton.setOnClickListener(v -> logoutUser());

        // Handle back button press using OnBackPressedCallback
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Custom back button behavior can be implemented here
            }
        };
        // Request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Register the callback
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    /**
     * Signs out the current user and redirects to the login screen.
     */
    private void logoutUser() {
        mAuth.signOut();
        Intent loginIntent = new Intent(HomePageActivity.this, StartActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    /**
     * Starts the location service to track user location in the background.
     */
    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, serviceIntent); // Start location service in the foreground
    }
}
