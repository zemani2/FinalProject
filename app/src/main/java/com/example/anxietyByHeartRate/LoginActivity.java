package com.example.anxietyByHeartRate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * LoginActivity handles user authentication by allowing the user to sign in using their email
 * and password. Once authenticated, it fetches the user's information from Firebase Firestore
 * and navigates to the home page.
 */
public class LoginActivity extends AppCompatActivity {

    // UI components for user input
    TextInputLayout emailInputLayout, passwordInputLayout;
    Button btnlogin;

    // Firebase authentication and Firestore database instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /**
     * Called when the activity is first created. Initializes UI components and Firebase instances.
     *
     * @param savedInstanceState The saved instance state from a previous instance, if any.
     */
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();  // Initialize Firebase authentication
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        // Bind UI components
        emailInputLayout = findViewById(R.id.emailLayout);
        passwordInputLayout = findViewById(R.id.passwordTextInputLayout);
        btnlogin = findViewById(R.id.btnSignIn);

        // Set up the login button click event
        btnlogin.setOnClickListener(view -> {
            String email = emailInputLayout.getEditText().getText().toString();
            String pass = passwordInputLayout.getEditText().getText().toString();

            // Validate user input
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter all the fields", Toast.LENGTH_SHORT).show();
            } else {
                signIn(email, pass); // Proceed with the sign-in process
            }
        });
    }

    /**
     * Handles the sign-in process using Firebase Authentication. Once signed in, it retrieves
     * user details from Firestore and stores them in SharedPreferences for future use.
     *
     * @param email    The email address entered by the user.
     * @param password The password entered by the user.
     */
    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign-in success, retrieve user details
                            String userId = mAuth.getCurrentUser().getEmail(); // Using email as UID
                            db.collection("users").document(userId).get().addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    DocumentSnapshot document = task1.getResult();
                                    if (document.exists()) {
                                        // Fetch user details from Firestore
                                        String firstName = document.getString("firstName");
                                        String userType = document.getString("userType");

                                        // Save user details in SharedPreferences
                                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString("firstName", firstName);
                                        editor.putString("userType", userType);
                                        editor.apply();

                                        // Notify the user and navigate to the home page
                                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, HomePageActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // Sign-in failure
                            Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
