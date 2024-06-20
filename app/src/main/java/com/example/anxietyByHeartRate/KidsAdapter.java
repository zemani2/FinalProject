package com.example.anxietyByHeartRate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class KidsAdapter extends RecyclerView.Adapter<KidsAdapter.KidViewHolder> {
    private List<Kid> kidsList;

    public KidsAdapter(List<Kid> kidsList) {
        this.kidsList = kidsList;
    }

    @NonNull
    @Override
    public KidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kid, parent, false);
        return new KidViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KidViewHolder holder, int position) {
        Kid kid = kidsList.get(position);
        holder.tvKidName.setText(kid.getFirstName() + " " + kid.getLastName());
        holder.tvKidEmail.setText(kid.getEmail());
    }

    @Override
    public int getItemCount() {
        return kidsList.size();
    }

    public void addKid(Kid kid) {
        kidsList.add(kid);
        notifyItemInserted(kidsList.size() - 1); // Notify adapter that an item was inserted at the last position
    }

    static class KidViewHolder extends RecyclerView.ViewHolder {
        TextView tvKidName, tvKidEmail;

        KidViewHolder(View itemView) {
            super(itemView);
            tvKidName = itemView.findViewById(R.id.tvKidName);
            tvKidEmail = itemView.findViewById(R.id.tvKidEmail);
        }
    }
}
