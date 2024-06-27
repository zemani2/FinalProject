package com.example.anxietyByHeartRate;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ParentDashboardActivity extends AppCompatActivity {
    private RecyclerView rvKids, rvRequests;
    private TextView tvKidsTitle, tvRequestsTitle;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String parentEmail;
    private KidsAdapter kidsAdapter;
    private RequestsAdapter requestsAdapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rvKids = findViewById(R.id.rvKids);
        rvRequests = findViewById(R.id.rvRequests);
        tvKidsTitle = findViewById(R.id.tvKidsTitle);
        tvRequestsTitle = findViewById(R.id.tvRequestsTitle);
        progressBar = findViewById(R.id.progressBar);

        rvKids.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            parentEmail = currentUser.getEmail();
            showProgressBar();
            loadKids();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private void loadKids() {
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

                            // Collect all kid emails from the parent's subcollection
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String email = document.getString("kidEmail");
                                if (email != null) {
                                    kidEmails.add(email);
                                }
                            }

                            // Fetch each kid's data from the users collection using their email
                            for (String kidEmail : kidEmails) {
                                db.collection("users")
                                        .whereEqualTo("email", kidEmail)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> kidTask) {
                                                if (kidTask.isSuccessful() && !kidTask.getResult().isEmpty()) {
                                                    QueryDocumentSnapshot kidDocument = (QueryDocumentSnapshot) kidTask.getResult().getDocuments().get(0);
                                                    String firstName = kidDocument.getString("firstName");
                                                    String lastName = kidDocument.getString("lastName");
                                                    if (firstName != null && lastName != null) {
                                                        Kid kid = new Kid(kidEmail, firstName, lastName);
                                                        kidsList.add(kid);
                                                    }
                                                }

                                                // Check if this is the last kid to process
                                                if (kidEmail.equals(kidEmails.get(kidEmails.size() - 1))) {
                                                    updateKidsAdapter(kidsList);
                                                    loadRequests();
                                                }
                                            }
                                        });
                            }

                            if (kidEmails.isEmpty()) {
                                // No kids to process, update adapter directly
                                updateKidsAdapter(kidsList);
                            }

                        } else {
                            Log.d("Firestore", "Error getting accepted kids: ", task.getException());
                            Toast.makeText(ParentDashboardActivity.this, "Error getting accepted kids", Toast.LENGTH_SHORT).show();
                            hideProgressBar();
                        }
                    }
                });
    }


    private void updateKidsAdapter(List<Kid> kidsList) {
        kidsAdapter = new KidsAdapter(kidsList);
        rvKids.setAdapter(kidsAdapter);
        hideProgressBar();
    }

    private void loadRequests() {
        db.collection("users")
                .document(parentEmail)
                .collection("kids")
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Request> requestsList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String kidEmail = document.getString("kidEmail");
                                String status = document.getString("status");
                                if (kidEmail != null && status != null) {
                                    Request request = new Request(kidEmail, status, false, parentEmail);
                                    requestsList.add(request);
                                }
                            }
                            updateRequestsAdapter(requestsList);
                        } else {
                            Log.d("Firestore", "Error getting pending requests: ", task.getException());
                            Toast.makeText(ParentDashboardActivity.this, "Error getting pending requests", Toast.LENGTH_SHORT).show();
                        }
                        hideProgressBar();
                    }
                });
    }

    private void updateRequestsAdapter(List<Request> requestsList) {
        requestsAdapter = new RequestsAdapter(requestsList, db, kidsAdapter);
        rvRequests.setAdapter(requestsAdapter);
    }
}
