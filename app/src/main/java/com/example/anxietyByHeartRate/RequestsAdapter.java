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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Adapter class for displaying and handling request items in a RecyclerView.
 */
public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {
    private List<Request> requestsList;
    private FirebaseFirestore db;
    private KidsAdapter kidsAdapter; // Reference to KidsAdapter for updating

    /**
     * Constructs a RequestsAdapter with the given parameters.
     *
     * @param requestsList List of requests to be displayed.
     * @param db           FirebaseFirestore instance for database operations.
     * @param kidsAdapter  KidsAdapter for updating the list of kids.
     */
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
        holder.btnAccept.setOnClickListener(v -> acceptRequest(request, holder));

        // Handle decline button click
        holder.btnDecline.setOnClickListener(v -> declineRequest(request, holder));
    }

    /**
     * Handles the acceptance of a request and updates the Firestore database.
     *
     * @param request The request to be accepted.
     * @param holder  The ViewHolder for the request item.
     */
    private void acceptRequest(Request request, RequestViewHolder holder) {
        // Show progress bar while performing database operation
        holder.progressBar.setVisibility(View.VISIBLE);

        // Update Firestore with accepted status
        db.collection("users")
                .whereEqualTo("email", request.getParentEmail()) // Find the parent document where email matches parentEmail
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Ensure there's exactly one document (parent) with this email
                        if (!task.getResult().isEmpty()) {
                            // Get the parent document
                            DocumentSnapshot parentDoc = task.getResult().getDocuments().get(0);

                            // Find the kid document under this parent's "kids" subcollection
                            db.collection("users").document(parentDoc.getId()).collection("kids")
                                    .whereEqualTo("kidEmail", request.getKidEmail()) // Assuming kidEmail is unique
                                    .get()
                                    .addOnCompleteListener(kidTask -> {
                                        if (kidTask.isSuccessful()) {
                                            // Ensure there's exactly one kid document with this kidEmail
                                            if (!kidTask.getResult().isEmpty()) {
                                                // Get the kid document
                                                DocumentSnapshot kidDoc = kidTask.getResult().getDocuments().get(0);

                                                // Update the kid document with accepted status
                                                db.collection("users").document(parentDoc.getId()).collection("kids")
                                                        .document(kidDoc.getId())
                                                        .update("status", "accepted")
                                                        .addOnSuccessListener(aVoid -> {
                                                            // Remove the request from the local list and notify adapter
                                                            requestsList.remove(request);
                                                            notifyDataSetChanged();

                                                            // Update UI to show the newly accepted kid
                                                            addKidToAdapter(request);
                                                            notifyDataSetChanged();

                                                            Toast.makeText(holder.itemView.getContext(), "Request accepted", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("Firestore", "Error updating acceptance status", e);
                                                            Toast.makeText(holder.itemView.getContext(), "Failed to accept request", Toast.LENGTH_SHORT).show();
                                                        });
                                            } else {
                                                Log.d("Firestore", "No kid document found with kidEmail: " + request.getKidEmail());
                                                Toast.makeText(holder.itemView.getContext(), "No kid document found", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Log.d("Firestore", "Error querying kid document: ", kidTask.getException());
                                            Toast.makeText(holder.itemView.getContext(), "Error querying kid document", Toast.LENGTH_SHORT).show();
                                        }

                                        // Hide progress bar after operation completes
                                        holder.progressBar.setVisibility(View.GONE);
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
                });
    }

    /**
     * Handles the decline of a request and updates the Firestore database.
     *
     * @param request The request to be declined.
     * @param holder  The ViewHolder for the request item.
     */
    private void declineRequest(Request request, RequestViewHolder holder) {
        // Show progress bar while performing database operation
        holder.progressBar.setVisibility(View.VISIBLE);

        // Update Firestore with declined status
        db.collection("users")
                .whereEqualTo("email", request.getParentEmail()) // Find the parent document where email matches parentEmail
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Ensure there's exactly one document (parent) with this email
                        if (!task.getResult().isEmpty()) {
                            // Get the parent document
                            DocumentSnapshot parentDoc = task.getResult().getDocuments().get(0);

                            // Find the kid document under this parent's "kids" subcollection
                            db.collection("users").document(parentDoc.getId()).collection("kids")
                                    .whereEqualTo("kidEmail", request.getKidEmail()) // Assuming kidEmail is unique
                                    .get()
                                    .addOnCompleteListener(kidTask -> {
                                        if (kidTask.isSuccessful()) {
                                            // Ensure there's exactly one kid document with this kidEmail
                                            if (!kidTask.getResult().isEmpty()) {
                                                // Get the kid document
                                                DocumentSnapshot kidDoc = kidTask.getResult().getDocuments().get(0);

                                                // Delete the kid document from Firestore
                                                db.collection("users").document(parentDoc.getId()).collection("kids")
                                                        .document(kidDoc.getId())
                                                        .delete()
                                                        .addOnSuccessListener(aVoid -> {
                                                            // Remove the request from the local list and notify adapter
                                                            requestsList.remove(request);
                                                            notifyDataSetChanged();

                                                            Toast.makeText(holder.itemView.getContext(), "Request declined", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("Firestore", "Error deleting kid document", e);
                                                            Toast.makeText(holder.itemView.getContext(), "Failed to decline request", Toast.LENGTH_SHORT).show();
                                                        });
                                            } else {
                                                Log.d("Firestore", "No kid document found with kidEmail: " + request.getKidEmail());
                                                Toast.makeText(holder.itemView.getContext(), "No kid document found", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Log.d("Firestore", "Error querying kid document: ", kidTask.getException());
                                            Toast.makeText(holder.itemView.getContext(), "Error querying kid document", Toast.LENGTH_SHORT).show();
                                        }

                                        // Hide progress bar after operation completes
                                        holder.progressBar.setVisibility(View.GONE);
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
                });
    }

    /**
     * Adds a new kid to the KidsAdapter based on the request information.
     *
     * @param request The request containing the kid's email.
     */
    private void addKidToAdapter(Request request) {
        Kid newKid = new Kid();
        newKid.setEmail(request.getKidEmail());

        db.collection("users")
                .whereEqualTo("email", request.getKidEmail())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            // Assuming there's only one document matching the email
                            String firstName = document.getString("firstName");
                            String lastName = document.getString("lastName");

                            // Set first and last name to the newKid object
                            newKid.setFirstName(firstName);
                            newKid.setLastName(lastName);

                            // Add the new kid to KidsAdapter
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
                });
    }

    @Override
    public int getItemCount() {
        return requestsList.size();
    }

    /**
     * ViewHolder class for request items in the RecyclerView.
     */
    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvKidEmail, tvRequestStatus;
        ImageButton btnAccept, btnDecline;
        ProgressBar progressBar;

        /**
         * Constructs a RequestViewHolder with the given view.
         *
         * @param itemView The view associated with the ViewHolder.
         */
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
