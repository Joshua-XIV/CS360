package com.example.joshuaEventApp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
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

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
            intent.putExtra("eventId", event.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView textEventDay, textEventMonth, textEventTime,
                 textEventTitle, textEventDesc, textEventYear;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textEventDay = itemView.findViewById(R.id.textEventDay);
            textEventMonth = itemView.findViewById(R.id.textEventMonth);
            textEventTime = itemView.findViewById(R.id.textEventTime);
            textEventTitle = itemView.findViewById(R.id.textEventTitle);
            textEventDesc = itemView.findViewById(R.id.textEventDesc);
            textEventYear = itemView.findViewById(R.id.textEventYear);
        }
    }
}