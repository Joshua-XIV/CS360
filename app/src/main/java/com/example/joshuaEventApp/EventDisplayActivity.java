package com.example.joshuaEventApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class EventDisplayActivity extends AppCompatActivity {
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

        // load some test events
        // databaseHelper.clearAllEvents();
        // databaseHelper.insertTestEvents(sessionManager.getUserId());
        loadEvents();

        addEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            startActivity(intent);
        });

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        buttonMenu = findViewById(R.id.buttonMenu);

        buttonMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                sessionManager.logout();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
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
}
