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

public class InfoFragment extends Fragment {

    private KidsAdapter kidsAdapter;
    private RecyclerView rvKids;
    private TextView tvKidsTitle;
    private ProgressBar progressBar;

    private String parentEmail;

    private TextView firstNameTextView, lastNameTextView;
    private int weight;
    private int height;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public InfoFragment() {
        // Required empty public constructor
    }

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

    private void updateKidsAdapter(List<Kid> kidsList) {
        kidsAdapter.updateKidsList(kidsList);
        hideProgressBar();
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private void loadKids(String parentEmail) {
        db.collection("users")
                .document(parentEmail)
                .collection("kids")
                .whereEqualTo("status", "accepted")
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

                                                if (kidEmail.equals(kidEmails.get(kidEmails.size() - 1))) {
                                                    updateKidsAdapter(kidsList);
                                                }
                                            }
                                        });
                            }

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

    private void updateNameView(View view, String parentEmail) {
        db.collection("users").document(parentEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            firstNameTextView = view.findViewById(R.id.firstNameTextView);
                            firstNameTextView.setText("First Name: "+ document.getString("firstName"));
                            lastNameTextView = view.findViewById(R.id.lastNameTextView);
                            lastNameTextView.setText("Last Name: "+ document.getString("lastName"));
                        } else {
                            Log.d("Firestore", "No such document");
                        }
                    } else {
                        Log.d("Firestore", "get failed with ", task.getException());
                    }
                });
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        rvKids = view.findViewById(R.id.rvKids);
        tvKidsTitle = view.findViewById(R.id.tvKidsTitle);
        progressBar = view.findViewById(R.id.progressBar);

        rvKids.setLayoutManager(new LinearLayoutManager(getContext()));
        kidsAdapter = new KidsAdapter(new ArrayList<>());
        rvKids.setAdapter(kidsAdapter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            parentEmail = currentUser.getEmail();
            updateNameView(view, parentEmail);
            showProgressBar();
            loadKids(parentEmail);
        }

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
    public void updateData(String firstName, String lastName) {
        if (firstNameTextView != null) {
            firstNameTextView.setText(firstName);
        }
        if (lastNameTextView != null) {
            lastNameTextView.setText(lastName);
        }
    }
}
