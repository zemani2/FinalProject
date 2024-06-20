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
import com.google.firebase.firestore.DocumentSnapshot;
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
    private KidsAdapter kidsAdapter = new KidsAdapter(new ArrayList<>());
    ;
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
            loadRequests();
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
                .whereEqualTo("email", parentEmail)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot parentDoc = task.getResult().getDocuments().get(0);
                            fetchAcceptedKids(parentDoc.getId());
                        } else {
                            Log.d("Firestore", "No parent document found with email: " + parentEmail);
                            Toast.makeText(ParentDashboardActivity.this, "No parent document found", Toast.LENGTH_SHORT).show();
                            hideProgressBar();
                        }
                    }
                });
    }

    private void fetchAcceptedKids(String parentId) {
        db.collection("users").document(parentId).collection("kids")
                .whereEqualTo("status", "accepted")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> kidsEmailsList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String email = document.getString("kidEmail");
                                if (email != null) {
                                    kidsEmailsList.add(email);
                                }
                            }
                            fetchKidsDetails(kidsEmailsList);
                        } else {
                            Log.d("Firestore", "Error getting kids emails: ", task.getException());
                            Toast.makeText(ParentDashboardActivity.this, "Error getting kids emails", Toast.LENGTH_SHORT).show();
                            hideProgressBar();
                        }
                    }
                });
    }

    private void fetchKidsDetails(List<String> kidsEmailsList) {
        if (kidsEmailsList.isEmpty()) {
            rvKids.setAdapter(kidsAdapter);
            hideProgressBar();
            return;
        }

        List<Kid> kidsList = new ArrayList<>();
        for (String email : kidsEmailsList) {
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String email = document.getString("email");
                                    String firstName = document.getString("firstName");
                                    String lastName = document.getString("lastName");
                                    if (email != null && firstName != null && lastName != null) {
                                        Kid kid = new Kid(email, firstName, lastName);
                                        kidsList.add(kid);
                                    }
                                }
                                kidsAdapter = new KidsAdapter(kidsList);
                                rvKids.setAdapter(kidsAdapter);
                                hideProgressBar();
                            } else {
                                Log.d("Firestore", "Error getting kids: ", task.getException());
                                Toast.makeText(ParentDashboardActivity.this, "Error getting kids details", Toast.LENGTH_SHORT).show();
                                hideProgressBar();
                            }
                        }
                    });
        }
    }

    private void loadRequests() {
        db.collection("users")
                .whereEqualTo("email", parentEmail)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot parentDoc = task.getResult().getDocuments().get(0);
                            fetchPendingRequests(parentDoc.getId());
                        } else {
                            Log.d("Firestore", "No parent document found with email: " + parentEmail);
                            Toast.makeText(ParentDashboardActivity.this, "No parent document found", Toast.LENGTH_SHORT).show();
                            hideProgressBar();
                        }
                    }
                });
    }

    private void fetchPendingRequests(String parentId) {
        db.collection("users").document(parentId).collection("kids")
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
                                } else {
                                    Log.d("Firestore", "Missing kidEmail or status field in document");
                                }
                            }
                            requestsAdapter = new RequestsAdapter(requestsList, db, kidsAdapter);
                            rvRequests.setAdapter(requestsAdapter);
                            hideProgressBar();
                        } else {
                            Log.d("Firestore", "Error getting requests: ", task.getException());
                            Toast.makeText(ParentDashboardActivity.this, "Error getting requests", Toast.LENGTH_SHORT).show();
                            hideProgressBar();
                        }
                    }
                });
    }
}
