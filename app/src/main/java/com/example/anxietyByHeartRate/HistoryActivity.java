package com.example.anxietyByHeartRate;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize DBHelper
        dbHelper = new DBHelper(this);

        // Retrieve data from the database
        Cursor cursor = retrieveData();

        // Display data in the table layout
        displayData(cursor);
    }

    private Cursor retrieveData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {DBHelper.COL_HEART_RATE, DBHelper.COL_TIMESTAMP}; // Remove username column
        String sortOrder = DBHelper.COL_TIMESTAMP + " DESC"; // Sort by timestamp in descending order
        return db.query(DBHelper.TABLE_NAME_STRESS, projection, null, null, null, null, sortOrder);
    }

    private void displayData(Cursor cursor) {
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
        while (cursor.moveToNext()) {
            int heartRate = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_HEART_RATE));
            long timestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_TIMESTAMP));

            String formattedTimestamp = formatDate(timestampMillis);

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

        // Close cursor after use
        cursor.close();
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


    private String formatDate(long timestampMillis) {
        // Create a SimpleDateFormat object with the desired date and time format
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        // Convert the timestamp to a Date object
        Date date = new Date(timestampMillis);
        // Format the Date object and return the formatted timestamp as a string
        return sdf.format(date);
    }
}
