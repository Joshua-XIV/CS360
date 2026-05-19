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
 *
 * Features:
 * - Enable/disable SMS reminders
 * - Enable/disable push notifications
 * - Configure reminder timing intervals
 * - View/update phone number
 * - Toggle dark mode
 * - Open Android app settings
 *
 * TODO: need to do password updating
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

    // Display labels for reminder selection dialog
    private final String[] reminderLabels = {
            "1 Week Before",
            "3 Days Before",
            "1 Day Before",
            "6 Hours Before",
            "3 Hours Before",
            "1 Hour Before"
    };

    // Millisecond offsets
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

    private void bindViews() {

        switchNotifications = findViewById(R.id.switchNotifications);
        switchSms = findViewById(R.id.switchSms);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        textCurrentPhoneNumber = findViewById(R.id.textCurrentPhoneNumber);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);

        Button btnReminderTimes =
                findViewById(R.id.btnReminderTimes);

        Button btnOpenPhoneSettings =
                findViewById(R.id.btnOpenPhoneSettings);

        Button btnSavePhoneNumber =
                findViewById(R.id.btnSavePhoneNumber);

        Button btnUpdatePassword =
                findViewById(R.id.btnUpdatePassword);

        btnReminderTimes.setOnClickListener(
                v -> showReminderTimesDialog()
        );

        btnOpenPhoneSettings.setOnClickListener(
                v -> openAppSettings()
        );

        btnSavePhoneNumber.setOnClickListener(
                v -> savePhoneNumber()
        );

        btnUpdatePassword.setOnClickListener(
                v -> Toast.makeText(
                        this,
                        "Password update coming next",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    // Loads saved preferences and user profile data
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

    // Attaches all event listeners
    private void setupListeners() {

        // Push notification toggle
        switchNotifications.setOnCheckedChangeListener(
            (buttonView, isChecked) -> {

                if (isChecked && needsNotificationPermission()) {
                    requestNotificationPermission();
                }

                preferencesManager
                        .setNotificationsEnabled(isChecked);

                Toast.makeText(
                        this,
                        "Notification setting updated",
                        Toast.LENGTH_SHORT
                ).show();
        });

        // SMS reminder toggle
        switchSms.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (isChecked && needsSmsPermission()) {
                        requestSmsPermission();
                    }

                    preferencesManager
                            .setSmsEnabled(isChecked);

                    Toast.makeText(
                            this,
                            "SMS setting updated",
                            Toast.LENGTH_SHORT
                    ).show();
                });

        // Dark mode toggle
        switchDarkMode.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    AppCompatDelegate.setDefaultNightMode(
                            isChecked
                                    ? AppCompatDelegate.MODE_NIGHT_YES
                                    : AppCompatDelegate.MODE_NIGHT_NO
                    );
                });
    }

    // Opens reminder interval multi-select dialog
    private void showReminderTimesDialog() {

        Set<String> savedOffsets =
                preferencesManager.getReminderOffsets();

        boolean[] checkedItems =
                new boolean[reminderValues.length];

        for (int i = 0; i < reminderValues.length; i++) {
            checkedItems[i] = savedOffsets.contains(String.valueOf(reminderValues[i]));
        }

        new AlertDialog.Builder(this)

                .setTitle("Reminder Times")

                .setMultiChoiceItems(reminderLabels, checkedItems,
                    (dialog, which, isChecked) -> checkedItems[which] = isChecked
                )

                .setPositiveButton(
                    "Save",
                    (dialog, which) -> {
                        Set<String> selectedOffsets = new LinkedHashSet<>();

                        for (int i = 0; i < reminderValues.length; i++) {
                            if (checkedItems[i]) {
                                selectedOffsets.add(String.valueOf(reminderValues[i]));
                            }
                        }

                        preferencesManager.setReminderOffsets(selectedOffsets);

                        Toast.makeText(this, "Reminder times updated", Toast.LENGTH_SHORT).show();
                    })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Saves updated phone number
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

    // Checks whether SMS permission is missing
    private boolean needsSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED;
    }

    // Requests SMS permission
    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.SEND_SMS},
            SMS_PERMISSION_CODE
        );
    }

    // Checks notification permission state
    private boolean needsNotificationPermission() {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return false;
        }

        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
    }

    // Requests notification permission
    private void requestNotificationPermission() {

        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.TIRAMISU) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE
            );
        }
    }

    // Opens Android app settings page
    private void openAppSettings() {

        Intent intent =
                new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts(
                                "package",
                                getPackageName(),
                                null
                        )
                );

        startActivity(intent);
    }

    // Handles toolbar back arrow
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}