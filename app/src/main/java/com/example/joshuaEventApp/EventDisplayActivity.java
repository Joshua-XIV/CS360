package com.example.joshuaEventApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

/*
 * EventDisplayActivity.java
 *
 * Main dashboard screen that displays all events belonging to the
 * currently logged-in user. Handles event loading, navigation drawer
 * interactions, SMS/notification permissions, and launching the
 * add-event screen.
 *
 * Also prompts the user for a phone number when SMS reminders are enabled.
 */
public class EventDisplayActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private RecyclerView recycleViewEvents;
    private FloatingActionButton addEvent;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private LinearLayout layoutEmptyState;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton buttonMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        recycleViewEvents = findViewById(R.id.recyclerViewEvents);
        addEvent = findViewById(R.id.fabAddEvent);

        recycleViewEvents.setLayoutManager(new GridLayoutManager(this, 1));
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // Loads all events
        loadEvents();

        // Opens the add event screen
        addEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            startActivity(intent);
        });

        // Navigation menu setup
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        buttonMenu = findViewById(R.id.buttonMenu);

        // Opens the nav menu
        buttonMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Handles nav menu actions
        navigationView.setNavigationItemSelectedListener(item -> {
            // Logs the user out and clears all activity
            if (item.getItemId() == R.id.action_logout) {
                sessionManager.logout();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else if (item.getItemId() == R.id.settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Displays the username in the nav menu
        navigationView.getMenu().findItem(R.id.menu_username).setTitle(sessionManager.getUsername());
        // Request permissions
        checkSmsPermission();
        checkNotificationPermission();
    }

    // Reload events when returning to this screen
    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    // Loads all events for current user and updates empty state
    private void loadEvents() {
        int userId = sessionManager.getUserId();
        List<Event> events = databaseHelper.getEventsByUser(userId);
        EventAdapter adapter = new EventAdapter(events, databaseHelper, this::loadEvents);
        recycleViewEvents.setAdapter(adapter);

        if (events.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recycleViewEvents.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recycleViewEvents.setVisibility(View.VISIBLE);
        }
    }

    // Requests SMS permissions if the user hasn't been asked
    private void checkSmsPermission() {
        if (sessionManager.hasSmsBeenAsked()) return;

        sessionManager.setSmsAsked();

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("SMS Notifications")
                .setMessage("Event Tracker would like to send you SMS reminders before your events. Would you like to enable this?")
                .setPositiveButton("Allow", (dialog, which) -> ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_CODE))
                .setNegativeButton("No Thanks", null)
                .create();
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.dialog_button));
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.error_red));
    }

    // Requests notification permissions
    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void checkPhoneNumber() {
        String phone = databaseHelper.getPhone(sessionManager.getUsername());
        if (phone == null || phone.isEmpty()) {
            showPhoneNumberDialog();
        }
    }

    // Prompts user to enter a phone number for SMS reminders
    private void showPhoneNumberDialog() {
        EditText editTextPhone = new EditText(this);
        editTextPhone.setHint("Enter your phone number");
        editTextPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Phone Number")
                .setMessage("Enter your phone number to receive SMS reminders")
                .setView(editTextPhone)
                .setPositiveButton("Save", (dialog, which) -> {
                    String phone = editTextPhone.getText().toString().trim();
                    if (!phone.isEmpty()) {
                        databaseHelper.updatePhone(sessionManager.getUsername(), phone);
                    }
                })
                .setNegativeButton("Skip", null)
                .create();
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.dialog_button));
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.error_red));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Prompt for phone number if permission is granted
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPhoneNumberDialog();
            }
        }  // app should just continue regardless

    }
}
