package com.example.joshuaEventApp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * EventDetailActivity.java
 *
 * Displays the full details of a selected event including title, description,
 * date, and time. Allows the user to edit or soft delete the event.
 * Receives the event ID via Intent extra and loads the event from the database.
 */
public class EventDetailActivity extends AppCompatActivity {

    private TextView textDetailDay, textDetailMonth, textDetailYear, textDetailTime;
    private TextView textDetailTitle, textDetailDesc;
    private Button buttonEditEvent, buttonDeleteEvent;
    private DatabaseHelper databaseHelper;
    private Event event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Details");
        }

        databaseHelper = new DatabaseHelper(this);

        textDetailDay = findViewById(R.id.textDetailDay);
        textDetailMonth = findViewById(R.id.textDetailMonth);
        textDetailYear = findViewById(R.id.textDetailYear);
        textDetailTime = findViewById(R.id.textDetailTime);
        textDetailTitle = findViewById(R.id.textDetailTitle);
        textDetailDesc = findViewById(R.id.textDetailDesc);
        buttonEditEvent = findViewById(R.id.buttonEditEvent);
        buttonDeleteEvent = findViewById(R.id.buttonDeleteEvent);

        int eventId = getIntent().getIntExtra("eventId", -1);
        if (eventId == -1) {
            finish();
            return;
        }

        event = databaseHelper.getEventById(eventId);
        if (event == null) {
            finish();
            return;
        }

        populateEventDetails();

        buttonEditEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
        });

        buttonDeleteEvent.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this event?")
                    .setPositiveButton("Delete", (d, which) -> {
                        databaseHelper.deleteEvent(event.getId());
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.dialog_button));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.dialog_button));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (event != null) {
            event = databaseHelper.getEventById(event.getId());
            if (event != null) {
                populateEventDetails();
            }
        }
    }

    private void populateEventDetails() {
        Date date = new Date(event.getTimestamp());

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        textDetailDay.setText(dayFormat.format(date));
        textDetailMonth.setText(monthFormat.format(date).toUpperCase());
        textDetailYear.setText(yearFormat.format(date));
        textDetailTime.setText(timeFormat.format(date));
        textDetailTitle.setText(event.getTitle());
        textDetailDesc.setText(event.getDescription());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}