package com.example.anxietyByHeartRate;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ReportActivity extends AppCompatActivity {

    private ListView stressHoursListView;
    private FirebaseFirestore db;
    private List<String> kidEmails = new ArrayList<>();
    private List<String> kidNames = new ArrayList<>();
    private String selectedKidEmail;
    private Button datePickerButton, buttonStressReport, buttonMapReport, buttonSleepReport;
    private TextView selectedDaysLabel;
    private Spinner kidNamesSpinner;
    private String selectedDate;
    private TextView noDataLabel;
    private SharedPreferences sharedPreferences;
    private static final String FRAGMENT_TAG_KEY = "current_fragment_tag";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        kidNamesSpinner = findViewById(R.id.kidNamesSpinner);
        db = FirebaseFirestore.getInstance();
        selectedDaysLabel = findViewById(R.id.selectedDaysLabel);
        datePickerButton = findViewById(R.id.datePickerButton);
        buttonSleepReport = findViewById(R.id.buttonSleepReport);
        buttonStressReport = findViewById(R.id.buttonStressReport);
        buttonMapReport = findViewById(R.id.buttonMapReport);
        sharedPreferences = getPreferences(MODE_PRIVATE);
        datePickerButton.setOnClickListener(v -> showDatePickerDialog());
        buttonStressReport.setOnClickListener(v -> loadStressDataFragment());
        buttonMapReport.setOnClickListener(v -> loadMapDataFragment());
        buttonSleepReport.setOnClickListener(v -> loadSleepReportFragment());
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        setDefaultDate();
        loadKidNames();
        restoreFragmentState();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    private void setDefaultDate() {
        final Calendar calendar = Calendar.getInstance();
        String year = Integer.toString(calendar.get(Calendar.YEAR));
        String month = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
        String day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));

        selectedDate = year + "-" + month + "-" + day;
        datePickerButton.setText(selectedDate);
        updateFragmentsOnDateChanged(selectedDate);

    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                ReportActivity.this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String monthStr = String.format("%02d", monthOfYear + 1);
                    String dayStr = String.format("%02d", dayOfMonth);
                    selectedDate = year1 + "-" + monthStr + "-" + dayStr;
                    datePickerButton.setText(selectedDate);
//                    restoreFragmentState();
                    updateFragmentsOnDateChanged(selectedDate);
                    },
                year, month, day);
// Set the initial date to the last selected date if available
        if (selectedDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf.parse(selectedDate);
                if (date != null) {
                    calendar.setTime(date);
                    datePickerDialog.updateDate(calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        datePickerDialog.show();    }

    private void loadStressDataFragment() {
        StressReportFragment fragment = StressReportFragment.newInstance(selectedKidEmail, selectedDate);
        replaceFragment(fragment, "stress_fragment");
    }
    private void loadSleepReportFragment() {
        SleepReportFragment fragment = SleepReportFragment.newInstance(selectedKidEmail, selectedDate);
        replaceFragment(fragment, "sleep_fragment");
    }
    private void loadMapDataFragment() {
        MapFragment mapFragment = MapFragment.newInstance(selectedDate);
        replaceFragment(mapFragment, "map_fragment");
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Remove any existing fragment with the same tag
        Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (existingFragment != null) {
            transaction.remove(existingFragment);
        }
        switch (tag){
            case "map_fragment":
                buttonMapReport.setTextColor(Color.BLACK);
                buttonStressReport.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                buttonSleepReport.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                break;
            case "stress_fragment":
                buttonStressReport.setTextColor(Color.BLACK);
                buttonMapReport.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                buttonSleepReport.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                break;
            case "sleep_fragment":
                buttonSleepReport.setTextColor(Color.BLACK);
                buttonMapReport.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                buttonStressReport.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                break;

        }

        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.addToBackStack(null);  // Optional: Add transaction to back stack
        transaction.commit();
    }

    private void restoreFragmentState() {
        String currentFragmentTag = sharedPreferences.getString(FRAGMENT_TAG_KEY, "");
        if (!currentFragmentTag.isEmpty()) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
            if (currentFragment != null) {
                replaceFragment(currentFragment, currentFragmentTag);
            }
        }
    }

    private void loadKidNames() {
        String currentUserEmail = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail();

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
                                                Collections.sort(kidNames);
                                                Collections.sort(kidEmails);
                                                ArrayAdapter<String> adapter = new ArrayAdapter<>(ReportActivity.this, android.R.layout.simple_spinner_item, kidNames);
                                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                                kidNamesSpinner.setAdapter(adapter);

                                                // Set a listener for Spinner item selection
                                                kidNamesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                    @Override
                                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                                        Collections.sort(kidEmails);
                                                        selectedKidEmail = kidEmails.get(position);
                                                        updateFragmentsOnKidSelected(selectedKidEmail);
                                                    }

                                                    @Override
                                                    public void onNothingSelected(AdapterView<?> parent) {
                                                        Collections.sort(kidEmails);
                                                        selectedKidEmail = kidEmails.get(0);
                                                        loadStressDataFragment();
                                                    }
                                                });

                                                // Show the first kid by default
                                                if (!kidEmails.isEmpty()) {
                                                    Collections.sort(kidEmails);
                                                    selectedKidEmail = kidEmails.get(0);
                                                    loadStressDataFragment();
                                                }
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

    private void updateFragmentsOnDateChanged(String selectedDate) {
        // Find all instances of StressReportFragment and MapFragment
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof OnDateChangedListener) {
                ((OnDateChangedListener) fragment).onDateChanged(selectedDate);
            }
        }
    }

    private void updateFragmentsOnKidSelected(String selectedKidEmail) {
        // Find all instances of StressReportFragment
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof OnKidSelectedListener) {
                ((OnKidSelectedListener) fragment).onKidSelected(selectedKidEmail);
            }
        }
    }
}
