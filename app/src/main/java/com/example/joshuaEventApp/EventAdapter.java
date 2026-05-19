package com.example.joshuaEventApp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * EventAdapter.java
 *
 * RecyclerView adapter for displaying a list of Event objects as cards.
 * Each card shows the event date, time, title, and description along with
 * a color coded progress bar based on how soon the event is.
 * Tapping a card opens EventDetailActivity. A delete button on each card
 * allows quick deletion.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final DatabaseHelper databaseHelper;
    private final OnEventDeletedListener deleteListener;
    private boolean isDeletedMode;

    public EventAdapter(List<Event> eventList, DatabaseHelper databaseHelper,
                        OnEventDeletedListener deleteListener, boolean isDeletedMode) {
        this.eventList = eventList;
        this.databaseHelper = databaseHelper;
        this.deleteListener = deleteListener;
        this.isDeletedMode = isDeletedMode;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        Date date = new Date(event.getTimestamp());

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        holder.textEventDay.setText(dayFormat.format(date));
        holder.textEventMonth.setText(monthFormat.format(date).toUpperCase());
        holder.textEventYear.setText(yearFormat.format(date));
        holder.textEventTime.setText(timeFormat.format(date));
        holder.textEventTitle.setText(event.getTitle());
        holder.textEventDesc.setText(event.getDescription());

        updateProgressBar(holder.progressBar, event.getTimestamp(), holder.itemView.getContext());

        // When tapping on a card, it opens the full event details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
            intent.putExtra("eventId", event.getId());
            v.getContext().startActivity(intent);
        });

        if (isDeletedMode) {
            holder.buttonDeleteEvent.setVisibility(View.GONE);
            holder.layoutDeletedActions.setVisibility(View.VISIBLE);

            holder.buttonRestoreEvent.setOnClickListener(v ->
                    showRestoreConfirmation(v, event));

            holder.buttonPermanentDeleteEvent.setOnClickListener(v ->
                showPermanentDeleteConfirmation(v, event));

            holder.itemView.setOnClickListener(null);

        } else {
            holder.buttonDeleteEvent.setVisibility(View.VISIBLE);
            holder.layoutDeletedActions.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
                intent.putExtra("eventId", event.getId());
                v.getContext().startActivity(intent);
            });

            holder.buttonDeleteEvent.setOnClickListener(v -> showDeleteConfirmation(v, event));
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    // Shows restore/permanent delete options for deleted events
    private void showDeletedEventOptions(View v, Event event) {
        String[] options = {"Restore", "Permanently Delete"};

        AlertDialog d = new AlertDialog.Builder(v.getContext())
                .setTitle(event.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        databaseHelper.restoreEvent(event.getId());
                        deleteListener.onEventDeleted();
                    } else {
                        showPermanentDeleteConfirmation(v, event);
                    }
                })
                .create();

        d.show();
    }

    // Confirms permanent deletion
    private void showPermanentDeleteConfirmation(View v, Event event) {
        AlertDialog d = new AlertDialog.Builder(v.getContext())
                .setTitle("Permanent Delete")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseHelper.permanentlyDeleteEvent(event.getId());
                    deleteListener.onEventDeleted();
                })
                .setNegativeButton("Cancel", null)
                .create();

        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(v.getContext().getColor(R.color.error_red));
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(v.getContext().getColor(R.color.dialog_button));
    }

    // Confirms soft deletion of active events
    private void showDeleteConfirmation(View v, Event event) {
        AlertDialog d = new AlertDialog.Builder(v.getContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    SmsScheduler.cancelReminder(v.getContext(), event.getId());
                    databaseHelper.deleteEvent(event.getId());
                    deleteListener.onEventDeleted();
                })
                .setNegativeButton("Cancel", null)
                .create();

        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(v.getContext().getColor(R.color.dialog_button));
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(v.getContext().getColor(R.color.error_red));
    }

    // Confirms restoring a deleted event
    private void showRestoreConfirmation(View v, Event event) {
        AlertDialog d = new AlertDialog.Builder(v.getContext())
                .setTitle("Restore Event")
                .setMessage("Restore this event?")
                .setPositiveButton("Restore", (dialog, which) -> {
                    databaseHelper.restoreEvent(event.getId());
                    deleteListener.onEventDeleted();
                })
                .setNegativeButton("Cancel", null)
                .create();

        d.show();

        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(v.getContext().getColor(R.color.event_far));
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(v.getContext().getColor(R.color.error_red));
    }

    // Updates the progress bar color based on how soon the event is
    // Green is more than 2 days away, Orange is between 1 and 2 days
    // Yellow is less than 1 day, and Red is when the event has passed/
    private void updateProgressBar(ProgressBar progressBar, long timestamp, Context context) {
        long now = System.currentTimeMillis();
        long diff = timestamp - now;
        long oneDayMs = 24 * 60 * 60 * 1000L;
        long twoDaysMs = 2 * oneDayMs;

        progressBar.setProgress(100);

        if (diff <= 0) {
            progressBar.setProgressTintList(ColorStateList.valueOf(context.getColor(R.color.event_past)));
        } else if (diff <= oneDayMs) {
            progressBar.setProgressTintList(ColorStateList.valueOf(context.getColor(R.color.event_soon)));
        } else if (diff <= twoDaysMs) {
            progressBar.setProgressTintList(ColorStateList.valueOf(context.getColor(R.color.event_upcoming)));
        } else {
            progressBar.setProgressTintList(ColorStateList.valueOf(context.getColor(R.color.event_far)));
        }
    }

    public interface OnEventDeletedListener {
        void onEventDeleted();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView textEventDay, textEventMonth, textEventTime,
                 textEventTitle, textEventDesc, textEventYear;
        ImageButton buttonDeleteEvent;
        Button buttonRestoreEvent, buttonPermanentDeleteEvent;
        LinearLayout layoutDeletedActions;
        ProgressBar progressBar;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textEventDay = itemView.findViewById(R.id.textEventDay);
            textEventMonth = itemView.findViewById(R.id.textEventMonth);
            textEventTime = itemView.findViewById(R.id.textEventTime);
            textEventTitle = itemView.findViewById(R.id.textEventTitle);
            textEventDesc = itemView.findViewById(R.id.textEventDesc);
            textEventYear = itemView.findViewById(R.id.textEventYear);
            buttonDeleteEvent = itemView.findViewById(R.id.buttonDeleteEvent);
            layoutDeletedActions = itemView.findViewById(R.id.layoutDeletedActions);
            buttonRestoreEvent = itemView.findViewById(R.id.buttonRestoreEvent);
            buttonPermanentDeleteEvent = itemView.findViewById(R.id.buttonPermanentDeleteEvent);
            progressBar = itemView.findViewById(R.id.eventProgressBar);
        }
    }
}