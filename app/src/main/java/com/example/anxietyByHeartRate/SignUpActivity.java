package com.example.anxietyByHeartRate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for user sign-up functionality, handling user registration and role assignment.
 */
public class SignUpActivity extends AppCompatActivity {
    EditText email, password, repassword, parentEmail;
    Button signup, signin;
    RadioButton kid, parent;
    RadioGroup radioGroup;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Authentication and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etPassword);
        repassword = findViewById(R.id.etRePassword);
        signin = findViewById(R.id.btnLogin);
        signup = findViewById(R.id.btnSignUp);
        kid = findViewById(R.id.rbKid);
        parent = findViewById(R.id.rbParent);
        radioGroup = findViewById(R.id.radioGroupRole);
        parentEmail = findViewById(R.id.etParentEmail);

        // Set visibility of parentEmail field based on selected role
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbKid) {
                parentEmail.setVisibility(View.VISIBLE);
            } else {
                parentEmail.setVisibility(View.GONE);
            }
        });

        // Handle sign-in button click
        signin.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        });

        // Handle sign-up button click
        signup.setOnClickListener(v -> {
            String emailText = email.getText().toString();
            String pass = password.getText().toString();
            String repass = repassword.getText().toString();
            String firstName = ((EditText) findViewById(R.id.etFirstName)).getText().toString();
            String lastName = ((EditText) findViewById(R.id.etLastName)).getText().toString();
            int selectedId = radioGroup.getCheckedRadioButtonId();
            String userType;
            String parentEmailText = parentEmail.getText().toString();

            // Determine user type based on selected role
            if (selectedId == parent.getId()) {
                userType = "parent";
            } else if (selectedId == kid.getId()) {
                userType = "kid";
            } else {
                userType = ""; // No selection made
            }

            // Validate input fields
            if (emailText.isEmpty() || pass.isEmpty() || repass.isEmpty() ||
                    (userType.equals("kid") && parentEmailText.isEmpty())) {
                Toast.makeText(SignUpActivity.this, "Please enter all the fields", Toast.LENGTH_SHORT).show();
            } else {
                if (pass.equals(repass)) {
                    if (userType.equals("kid")) {
                        checkParentEmailAndCreateAccount(parentEmailText, emailText, pass, firstName, lastName, userType);
                    } else {
                        createAccount(emailText, pass, firstName, lastName, userType);
                    }
                } else {
                    Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Checks if the parent email exists in Firestore and proceeds to create an account if found.
     *
     * @param parentEmailText The email of the parent.
     * @param emailText       The email of the kid.
     * @param pass            The password for the new account.
     * @param firstName       The first name of the user.
     * @param lastName        The last name of the user.
     * @param userType        The type of user (parent or kid).
     */
    private void checkParentEmailAndCreateAccount(String parentEmailText, String emailText, String pass, String firstName, String lastName, String userType) {
        db.collection("users").whereEqualTo("email", parentEmailText).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Parent email found, proceed with account creation
                            Log.d("Parent Email Check", "Parent email found: " + parentEmailText);
                            createAccount(emailText, pass, firstName, lastName, userType);
                            sendParentRequest(parentEmailText, emailText);
                        } else {
                            // Parent email not found
                            Log.d("Parent Email Check", "Parent email not found: " + parentEmailText);
                            Toast.makeText(SignUpActivity.this, "Parent email not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Error querying Firestore
                        Log.e("Parent Email Check", "Error checking parent email: " + task.getException().getMessage());
                        Toast.makeText(SignUpActivity.this, "Error checking parent email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Sends a request to the parent to add the kid to their list of children.
     *
     * @param parentEmail The email of the parent.
     * @param kidEmail    The email of the kid.
     */
    private void sendParentRequest(String parentEmail, String kidEmail) {
        db.collection("users")
                .whereEqualTo("email", parentEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            DocumentSnapshot parentDoc = task.getResult().getDocuments().get(0);
                            String parentId = parentDoc.getId();

                            Map<String, Object> request = new HashMap<>();
                            request.put("kidEmail", kidEmail);
                            request.put("status", "pending");

                            db.collection("users").document(parentId).collection("kids").document(kidEmail)
                                    .set(request, SetOptions.merge())
                                    .addOnCompleteListener(innerTask -> {
                                        if (innerTask.isSuccessful()) {
                                            Log.d("Firestore", "Parent request sent successfully");
                                            Toast.makeText(this, "Parent request sent successfully", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.d("Firestore", "Error sending parent request: " + innerTask.getException().getMessage());
                                            Toast.makeText(this, "Failed to send parent request", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Log.d("Firestore", "No user found with email: " + parentEmail);
                            Toast.makeText(this, "No user found with email: " + parentEmail, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("Firestore", "Error querying user: " + task.getException().getMessage());
                        Toast.makeText(this, "Error querying user", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Creates a new user account with the provided details.
     *
     * @param emailText  The email address for the new account.
     * @param pass       The password for the new account.
     * @param firstName  The first name of the user.
     * @param lastName   The last name of the user.
     * @param userType   The type of user (parent or kid).
     */
    private void createAccount(String emailText, String pass, String firstName, String lastName, String userType) {
        mAuth.createUserWithEmailAndPassword(emailText, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserData(emailText, firstName, lastName, userType);
                                Toast.makeText(SignUpActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SignUpActivity.this, HomePageActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this, "Sign Up Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            Log.d("Error: sign up", "Sign Up Failed: " + task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Saves the user data to Firestore and SharedPreferences.
     *
     * @param email      The email address of the user.
     * @param firstName  The first name of the user.
     * @param lastName   The last name of the user.
     * @param userType   The type of user (parent or kid).
     */
    private void saveUserData(String email, String firstName, String lastName, String userType) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("userType", userType);

        db.collection("users").document(email)
                .set(userData, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SignUpActivity.this);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("firstName", firstName);
                            editor.putString("userType", userType);
                            editor.apply();
                            Log.d("Firestore", "User data added successfully");
                        } else {
                            Log.d("Firestore", "Error adding user data: " + task.getException().getMessage());
                        }
                    }
                });
    }
}
