package com.example.idanlogin;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    EditText username, password, repassword;
    Button signup, signin;
    private ToggleButton toggleButtonPassword;
    private ToggleButton toggleButtonRePassword;
    private Spinner ageSpinner;
    private Spinner heightSpinner;
    private Spinner weightSpinner;


    DBHelper DB;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        repassword = (EditText) findViewById(R.id.repassword);
        signin = (Button) findViewById(R.id.signin);
        signup = (Button) findViewById(R.id.signup);
        DB = new DBHelper(this);
        toggleButtonPassword = findViewById(R.id.toggleButtonpassword);
        toggleButtonRePassword = findViewById(R.id.toggleButtonrepassword);
        heightSpinner = findViewById(R.id.heightSpinner);
        weightSpinner = findViewById(R.id.weightSpinner);
        heightSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedHeight = (String) parent.getItemAtPosition(position);
                // Do something with the selected height
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        ageSpinner = findViewById(R.id.ageSpinner);
        List<String> ageOptions = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            ageOptions.add(String.valueOf(i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ageOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ageSpinner.setAdapter(adapter);
        List<String> weightOptions = new ArrayList<>();
        for (int i = 30; i <= 120; i++) {
            weightOptions.add(String.valueOf(i) + " kg");
        }
        ArrayAdapter<String> weight_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weightOptions);
        weight_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weightSpinner.setAdapter(weight_adapter);
        List<String> heightOptions = new ArrayList<>();
        for (int i = 70; i <= 200; i++) {
            heightOptions.add(String.valueOf(i) + " cm");
        }
        ArrayAdapter<String> height_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, heightOptions);
        height_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        heightSpinner.setAdapter(height_adapter);

        toggleButtonPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Show/Hide the password
                if (isChecked) {
                    password.setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                // Move cursor to the end of the text
                password.setSelection(password.getText().length());
            }
        });
        toggleButtonRePassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Show/Hide the password
                if (isChecked) {
                    repassword.setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    repassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                // Move cursor to the end of the text
                repassword.setSelection(repassword.getText().length());
            }
        });
        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = username.getText().toString();
                String pass = password.getText().toString();
                String repass = repassword.getText().toString();
                int age = Integer.parseInt(ageSpinner.getSelectedItem().toString());
                int weight = Integer.parseInt(weightSpinner.getSelectedItem().toString().replaceAll("[^0-9]", ""));
                int height = Integer.parseInt(heightSpinner.getSelectedItem().toString().replaceAll("[^0-9]", ""));
                if (user.equals("") || pass.equals("") || repass.equals(""))
                    Toast.makeText(MainActivity.this, "Please enter all the fields", Toast.LENGTH_SHORT).show();
                else {
                    if (pass.equals(repass)) {
                        Boolean checkuser = DB.checkusername(user);
                        if (checkuser == false) {
                            Boolean insert = DB.insertData(user, pass, age, height, weight); // Pass age to the insertData method
                            if (insert == true) {
                                Toast.makeText(MainActivity.this, "Registered successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this, HeartRateActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(MainActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "User already exists! please sign in", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Passwords not matching", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}