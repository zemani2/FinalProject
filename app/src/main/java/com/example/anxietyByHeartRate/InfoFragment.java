package com.example.anxietyByHeartRate;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * InfoFragment is responsible for displaying user information and a list of associated kids
 * (for users of type "parent"). It retrieves user data from FirebaseFirestore and displays
 * relevant details like first name, last name, and kids' details in a RecyclerView.
 */
public class InfoFragment extends Fragment {

    // Adapter for displaying kids' data
    private KidsAdapter kidsAdapter;
    // RecyclerView for the kids list
    private RecyclerView rvKids;
    // Title TextView for the kids section
    private TextView tvKidsTitle;
    // Progress bar to show loading state
    private ProgressBar progressBar;

    // Email address of the parent user
    private String parentEmail;

    // TextViews for displaying first name and last name of the user
    private TextView firstNameTextView, lastNameTextView;

    // Firebase authentication and Firestore database instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Constructor for the fragment
    public InfoFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of InfoFragment.
     *
     * @return A new instance of InfoFragment.
     */
    public static InfoFragment newInstance() {
        InfoFragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Updates the RecyclerView's adapter with the list of kids.
     *
     * @param kidsList A list of Kid objects to display.
     */
    private void updateKidsAdapter(List<Kid> kidsList) {
        kidsAdapter.updateKidsList(kidsList);
        hideProgressBar();
    }

    /**
     * Shows the progress bar to indicate data loading.
     */
    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the progress bar after data loading is complete.
     */
    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    /**
     * Loads the list of kids associated with a parent user from Firestore and updates
     * the RecyclerView with the retrieved data.
     *
     * @param parentEmail The email address of the parent user.
     */
    private void loadKids(String parentEmail) {
        db.collection("users")
                .document(parentEmail)
                .collection("kids")
                .whereEqualTo("status", "accepted") // Only fetch accepted kids
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Kid> kidsList = new ArrayList<>();
                            List<String> kidEmails = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String email = document.getString("kidEmail");
                                if (email != null) {
                                    kidEmails.add(email);
                                }
                            }

                            // Fetch kid details using their email
                            for (String kidEmail : kidEmails) {
                                db.collection("users")
                                        .whereEqualTo("email", kidEmail)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> kidTask) {
                                                if (kidTask.isSuccessful() && !kidTask.getResult().isEmpty()) {
                                                    DocumentSnapshot kidDocument = kidTask.getResult().getDocuments().get(0);
                                                    String firstName = kidDocument.getString("firstName");
                                                    String lastName = kidDocument.getString("lastName");
                                                    if (firstName != null && lastName != null) {
                                                        Kid kid = new Kid(kidEmail, firstName, lastName);
                                                        kidsList.add(kid);
                                                    }
                                                }

                                                // Update the adapter after loading the last kid
                                                if (kidEmail.equals(kidEmails.get(kidEmails.size() - 1))) {
                                                    updateKidsAdapter(kidsList);
                                                }
                                            }
                                        });
                            }

                            // If no kids are found, update the adapter with an empty list
                            if (kidEmails.isEmpty()) {
                                updateKidsAdapter(kidsList);
                            }
                        } else {
                            Log.d("Firestore", "Error getting accepted kids: ", task.getException());
                            hideProgressBar();
                        }
                    }
                });
    }

    /**
     * Updates the user information (first name, last name) and triggers the loading
     * of associated kids if the user is a parent.
     *
     * @param view       The root view of the fragment.
     * @param parentEmail The email address of the parent user.
     */
    private void updateNameView(View view, String parentEmail) {
        db.collection("users").document(parentEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            firstNameTextView = view.findViewById(R.id.firstNameTextView);
                            firstNameTextView.setText("First Name: " + document.getString("firstName"));
                            lastNameTextView = view.findViewById(R.id.lastNameTextView);
                            lastNameTextView.setText("Last Name: " + document.getString("lastName"));

                            String userType = document.getString("userType");
                            if ("parent".equals(userType)) {
                                tvKidsTitle.setVisibility(View.VISIBLE);
                                rvKids.setVisibility(View.VISIBLE);
                                showProgressBar();
                                loadKids(parentEmail);
                            }
                        } else {
                            Log.d("Firestore", "No such document");
                        }
                    } else {
                        Log.d("Firestore", "get failed with ", task.getException());
                    }
                });
    }

    /**
     * Inflates the layout for this fragment and initializes UI elements.
     *
     * @param inflater           LayoutInflater to inflate the layout.
     * @param container          The parent container.
     * @param savedInstanceState Saved instance state.
     * @return The root view of the fragment.
     */
    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        // Initialize UI elements
        rvKids = view.findViewById(R.id.rvKids);
        tvKidsTitle = view.findViewById(R.id.tvKidsTitle);
        progressBar = view.findViewById(R.id.progressBar);

        rvKids.setLayoutManager(new LinearLayoutManager(getContext()));
        kidsAdapter = new KidsAdapter(new ArrayList<>());
        rvKids.setAdapter(kidsAdapter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get the current user and load their information
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            parentEmail = currentUser.getEmail();
            updateNameView(view, parentEmail);
        }

        // Handle the edit button click event
        ImageButton editButton = view.findViewById(R.id.editButton);
        editButton.setOnClickListener(v -> {
            // Open EditFragment when edit button is clicked
            EditFragment editFragment = EditFragment.newInstance();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, editFragment, "EditFragment")
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    /**
     * Updates the UI with new first and last name values.
     *
     * @param firstName The updated first name.
     * @param lastName  The updated last name.
     */
    public void updateData(String firstName, String lastName) {
        if (firstNameTextView != null) {
            firstNameTextView.setText(firstName);
        }
        if (lastNameTextView != null) {
            lastNameTextView.setText(lastName);
        }
    }
}
