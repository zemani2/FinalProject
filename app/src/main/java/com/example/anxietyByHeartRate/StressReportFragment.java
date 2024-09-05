package com.example.anxietyByHeartRate;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment that displays a bar chart showing the frequency of stress events by hour of the day.
 * The data is fetched from Firestore based on the selected kid and date.
 */
public class StressReportFragment extends Fragment implements OnDateChangedListener, OnKidSelectedListener {

    private static final String ARG_SELECTED_KID_EMAIL = "selected_kid_email";
    private static final String ARG_SELECTED_DATE = "selected_date";
    private String selectedKidEmail;
    private String selectedDate;

    private BarChart stressEventsChart;
    private FirebaseFirestore db;
    private TextView stressHoursLabel;
    private TextView noDataLabel;

    public StressReportFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new instance of StressReportFragment with the provided parameters.
     *
     * @param selectedKidEmail The email of the selected kid.
     * @param selectedDate     The selected date for the report.
     * @return A new instance of StressReportFragment.
     */
    public static StressReportFragment newInstance(String selectedKidEmail, String selectedDate) {
        StressReportFragment fragment = new StressReportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_KID_EMAIL, selectedKidEmail);
        args.putString(ARG_SELECTED_DATE, selectedDate);
        fragment.setArguments(args);
        return fragment;
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
        stressEventsChart = view.findViewById(R.id.stressEventsChart);
        stressHoursLabel = view.findViewById(R.id.stressHoursLabel);
        noDataLabel = view.findViewById(R.id.noDataLabel);

        loadStressEvents();

        return view;
    }

    @Override
    public void onDateChanged(String selectedDate) {
        // Update fragment view based on new selected date
        this.selectedDate = selectedDate;
        loadStressEvents();
    }

    @Override
    public void onKidSelected(String selectedKidEmail) {
        // Update fragment view based on new selected kid
        this.selectedKidEmail = selectedKidEmail;
        loadStressEvents();
    }

    /**
     * Loads stress event data from Firestore and updates the chart and UI accordingly.
     */
    private void loadStressEvents() {
        db.collection("users")
                .document(selectedKidEmail)
                .collection("stress")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Long> stressEventTimes = new ArrayList<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                        for (DocumentSnapshot document : task.getResult()) {
                            Timestamp timestamp = document.getTimestamp("timestamp");
                            if (timestamp != null) {
                                Date date = timestamp.toDate();
                                String formattedDate = sdf.format(date);
                                if (formattedDate.equals(selectedDate)) {
                                    stressEventTimes.add(date.getTime());
                                }
                            }
                        }

                        if (stressEventTimes.isEmpty()) {
                            noDataLabel.setVisibility(View.VISIBLE);
                            stressEventsChart.setVisibility(View.GONE);
                        } else {
                            noDataLabel.setVisibility(View.GONE);
                            stressEventsChart.setVisibility(View.VISIBLE);
                            setupStressEventsChart(stressEventTimes);
                        }
                    } else {
                        Log.d("Firestore", "Error getting stressData documents: ", task.getException());
                    }
                });
    }

    /**
     * Sets up the bar chart to display stress events.
     *
     * @param stressEventTimes A list of timestamps representing stress events.
     */
    private void setupStressEventsChart(List<Long> stressEventTimes) {
        Collections.sort(stressEventTimes);

        List<BarEntry> entries = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Track stress events per hour
        Map<Integer, Integer> stressEventsPerHour = new HashMap<>();
        for (Long stressEventTime : stressEventTimes) {
            Date date = new Date(stressEventTime);
            int hour = date.getHours();
            stressEventsPerHour.put(hour, stressEventsPerHour.getOrDefault(hour, 0) + 1);
        }

        // Add stress events data to the chart entries
        for (Map.Entry<Integer, Integer> entry : stressEventsPerHour.entrySet()) {
            entries.add(new BarEntry(entry.getKey(), entry.getValue()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Stress Events");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        Description description = new Description();
        description.setText("Stress Events Timeline");
        description.setTextSize(12f);
        stressEventsChart.setDescription(description);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        stressEventsChart.setData(barData);

        XAxis xAxis = stressEventsChart.getXAxis();
        xAxis.setLabelCount(12); // Number of labels to show

        stressEventsChart.setFitBars(true);
        stressEventsChart.invalidate(); // Refresh the chart
    }
}
