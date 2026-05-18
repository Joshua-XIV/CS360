package com.example.joshuaEventApp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/*
 * DatabaseHelper.java
 *
 * Manages the local SQLite database for the Event Tracker app.
 * Contains two tables: users and events.
 *
 * Users table stores account credentials with salted SHA-256 hashed passwords
 * and an optional phone number for SMS reminders.
 *
 * Events table stores event data tied to a user account. Events are soft
 * deleted by setting a deleted flag rather than removing the record.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "eventtracker.db";
    private static final int DATABASE_VERSION = 3;

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_HASHED_PASSWORD = "password";
    public static final String COLUMN_SALT = "salt";

    // Events table
    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_EVENT_ID = "id";
    public static final String COLUMN_EVENT_TITLE = "title";
    public static final String COLUMN_EVENT_DESC = "description";
    public static final String COLUMN_EVENT_TIMESTAMP = "timestamp";
    public static final String COLUMN_EVENT_USER_ID = "user_id";
    public static final String COLUMN_EVENT_DELETED = "deleted";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_EMAIL + " TEXT UNIQUE, " +
                COLUMN_HASHED_PASSWORD + " TEXT, " +
                COLUMN_SALT + " TEXT, " +
                COLUMN_PHONE + " TEXT)";

        String createEvents = "CREATE TABLE " + TABLE_EVENTS + " (" +
                COLUMN_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EVENT_TITLE + " TEXT, " +
                COLUMN_EVENT_DESC + " TEXT, " +
                COLUMN_EVENT_TIMESTAMP + " INTEGER, " +
                COLUMN_EVENT_USER_ID + " INTEGER, " +
                COLUMN_EVENT_DELETED + " INTEGER DEFAULT 0)";

        db.execSQL(createUsers);
        db.execSQL(createEvents);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

    // Returns true if a user with the given
    // username or emails exists in the user table
    public boolean userExists(String username, String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=? OR " + COLUMN_EMAIL + "=?",
                new String[]{username, email}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Creates a new user with a salted and hashed password
    // Returns false if the username or email already exists
    public boolean createUser(String username, String email, String password) {
        if (userExists(username, email)) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String salt = generateSalt();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_HASHED_PASSWORD, hashPassword(password, salt));
        values.put(COLUMN_SALT, salt);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Validates login credentials by comparing the given password after it's hashed
    // against the hashed and salted password in the user table
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_HASHED_PASSWORD, COLUMN_SALT},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null);

        if (cursor.moveToFirst()) {
            String storedHash = cursor.getString(0);
            String salt = cursor.getString(1);
            cursor.close();
            return storedHash.equals(hashPassword(password, salt));
        }

        cursor.close();
        return false;
    }

    // Updates the user's password with a new salt and hash
    // TODO: add to settings page
    public boolean updatePassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        String newSalt = generateSalt();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HASHED_PASSWORD, hashPassword(newPassword, newSalt));
        values.put(COLUMN_SALT, newSalt);
        int rows = db.update(TABLE_USERS, values, COLUMN_USERNAME + "=?", new String[]{username});
        return rows > 0;
    }

    // Updates the user's phone number for any SMS reminders
    // TODO: add to settings page
    public boolean updatePhone(String username, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHONE, phone);
        int rows = db.update(TABLE_USERS, values, COLUMN_USERNAME + "=?", new String[]{username});
        return rows > 0;
    }

    // Returns the phone number of a user if exists or null
    public String getPhone(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_PHONE},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null);
        if (cursor.moveToFirst()) {
            String phone = cursor.getString(0);
            cursor.close();
            return phone;
        }
        cursor.close();
        return null;
    }

    // Returns user ID for a given username or -1
    public int getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + "=?",
                new String[]{username}, null, null, null);
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            cursor.close();
            return id;
        }
        cursor.close();
        return -1;
    }

    // Returns all non-deleted events for a user
    // and is ordered by timestamps ascending
    public List<Event> getEventsByUser(int userId) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EVENTS,
                null,
                COLUMN_EVENT_USER_ID + "=? AND " + COLUMN_EVENT_DELETED + "=0",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_EVENT_TIMESTAMP + " ASC");

        while (cursor.moveToNext()) {
            Event event = new Event(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DESC)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TIMESTAMP)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_USER_ID))
            );
            events.add(event);
        }

        cursor.close();
        return events;
    }

    // Returns a single event by its ID, or null
    public Event getEventById(int eventId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EVENTS, null,
                COLUMN_EVENT_ID + "=?",
                new String[]{String.valueOf(eventId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            Event event = new Event(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DESC)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TIMESTAMP)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_USER_ID))
            );
            cursor.close();
            return event;
        }

        cursor.close();
        return null;
    }

    // Inserts a new event and returns the inserted row ID, or -1
    public long addEvent(String title, String description, long timestamp, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_TITLE, title);
        values.put(COLUMN_EVENT_DESC, description);
        values.put(COLUMN_EVENT_TIMESTAMP, timestamp);
        values.put(COLUMN_EVENT_USER_ID, userId);
        values.put(COLUMN_EVENT_DELETED, 0);
        return db.insert(TABLE_EVENTS, null, values);
    }

    // Updates an existing event's title, description, and timestamp
    public boolean updateEvent(int eventId, String title, String description, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_TITLE, title);
        values.put(COLUMN_EVENT_DESC, description);
        values.put(COLUMN_EVENT_TIMESTAMP, timestamp);
        int rows = db.update(TABLE_EVENTS, values, COLUMN_EVENT_ID + "=?", new String[]{String.valueOf(eventId)});
        return rows > 0;
    }

    // Soft deletes an event by setting the deleted flag in the user table to 1
    public boolean deleteEvent(int eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_DELETED, 1);
        int rows = db.update(TABLE_EVENTS, values, COLUMN_EVENT_ID + "=?",
                new String[]{String.valueOf(eventId)});
        return rows > 0;
    }

    // Generates a random 16 byte salt that is encoded
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    // Hashes a password using SHA-256 combined with saly
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
