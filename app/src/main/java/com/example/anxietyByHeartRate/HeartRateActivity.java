package com.example.anxietyByHeartRate;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
public class HeartRateActivity extends AppCompatActivity {

    private TextView heartRateTextView;
    private int heartRate = 60; // Initial heart rate value (beats per minute)
    private Handler handler = new Handler();
    private DBHelper DB;
    private String username;
    private LinkedList<HeartRateStamp> HeartRateData;


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
        try {
            readHeartRateData(); // Assuming the file name is data.txt
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        heartRate = HeartRateData.remove(HeartRateData.size()-1).beatsPerMinute;

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
        heartRate = HeartRateData.remove(HeartRateData.size()-1).beatsPerMinute;
        heartRateTextView.setText(getString(R.string.heart_rate, heartRate)); // Update heart rate display

        // Increment the counter
        updateCounter++;

        // Save heart rate to the database every 2 seconds
        if (updateCounter % 2 == 0) {
            saveHeartRateToDatabase(heartRate);
//            DB.printTable("heart_rate");
        }
    }
    private void readHeartRateData() throws IOException {
        HeartRateData = new LinkedList<>();
        InputStream is = this.getResources().openRawResource(R.raw.data);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        // Skip the header
        reader.readLine();
        while ((line = reader.readLine()) != null) {
            // Split the line by tab character
            String[] parts = line.split(",");
            // Extract the beats per minute value (assuming it's the last column)
            int beatsPerMinute = Integer.parseInt(parts[parts.length - 1]);
            String time = parts[parts.length - 2];
            // Push the beats per minute value onto the stack
            HeartRateData.push(new HeartRateStamp(time, beatsPerMinute));
        }
        reader.close();

    }
    private void saveHeartRateToDatabase(int heartRate) {
        // Call the insertHeartRate method of DBHelper to save the heart rate to the database
        DB.insertHeartRate(username, heartRate);
    }
    static class HeartRateStamp{
        int beatsPerMinute;
        String isoDate;
        public HeartRateStamp(String isoDate, int beatsPerMinute) {
            this.beatsPerMinute = beatsPerMinute;
            this.isoDate = isoDate;
        }
    }

}
