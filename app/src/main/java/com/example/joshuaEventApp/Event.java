package com.example.joshuaEventApp;

/*
 * Event.java
 *
 * Model class representing a single event in the Event Tracker application.
 * Each event belongs to a user and contains a title, description, date, and time.
 */
public class Event {
    private final int id;
    private String title;
    private String description;
    private long timestamp;
    private final int userId;

    public Event(int id, String title,
                 String description, long timestamp, int userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getTimestamp() { return timestamp; }
    public int getUserId() { return userId; }
}
