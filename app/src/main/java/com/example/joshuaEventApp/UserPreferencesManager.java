package com.example.joshuaEventApp;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

/*
 * UserPreferencesManager.java
 *
 * Stores user-configurable app settings such as reminder timing,
 * SMS reminders, and push notifications.
 *
 * This keeps app preferences separate from SessionManager, which should
 * mostly focus on login/session state.
 */
public class UserPreferencesManager {

    private static final String PREFERENCE_NAME = "EventTrackerUserPreferences";

    private static final String KEY_SMS_ENABLED = "smsEnabled";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notificationsEnabled";
    private static final String KEY_REMINDER_OFFSETS = "reminderOffsets";

    public static final long ONE_WEEK = 7L * 24 * 60 * 60 * 1000;
    public static final long THREE_DAYS = 3L * 24 * 60 * 60 * 1000;
    public static final long ONE_DAY = 24L * 60 * 60 * 1000;
    public static final long SIX_HOURS = 6L * 60 * 60 * 1000;
    public static final long THREE_HOURS = 3L * 60 * 60 * 1000;
    public static final long ONE_HOUR = 60L * 60 * 1000;

    private final SharedPreferences prefs;

    public UserPreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public boolean isSmsEnabled() {
        return prefs.getBoolean(KEY_SMS_ENABLED, true);
    }

    public void setSmsEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(KEY_SMS_ENABLED, enabled)
                .apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
                .apply();
    }

    public Set<String> getReminderOffsets() {
        Set<String> defaultOffsets = new LinkedHashSet<>();
        defaultOffsets.add(String.valueOf(ONE_DAY));
        defaultOffsets.add(String.valueOf(THREE_HOURS));
        defaultOffsets.add(String.valueOf(ONE_HOUR));

        return prefs.getStringSet(KEY_REMINDER_OFFSETS, defaultOffsets);
    }

    public void setReminderOffsets(Set<String> offsets) {
        prefs.edit()
                .putStringSet(KEY_REMINDER_OFFSETS, offsets)
                .apply();
    }

    public long[] getReminderOffsetsAsLongArray() {
        Set<String> savedOffsets = getReminderOffsets();
        long[] offsets = new long[savedOffsets.size()];

        int index = 0;
        for (String offset : savedOffsets) {
            offsets[index] = Long.parseLong(offset);
            index++;
        }

        return offsets;
    }

    public static String getReminderLabel(long offset) {
        if (offset == ONE_WEEK) return "in 1 week";
        if (offset == THREE_DAYS) return "in 3 days";
        if (offset == ONE_DAY) return "tomorrow";
        if (offset == SIX_HOURS) return "in 6 hours";
        if (offset == THREE_HOURS) return "in 3 hours";
        if (offset == ONE_HOUR) return "in 1 hour";

        return "soon";
    }
}