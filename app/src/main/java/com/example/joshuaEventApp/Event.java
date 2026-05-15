package com.example.joshuaEventApp;

/*
 * Event.java
 *
 * Model class representing a single event in the Event Tracker application.
 * Each event belongs to a user and contains a title, description, date, and time.
 */
public class Event {
    private int id;
    private String title;
    private String description;
    private String date;
    private String time;
    private int userId;

    public Event(int id, String title,
                 String description, String date,
                 String time, int userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.userId = userId;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public int getUserId() { return userId; }
}
