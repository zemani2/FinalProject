package com.example.anxietyByHeartRate;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity that displays user information and manages the display of user details.
 * Retrieves and displays user data from Firestore and handles updates from the EditFragment.
 */
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
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                // Assuming there's only one document matching the email
                                String firstName = document.getString("firstName");
                                String lastName = document.getString("lastName");
                                TextView usernameTextView = findViewById(R.id.usernameTextView);
                                usernameTextView.setText("Welcome " + firstName);

                                // Replace the fragment with InfoFragment
                                InfoFragment infoFragment = InfoFragment.newInstance();
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.fragmentContainerView, infoFragment, "InfoFragment")
                                        .commit();
                            }
                        } else {
                            Log.d("Firestore", "Error getting documents: ", task.getException());
                        }
                    });
        }
    }

    /**
     * Callback method invoked when data is saved in the EditFragment.
     * Updates the InfoFragment with the new data.
     *
     * @param firstName The updated first name of the user.
     * @param lastName  The updated last name of the user.
     */
    @Override
    public void onDataSaved(String firstName, String lastName) {
        InfoFragment infoFragment = (InfoFragment) getSupportFragmentManager().findFragmentByTag("InfoFragment");
        if (infoFragment != null) {
            infoFragment.updateData(firstName, lastName);
        }
    }
}
