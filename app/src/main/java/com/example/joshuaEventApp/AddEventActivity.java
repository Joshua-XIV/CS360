package com.example.joshuaEventApp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/*
 * AddEventActivity.java
 *
 * Handles both creating a new event and editing an existing one.
 * When launched with an eventId, enters edit mode and pre-populates
 * all fields with the existing event data.
 *
 * On save, the selected date and time are combined into a Unix timestamp
 * and stored in the database. SMS and push notification reminders are
 * scheduled via SmsScheduler for 24 hours, 3 hours, and 1 hour before
 * the event. If in edit mode, existing reminders should be canceled and rescheduled.
 */
public class AddEventActivity extends AppCompatActivity {
    private EditText editTextEventTitle, editTextEventDesc;
    private Button buttonPickDate, buttonPickTime, buttonSaveEvent, buttonCancelEvent;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private int selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, selectedAmPm;
    private boolean dateSelected = false;
    private boolean timeSelected = false;
    private int editEventId = -1;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        Toolbar toolbar = findViewById(R.id.toolbarAddEvent);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        editTextEventTitle = findViewById(R.id.editTextEventTitle);
        editTextEventDesc = findViewById(R.id.editTextEventDesc);
        buttonPickDate = findViewById(R.id.buttonPickDate);
        buttonPickTime = findViewById(R.id.buttonPickTime);
        buttonSaveEvent = findViewById(R.id.buttonSaveEvent);
        buttonCancelEvent = findViewById(R.id.buttonCancelEvent);

        // Check if we are in edit mode
        editEventId = getIntent().getIntExtra("eventId", -1);
        if (editEventId != -1) {
            isEditMode = true;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Event");
            }
            buttonSaveEvent.setText("Update Event");
            populateExistingEvent();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("New Event");
            }
        }

        buttonPickDate.setOnClickListener(v -> showDatePicker());
        buttonPickTime.setOnClickListener(v -> showTimePicker());
        buttonSaveEvent.setOnClickListener(v -> saveEvent());
        buttonCancelEvent.setOnClickListener(v -> finish());
    }

    // Pre-populates fields with event data if we're in edit mode
    private void populateExistingEvent() {
        Event event = databaseHelper.getEventById(editEventId);
        if (event == null) {
            finish();
            return;
        }

        editTextEventTitle.setText(event.getTitle());
        editTextEventDesc.setText(event.getDescription());

        // Pre-populate date and time from timestamp
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(event.getTimestamp());

        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
        selectedHour = calendar.get(Calendar.HOUR);
        selectedMinute = calendar.get(Calendar.MINUTE);
        selectedAmPm = calendar.get(Calendar.AM_PM);
        dateSelected = true;
        timeSelected = true;

        if (selectedHour == 0) selectedHour = 12;

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM / dd / yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        Date date = new Date(event.getTimestamp());

        buttonPickDate.setText(dateFormat.format(date));
        buttonPickTime.setText(timeFormat.format(date));
    }

    // Uses Android's data picker
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            selectedYear = year;
            selectedMonth = month;
            selectedDay = day;
            dateSelected = true;
            buttonPickDate.setText(String.format(Locale.getDefault(), "%02d / %02d / %04d", month + 1, day, year));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.dialog_button));
        dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.dialog_button));
    }

    // Shows a custom NumberPicker for selecting the time, hours, minutes, and AM or PM
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_time_picker, null);

        NumberPicker hourPicker = dialogView.findViewById(R.id.pickerHour);
        NumberPicker minutePicker = dialogView.findViewById(R.id.pickerMinute);
        NumberPicker amPmPicker = dialogView.findViewById(R.id.pickerAmPm);

        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setValue(timeSelected ? selectedHour : (calendar.get(Calendar.HOUR) == 0 ? 12 : calendar.get(Calendar.HOUR)));

        String[] minutes = new String[60];
        for (int i = 0; i < 60; i++) {
            minutes[i] = String.format(Locale.getDefault(), "%02d", i);
        }
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setDisplayedValues(minutes);
        minutePicker.setValue(timeSelected ? selectedMinute : calendar.get(Calendar.MINUTE));

        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        amPmPicker.setValue(timeSelected ? selectedAmPm : calendar.get(Calendar.AM_PM));

        AlertDialog timeDialog = new AlertDialog.Builder(this)
                .setTitle("Select Time")
                .setView(dialogView)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    selectedHour = hourPicker.getValue();
                    selectedMinute = minutePicker.getValue();
                    selectedAmPm = amPmPicker.getValue();
                    timeSelected = true;
                    String amPm = selectedAmPm == 0 ? "AM" : "PM";
                    String minute = minutes[selectedMinute];
                    buttonPickTime.setText(selectedHour + ":" + minute + " " + amPm);
                })
                .setNegativeButton("Cancel", null)
                .create();

        timeDialog.show();
        timeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.dialog_button));
        timeDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.error_red));
    }

    // Validates inputs, also converts date and time to a Unix timestamp,
    // saves the event, and then schedules SMS and push notification reminders
    private void saveEvent() {
        String title = editTextEventTitle.getText().toString().trim();
        String desc = editTextEventDesc.getText().toString().trim();

        if (title.isEmpty()) {
            editTextEventTitle.setError("Event title is required");
            return;
        }

        if (!dateSelected) {
            buttonPickDate.setError("Please select a date");
            return;
        }

        if (!timeSelected) {
            buttonPickTime.setError("Please select a time");
            return;
        }

        // Convert 12 hour to 24 hour format for timestamps
        int hour24 = selectedHour;
        if (selectedAmPm == 1 && selectedHour != 12) hour24 = selectedHour + 12;
        if (selectedAmPm == 0 && selectedHour == 12) hour24 = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear, selectedMonth, selectedDay, hour24, selectedMinute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long timestamp = calendar.getTimeInMillis();

        String phone = databaseHelper.getPhone(sessionManager.getUsername());

        // Cancels and reschedules reminders if in edit mode, otherwise
        // schedules new reminders
        if (isEditMode) {
            databaseHelper.updateEvent(editEventId, title, desc, timestamp);
            SmsScheduler.cancelReminder(this, editEventId);
            SmsScheduler.scheduleReminder(this, editEventId, title, timestamp, phone);
        } else {
            int userId = sessionManager.getUserId();
            long newEventId = databaseHelper.addEvent(title, desc, timestamp, userId);
            if (newEventId != -1) {
                SmsScheduler.scheduleReminder(this, (int)newEventId, title, timestamp, phone);
            }
        }

        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}