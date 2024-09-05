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

/**
 * MapFragment displays a map with location markers based on user data fetched from Firestore.
 * It handles user interaction with the map and refreshes the view when the selected date changes.
 */
public class MapFragment extends Fragment implements OnDateChangedListener {

    private MapView mapView;
    private FirebaseFirestore db;
    private static final String ARG_SELECTED_DATE = "selected_date";
    private String selectedDate;
    private FirebaseUser currentUser;

    /**
     * Default constructor for MapFragment.
     */
    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Called when the fragment is first created. Retrieves the selected date from the arguments.
     *
     * @param savedInstanceState The saved instance state from a previous instance, if any.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedDate = getArguments().getString(ARG_SELECTED_DATE);
        }
    }

    /**
     * Static factory method to create a new instance of MapFragment with a selected date.
     *
     * @param selectedDate The selected date for filtering location data.
     * @return A new instance of MapFragment.
     */
    public static MapFragment newInstance(String selectedDate) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_DATE, selectedDate);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Handles the date change event. Refreshes the UI with the new selected date.
     *
     * @param selectedDate The new selected date.
     */
    @Override
    public void onDateChanged(String selectedDate) {
        // Update fragment view based on new selected date
        this.selectedDate = selectedDate;
        refreshUI(currentUser);
    }

    /**
     * Inflates the fragment's view and initializes the map and Firestore instance.
     *
     * @param inflater           The LayoutInflater used to inflate the view.
     * @param container          The ViewGroup that contains the fragment's UI.
     * @param savedInstanceState The saved instance state from a previous instance, if any.
     * @return The inflated view for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true); // Enable touch gestures
        mapView.setBuiltInZoomControls(true); // Enable zoom controls

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Set the osmdroid configuration
        Configuration.getInstance().load(getContext(), getActivity().getPreferences(MODE_PRIVATE));

        // Get the current user
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        refreshUI(currentUser);

        return view;
    }

    /**
     * Refreshes the map UI by fetching location data from Firestore and adding markers to the map.
     *
     * @param currentUser The currently authenticated Firebase user.
     */
    private void refreshUI(FirebaseUser currentUser) {
        mapView.getOverlays().clear(); // Clear existing markers
        mapView.invalidate(); // Redraw the map
        if (currentUser != null) {
            // Fetch the location data from Firestore
            db.collection("users").document("kid1@gmail.com").collection("locationsData")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            IMapController mapController = mapView.getController();
                            mapController.setCenter(new GeoPoint(31.778263, 35.197334)); // Set default center
                            mapController.setZoom(11); // Set default zoom level
                            boolean firstLocation = true;
                            for (DocumentSnapshot document : task.getResult()) {
                                if (document.exists()) {
                                    Long locationLatE7 = document.getLong("location_latitudee7");
                                    Long locationLngE7 = document.getLong("location_longitudee7");
                                    if (locationLatE7 == null || locationLngE7 == null) continue;

                                    String locationName = document.getString("location_name");
                                    String timeStamp = document.getString("timestamp");
                                    String date = timeStamp;

                                    // Add a marker if the date matches the selected date
                                    if (date.equals(selectedDate)) {
                                        GeoPoint location = new GeoPoint(locationLatE7 / 1e7, locationLngE7 / 1e7);
                                        Marker marker = new Marker(mapView);
                                        marker.setPosition(location);
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                        marker.setTitle(date + "\n" + locationName);
                                        mapView.getOverlays().add(marker);

                                        // Set the first location as the map's center
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

    /**
     * Lifecycle method to handle the fragment's resume state. Resumes the map view.
     */
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * Lifecycle method to handle the fragment's pause state. Pauses the map view.
     */
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * Lifecycle method to handle the fragment's detach state. Detaches the map view.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mapView.onDetach();
    }
}
