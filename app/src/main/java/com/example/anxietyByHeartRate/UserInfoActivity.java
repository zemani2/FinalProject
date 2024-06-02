package com.example.anxietyByHeartRate;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class UserInfoActivity extends AppCompatActivity implements EditFragment.OnDataSavedListener {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String email = currentUser.getEmail();
            String username = extractUsername(email);
            if (currentUser != null) {
                String userId = currentUser.getUid();
                getUserData(userId);
            }        }
    }

    private String extractUsername(String email) {
        // Extract username from email before the "@" symbol
        int atIndex = email.indexOf('@');
        if (atIndex != -1) {
            return email.substring(0, atIndex);
        } else {
            // If "@" symbol is not found, return full email
            return email;
        }
    }

    private void getUserData(String userId) {
            db.collection("users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                Map<String, Object> userDetails = document.getData();
                                if (userDetails != null) {
                                    String firstName = String.valueOf(userDetails.get("firstName"));

                                    TextView usernameTextView = findViewById(R.id.usernameTextView);
                                    usernameTextView.setText("Welcome " + firstName);

                                    InfoFragment infoFragment = InfoFragment.newInstance();
                                    getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.fragmentContainerView, infoFragment, "InfoFragment")
                                            .commit();
                                }
                            } else {
                                // Document doesn't exist
                                Log.d("Firestore", "No such document");
                            }
                        } else {
                            // Error fetching document
                            Log.d("Firestore", "get failed with ", task.getException());
                        }
                    });
        }

    public void onDataSaved(int selectedAge, int selectedWeight, int selectedHeight) {
        InfoFragment infoFragment = (InfoFragment) getSupportFragmentManager().findFragmentByTag("InfoFragment");
        if (infoFragment != null) {
            infoFragment.updateData(selectedAge, selectedWeight, selectedHeight);
        }
    }
}
