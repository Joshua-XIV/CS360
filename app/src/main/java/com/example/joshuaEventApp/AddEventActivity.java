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

import java.util.Calendar;
import java.util.Locale;

/*
 * AddEventActivity.java
 *
 * Allows the user to create a new event by filling out a form
 * with a title, description, date, and time. Date and time are selected
 * via built in Android picker dialogs. On save, the event is inserted
 * into the database and the user is returned to EventDisplayActivity.
 */
public class AddEventActivity extends AppCompatActivity {
    private EditText editTextEventTitle, editTextEventDesc;
    private Button buttonPickDate, buttonPickTime, buttonSaveEvent, buttonCancelEvent;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private int selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, selectedAmPm;
    private String selectedTime = "";
    private boolean dateSelected = false;
    private boolean timeSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        editTextEventTitle = findViewById(R.id.editTextEventTitle);
        editTextEventDesc = findViewById(R.id.editTextEventDesc);
        buttonPickDate = findViewById(R.id.buttonPickDate);
        buttonPickTime = findViewById(R.id.buttonPickTime);
        buttonSaveEvent = findViewById(R.id.buttonSaveEvent);
        buttonCancelEvent = findViewById(R.id.buttonCancelEvent);

        buttonPickDate.setOnClickListener(v -> showDatePicker());
        buttonPickTime.setOnClickListener(v -> showTimePicker());
        buttonSaveEvent.setOnClickListener(v -> saveEvent());
        buttonCancelEvent.setOnClickListener(v -> finish());
    }

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
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();

        // Build the NumberPicker layout programmatically
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_time_picker, null);

        NumberPicker hourPicker = dialogView.findViewById(R.id.pickerHour);
        NumberPicker minutePicker = dialogView.findViewById(R.id.pickerMinute);
        NumberPicker amPmPicker = dialogView.findViewById(R.id.pickerAmPm);

        // Hour 1-12
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setValue(calendar.get(Calendar.HOUR) == 0 ? 12 : calendar.get(Calendar.HOUR));

        // Minutes 00-59 with leading zeros
        String[] minutes = new String[60];
        for (int i = 0; i < 60; i++) {
            minutes[i] = String.format(Locale.getDefault(), "%02d", i);
        }
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setDisplayedValues(minutes);
        minutePicker.setValue(calendar.get(Calendar.MINUTE));

        // AM/PM
        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        amPmPicker.setValue(calendar.get(Calendar.AM_PM));

        new AlertDialog.Builder(this)
                .setTitle("Select Time")
                .setView(dialogView)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    selectedHour = hourPicker.getValue();
                    selectedMinute = minutePicker.getValue();
                    selectedAmPm = amPmPicker.getValue();
                    timeSelected = true;
                    String amPm = selectedAmPm == 0 ? "AM" : "PM";
                    String minute = minutes[selectedMinute];
                    selectedTime = selectedHour + ":" + minute + " " + amPm;
                    buttonPickTime.setText(selectedTime);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

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

        // Convert to 24 hour for Calendar
        int hour24 = selectedHour;
        if (selectedAmPm == 1 && selectedHour != 12) hour24 = selectedHour + 12;
        if (selectedAmPm == 0 && selectedHour == 12) hour24 = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear, selectedMonth, selectedDay, hour24, selectedMinute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long timestamp = calendar.getTimeInMillis();

        int userId = sessionManager.getUserId();
        boolean saved = databaseHelper.addEvent(title, desc, timestamp, userId);

        if (saved) {
            finish();
        }
    }
}
