package com.example.anxietyByHeartRate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomePageActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        Button myInfoButton = findViewById(R.id.myInfoButton);
        Button reportsButton = findViewById(R.id.reportsButton);
        Button historyButton = findViewById(R.id.historyButton);
        Button myKidsButton = findViewById(R.id.myKidsButton);
        ImageButton logoutButton = findViewById(R.id.logoutButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String cachedFirstName = prefs.getString("firstName", null);
        String cachedUserType = prefs.getString("userType", null);

        if (cachedFirstName != null) {
            usernameTextView.setText("Welcome, " + cachedFirstName + "!");
        }

        if ("parent".equals(cachedUserType)) {
            Log.d("HomePageActivity", "User Type is: " + cachedUserType);

            myKidsButton.setVisibility(View.VISIBLE);
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            loadingIndicator.setVisibility(View.VISIBLE); // Show loading indicator

            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            loadingIndicator.setVisibility(View.GONE); // Hide loading indicator
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String firstName = document.getString("firstName");
                                    String userType = document.getString("userType");
                                    if (firstName != null) {
                                        usernameTextView.setText("Welcome, " + firstName + "!");
                                        prefs.edit().putString("firstName", firstName).apply();
                                    } else {
                                        Log.d("HomePageActivity", "No first name found for user with ID: " + userId);
                                    }
                                    if ("parent".equals(userType)) {
                                        myKidsButton.setVisibility(View.VISIBLE);
                                        prefs.edit().putString("userType", userType).apply();
                                    }
                                } else {
                                    Log.d("HomePageActivity", "No such document found for user with ID: " + userId);
                                }
                            } else {
                                Log.e("HomePageActivity", "Error getting user document", task.getException());
                            }
                        }
                    });
        } else {
            Log.d("HomePageActivity", "No user logged in.");
        }

        // Button click listeners
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

        logoutButton.setOnClickListener(v -> logoutUser());

        historyButton.setOnClickListener(v -> {
            Intent historyIntent = new Intent(HomePageActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
        });
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent loginIntent = new Intent(HomePageActivity.this, StartActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}
