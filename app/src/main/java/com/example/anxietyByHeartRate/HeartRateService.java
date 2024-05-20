package com.example.anxietyByHeartRate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class HeartRateService extends Service {

    private Handler handler = new Handler();
    private DBHelper DB;
    private String username;
    private LinkedList<HeartRateActivity.HeartRateStamp> HeartRateData;
    private int heartRate;
    private int updateCounter = 0;
    private static final String CHANNEL_ID = "HeartRateNotificationChannel";
    private static final String ACTION_INSERT_STRESS_DATA = "com.example.anxietyByHeartRate.ACTION_INSERT_STRESS_DATA";
    private static final String ACTION_DISMISS_NOTIFICATION = "com.example.anxietyByHeartRate.ACTION_DISMISS_NOTIFICATION";
    boolean inStress = false;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            int notificationId = intent.getIntExtra("NOTIFICATION_ID", -1); // Get notification ID
            if (ACTION_INSERT_STRESS_DATA.equals(action)) {
                // Insert stress data
                username = intent.getStringExtra("USERNAME");
                heartRate = intent.getIntExtra("HEART_RATE", 0);
                DB = new DBHelper(this);
                insertStressData(username, heartRate);

                // Cancel the notification
                if (notificationId != -1) {
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                    notificationManager.cancel(notificationId);
                }

                // Stop the service
//                stopSelf();
            } else if (ACTION_DISMISS_NOTIFICATION.equals(action)) {
                // Just cancel the notification
                if (notificationId != -1) {
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                    notificationManager.cancel(notificationId);
                }
            } else {
                username = intent.getStringExtra("USERNAME");
                DB = new DBHelper(this);
                try {
                    readHeartRateData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                createNotificationChannel();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateHeartRate();
                        handler.postDelayed(this, 1000); // Repeat every second
                    }
                }, 1000);

                // Start the service in the foreground to keep it running
                Notification notification = getNotification("Heart Rate Service Running").build();
                startForeground(1, notification);
            }
        }
        return START_STICKY;
    }

    private void updateHeartRate() {
        if (!HeartRateData.isEmpty()) {
            heartRate = HeartRateData.remove(HeartRateData.size() - 1).beatsPerMinute;
            updateCounter++;
            if (updateCounter % 2 == 0) {
                saveHeartRateToDatabase(heartRate);
                if (heartRate > 50 & !inStress) { // Example threshold for high heart rate

                    sendHighHeartRateNotification();
                    inStress = true;
                }
                if (heartRate < 49){
                    inStress = false;
                }
                Intent intent = new Intent("HEART_RATE_UPDATE");
                intent.putExtra("HEART_RATE", heartRate);
                sendBroadcast(intent);
            }
        }

        Log.d("updateHeartRate", "Updating heart rate with: " + heartRate);
    }

    private void readHeartRateData() throws IOException {
        HeartRateData = new LinkedList<>();
        InputStream is = this.getResources().openRawResource(R.raw.data);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        reader.readLine(); // Skip the header
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            int beatsPerMinute = Integer.parseInt(parts[parts.length - 1]);
            String time = parts[parts.length - 2];
            HeartRateData.push(new HeartRateActivity.HeartRateStamp(time, beatsPerMinute));
        }
        reader.close();
    }

    private void saveHeartRateToDatabase(int heartRate) {
        DB.insertHeartRate(username, heartRate);
    }

    private void insertStressData(String username, int heartRate) {
        SQLiteDatabase db = DB.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DB.COL_USERNAME, username);
        contentValues.put(DB.COL_HEART_RATE, heartRate);
        contentValues.put(DB.COL_TIMESTAMP, System.currentTimeMillis()); // Timestamp
        long result = db.insert(DB.TABLE_NAME_STRESS, null, contentValues);
        Log.d("insertStressData:", "result: " + result);
    }

    private void sendHighHeartRateNotification() {
        Log.d("sendHighHeartRateNotification", "Sending High Heart Rate Notification");

        int notificationId = 2;

        Intent yesIntent = new Intent(this, HeartRateService.class);
        yesIntent.setAction(ACTION_INSERT_STRESS_DATA);
        yesIntent.putExtra("USERNAME", username);
        yesIntent.putExtra("HEART_RATE", heartRate); // Pass heart rate value
        yesIntent.putExtra("NOTIFICATION_ID", notificationId); // Pass notification ID
        PendingIntent yesPendingIntent = PendingIntent.getService(this, 0, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent noIntent = new Intent(this, HeartRateService.class);
        noIntent.setAction(ACTION_DISMISS_NOTIFICATION);
        noIntent.putExtra("NOTIFICATION_ID", notificationId); // Pass notification ID
        PendingIntent noPendingIntent = PendingIntent.getService(this, 1, noIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_heartrate) // Replace with your app's icon
                .setContentTitle("High Heart Rate Alert")
                .setContentText("Your heart rate is above the normal threshold. Are you feeling stressed?")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_like, "Yes", yesPendingIntent)
                .addAction(R.drawable.ic_unlike, "No", noPendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(notificationId, builder.build());
    }

    private void createNotificationChannel() {
        Log.d("createNotificationChannel", "Creating Notification Channel");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Heart Rate Channel";
            String description = "Channel for heart rate notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private NotificationCompat.Builder getNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_heartrate) // Replace with your app's icon
                .setContentTitle("Heart Rate Service")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }
}
