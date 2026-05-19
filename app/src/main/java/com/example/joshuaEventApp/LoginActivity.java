package com.example.joshuaEventApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/*
 * LoginActivity.java
 *
 * Entry point of the Event Tracker application. This handles both user login and
 * account creation from a single screen. On launch, checks if a session already
 * exists in SharedPreferences and skips directly to EventDisplayActivity if so.
 *
 * On successful login or signup, the user ID and username are stored in
 * SharedPreferences and the user is redirected to EventDisplayActivity.
 */

public class LoginActivity extends AppCompatActivity {
    private EditText editTextUsername, editTextPassword, editTextEmail;
    private TextView textViewError, labelEmail;
    private Button buttonLogin, buttonSignup;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private boolean isSignup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserPreferencesManager preferencesManager = new UserPreferencesManager(this);
        AppCompatDelegate.setDefaultNightMode(
                preferencesManager.isDarkModeEnabled()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        // if shared preferences contains login, just go directly to the Event Screen
        if (sessionManager.isLoggedIn()) {
            goToEventDisplay();
            return;
        }

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextEmail = findViewById(R.id.editTextEmail);
        textViewError = findViewById(R.id.textViewError);
        labelEmail = findViewById(R.id.labelEmail);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonSignup = findViewById(R.id.buttonSignup);

        buttonLogin.setOnClickListener(v -> handleLogin());
        buttonSignup.setOnClickListener(v -> handleSignup());
    }

    private void handleLogin() {
        if (isSignup) {
            // Switch to login mode, hide email field
            isSignup = false;
            labelEmail.setVisibility(View.GONE);
            editTextEmail.setVisibility(View.GONE);
            buttonSignup.setText(getString(R.string.signup_button));
            buttonLogin.setText(getString(R.string.login_button));
            clearError();
            return;
        }

        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        // Validates credentials against the database
        if (databaseHelper.validateUser(username, password)) {
            int userId = databaseHelper.getUserId(username);
            sessionManager.login(userId, username);
            goToEventDisplay();
        } else {
           showError("Invalid username or password");
        }
    }

    // Logic for handling signup, checks if credentials exists in the database
    // before creating an account
    private void handleSignup() {
        if (!isSignup) {
            // Switch to signup mode and show email field
            isSignup = true;
            labelEmail.setVisibility(View.VISIBLE);
            editTextEmail.setVisibility(View.VISIBLE);
            buttonSignup.setText(getString(R.string.signup_confirm_button));
            buttonLogin.setText(getString(R.string.back_button));
            clearError();
            return;
        }

        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            showError("Please fill in all the fields");
            return;
        }

        // Android's email pattern matching
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        // Checks if it is possible to create account
        if (databaseHelper.createUser(username, email, password)) {
            int userId = databaseHelper.getUserId(username);
            sessionManager.login(userId, username);
            goToEventDisplay();
        } else {
            showError("Username or Email already exists");
        }
    }

    // Goes to the Event Screen
    private void goToEventDisplay() {
        Intent intent = new Intent(this, EventDisplayActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Error helpers
    private void showError(String message) {
        textViewError.setText(message);
        textViewError.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        textViewError.setText("");
        textViewError.setVisibility(View.GONE);
    }
}
