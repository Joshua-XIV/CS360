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
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "eventtracker.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_HASHED_PASSWORD = "password";
    public static final String COLUMN_SALT = "salt";

    // Events table
    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_EVENT_ID = "id";
    public static final String COLUMN_EVENT_TITLE = "title";
    public static final String COLUMN_EVENT_DESC = "description";
    public static final String COLUMN_EVENT_DATE = "date";
    public static final String COLUMN_EVENT_TIME = "time";
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
                COLUMN_SALT + " TEXT)";

        String createEvents = "CREATE TABLE " + TABLE_EVENTS + " (" +
                COLUMN_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EVENT_TITLE + " TEXT, " +
                COLUMN_EVENT_DESC + " TEXT, " +
                COLUMN_EVENT_DATE + " TEXT, " +
                COLUMN_EVENT_TIME + " TEXT, " +
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

    public boolean userExists(String username, String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=? OR " + COLUMN_EMAIL + "=?",
                new String[]{username, email}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

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

    public boolean updatePassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        String newSalt = generateSalt();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HASHED_PASSWORD, hashPassword(newPassword, newSalt));
        values.put(COLUMN_SALT, newSalt);
        int rows = db.update(TABLE_USERS, values, COLUMN_USERNAME + "=?", new String[]{username});
        return rows > 0;
    }

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

    public List<Event> getEventsByUser(int userId) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EVENTS,
                null,
                COLUMN_EVENT_USER_ID + "=? AND " + COLUMN_EVENT_DELETED + "=0",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_EVENT_DATE + " ASC");

        while (cursor.moveToNext()) {
            Event event = new Event(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DESC)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TIME)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_USER_ID))
            );
            events.add(event);
        }

        cursor.close();
        return events;
    }

    public void insertTestEvents(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_EVENT_TITLE, "Team Meeting");
        values.put(COLUMN_EVENT_DESC, "Discuss Q3 goals and review project progress with the team");
        values.put(COLUMN_EVENT_DATE, "03/01/2025");
        values.put(COLUMN_EVENT_TIME, "7:00 PM");
        values.put(COLUMN_EVENT_USER_ID, userId);
        values.put(COLUMN_EVENT_DELETED, 0);
        db.insert(TABLE_EVENTS, null, values);
        values.clear();

        values.put(COLUMN_EVENT_TITLE, "Doctor Appointment");
        values.put(COLUMN_EVENT_DESC, "Annual checkup");
        values.put(COLUMN_EVENT_DATE, "15/01/2025");
        values.put(COLUMN_EVENT_TIME, "10:00 AM");
        values.put(COLUMN_EVENT_USER_ID, userId);
        values.put(COLUMN_EVENT_DELETED, 0);
        db.insert(TABLE_EVENTS, null, values);
        values.clear();

        values.put(COLUMN_EVENT_TITLE, "Birthday Party");
        values.put(COLUMN_EVENT_DESC, "Sarah's surprise birthday party at the downtown venue");
        values.put(COLUMN_EVENT_DATE, "20/01/2025");
        values.put(COLUMN_EVENT_TIME, "6:00 PM");
        values.put(COLUMN_EVENT_USER_ID, userId);
        values.put(COLUMN_EVENT_DELETED, 0);
        db.insert(TABLE_EVENTS, null, values);

        values.put(COLUMN_EVENT_TITLE, "Birthday Party 2");
        values.put(COLUMN_EVENT_DESC, "Sarah's surprise birthday party at the downtown venue");
        values.put(COLUMN_EVENT_DATE, "20/01/2025");
        values.put(COLUMN_EVENT_TIME, "6:00 PM");
        values.put(COLUMN_EVENT_USER_ID, userId);
        values.put(COLUMN_EVENT_DELETED, 0);
        db.insert(TABLE_EVENTS, null, values);

        values.put(COLUMN_EVENT_TITLE, "Birthday Party 3");
        values.put(COLUMN_EVENT_DESC, "Sarah's surprise birthday party at the downtown venue");
        values.put(COLUMN_EVENT_DATE, "20/01/2025");
        values.put(COLUMN_EVENT_TIME, "6:00 PM");
        values.put(COLUMN_EVENT_USER_ID, userId);
        values.put(COLUMN_EVENT_DELETED, 0);
        db.insert(TABLE_EVENTS, null, values);

        values.put(COLUMN_EVENT_TITLE, "Birthday Party 4");
        values.put(COLUMN_EVENT_DESC, "Sarah's surprise birthday party at the downtown venue");
        values.put(COLUMN_EVENT_DATE, "20/01/2025");
        values.put(COLUMN_EVENT_TIME, "6:00 PM");
        values.put(COLUMN_EVENT_USER_ID, userId);
        values.put(COLUMN_EVENT_DELETED, 0);
        db.insert(TABLE_EVENTS, null, values);

        values.put(COLUMN_EVENT_TITLE, "Birthday Party 5");
        values.put(COLUMN_EVENT_DESC, "Sarah's surprise birthday party at the downtown venue");
        values.put(COLUMN_EVENT_DATE, "20/01/2025");
        values.put(COLUMN_EVENT_TIME, "6:00 PM");
        values.put(COLUMN_EVENT_USER_ID, userId);
        values.put(COLUMN_EVENT_DELETED, 0);
        db.insert(TABLE_EVENTS, null, values);
    }

    public void clearAllEvents() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, null, null);
    }

    public boolean addEvent(String title, String description, String date, String time, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_TITLE, title);
        values.put(COLUMN_EVENT_DESC, description);
        values.put(COLUMN_EVENT_DATE, date);
        values.put(COLUMN_EVENT_TIME, time);
        values.put(COLUMN_EVENT_USER_ID, userId);
        values.put(COLUMN_EVENT_DELETED, 0);
        long result = db.insert(TABLE_EVENTS, null, values);
        return result != -1;
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

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
