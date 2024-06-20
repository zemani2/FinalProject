package com.example.anxietyByHeartRate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ListView stressHoursListView;
    private FirebaseFirestore db;
    private List<String> kidEmails = new ArrayList<>();
    private List<String> kidNames = new ArrayList<>();
    private String selectedKidEmail;
    private KidAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        recyclerView = findViewById(R.id.kidNamesRecyclerView);
        stressHoursListView = findViewById(R.id.stressHoursListView);
        db = FirebaseFirestore.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadKidNames();
    }

    private void loadKidNames() {
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final String[] firstName = {""};
        final String[] lastName = {""};

        db.collection("users").document(currentUserUid).collection("kids")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        kidEmails = new ArrayList<>();
                        kidNames = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            String kidEmail = document.getString("kidEmail");
                            db.collection("users")
                                    .whereEqualTo("email", kidEmail)
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    firstName[0] = document.getString("firstName");
                                                    lastName[0] = document.getString("lastName");
                                                }
                                                kidEmails.add(kidEmail);
                                                kidNames.add(firstName[0] + " " + lastName[0]);
                                                if (adapter == null) {
                                                    adapter = new KidAdapter(kidNames);
                                                    recyclerView.setAdapter(adapter);
                                                    adapter.setOnItemClickListener((view, position) -> {
                                                        String selectedKidName = kidNames.get(position);
                                                        Toast.makeText(ReportActivity.this, "Selected: " + selectedKidName, Toast.LENGTH_SHORT).show();
                                                        selectedKidEmail = kidEmails.get(position);
                                                        adapter.setSelectedPosition(position);
                                                        loadStressData();
                                                    });
                                                    // Show the first kid by default
                                                    if (!kidEmails.isEmpty()) {
                                                        selectedKidEmail = kidEmails.get(0);
                                                        adapter.setSelectedPosition(0);
                                                        loadStressData();
                                                    }
                                                } else {
                                                    adapter.notifyDataSetChanged();
                                                }
                                            } else {
                                                Log.d("Firestore", "Error getting documents: ", task.getException());
                                            }
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(ReportActivity.this, "Failed to load kid names: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadStressData() {
        db.collection("users")
                .whereEqualTo("email", selectedKidEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);

                            db.collection("users").document(userDoc.getId()).collection("stressData")
                                    .get()
                                    .addOnCompleteListener(stressDataTask -> {
                                        if (stressDataTask.isSuccessful()) {
                                            List<String> stressHours = new ArrayList<>();
                                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                            for (DocumentSnapshot document : stressDataTask.getResult()) {
                                                String timestamp = document.getString("timeStamp");
                                                stressHours.add(timestamp);
                                            }

                                            StressListAdapter adapter = new StressListAdapter(stressHours);
                                            stressHoursListView.setAdapter(adapter);
                                        } else {
                                            Log.d("Firestore", "Error getting stressData documents: ", stressDataTask.getException());
                                        }
                                    });
                        } else {
                            Log.d("Firestore", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private static class KidAdapter extends RecyclerView.Adapter<KidAdapter.ViewHolder> {

        private List<String> kidNames;
        private OnItemClickListener listener;
        private int selectedPosition = RecyclerView.NO_POSITION;

        public KidAdapter(List<String> kidNames) {
            this.kidNames = kidNames;
        }

        public interface OnItemClickListener {
            void onItemClick(View view, int position);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        public void setSelectedPosition(int position) {
            int previousPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousPosition);
            notifyItemChanged(position);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kid_name, parent, false);
            return new ViewHolder(view, listener);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.kidName.setText(kidNames.get(position));
            if (position == selectedPosition) {
                holder.kidName.setTextSize(20);  // Set text size to larger
                holder.kidName.setTypeface(null, android.graphics.Typeface.BOLD);  // Set text to bold
            } else {
                holder.kidName.setTextSize(16);  // Reset text size
                holder.kidName.setTypeface(null, android.graphics.Typeface.NORMAL);  // Reset text to normal
            }
        }

        @Override
        public int getItemCount() {
            return kidNames.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {

            TextView kidName;

            public ViewHolder(@NonNull View itemView, OnItemClickListener listener) {
                super(itemView);
                kidName = itemView.findViewById(R.id.kidName);

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(itemView, position);
                        }
                    }
                });
            }
        }
    }

    private class StressListAdapter extends ArrayAdapter<String> {

        private List<String> stressHours;

        public StressListAdapter(List<String> stressHours) {
            super(ReportActivity.this, R.layout.item_stress_hour, stressHours);
            this.stressHours = stressHours;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_stress_hour, parent, false);
            }

            TextView stressHourTextView = convertView.findViewById(R.id.stressHourTextView);
            stressHourTextView.setText(stressHours.get(position));

            return convertView;
        }
    }
}
