package com.example.anxietyByHeartRate;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditFragment extends Fragment {

    private Spinner ageSpinner;
    private Spinner weightSpinner;
    private Spinner heightSpinner;
    private Button saveButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private int age;
    private int weight ;
    private int height;
    private TextView ageTextView;
    private TextView weightTextView;
    private TextView heightTextView;
    public EditFragment() {
        // Required empty public constructor
    }

    public static EditFragment newInstance(int age, int weight, int height, String username) {
        EditFragment fragment = new EditFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit, container, false);

        ageSpinner = view.findViewById(R.id.ageSpinner);
        weightSpinner = view.findViewById(R.id.weightSpinner);
        heightSpinner = view.findViewById(R.id.heightSpinner);
        saveButton = view.findViewById(R.id.saveButton);
        ageTextView = view.findViewById(R.id.ageTextView);
        weightTextView = view.findViewById(R.id.weightTextView);
        heightTextView = view.findViewById(R.id.heightTextView);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        assert currentUser != null;
        String userId = currentUser.getUid();
        getUserData(view, userId);



        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get selected values from spinners
                String selectedAgeString = (String) ageSpinner.getSelectedItem();
                int selectedAge = selectedAgeString != null ? Integer.parseInt(selectedAgeString): 0;
                String selectedWeightString = (String) weightSpinner.getSelectedItem();
                int selectedWeight = selectedAgeString != null ? Integer.parseInt(selectedWeightString.replaceAll("[^\\d]", "")): 0;
                String selectedHeightString = (String) heightSpinner.getSelectedItem();
                int selectedHeight = selectedAgeString != null ? Integer.parseInt(selectedHeightString.replaceAll("[^\\d]", "")): 0;

                // Save the data to Firestore
                saveUserData(selectedAge, selectedWeight, selectedHeight, userId);

                Toast.makeText(getContext(), "Data saved successfully", Toast.LENGTH_SHORT).show();
                ((OnDataSavedListener) requireActivity()).onDataSaved(selectedAge, selectedWeight, selectedHeight);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }
    private void getUserData(View view, String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> userDetails = document.getData();
                            if (userDetails != null) {
                                age = Integer.parseInt(String.valueOf(userDetails.get("age")));
                                weight = Integer.parseInt(String.valueOf(userDetails.get("weight")));
                                height = Integer.parseInt(String.valueOf(userDetails.get("height")));
                            }
                            populateSpinners();
                        } else {
                            // Document doesn't exist
                            Log.d("Firestore", "No such document");
                        }
                    } else {
                        // Error fetching document
                        Log.d("Firestore", "get failed with ", task.getException());
                    }
                });
    }
    // Method to save user data to Firestore
    private void saveUserData(int age, int weight, int height, String userId) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("age", age);
        userData.put("weight", weight);
        userData.put("height", height);

        db.collection("users").document(userId)
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    // Data saved successfully
                })
                .addOnFailureListener(e -> {
                    // Error saving data
                });
    }

    // Method to populate Spinners with data
    private void populateSpinners() {
        // Populate Age Spinner
        List<String> ageOptions = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            ageOptions.add(String.valueOf(i));
        }
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, ageOptions);
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ageSpinner.setAdapter(ageAdapter);
        ageSpinner.setSelection(age - 1); // Subtract 1 since age starts from 1

        // Populate Weight Spinner
        List<String> weightOptions = new ArrayList<>();
        for (int i = 30; i <= 120; i++) {
            weightOptions.add(String.valueOf(i) + " kg");
        }
        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, weightOptions);
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weightSpinner.setAdapter(weightAdapter);
        weightSpinner.setSelection(weight - 30); // Subtract 30 since weight starts from 30

        // Populate Height Spinner
        List<String> heightOptions = new ArrayList<>();
        for (int i = 70; i <= 200; i++) {
            heightOptions.add(String.valueOf(i) + " cm");
        }
        ArrayAdapter<String> heightAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, heightOptions);
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        heightSpinner.setAdapter(heightAdapter);
        heightSpinner.setSelection(height - 70); // Subtract 70 since height starts from 70
    }
    public interface OnDataSavedListener {
        void onDataSaved(int selectedAge, int selectedWeight, int selectedHeight);
    }
}
