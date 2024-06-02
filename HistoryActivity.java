package com.example.anxietyByHeartRate;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        DBHelper db = new DBHelper(this);
        Intent intent = getIntent();
        String username = intent.getStringExtra("USERNAME");
        List<Map<String, String>> tableData = null;
        try {
            tableData = db.getTableData(DBHelper.TABLE_NAME_HEART_RATE, username);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        HeartRateAdapter adapter = new HeartRateAdapter(this, tableData);
        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(adapter);
    }
}
