package com.example.anxietyByHeartRate;

import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize DBHelper
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        // Retrieve data from Firestore
        retrieveData();  }

    private void retrieveData() {
        if (mAuth.getCurrentUser() != null) {
            // Get the current user's UID
            String uid = mAuth.getCurrentUser().getUid();

            // Reference to the "stressData" subcollection for the current user
            CollectionReference stressDataRef = db.collection("users").document(uid).collection("stressData");

            // Query the "stressData" subcollection
            stressDataRef.get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            displayData(queryDocumentSnapshots);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("HistoryActivity", "Error retrieving data: " + e.getMessage());
                        }
                    });
        } else {
            Log.e("HistoryActivity", "User is not authenticated");
        }
    }



    private void displayData(QuerySnapshot queryDocumentSnapshots) {
        TableLayout tableLayout = findViewById(R.id.tableLayout);

        // Add table headers
        TableRow headerRow = new TableRow(this);
        TextView heartRateHeader = new TextView(this);
        TextView timestampHeader = new TextView(this);

        heartRateHeader.setText("Heart Rate");
        heartRateHeader.setTextSize(20); // Set text size to 20sp
        timestampHeader.setText("Timestamp");
        timestampHeader.setTextSize(20); // Set text size to 20sp

        headerRow.addView(heartRateHeader);
        headerRow.addView(timestampHeader);

        tableLayout.addView(headerRow);

        // Add separator line
        addSeparatorLine(tableLayout);

        // Add data rows
        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            int heartRate = document.getLong("heartRate").intValue();
            Date timestamp = document.getDate("timestamp");

            String formattedTimestamp = formatDate(timestamp);

            TableRow dataRow = new TableRow(this);
            TextView heartRateTextView = new TextView(this);
            TextView timestampTextView = new TextView(this);
            heartRateTextView.setTextSize(18); // Set text size to 18sp
            timestampTextView.setTextSize(18); // Set text size to 18sp

            heartRateTextView.setText(String.valueOf(heartRate));
            timestampTextView.setText(formattedTimestamp);

            dataRow.addView(heartRateTextView);
            dataRow.addView(timestampTextView);

            tableLayout.addView(dataRow);
        }
    }


    private void addSeparatorLine(TableLayout tableLayout) {
        TableRow separatorRow = new TableRow(this);
        TextView separator = new TextView(this);
        separator.setBackgroundColor(getResources().getColor(android.R.color.black));
        separator.setHeight(2); // Set the height of the separator line
        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        params.span = 2; // Span the separator across all columns
        separator.setLayoutParams(params);
        separatorRow.addView(separator);
        tableLayout.addView(separatorRow);
    }


    private String formatDate(Date timestamp) {
        // Create a SimpleDateFormat object with the desired date and time format
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        // Format the Date object and return the formatted timestamp as a string
        return sdf.format(timestamp);
    }
}
