package com.example.anxietyByHeartRate;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that displays sleep report data for a selected kid on a selected date.
 * This includes visualizing sleep quality and battery-like indicators for total sleep time.
 */
public class SleepReportFragment extends Fragment implements OnDateChangedListener, OnKidSelectedListener {

    private static final String ARG_SELECTED_KID_EMAIL = "selected_kid_email";
    private static final String ARG_SELECTED_DATE = "selected_date";
    private String selectedKidEmail;
    private String selectedDate;
    private float deepSleepSeconds;
    private float lightSleepSeconds;
    private float remSleepSeconds;
    private float awakeSleepSeconds;
    private FirebaseFirestore db;
    private ImageView batteryView;
    private TextView batteryTextView;
    private PieChart pieChartSleepQuality;

    /**
     * Creates a new instance of SleepReportFragment with the given parameters.
     *
     * @param selectedKidEmail The email of the selected kid.
     * @param selectedDate     The selected date for the report.
     * @return A new instance of SleepReportFragment.
     */
    public static SleepReportFragment newInstance(String selectedKidEmail, String selectedDate) {
        SleepReportFragment fragment = new SleepReportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_KID_EMAIL, selectedKidEmail);
        args.putString(ARG_SELECTED_DATE, selectedDate);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sleep_report, container, false);

        if (getArguments() != null) {
            selectedKidEmail = getArguments().getString(ARG_SELECTED_KID_EMAIL);
            selectedDate = getArguments().getString(ARG_SELECTED_DATE);
        }

        db = FirebaseFirestore.getInstance();
        // Initialize UI elements
        batteryView = view.findViewById(R.id.batteryView);
        batteryTextView = view.findViewById(R.id.batteryTextView);
        pieChartSleepQuality = view.findViewById(R.id.pieChartSleepQuality);

        loadSleepData();
        return view;
    }

    /**
     * Loads sleep data from Firestore and updates the UI.
     */
    private void loadSleepData() {
        db.collection("users")
                .document(selectedKidEmail)
                .collection("sleepData")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String date = document.getString("timestamp");
                            if (date.equals(selectedDate)) {
                                deepSleepSeconds = document.getLong("deep_sleep_seconds");
                                lightSleepSeconds = document.getLong("light_sleep_seconds");
                                remSleepSeconds = document.getLong("rem_sleep_seconds");
                                awakeSleepSeconds = document.getLong("awake_sleep_seconds");

                                updateBatteryView(deepSleepSeconds + lightSleepSeconds + remSleepSeconds);
                                setupSleepQualityChart(pieChartSleepQuality);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting sleepData documents: ", task.getException());
                    }
                });
    }

    /**
     * Updates the battery-like indicator based on the total sleep time.
     *
     * @param totalSleepTime The total amount of sleep time in seconds.
     */
    private void updateBatteryView(float totalSleepTime) {
        int hours = (int) (totalSleepTime / 3600);
        int minutes = (int) (totalSleepTime % 3600 / 60);
        batteryView.setVisibility(View.VISIBLE);
        batteryTextView.setText("Total amount of sleep: " + hours + ":" + minutes + " hours");

        if (totalSleepTime < 21600) { // 6 hours (low)
            batteryView.setImageResource(R.drawable.ic_low_battery);
        } else if (totalSleepTime < 25200) { // 7 hours (medium)
            batteryView.setImageResource(R.drawable.ic_med_battery);
        } else {
            batteryView.setImageResource(R.drawable.ic_high_battery);
        }
    }

    /**
     * Sets up the sleep quality chart using the provided PieChart view.
     *
     * @param pieChart The PieChart view to be set up.
     */
    private void setupSleepQualityChart(PieChart pieChart) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(deepSleepSeconds, "Deep Sleep"));
        entries.add(new PieEntry(lightSleepSeconds, "Light Sleep"));
        entries.add(new PieEntry(remSleepSeconds, "REM Sleep"));
        entries.add(new PieEntry(awakeSleepSeconds, "Awake"));

        PieDataSet dataSet = new PieDataSet(entries, "Sleep Quality");
        dataSet.setColors(new int[]{Color.parseColor("#1f77b4"),
                Color.parseColor("#2ca02c"),
                Color.parseColor("#ff7f0e"),
                Color.parseColor("#9467bd")});
        PieData pieData = new PieData(dataSet);

        pieChart.setCenterText("Sleep Quality");
        pieChart.setEntryLabelTextSize(0);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.invalidate(); // refresh
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Handles date change events and refreshes the data accordingly.
     *
     * @param selectedDate The new selected date for the report.
     */
    @Override
    public void onDateChanged(String selectedDate) {
        this.selectedDate = selectedDate;
        batteryView.setVisibility(View.GONE);
        batteryTextView.setText("No data to display");
        pieChartSleepQuality.clear();
        loadSleepData();
    }

    /**
     * Handles kid selection events and refreshes the data accordingly.
     *
     * @param selectedKidEmail The email of the newly selected kid.
     */
    @Override
    public void onKidSelected(String selectedKidEmail) {
        this.selectedKidEmail = selectedKidEmail;
        batteryView.setVisibility(View.GONE);
        batteryTextView.setText("No data to display");
        pieChartSleepQuality.clear();
        loadSleepData();
    }
}
