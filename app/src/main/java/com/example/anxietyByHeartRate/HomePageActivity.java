package com.example.anxietyByHeartRate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        Intent intent = getIntent();
        String username = intent.getStringExtra("USERNAME");
        String age = intent.getStringExtra("AGE");
        String weight = intent.getStringExtra("WEIGHT");
        String height = intent.getStringExtra("HEIGHT");

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        usernameTextView.setText("Welcome, " + username + "!");

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
                intent.putExtra("USERNAME", username);
                intent.putExtra("AGE", age);
                intent.putExtra("WEIGHT", weight);
                intent.putExtra("HEIGHT", height);
                startActivity(intent);
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
}
