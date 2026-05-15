package com.example.joshuaEventApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

        recycleViewEvents.setLayoutManager(new GridLayoutManager(this, 2));

        addEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            startActivity(intent);
        });
    }
}
