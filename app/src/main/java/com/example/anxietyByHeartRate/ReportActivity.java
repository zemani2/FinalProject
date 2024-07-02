    package com.example.anxietyByHeartRate;

    import android.annotation.SuppressLint;
    import android.app.DatePickerDialog;
    import android.icu.util.Calendar;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.AdapterView;
    import android.widget.ArrayAdapter;
    import android.widget.Button;
    import android.widget.DatePicker;
    import android.widget.ListView;
    import android.widget.Spinner;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.RecyclerView;

    import com.google.firebase.Timestamp;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FirebaseFirestore;

    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.Date;
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
        private Button datePickerButton;
        private TextView stressHoursLabel;
        private Spinner kidNamesSpinner;
        private String selectedDate;
        private TextView noDataLabel;

        @SuppressLint("MissingInflatedId")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_report);

    //        recyclerView = findViewById(R.id.kidNamesRecyclerView);
            kidNamesSpinner = findViewById(R.id.kidNamesSpinner);

            stressHoursListView = findViewById(R.id.stressHoursListView);
            db = FirebaseFirestore.getInstance();
            stressHoursLabel = findViewById(R.id.stressHoursLabel);
            noDataLabel = findViewById(R.id.noDataLabel);
            setDefaultDate();
            datePickerButton = findViewById(R.id.datePickerButton);
            datePickerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePickerDialog();
                }
            });
            //        recyclerView.setLayoutManager(new LinearLayoutManager(this));

            loadKidNames();
        }
        private void setDefaultDate() {
            final Calendar calendar = Calendar.getInstance();
            String year = Integer.toString(calendar.get(Calendar.YEAR));
            String month = calendar.get(Calendar.MONTH) + 1 >= 10 ? Integer.toString(calendar.get(Calendar.MONTH)+1) :
                    "0"+Integer.toString(calendar.get(Calendar.MONTH)+1);
            String day = calendar.get(Calendar.DAY_OF_MONTH) >= 10 ? Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)) :
                    "0"+Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));

            selectedDate = year + "-" + month + "-" + day;
            stressHoursLabel.setText("Selected Date: " + selectedDate);
        }
        private void showDatePickerDialog() {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    ReportActivity.this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            // Handle the selected date
                            String monthStr = month + 1 >= 10 ? Integer.toString(month+1) :
                                    "0"+ (month+1);
                            String dayStr = dayOfMonth >= 10 ? Integer.toString(dayOfMonth) :
                                    "0" + dayOfMonth;
                            selectedDate = year + "-" + monthStr + "-" + dayStr;
                            stressHoursLabel.setText("Selected Date: " + selectedDate);
                            loadStressData();
                            // You can also use the selected date to filter data, etc.
                        }
                    },
                    year, month, day);
            datePickerDialog.show();
        }
        private void loadKidNames() {
            String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            db.collection("users")
                    .document(currentUserEmail)
                    .collection("kids")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            kidEmails.clear();
                            kidNames.clear();
                            for (DocumentSnapshot document : task.getResult()) {
                                String kidEmail = document.getString("kidEmail");
                                if (kidEmail != null) {
                                    db.collection("users")
                                            .whereEqualTo("email", kidEmail)
                                            .get()
                                            .addOnCompleteListener(kidTask -> {
                                                if (kidTask.isSuccessful()) {
                                                    for (DocumentSnapshot kidDocument : kidTask.getResult()) {
                                                        String firstName = kidDocument.getString("firstName");
                                                        String lastName = kidDocument.getString("lastName");
                                                        if (firstName != null && lastName != null) {
                                                            String fullName = firstName + " " + lastName;
                                                            kidNames.add(fullName);
                                                            kidEmails.add(kidEmail);
                                                        }
                                                    }
                                                    // Populate the Spinner with kid names
                                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ReportActivity.this, android.R.layout.simple_spinner_item, kidNames);
                                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                                    kidNamesSpinner.setAdapter(adapter);

                                                    // Set a listener for Spinner item selection
                                                    kidNamesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                        @Override
                                                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                                            selectedKidEmail = kidEmails.get(position);
                                                            loadStressData();
                                                        }

                                                        @Override
                                                        public void onNothingSelected(AdapterView<?> parent) {
                                                            Collections.sort(kidEmails);
                                                            selectedKidEmail = kidEmails.get(0);
                                                            loadStressData();                                                    }
                                                    });

                                                    // Show the first kid by default
                                                    if (!kidEmails.isEmpty()) {
                                                    }
    //                                                if (adapter == null) {
    //                                                    adapter = new KidAdapter(kidNames);
    //                                                    recyclerView.setAdapter(adapter);
    //                                                    adapter.setOnItemClickListener((view, position) -> {
    //                                                        String selectedKidName = kidNames.get(position);
    //                                                        Toast.makeText(ReportActivity.this, "Selected: " + selectedKidName, Toast.LENGTH_SHORT).show();
    //                                                        selectedKidEmail = kidEmails.get(position);
    //                                                        adapter.setSelectedPosition(position);
    //                                                        loadStressData();
    //                                                    });
    //                                                    // Show the first kid by default
    //                                                    if (!kidEmails.isEmpty()) {
    //                                                        selectedKidEmail = kidEmails.get(0);
    //                                                        adapter.setSelectedPosition(0);
    //                                                        loadStressData();
    //                                                    }
    //                                                } else {
    //                                                    adapter.notifyDataSetChanged();
    //                                                }
                                                } else {
                                                    Log.d("Firestore", "Error getting kid documents: ", kidTask.getException());
                                                }
                                            });
                                }
                            }
                        } else {
                            Toast.makeText(ReportActivity.this, "Failed to load kid names: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        private void loadStressData() {
            db.collection("users")
                    .document(selectedKidEmail)
                    .collection("stress")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<String> stressHours = new ArrayList<>();
                            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            for (DocumentSnapshot document : task.getResult()) {
                                Timestamp timestamp = document.getTimestamp("timestamp");
                                if (timestamp != null) {
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                    // Convert timestamp to Date object
                                    Date date = timestamp.toDate();
                                    String formattedDate = dayFormat.format(date);
                                    if (formattedDate.equals(selectedDate)){
                                        stressHours.add(date.toString());
                                    }                                }
                            }
                            if (stressHours.isEmpty()){
                                noDataLabel.setVisibility(View.VISIBLE);
                            }else{
                                noDataLabel.setVisibility(View.GONE);
                            }
                            Collections.sort(stressHours);
                            StressListAdapter adapter = new StressListAdapter(stressHours);
                            stressHoursListView.setAdapter(adapter);
                        } else {
                            Log.d("Firestore", "Error getting stressData documents: ", task.getException());
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
