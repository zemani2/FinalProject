package com.example.anxietyByHeartRate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

public class UserInfoActivity extends AppCompatActivity implements EditFragment.OnDataSavedListener {
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
        int height = Integer.parseInt(details.get("height"));

        TextView usernameTextView = findViewById(R.id.usernameTextView);
//        TextView ageTextView = findViewById(R.id.ageTextView);
//        TextView weightTextView = findViewById(R.id.weightTextView);
//        TextView heightTextView = findViewById(R.id.heightTextView);

        usernameTextView.setText("Welcome " + username);
//        ageTextView.setText("Age: " + age);
//        weightTextView.setText("Weight: " + weight + " kg");
//        heightTextView.setText("Height: " + height + " cm");

        // Pass data to the fragment
        InfoFragment infoFragment = InfoFragment.newInstance(age, weight, height, username);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, infoFragment, "InfoFragment") // Set a tag here
                .commit();
    }

    public void onDataSaved(int selectedAge, int selectedWeight, int selectedHeight) {
        // Pass the updated data to the previous fragment
        InfoFragment infoFragment = (InfoFragment) getSupportFragmentManager().findFragmentByTag("InfoFragment");
        if (infoFragment != null) {
            infoFragment.updateData(selectedAge, selectedWeight, selectedHeight);
        }
    }

}
