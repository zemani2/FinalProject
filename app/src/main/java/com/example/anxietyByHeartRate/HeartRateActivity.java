package com.example.anxietyByHeartRate;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class HeartRateActivity extends AppCompatActivity {

    private TextView heartRateTextView;
    private int heartRate = 60; // Initial heart rate value (beats per minute)
    private Handler handler = new Handler();
    private DBHelper DB;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate);
        Intent intent = getIntent();
        if (intent != null) {
            username = intent.getStringExtra("USERNAME");
        }
        heartRateTextView = findViewById(R.id.heartRateTextView);

        // Retrieve serialized DBHelper instance from intent
        DB = new DBHelper(this);
        VideoView videoView = findViewById(R.id.videoView);
        videoView.setVideoPath("android.resource://" + getPackageName() + "/" + R.raw.heart_rate_video_2);
        videoView.start();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.start();
            }
        });


        // Simulate changing heart rate every second
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateHeartRate();
                handler.postDelayed(this, 1000); // Repeat every second
            }
        }, 1000);
    }

    private int updateCounter = 0;

    private void updateHeartRate() {
        // Simulate changing heart rate value
        heartRate += (int) (Math.random() * 11) - 5; // Change heart rate by -5 to +5 beats
        if (heartRate < 50) {
            heartRate = 50; // Ensure heart rate does not fall below 50
        } else if (heartRate > 75) {
            heartRate = 75; // Ensure heart rate does not exceed 75
        }
        heartRateTextView.setText(getString(R.string.heart_rate, heartRate)); // Update heart rate display

        // Increment the counter
        updateCounter++;

        // Save heart rate to the database every 2 seconds
        if (updateCounter % 2 == 0) {
            saveHeartRateToDatabase(heartRate);
//            DB.printTable("heart_rate");
        }
    }

    private void saveHeartRateToDatabase(int heartRate) {
        // Call the insertHeartRate method of DBHelper to save the heart rate to the database
        DB.insertHeartRate(username, heartRate);
    }

}
