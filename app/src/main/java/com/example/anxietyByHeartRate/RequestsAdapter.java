package com.example.anxietyByHeartRate;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {
    private List<Request> requestsList;
    private FirebaseFirestore db;
    private KidsAdapter kidsAdapter; // Reference to KidsAdapter for updating

    public RequestsAdapter(List<Request> requestsList, FirebaseFirestore db, KidsAdapter kidsAdapter) {
        this.requestsList = requestsList;
        this.db = db;
        this.kidsAdapter = kidsAdapter;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        Request request = requestsList.get(position);
        holder.tvKidEmail.setText(request.getKidEmail());
        holder.tvRequestStatus.setText(request.getStatus());

        // Ensure the progress bar is hidden initially
        holder.progressBar.setVisibility(View.GONE);

        // Handle acceptance button click
        holder.btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptRequest(request, holder);
            }
        });

        // Handle decline button click
        holder.btnDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                declineRequest(request, holder);
            }
        });
    }

    private void acceptRequest(Request request, RequestViewHolder holder) {
        // Show progress bar while performing database operation
        holder.progressBar.setVisibility(View.VISIBLE);

        // Update Firestore with accepted status
        db.collection("users")
                .whereEqualTo("email", request.getParentEmail()) // Find the parent document where email matches parentEmail
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // Ensure there's exactly one document (parent) with this email
                            if (!task.getResult().isEmpty()) {
                                // Get the parent document
                                DocumentSnapshot parentDoc = task.getResult().getDocuments().get(0);

                                // Find the kid document under this parent's "kids" subcollection
                                db.collection("users").document(parentDoc.getId()).collection("kids")
                                        .whereEqualTo("kidEmail", request.getKidEmail()) // Assuming kidEmail is unique
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    // Ensure there's exactly one kid document with this kidEmail
                                                    if (!task.getResult().isEmpty()) {
                                                        // Get the kid document
                                                        DocumentSnapshot kidDoc = task.getResult().getDocuments().get(0);

                                                        // Update the kid document with accepted status
                                                        db.collection("users").document(parentDoc.getId()).collection("kids")
                                                                .document(kidDoc.getId())
                                                                .update("status", "accepted")
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        // Remove the request from the local list and notify adapter
                                                                        requestsList.remove(request);
                                                                        notifyDataSetChanged();

                                                                        // Update UI to show the newly accepted kid
                                                                        addKidToAdapter(request);
                                                                        notifyDataSetChanged();

                                                                        Toast.makeText(holder.itemView.getContext(), "Request accepted", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Log.e("Firestore", "Error updating acceptance status", e);
                                                                        Toast.makeText(holder.itemView.getContext(), "Failed to accept request", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    } else {
                                                        Log.d("Firestore", "No kid document found with kidEmail: " + request.getKidEmail());
                                                        Toast.makeText(holder.itemView.getContext(), "No kid document found", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    Log.d("Firestore", "Error querying kid document: ", task.getException());
                                                    Toast.makeText(holder.itemView.getContext(), "Error querying kid document", Toast.LENGTH_SHORT).show();
                                                }

                                                // Hide progress bar after operation completes
                                                holder.progressBar.setVisibility(View.GONE);
                                            }
                                        });
                            } else {
                                Log.d("Firestore", "No parent document found with email: " + request.getParentEmail());
                                Toast.makeText(holder.itemView.getContext(), "No parent document found", Toast.LENGTH_SHORT).show();

                                // Hide progress bar on failure
                                holder.progressBar.setVisibility(View.GONE);
                            }
                        } else {
                            Log.d("Firestore", "Error querying parent document: ", task.getException());
                            Toast.makeText(holder.itemView.getContext(), "Error querying parent document", Toast.LENGTH_SHORT).show();

                            // Hide progress bar on failure
                            holder.progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void declineRequest(Request request, RequestViewHolder holder) {
        // Show progress bar while performing database operation
        holder.progressBar.setVisibility(View.VISIBLE);

        // Update Firestore with declined status
        db.collection("users")
                .whereEqualTo("email", request.getParentEmail()) // Find the parent document where email matches parentEmail
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // Ensure there's exactly one document (parent) with this email
                            if (!task.getResult().isEmpty()) {
                                // Get the parent document
                                DocumentSnapshot parentDoc = task.getResult().getDocuments().get(0);

                                // Find the kid document under this parent's "kids" subcollection
                                db.collection("users").document(parentDoc.getId()).collection("kids")
                                        .whereEqualTo("kidEmail", request.getKidEmail()) // Assuming kidEmail is unique
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    // Ensure there's exactly one kid document with this kidEmail
                                                    if (!task.getResult().isEmpty()) {
                                                        // Get the kid document
                                                        DocumentSnapshot kidDoc = task.getResult().getDocuments().get(0);

                                                        // Delete the kid document from Firestore
                                                        db.collection("users").document(parentDoc.getId()).collection("kids")
                                                                .document(kidDoc.getId())
                                                                .delete()
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        // Remove the request from the local list and notify adapter
                                                                        requestsList.remove(request);
                                                                        notifyDataSetChanged();

                                                                        Toast.makeText(holder.itemView.getContext(), "Request declined", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Log.e("Firestore", "Error deleting kid document", e);
                                                                        Toast.makeText(holder.itemView.getContext(), "Failed to decline request", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    } else {
                                                        Log.d("Firestore", "No kid document found with kidEmail: " + request.getKidEmail());
                                                        Toast.makeText(holder.itemView.getContext(), "No kid document found", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    Log.d("Firestore", "Error querying kid document: ", task.getException());
                                                    Toast.makeText(holder.itemView.getContext(), "Error querying kid document", Toast.LENGTH_SHORT).show();
                                                }

                                                // Hide progress bar after operation completes
                                                holder.progressBar.setVisibility(View.GONE);
                                            }
                                        });
                            } else {
                                Log.d("Firestore", "No parent document found with email: " + request.getParentEmail());
                                Toast.makeText(holder.itemView.getContext(), "No parent document found", Toast.LENGTH_SHORT).show();

                                // Hide progress bar on failure
                                holder.progressBar.setVisibility(View.GONE);
                            }
                        } else {
                            Log.d("Firestore", "Error querying parent document: ", task.getException());
                            Toast.makeText(holder.itemView.getContext(), "Error querying parent document", Toast.LENGTH_SHORT).show();

                            // Hide progress bar on failure
                            holder.progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void addKidToAdapter(Request request) {
        // Create a Kid object from the accepted request
        Kid newKid = new Kid();
        newKid.setEmail(request.getKidEmail());

        // Fetch first and last name from Firestore based on kid's email
        db.collection("users")
                .whereEqualTo("email", request.getKidEmail())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull
                                           Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Assuming there's only one document matching the email
                                String firstName = document.getString("firstName");
                                String lastName = document.getString("lastName");

                                // Set first and last name to the newKid object
                                newKid.setFirstName(firstName);
                                newKid.setLastName(lastName);

                                // Add the new kid to KidsAdapter\
                                if (kidsAdapter != null) {
                                    kidsAdapter.addKid(newKid);
                                } else {
                                    // Handle the error, maybe log it or throw an exception
                                    Log.e("KidsAdapter", "KidsAdapter is null");
                                }
                            }
                        } else {
                            Log.d("Firestore", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }


    @Override
    public int getItemCount() {
        return requestsList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvKidEmail, tvRequestStatus;
        ImageButton btnAccept, btnDecline;
        ProgressBar progressBar;

        RequestViewHolder(View itemView) {
            super(itemView);
            tvKidEmail = itemView.findViewById(R.id.tvKidEmail);
            tvRequestStatus = itemView.findViewById(R.id.tvRequestStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            progressBar = itemView.findViewById(R.id.progressBar); // Initialize progress bar
        }
    }
}
