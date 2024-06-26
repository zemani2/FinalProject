package com.example.anxietyByHeartRate;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DBHelper extends SQLiteOpenHelper implements Serializable {
    public static final String DBNAME = "Login4.db";
    public static final String USERS_TABLE_NAME = "users";

    public static final String TABLE_NAME_STRESS = "stress";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";
    public static final String COL_AGE = "age"; // Add age column

    public static final String COL_HEIGHT = "height";
    public static final String COL_WEIGHT = "weight";
    public static final String TABLE_NAME_HEART_RATE = "heart_rate";
    // Columns for heart_rate table
    public static final String COL_HEART_RATE = "heart_rate";
    public static final String COL_TIMESTAMP = "timestamp";
    private Context context;
    private static final String CREATE_TABLE_HEART_RATE = "CREATE TABLE " + TABLE_NAME_HEART_RATE +
            " (" + COL_USERNAME + " TEXT, " +
            COL_HEART_RATE + " INTEGER, " +
            COL_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
    private static final String CREATE_TABLE = "CREATE TABLE " + USERS_TABLE_NAME +
            " (" + COL_USERNAME + " TEXT PRIMARY KEY, " +
            COL_PASSWORD + " TEXT, " +
            COL_AGE + " INTEGER, " +
            COL_HEIGHT + " INTEGER, " +
            COL_WEIGHT + " INTEGER)";
    private static final String CREATE_TABLE_STRESS = "CREATE TABLE " + TABLE_NAME_STRESS +
            " (" + COL_USERNAME + " TEXT, " +
            COL_HEART_RATE + " INTEGER, " +
            COL_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";


    public DBHelper(Context context) {
        super(context, DBNAME, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase myDB) {

        myDB.execSQL(CREATE_TABLE);
        myDB.execSQL(CREATE_TABLE_HEART_RATE);
        myDB.execSQL(CREATE_TABLE_STRESS);

    }
    public Map<String, String> getDetails(String username, String fields []) {
        Map<String, String> details = new HashMap<>();
        String query = "SELECT * FROM " + USERS_TABLE_NAME + " WHERE " + COL_USERNAME + "='" + username + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            for (String field : fields) {
                int columnIndex = cursor.getColumnIndex(field);
                if (columnIndex != -1) {
                    // Retrieve the value of each field from the cursor
                    String value = cursor.getString(columnIndex);
                    // Store the field name and value in the details map
                    details.put(field, value);
                }
            }
        }
        cursor.close();
        return details;
    }
    @Override
    public void onUpgrade(SQLiteDatabase myDB, int oldVersion, int newVersion) {
        myDB.execSQL("drop Table if exists "+ USERS_TABLE_NAME);
        myDB.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_HEART_RATE);
        myDB.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_STRESS);
    }
    private static boolean isStressPrompted = false; // Flag to track if stress prompt is shown
    private static boolean isStressed = false; // Flag to track user's response
    // Method to insert heart rate data
    public boolean insertHeartRate(String username, int heartRate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USERNAME, username);
        contentValues.put(COL_HEART_RATE, heartRate);
        long result = db.insert(TABLE_NAME_HEART_RATE, null, contentValues);
        if (heartRate > 65 && !isStressPrompted) {
            Log.d("isStressPrompted", "isStressPrompted : "+ isStressPrompted);
            // Prompt user with dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            isStressPrompted = true; // Stress prompt shown
            builder.setTitle("Are you in stress?")
                    .setMessage("Your heart rate is above 65. Are you in stress?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Insert stress data into new table
                            insertStressData(username, heartRate);
                            printTable(TABLE_NAME_STRESS);
                            isStressed = true; // User is stressed
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            isStressed = false; // User is not stressed
                        }
                    })
                    .show();
        }

        // Check if heart rate is below 60 and user was previously stressed
        if (heartRate < 60 && isStressed) {
            isStressPrompted = false; // Reset stress prompt flag
            isStressed = false; // Reset stress flag
        }
        return result != -1;
    }
    private void insertStressData(String username, int heartRate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USERNAME, username);
        contentValues.put(COL_HEART_RATE, heartRate);
        contentValues.put(COL_TIMESTAMP, System.currentTimeMillis()); // Timestamp
        long result = db.insert(TABLE_NAME_STRESS, null, contentValues);
        Log.d("Result:", "result: " + result);
    }
    // Method to retrieve heart rate data for a specific user
    public Cursor getHeartRateData(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COL_HEART_RATE, COL_TIMESTAMP};
        String selection = COL_USERNAME + "=?";
        String[] selectionArgs = {username};
        return db.query(TABLE_NAME_HEART_RATE, columns, selection, selectionArgs, null, null, null);
    }
    public boolean insertData(String username, String password, int age, int height, int weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USERNAME, username);
        contentValues.put(COL_PASSWORD, password);
        contentValues.put(COL_AGE, age); // Insert age into the database
        contentValues.put(COL_HEIGHT, height);
        contentValues.put(COL_WEIGHT, weight);
        long result = db.insert(USERS_TABLE_NAME, null, contentValues);
        return result != -1;
    }


    public Boolean checkusername(String username) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where username = ?", new String[]{username});
        if (cursor.getCount() > 0)
            return true;
        else
            return false;
    }

    public Boolean checkusernamepassword(String username, String password) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where username = ? and password = ?", new String[]{username, password});
        if (cursor.getCount() > 0)
            return true;
        else
            return false;
    }

    public void printTable(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + tableName, null);

        // Get the column names
        String[] columnNames = cursor.getColumnNames();

        // Print column names
        StringBuilder columnNamesBuilder = new StringBuilder();
        for (String columnName : columnNames) {
            columnNamesBuilder.append(columnName).append("\t\t");
        }
        Log.d("TableColumns", columnNamesBuilder.toString());

        // Print rows
        while (cursor.moveToNext()) {
            StringBuilder rowBuilder = new StringBuilder();
            for (String columnName : columnNames) {
                int columnIndex = cursor.getColumnIndex(columnName);
                String columnValue = cursor.getString(columnIndex);
                // Format timestamp if column is COL_TIMESTAMP
                if (columnName.equals(COL_TIMESTAMP)) {
                    long timestamp = Long.parseLong(columnValue);
                    String formattedTimestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
                    rowBuilder.append(formattedTimestamp).append("\t\t");
                } else {
                    rowBuilder.append(columnValue).append("\t\t");
                }
            }
            Log.d("TableRow", rowBuilder.toString());
        }

        cursor.close();
    }
    public List<Map<String, String>> getTableData(String tableName, String username) throws ParseException {
        List<Map<String, String>> tableData = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * " +
                                        "FROM " + tableName + " "+
                                        "WHERE "+COL_USERNAME+ "='"+username+"'", null);

        // Get the column names
        String[] columnNames = cursor.getColumnNames();

        // Iterate through the rows
        while (cursor.moveToNext()) {
            // Create a map to store column values for this row
            Map<String, String> rowData = new HashMap<>();
            for (String columnName : columnNames) {
                int columnIndex = cursor.getColumnIndex(columnName);
                String columnValue = cursor.getString(columnIndex);
                // Format timestamp if column is COL_TIMESTAMP
                if (columnName.equals(COL_TIMESTAMP)) {
                    String formattedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(columnValue)));
                    rowData.put(columnName, formattedTimestamp);
                } else {
                    rowData.put(columnName, columnValue);
                }
            }
            // Add the row data to the table data list
            tableData.add(rowData);
        }

        cursor.close();
        return tableData;
    }



}
