package com.example.anxietyByHeartRate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * KidsAdapter is a RecyclerView adapter used to display a list of Kid objects in a RecyclerView.
 * Each item represents a child's details such as their name and email.
 */
public class KidsAdapter extends RecyclerView.Adapter<KidsAdapter.KidViewHolder> {

    // List to hold Kid objects
    private List<Kid> kidsList;

    /**
     * Constructor for KidsAdapter to initialize the list of kids.
     *
     * @param kidsList The initial list of kids to display.
     */
    public KidsAdapter(List<Kid> kidsList) {
        this.kidsList = kidsList;
    }

    /**
     * Creates and inflates the view for a single item (child) in the RecyclerView.
     *
     * @param parent The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new view.
     * @return A new instance of KidViewHolder containing the inflated layout.
     */
    @NonNull
    @Override
    public KidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kid, parent, false);
        return new KidViewHolder(view);
    }

    /**
     * Binds the data for a specific Kid object to the views in the ViewHolder.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull KidViewHolder holder, int position) {
        Kid kid = kidsList.get(position);
        holder.tvKidName.setText(kid.getFirstName() + " " + kid.getLastName());  // Display the kid's full name
        holder.tvKidEmail.setText(kid.getEmail());  // Display the kid's email address
    }

    /**
     * Returns the total number of items (kids) in the data set held by the adapter.
     *
     * @return The number of kids in the list.
     */
    @Override
    public int getItemCount() {
        return kidsList.size();
    }

    /**
     * Adds a new Kid to the kidsList and updates the RecyclerView.
     *
     * @param kid The Kid object to be added.
     */
    public void addKid(Kid kid) {
        kidsList.add(kid);
        notifyItemInserted(kidsList.size() - 1);  // Notify that a new item has been added at the last position
    }

    /**
     * Updates the entire list of kids and notifies the RecyclerView of the changes.
     *
     * @param newKidsList The new list of kids to replace the existing data set.
     */
    public void updateKidsList(List<Kid> newKidsList) {
        this.kidsList = newKidsList;
        notifyDataSetChanged();  // Notify that the data set has changed
    }

    /**
     * ViewHolder class to hold the view elements for a single kid item in the RecyclerView.
     */
    static class KidViewHolder extends RecyclerView.ViewHolder {

        // TextViews for displaying the kid's name and email
        TextView tvKidName, tvKidEmail;

        /**
         * Constructor for KidViewHolder to initialize the views for each list item.
         *
         * @param itemView The view for a single item in the RecyclerView.
         */
        KidViewHolder(View itemView) {
            super(itemView);
            tvKidName = itemView.findViewById(R.id.tvKidName);  // Reference to TextView for kid's name
            tvKidEmail = itemView.findViewById(R.id.tvKidEmail);  // Reference to TextView for kid's email
        }
    }
}
