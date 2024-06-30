package com.example.anxietyByHeartRate;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditFragment extends Fragment {

    private Button saveButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView firstNameEditText, lastNameEditText;
    private String firstName, lastName;
    public EditFragment() {
        // Required empty public constructor
    }

    public static EditFragment newInstance() {
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

        firstNameEditText = view.findViewById(R.id.etFirstName);
        lastNameEditText = view.findViewById(R.id.etLastName);
        saveButton = view.findViewById(R.id.saveButton);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            firstName = documentSnapshot.getString("firstName");
                            lastName = documentSnapshot.getString("lastName");
                            firstNameEditText.setText(firstName);
                            lastNameEditText.setText(lastName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    });
        }
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save the data to Firestore
                firstName = firstNameEditText.getText().toString();
                lastName = lastNameEditText.getText().toString();
                saveUserData(firstName, lastName, currentUser.getUid());

                Toast.makeText(getContext(), "Data saved successfully", Toast.LENGTH_SHORT).show();
                ((OnDataSavedListener) requireActivity()).onDataSaved(firstName, lastName);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    // Method to save user data to Firestore
    private void saveUserData(String firstName, String lastName, String userId) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);

        db.collection("users").document(userId)
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Data saved successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "ERROR: Data wasn't saved", Toast.LENGTH_SHORT).show();
                });
    }

    public interface OnDataSavedListener {
        void onDataSaved(String firstName, String lastName);
    }
}
