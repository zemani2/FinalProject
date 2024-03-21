package com.example.anxietyByHeartRate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    TextInputLayout usernameInputLayout, passwordInputLayout;
    Button btnlogin;
    DBHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInputLayout = findViewById(R.id.usernameTextInputLayout);
        passwordInputLayout = findViewById(R.id.passwordTextInputLayout);
        btnlogin = findViewById(R.id.btnsignin1);
        DB = new DBHelper(this);

        btnlogin.setOnClickListener(view -> {
            String user = usernameInputLayout.getEditText().getText().toString();
            String pass = passwordInputLayout.getEditText().getText().toString();

            if(user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter all the fields", Toast.LENGTH_SHORT).show();
            } else {
                Boolean checkuserpass = DB.checkusernamepassword(user, pass);
                if(checkuserpass){
                    Toast.makeText(LoginActivity.this, "Sign in successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, HomePageActivity.class);
                    intent.putExtra("USERNAME", user);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
