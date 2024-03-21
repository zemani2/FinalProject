package com.example.anxietyByHeartRate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {
    private DBHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        DB = new DBHelper(this);
        Intent intent = getIntent();
        String username = intent.getStringExtra("USERNAME");
        Map<String, String> details = DB.getDetails(username, new String[]{"age", "weight", "height"});
        int age = Integer.parseInt(details.get("age"));
        int weight = Integer.parseInt(details.get("weight"));
        int height = Integer.parseInt(details.get("weight"));

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        TextView ageTextView = findViewById(R.id.ageTextView);
        TextView weightTextView = findViewById(R.id.weightTextView);
        TextView heightTextView = findViewById(R.id.heightTextView);

        usernameTextView.setText("Welcome " + username);
        ageTextView.setText("Age: " + age);
        weightTextView.setText("Weight: " + weight + " kg");
        heightTextView.setText("Height: " + height + " cm");
    }

}
