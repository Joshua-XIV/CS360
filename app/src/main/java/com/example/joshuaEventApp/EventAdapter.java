package com.example.joshuaEventApp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

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

        // Split date into day and month for display
        // Date is stored as DD/MM/YYYY
        String[] dateParts = event.getDate().split("/");
        if (dateParts.length == 3) {
            holder.textEventDay.setText(dateParts[0]);
            holder.textEventMonth.setText(getMonthName(Integer.parseInt(dateParts[1])));
        }

        holder.textEventTime.setText(event.getTime());
        holder.textEventTitle.setText(event.getTitle());
        holder.textEventDesc.setText(event.getDescription());

        // Tap card to open event detail
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

    private String getMonthName(int month) {
        String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        if (month >= 1 && month <= 12) return months[month - 1];
        return "";
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView textEventDay, textEventMonth, textEventTime, textEventTitle, textEventDesc;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textEventDay = itemView.findViewById(R.id.textEventDay);
            textEventMonth = itemView.findViewById(R.id.textEventMonth);
            textEventTime = itemView.findViewById(R.id.textEventTime);
            textEventTitle = itemView.findViewById(R.id.textEventTitle);
            textEventDesc = itemView.findViewById(R.id.textEventDesc);
        }
    }
}