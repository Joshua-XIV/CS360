package com.example.joshuaEventApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class EventDisplayActivity extends AppCompatActivity {
    private RecyclerView recycleViewEvents;
    private FloatingActionButton addEvent;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        recycleViewEvents = findViewById(R.id.recyclerViewEvents);
        addEvent = findViewById(R.id.fabAddEvent);

        recycleViewEvents.setLayoutManager(new GridLayoutManager(this, 1));

        // load some test events
        // databaseHelper.clearAllEvents();
        // databaseHelper.insertTestEvents(sessionManager.getUserId());
        loadEvents();

        addEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        int userId = sessionManager.getUserId();
        List<Event> events = databaseHelper.getEventsByUser(userId);
        EventAdapter adapter = new EventAdapter(events);
        recycleViewEvents.setAdapter(adapter);
    }
}
