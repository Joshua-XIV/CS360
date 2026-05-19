package com.example.joshuaEventApp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.LinkedHashSet;
import java.util.Set;

/*
 * SettingsActivity.java
 *
 * Allows users to configure application preferences and account settings.
 * Handles SMS and push notification toggles, reminder timing configuration,
 * phone number management, password updates, dark mode toggle, and
 * direct access to Android app settings.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 200;
    private static final int NOTIFICATION_PERMISSION_CODE = 201;

    private UserPreferencesManager preferencesManager;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    private SwitchCompat switchNotifications;
    private SwitchCompat switchSms;
    private SwitchCompat switchDarkMode;
    private TextView textCurrentPhoneNumber;
    private EditText editPhoneNumber;

    private final String[] reminderLabels = {
            "1 Week Before", "3 Days Before", "1 Day Before",
            "6 Hours Before", "3 Hours Before", "1 Hour Before"
    };

    private final long[] reminderValues = {
            UserPreferencesManager.ONE_WEEK,
            UserPreferencesManager.THREE_DAYS,
            UserPreferencesManager.ONE_DAY,
            UserPreferencesManager.SIX_HOURS,
            UserPreferencesManager.THREE_HOURS,
            UserPreferencesManager.ONE_HOUR
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferencesManager = new UserPreferencesManager(this);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        setupToolbar();
        bindViews();
        loadSettings();
        setupListeners();
    }

    // Configures top toolbar and back button behavior
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    // Binds views and sets up button click listeners
    private void bindViews() {
        switchNotifications = findViewById(R.id.switchNotifications);
        switchSms = findViewById(R.id.switchSms);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        textCurrentPhoneNumber = findViewById(R.id.textCurrentPhoneNumber);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);

        Button btnReminderTimes = findViewById(R.id.btnReminderTimes);
        Button btnOpenPhoneSettings = findViewById(R.id.btnOpenPhoneSettings);
        Button btnSavePhoneNumber = findViewById(R.id.btnSavePhoneNumber);
        Button btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        btnReminderTimes.setOnClickListener(v -> showReminderTimesDialog());
        btnOpenPhoneSettings.setOnClickListener(v -> openAppSettings());
        btnSavePhoneNumber.setOnClickListener(v -> savePhoneNumber());
        btnUpdatePassword.setOnClickListener(v -> updatePassword());
    }

    // Loads saved preferences and user profile data into the UI
    private void loadSettings() {
        switchNotifications.setChecked(preferencesManager.isNotificationsEnabled());
        switchSms.setChecked(preferencesManager.isSmsEnabled());
        switchDarkMode.setChecked(preferencesManager.isDarkModeEnabled());

        String phone = databaseHelper.getPhone(sessionManager.getUsername());
        if (phone == null || phone.trim().isEmpty()) {
            textCurrentPhoneNumber.setText("Current phone number: Not set");
        } else {
            textCurrentPhoneNumber.setText("Current phone number: " + phone);
            editPhoneNumber.setText(phone);
        }
    }

    // Attaches toggle listeners for notifications, SMS, and dark mode
    private void setupListeners() {
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && needsNotificationPermission()) requestNotificationPermission();
            preferencesManager.setNotificationsEnabled(isChecked);
            Toast.makeText(this, "Notification setting updated", Toast.LENGTH_SHORT).show();
        });

        switchSms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && needsSmsPermission()) requestSmsPermission();
            preferencesManager.setSmsEnabled(isChecked);
            Toast.makeText(this, "SMS setting updated", Toast.LENGTH_SHORT).show();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setDarkModeEnabled(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    // Opens a multi-select dialog for configuring reminder intervals
    private void showReminderTimesDialog() {
        Set<String> savedOffsets = preferencesManager.getReminderOffsets();
        boolean[] checkedItems = new boolean[reminderValues.length];

        for (int i = 0; i < reminderValues.length; i++) {
            checkedItems[i] = savedOffsets.contains(String.valueOf(reminderValues[i]));
        }

        new AlertDialog.Builder(this)
                .setTitle("Reminder Times")
                .setMultiChoiceItems(reminderLabels, checkedItems,
                        (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("Save", (dialog, which) -> {
                    Set<String> selectedOffsets = new LinkedHashSet<>();
                    for (int i = 0; i < reminderValues.length; i++) {
                        if (checkedItems[i]) selectedOffsets.add(String.valueOf(reminderValues[i]));
                    }
                    preferencesManager.setReminderOffsets(selectedOffsets);
                    Toast.makeText(this, "Reminder times updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Saves the entered phone number to the database
    private void savePhoneNumber() {
        String phone = editPhoneNumber.getText().toString().trim();
        if (phone.isEmpty()) {
            Toast.makeText(this, "Enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        databaseHelper.updatePhone(sessionManager.getUsername(), phone);
        textCurrentPhoneNumber.setText("Current phone number: " + phone);
        Toast.makeText(this, "Phone number updated", Toast.LENGTH_SHORT).show();
    }

    // Validates and updates the user's password
    private void updatePassword() {
        EditText editCurrentPassword = findViewById(R.id.editCurrentPassword);
        EditText editNewPassword = findViewById(R.id.editNewPassword);
        EditText editConfirmPassword = findViewById(R.id.editConfirmPassword);

        String currentPassword = editCurrentPassword.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all password fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!databaseHelper.validateUser(sessionManager.getUsername(), currentPassword)) {
            Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseHelper.updatePassword(sessionManager.getUsername(), newPassword);
        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();

        editCurrentPassword.setText("");
        editNewPassword.setText("");
        editConfirmPassword.setText("");
    }

    // Returns true if SMS permission has not been granted
    private boolean needsSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED;
    }

    // Requests SMS permission from the user
    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
    }

    // Returns true if notification permission has not been granted on Android 13+
    private boolean needsNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return false;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
    }

    // Requests notification permission on Android 13+
    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
        }
    }

    // Opens the Android system settings page for this app
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}