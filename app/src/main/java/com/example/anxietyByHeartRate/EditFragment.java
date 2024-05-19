package com.example.anxietyByHeartRate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class EditFragment extends Fragment {

    private Spinner ageSpinner;
    private Spinner weightSpinner;
    private Spinner heightSpinner;
    private Button saveButton;
    private DBHelper DB;
    private String username;

    public EditFragment() {
        // Required empty public constructor
    }
    public static EditFragment newInstance(int age, int weight, int height, String username) {
        EditFragment fragment = new EditFragment();
        Bundle args = new Bundle();
        args.putInt("age", age);
        args.putInt("weight", weight);
        args.putInt("height", height);
        args.putString("username", username);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit, container, false);

        ageSpinner = view.findViewById(R.id.ageSpinner);
        weightSpinner = view.findViewById(R.id.weightSpinner);
        heightSpinner = view.findViewById(R.id.heightSpinner);
        saveButton = view.findViewById(R.id.saveButton);
        DB = new DBHelper(getContext());
        if (getArguments() != null) {
            int age = getArguments().getInt("age", 0);
            int weight = getArguments().getInt("weight", 0);
            int height = getArguments().getInt("height", 0);
            username = getArguments().getString("username", "");

            // Set initial values to Spinners
            populateSpinners(age, weight, height);
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get selected values from spinners
                int selectedAge = Integer.parseInt(ageSpinner.getSelectedItem().toString());
                int selectedWeight = Integer.parseInt(weightSpinner.getSelectedItem().toString().replaceAll("[^0-9]", ""));
                int selectedHeight = Integer.parseInt(heightSpinner.getSelectedItem().toString().replaceAll("[^0-9]", ""));

                // Save the data to the database
                boolean isUpdated = DB.updateUserData(selectedAge, selectedHeight, selectedWeight, username);

                if (isUpdated) {
                    Toast.makeText(getContext(), "Data saved successfully", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof OnDataSavedListener) {
                        ((OnDataSavedListener) getActivity()).onDataSaved(selectedAge, selectedWeight, selectedHeight);
                    }
                    requireActivity().getSupportFragmentManager().popBackStack();

                } else {
                    Toast.makeText(getContext(), "Failed to save data", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }
    public interface OnDataSavedListener {
        void onDataSaved(int selectedAge, int selectedWeight, int selectedHeight);
    }
    // Method to get index of a value in a Spinner
    private int getIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0; // Default to first item if not found
    }
    // Method to populate Spinners with data
    private void populateSpinners(int age, int weight, int height) {
        // Populate Age Spinner
        List<String> ageOptions = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            ageOptions.add(String.valueOf(i));
        }
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, ageOptions);
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ageSpinner.setAdapter(ageAdapter);
        ageSpinner.setSelection(age - 1); // Subtract 1 since age starts from 1

        List<String> weightOptions = new ArrayList<>();
        for (int i = 30; i <= 120; i++) {
            weightOptions.add(String.valueOf(i) + " kg");
        }
        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, weightOptions);
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weightSpinner.setAdapter(weightAdapter);
        weightSpinner.setSelection(weight - 30); // Corrected

// Populate Height Spinner
        List<String> heightOptions = new ArrayList<>();
        for (int i = 70; i <= 200; i++) {
            heightOptions.add(String.valueOf(i) + " cm");
        }
        ArrayAdapter<String> heightAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, heightOptions);
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        heightSpinner.setAdapter(heightAdapter);
        heightSpinner.setSelection(height - 70);
    }
}
