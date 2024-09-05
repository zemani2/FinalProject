package com.example.anxietyByHeartRate;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
/**
 * EditFragment allows users to view and edit their first and last names stored in Firebase Firestore.
 * It retrieves the current user’s information from Firestore and updates the data when the user saves changes.
 *
 * This fragment is used as part of the anxiety detection app, which uses user information to customize the user experience.
 * The user’s first and last names are editable, and any changes made are saved back to Firestore.
 *
 * The app is designed to detect anxiety by heart rate and other metrics, using Firestore for authentication and data storage.
 *
 * Key Components:
 * - FirebaseFirestore (db): The Firestore instance used to retrieve and update user data.
 * - FirebaseAuth (mAuth): Handles authentication to get the current logged-in user.
 * - EditText (firstNameEditText, lastNameEditText): Input fields for the user to view and edit their first and last names.
 * - Button (saveButton): Saves the user's changes to Firestore.
 *
 * How it works:
 * - On creation, the fragment fetches the current user's first and last names from Firestore using the user's email as a key.
 * - If the data is found, it populates the EditText fields with the retrieved values.
 * - When the user clicks the save button, it saves any changes back to Firestore and notifies the activity using the OnDataSavedListener interface.
 */
public class EditFragment extends Fragment {

    private Button saveButton;
    private FirebaseFirestore db; // Firestore database instance
    private FirebaseAuth mAuth; // Firebase authentication instance
    private EditText firstNameEditText, lastNameEditText;
    private String firstName, lastName;

    // Required empty public constructor
    public EditFragment() {}

    /**
     * Static factory method to create a new instance of EditFragment.
     *
     * @return A new instance of EditFragment.
     */
    public static EditFragment newInstance() {
        EditFragment fragment = new EditFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    /**
     * Called when the fragment's view is created.
     *
     * @param inflater Inflater to inflate the fragment's layout.
     * @param container The parent view that this fragment's UI should be attached to.
     * @param savedInstanceState Bundle containing previous instance state (if available).
     * @return The created view for this fragment.
     */
    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit, container, false);

        // Initialize UI elements
        firstNameEditText = view.findViewById(R.id.etFirstName);
        lastNameEditText = view.findViewById(R.id.etLastName);
        saveButton = view.findViewById(R.id.saveButton);

        // Get Firebase instances for authentication and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();

            // Retrieve user data from Firestore and populate the fields
            db.collection("users").document(userEmail)
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
                        // Uncomment to show a failure message
                        // Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    });
        }

        // Set up the Save button click listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve updated first and last names from EditText fields
                firstName = firstNameEditText.getText().toString();
                lastName = lastNameEditText.getText().toString();

                // Save the updated data to Firestore
                saveUserData(firstName, lastName, currentUser.getEmail());

                // Notify the activity that the data was saved and go back to the previous screen
                ((OnDataSavedListener) requireActivity()).onDataSaved(firstName, lastName);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    /**
     * Saves the user's first and last names to Firestore.
     *
     * @param firstName The updated first name.
     * @param lastName The updated last name.
     * @param userEmail The current user's email address, used as the document key in Firestore.
     */
    private void saveUserData(String firstName, String lastName, String userEmail) {
        // Create a map to store the updated user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);

        // Update the Firestore document for the current user
        db.collection("users").document(userEmail)
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    // Show a success message if the data was saved successfully
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Data saved successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Show an error message if there was a failure
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "ERROR: Data wasn't saved", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Interface to communicate data saving events to the activity.
     */
    public interface OnDataSavedListener {
        /**
         * Called when user data has been successfully saved.
         *
         * @param firstName The updated first name.
         * @param lastName The updated last name.
         */
        void onDataSaved(String firstName, String lastName);
    }
}
