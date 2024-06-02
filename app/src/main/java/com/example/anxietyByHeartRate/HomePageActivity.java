package com.example.anxietyByHeartRate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class HomePageActivity extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        Intent intent = getIntent();
        String email = intent.getStringExtra("EMAIL");
        String age = intent.getStringExtra("AGE");
        String weight = intent.getStringExtra("WEIGHT");
        String height = intent.getStringExtra("HEIGHT");

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String firstName = document.getString("firstName");
                                if (firstName != null) {
                                    // Use the first name as needed
                                    usernameTextView.setText("Welcome, " + firstName + "!");
                                } else {
                                    Log.d("Error: firstName", "couldn't get the name from the db in firebase");
                                }
                            }
                        } else {
                            System.out.println("No document found with the given email.");
                        }
                    } else {
                        System.out.println("Task failed with exception: " + task.getException());
                    }
                });

        Button myInfoButton = findViewById(R.id.myInfoButton);
        Button heartRateButton = findViewById(R.id.heartRateButton);
        Button historyButton = findViewById(R.id.historyButton); // Reference to the History Button

        heartRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePageActivity.this, HeartRateActivity.class);
                startActivity(intent);
            }
        });

        myInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePageActivity.this, UserInfoActivity.class);
                intent.putExtra("EMAIL", email);
                startActivity(intent);
            }
        });
        @SuppressLint("WrongViewCast") ImageButton logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform logout operation
                logoutUser();
            }
        });
        // Open History Activity when History Button is clicked
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePageActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
    }
    private void logoutUser() {
        // Perform logout operations here, such as clearing the session, updating UI, etc.
        // For example, you might want to clear any stored user credentials or tokens.

        // After performing logout operations, navigate back to the login screen
        Intent intent = new Intent(HomePageActivity.this, StartActivity.class);
        // Clear the activity stack so the user can't navigate back to the home screen using the back button
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // Finish the current activity
        finish();
    }
}
