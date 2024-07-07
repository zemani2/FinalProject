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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SleepReportFragment extends Fragment implements OnDateChangedListener, OnKidSelectedListener{

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
        // Get the PieChart views
        batteryView = view.findViewById(R.id.batteryView);
        batteryTextView = view.findViewById(R.id.batteryTextView);
        pieChartSleepQuality = view.findViewById(R.id.pieChartSleepQuality);
        loadSleepData();

        return view;
    }
    private void loadSleepData() {
        db.collection("users")
                .document(selectedKidEmail)
                .collection("sleepData")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> stressHours = new ArrayList<>();
                        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        for (DocumentSnapshot document : task.getResult()) {
                            String date = document.getString("timestamp");
                            if (date.equals(selectedDate)) {
                                deepSleepSeconds = document.getLong("deep_sleep_seconds");
                                lightSleepSeconds = document.getLong("light_sleep_seconds");
                                remSleepSeconds = document.getLong("rem_sleep_seconds");
                                awakeSleepSeconds = document.getLong("awake_sleep_seconds");

                                updateBatteryView(deepSleepSeconds+lightSleepSeconds+remSleepSeconds);


                                // Setup sleep quality chart
                                setupSleepQualityChart(pieChartSleepQuality);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting stressData documents: ", task.getException());
                    }
                });
    }
    private void updateBatteryView(float totalSleepTime) {
        int hours = (int) (totalSleepTime / 3600);
        int minuets = (int) (totalSleepTime % 3600 / 60);
        batteryView.setVisibility(View.VISIBLE);
        batteryTextView.setText("Total amount of sleep: " + hours +":"+minuets+" hours");
        if (totalSleepTime < 21600) { // 7 hours (low)
            batteryView.setImageResource(R.drawable.ic_low_battery);
        } else if (totalSleepTime < 25200) {
            batteryView.setImageResource(R.drawable.ic_med_battery);
        } else {
            batteryView.setImageResource(R.drawable.ic_high_battery);
        }
    }
    private void setupTotalSleepChart(PieChart pieChart) {
        float totalSleepTime = deepSleepSeconds + lightSleepSeconds + remSleepSeconds + awakeSleepSeconds;

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(deepSleepSeconds, "Deep Sleep"));
        entries.add(new PieEntry(lightSleepSeconds, "Light Sleep"));
        entries.add(new PieEntry(remSleepSeconds, "REM Sleep"));
        entries.add(new PieEntry(awakeSleepSeconds, "Awake"));

        PieDataSet dataSet = new PieDataSet(entries, "Total Sleep Time");
        dataSet.setColors(new int[]{Color.parseColor("#1f77b4"),
                Color.parseColor("#2ca02c"),
                Color.parseColor("#ff7f0e"),
                Color.parseColor("#9467bd")});
        PieData pieData = new PieData(dataSet);
        dataSet.setDrawValues(false);
        pieChart.setEntryLabelTextSize(0);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);

        pieChart.invalidate(); // refresh
    }

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
        pieChart.setCenterText("Sleep Quality");
        pieChart.invalidate(); // refresh
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDateChanged(String selectedDate) {
        // Update fragment view based on new selected kid
        this.selectedDate = selectedDate;
        // Call a method to refresh your UI with new selectedKidEmail

        batteryView.setVisibility(View.GONE);
        batteryTextView.setText("No data to display");
        pieChartSleepQuality.clear();
        loadSleepData();
    }

    @Override
    public void onKidSelected(String selectedKidEmail) {
        // Update fragment view based on new selected kid
        this.selectedKidEmail = selectedKidEmail;
        batteryView.setVisibility(View.GONE);
        batteryTextView.setText("No data to display");
        pieChartSleepQuality.clear();
        // Call a method to refresh your UI with new selectedKidEmail
        loadSleepData();

    }
}
