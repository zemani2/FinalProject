package com.example.anxietyByHeartRate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class HeartRateActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static final int REQUEST_PERMISSIONS_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.FOREGROUND_SERVICE_HEALTH,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.HIGH_SAMPLING_RATE_SENSORS
    };

    private TextView heartRateTextView;
    private int heartRate = 60;
    private Handler handler = new Handler();
    private FirebaseFirestore db;
    private LinkedList<HeartRateStamp> HeartRateData;
    private Button startStopButton;
    private static boolean isServiceRunning = false;
    private VideoView videoView;
    private ConstraintLayout constraintLayout;

    @Nullable
    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        return super.startActionMode(callback);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate);
        db = FirebaseFirestore.getInstance();

        heartRateTextView = findViewById(R.id.heartRateTextView);
        startStopButton = findViewById(R.id.startStopButton);
        videoView = findViewById(R.id.videoView);
        constraintLayout = findViewById(R.id.constraintLayout);

        videoView.setVideoPath("android.resource://" + getPackageName() + "/" + R.raw.heart_rate_video_2);
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (isServiceRunning) {
                    videoView.start();
                }
            }
        });

        // Check and request necessary permissions
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        } else {
            initializeHeartRateService();
        }
    }

    // Check if all required permissions are granted
    private boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Initialize the heart rate service and other components
    private void initializeHeartRateService() {
        try {
            readHeartRateData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        heartRate = HeartRateData.remove(HeartRateData.size() - 1).beatsPerMinute;

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning) {
                    stopHeartRateService();
                } else {
                    startHeartRateService();
                }
            }
        });

        setInitialHeartRateTextView();

        IntentFilter filter = new IntentFilter();
        filter.addAction("HEART_RATE_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(heartRateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(heartRateReceiver, filter);
        }
    }

    // Handle the permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.length > 0 && areAllPermissionsGranted(grantResults)) {
                initializeHeartRateService();
            } else {
                // Handle the case where the user denies the permission
                // You can show a message or disable certain features
            }
        }
    }

    // Check if all permissions are granted
    private boolean areAllPermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void setInitialHeartRateTextView() {
        if (!isServiceRunning) {
            heartRateTextView.setText("Tap Start to start measuring");
        } else {
            heartRateTextView.setText("Heart Rate: " + heartRate + " BPM");
        }
    }

    private void startHeartRateService() {
        Intent serviceIntent = new Intent(this, HeartRateService.class);
        startService(serviceIntent);
        isServiceRunning = true;
        startStopButton.setText("Stop");
        heartRateTextView.setText("Heart Rate: " + heartRate + " BPM");

        moveButtonToBottom();

        videoView.setVisibility(View.VISIBLE);
        videoView.start();
    }

    private void stopHeartRateService() {
        Intent serviceIntent = new Intent(this, HeartRateService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
        startStopButton.setText("Start");

        moveButtonToCenter();

        videoView.stopPlayback();
        videoView.resume();
        videoView.setVisibility(View.GONE);

        setInitialHeartRateTextView();
    }

    private void moveButtonToBottom() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.clear(R.id.startStopButton, ConstraintSet.TOP);
        constraintSet.connect(R.id.startStopButton, ConstraintSet.TOP, R.id.heartRateTextView, ConstraintSet.BOTTOM);
        constraintSet.applyTo(constraintLayout);
    }

    private void moveButtonToCenter() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.clear(R.id.startStopButton, ConstraintSet.BOTTOM);
        constraintSet.connect(R.id.startStopButton, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        constraintSet.connect(R.id.startStopButton, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        constraintSet.setVerticalBias(R.id.startStopButton, 0.5f);
        constraintSet.applyTo(constraintLayout);
    }

    private void readHeartRateData() throws IOException {
        HeartRateData = new LinkedList<>();
        InputStream is = this.getResources().openRawResource(R.raw.data);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        reader.readLine();
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            int beatsPerMinute = Integer.parseInt(parts[parts.length - 1]);
            String time = parts[parts.length - 2];
            HeartRateData.push(new HeartRateStamp(time, beatsPerMinute));
        }
        reader.close();
    }

    private BroadcastReceiver heartRateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("HEART_RATE_UPDATE".equals(intent.getAction())) {
                int heartRate = intent.getIntExtra("HEART_RATE", 0);
                if (isServiceRunning) {
                    heartRateTextView.setText("Heart Rate: " + heartRate + " BPM");
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (isServiceRunning) {
            startStopButton.setText("Stop");
            heartRateTextView.setText("Heart Rate: " + heartRate + " BPM");
            moveButtonToBottom();
            videoView.setVisibility(View.VISIBLE);
            videoView.start();
        } else {
            videoView.stopPlayback();
            videoView.setVisibility(View.GONE);
            setInitialHeartRateTextView();
        }
    }

    static class HeartRateStamp {
        int beatsPerMinute;
        String isoDate;

        public HeartRateStamp(String isoDate, int beatsPerMinute) {
            this.beatsPerMinute = beatsPerMinute;
            this.isoDate = isoDate;
        }
    }
}
