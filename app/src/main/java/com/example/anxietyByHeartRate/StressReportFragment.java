package com.example.anxietyByHeartRate;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StressReportFragment extends Fragment implements OnDateChangedListener, OnKidSelectedListener {
    private static final String ARG_SELECTED_KID_EMAIL = "selected_kid_email";
    private static final String ARG_SELECTED_DATE = "selected_date";
    private String selectedKidEmail;
    private String selectedDate;
    private WeakReference<Context> contextRef;

    private ListView stressHoursListView;
    private FirebaseFirestore db;
    private TextView stressHoursLabel;
    private TextView noDataLabel;
    public StressReportFragment() {
        // Required empty public constructor
    }

    public static StressReportFragment newInstance(String selectedKidEmail, String selectedDate) {
        StressReportFragment fragment = new StressReportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_KID_EMAIL, selectedKidEmail);
        args.putString(ARG_SELECTED_DATE, selectedDate);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        contextRef = new WeakReference<>(context);
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedKidEmail = getArguments().getString(ARG_SELECTED_KID_EMAIL);
            selectedDate = getArguments().getString(ARG_SELECTED_DATE);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stress_report, container, false);
        stressHoursListView = view.findViewById(R.id.stressHoursListView);
        stressHoursLabel = view.findViewById(R.id.stressHoursLabel);
        noDataLabel = view.findViewById(R.id.noDataLabel);
        loadStressData();
        return view;
    }
    @Override
    public void onDateChanged(String selectedDate) {
        // Update fragment view based on new selected date
        this.selectedDate = selectedDate;
        // Call a method to refresh your UI with new selectedDate
        loadStressData();
    }

    @Override
    public void onKidSelected(String selectedKidEmail) {
        // Update fragment view based on new selected kid
        this.selectedKidEmail = selectedKidEmail;
        // Call a method to refresh your UI with new selectedKidEmail
        loadStressData();
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
                                Date date = timestamp.toDate();
                                String formattedDate = dayFormat.format(date);
                                if (formattedDate.equals(selectedDate)){
                                    stressHours.add(date.toString());
                                }
                            }
                        }
                        if (stressHours.isEmpty()){
                            noDataLabel.setVisibility(View.VISIBLE);
                        } else {
                            noDataLabel.setVisibility(View.GONE);
                        }
                        Collections.sort(stressHours);
                        Context context = contextRef.get();
                        if (context != null) {
                            StressListAdapter adapter = new StressListAdapter(stressHours, context);
                            stressHoursListView.setAdapter(adapter);
                        } else {
                            Log.e("StressReportFragment", "Context is null");
                        }
                    } else {
                        Log.d("Firestore", "Error getting stressData documents: ", task.getException());
                    }
                });
    }

    private class StressListAdapter extends ArrayAdapter<String> {

        private List<String> stressHours;

        public StressListAdapter(List<String> stressHours, Context context) {
            super(context, R.layout.item_stress_hour, stressHours);
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
