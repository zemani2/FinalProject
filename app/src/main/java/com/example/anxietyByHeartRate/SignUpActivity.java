package com.example.anxietyByHeartRate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    EditText email, password, repassword;
    Button signup, signin;
    private Spinner ageSpinner;
    private Spinner heightSpinner;
    private Spinner weightSpinner;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        repassword = findViewById(R.id.repassword);
        signin = findViewById(R.id.signin);
        signup = findViewById(R.id.signup);
        heightSpinner = findViewById(R.id.heightSpinner);
        weightSpinner = findViewById(R.id.weightSpinner);
        ageSpinner = findViewById(R.id.ageSpinner);

        // Setup age spinner
        List<String> ageOptions = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            ageOptions.add(String.valueOf(i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ageOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ageSpinner.setAdapter(adapter);
        ageSpinner.setSelection(19); // Set default age to 20

        // Setup weight spinner
        List<String> weightOptions = new ArrayList<>();
        for (int i = 30; i <= 120; i++) {
            weightOptions.add(String.valueOf(i) + " kg");
        }
        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weightOptions);
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weightSpinner.setAdapter(weightAdapter);
        weightSpinner.setSelection(40); // Set default weight to 70

        // Setup height spinner
        List<String> heightOptions = new ArrayList<>();
        for (int i = 70; i <= 200; i++) {
            heightOptions.add(String.valueOf(i) + " cm");
        }
        ArrayAdapter<String> heightAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, heightOptions);
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        heightSpinner.setAdapter(heightAdapter);
        heightSpinner.setSelection(100); // Set default height to 170

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailText = email.getText().toString();
                String pass = password.getText().toString();
                String repass = repassword.getText().toString();
                String firstName = ((EditText)findViewById(R.id.firstName)).getText().toString();
                String lastName = ((EditText)findViewById(R.id.lastName)).getText().toString();
                int age = Integer.parseInt(ageSpinner.getSelectedItem().toString());
                int weight = Integer.parseInt(weightSpinner.getSelectedItem().toString().replaceAll("[^0-9]", ""));
                int height = Integer.parseInt(heightSpinner.getSelectedItem().toString().replaceAll("[^0-9]", ""));

                if (emailText.equals("") || pass.equals("") || repass.equals("")) {
                    Toast.makeText(SignUpActivity.this, "Please enter all the fields", Toast.LENGTH_SHORT).show();
                } else {
                    if (pass.equals(repass)) {
                        createAccount(emailText, pass, age, weight, height, firstName, lastName);
                    } else {
                        Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void createAccount(String emailText, String pass, int age, int weight, int height, String firstName, String lastName) {
        mAuth.createUserWithEmailAndPassword(emailText, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                saveUserData(userId, emailText, age, weight, height, firstName, lastName);
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

    private void saveUserData(String userId, String email, int age, int weight, int height, String firstName, String lastName) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("age", age);
        userData.put("weight", weight);
        userData.put("height", height);
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);

        db.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("Firestore", "User data added successfully");
                        } else {
                            Log.d("Firestore", "Error adding user data: " + task.getException().getMessage());
                        }
                    }
                });
    }

}
