package com.example.anxietyByHeartRate;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * LocationService is a foreground service that periodically retrieves the device's location
 * and uploads it to Firebase Firestore. It runs in the background and handles location updates
 * even when the app is not in the foreground.
 */
public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;

    /**
     * Called when the service is first created. Initializes the location client,
     * Firebase Firestore, and the notification channel. It also sets up the location
     * callback to handle location updates.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Initializing Location Service");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        createNotificationChannel();

        // Callback that handles location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w(TAG, "onLocationResult: LocationResult is null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Handle location update here
                    Log.d(TAG, "Location: " + location.toString());
                    uploadLocationToFirebase(location);
                }
            }
        };
        startLocationUpdates();
    }

    /**
     * Creates a notification channel for Android O and above. This is required for starting the service
     * in the foreground.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location Service Channel";
            String description = "Channel for Location Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("location_channel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    /**
     * Starts requesting location updates from the device at a specified interval.
     * This method ensures that the app has the appropriate location permissions before
     * requesting updates.
     */
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(60000); // 1 minute
        locationRequest.setFastestInterval(60000); // 1 minute
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "startLocationUpdates: Missing location permissions");
            return;
        }
        Log.d(TAG, "Requesting location updates");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Uploads the retrieved location data (latitude, longitude, and timestamp) to Firebase Firestore.
     *
     * @param location The location data that needs to be uploaded to Firebase.
     */
    private void uploadLocationToFirebase(Location location) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getEmail(); // Retrieve the current user's email
        if (userId == null) {
            Log.e(TAG, "uploadLocationToFirebase: User ID is null");
            return;
        }

        // Prepare location data to upload to Firebase
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", System.currentTimeMillis());

        // Upload location data to the Firestore database
        db.collection("users").document(userId).collection("locations")
                .add(locationData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Location uploaded: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error uploading location", e));
    }

    /**
     * Called when the service is destroyed. This method removes the location updates
     * and performs necessary cleanup.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Removing location updates");
        fusedLocationClient.removeLocationUpdates(locationCallback); // Stop location updates
    }

    /**
     * Starts the service in the foreground with a persistent notification.
     *
     * @param intent  The Intent supplied to {@link Service#startService(Intent)}.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request.
     * @return Indicates how the system should continue the service in case it is killed.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Starting foreground service");

        // Create and display a persistent notification for the service
        Notification notification = new NotificationCompat.Builder(this, "location_channel")
                .setContentTitle("Location Service")
                .setContentText("Running...")
                .setSmallIcon(R.drawable.ic_location) // Ensure a location icon is present
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        startForeground(1, notification); // Start the service in the foreground
        return START_STICKY; // Ensure the service is restarted if killed
    }

    /**
     * This method is required by the Service class but is not used in this case,
     * as binding is not necessary for this location service.
     *
     * @param intent The Intent supplied to {@link Service#bindService(Intent, android.content.ServiceConnection, int)}.
     * @return Always returns null because binding is not used.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding functionality in this service
    }
}
