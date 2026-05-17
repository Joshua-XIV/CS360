package com.example.joshuaEventApp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
 * RecyclerView adapter for displaying a list of Event objects in a grid.
 * Each card displays the event day, month, time, title, and description.
 * Tapping a card navigates to EventDetailActivity.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final DatabaseHelper databaseHelper;
    private final OnEventDeletedListener deleteListener;

    public EventAdapter(List<Event> eventList, DatabaseHelper databaseHelper, OnEventDeletedListener deleteListener) {
        this.eventList = eventList;
        this.databaseHelper = databaseHelper;
        this.deleteListener = deleteListener;
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

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
            intent.putExtra("eventId", event.getId());
            v.getContext().startActivity(intent);
        });

        ImageButton buttonDelete = holder.itemView.findViewById(R.id.buttonDeleteEvent);
        buttonDelete.setOnClickListener(v -> {
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
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

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
            progressBar = itemView.findViewById(R.id.eventProgressBar);
        }
    }
}