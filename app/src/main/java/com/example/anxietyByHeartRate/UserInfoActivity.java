package com.example.anxietyByHeartRate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class UserInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        Intent intent = getIntent();
        String username = intent.getStringExtra("USERNAME");
        int age = intent.getIntExtra("AGE", 0);
        int weight = intent.getIntExtra("WEIGHT", 0);
        int height = intent.getIntExtra("HEIGHT", 0);

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        TextView ageTextView = findViewById(R.id.ageTextView);
        TextView weightTextView = findViewById(R.id.weightTextView);
        TextView heightTextView = findViewById(R.id.heightTextView);

        usernameTextView.setText("Username: " + username);
        ageTextView.setText("Age: " + age);
        weightTextView.setText("Weight: " + weight + " kg");
        heightTextView.setText("Height: " + height + " cm");
    }

}
