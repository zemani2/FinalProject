package com.example.anxietyByHeartRate;

import static android.content.Context.MODE_PRIVATE;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapFragment extends Fragment implements OnDateChangedListener {

    private MapView mapView;
    private FirebaseFirestore db;
    private static final String ARG_SELECTED_DATE = "selected_date";
    private String selectedDate;
    private FirebaseUser currentUser;
    public MapFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedDate = getArguments().getString(ARG_SELECTED_DATE);
        }
    }
    public static MapFragment newInstance(String selectedDate) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_DATE, selectedDate);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onDateChanged(String selectedDate) {
        // Update fragment view based on new selected date
        this.selectedDate = selectedDate;
        // Call a method to refresh your UI with new selectedDate
        refreshUI(currentUser);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Set the osmdroid configuration
        Configuration.getInstance().load(getContext(), getActivity().getPreferences(MODE_PRIVATE));

        // Get the current user
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        refreshUI(currentUser);

        return view;
    }

    private void refreshUI(FirebaseUser currentUser) {
        if (currentUser != null) {
            // Fetch the location data from Firestore
            db.collection("users").document("kid1@gmail.com").collection("locationsData")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            IMapController mapController = mapView.getController();
                            mapController.setCenter(new GeoPoint(31.778263, 35.197334));
                            mapController.setZoom(11);
                            boolean firstLocation = true;
                            for (DocumentSnapshot document : task.getResult()) {
                                if (document.exists()) {
                                    long locationLatE7 = document.getLong("location_latitudee7");
                                    long locationLngE7 = document.getLong("location_longitudee7");
                                    String locationName = document.getString("location_name");
                                    String timeStamp = document.getString("duration_starttimestamp");
                                    String[] parts = timeStamp.split("T");

                                    // Extract the date and time
                                    String date = parts[0];
                                    date = date.substring(0,4) + date.substring(7) + date.substring(4,7);
                                    String time = parts[1].substring(0, 5); // This will give you the hour and minute (HH:mm)
                                    if (date.equals(selectedDate)) {
                                        GeoPoint location = new GeoPoint(locationLatE7 / 1e7, locationLngE7 / 1e7);

                                        // Add a marker for each location
                                        Marker marker = new Marker(mapView);
                                        marker.setPosition(location);
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                        marker.setTitle(time + "\n" + locationName);
                                        mapView.getOverlays().add(marker);

                                        if (firstLocation) {
                                            mapController.setCenter(location);
                                            firstLocation = false;
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.d("MapFragment", "Error getting documents: ", task.getException());
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mapView.onDetach();
    }
}
