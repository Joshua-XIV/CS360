package com.example.joshuaEventApp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * SmsScheduler.java
 *
 * Schedules SMS reminders via AlarmManager to fire 24 hours, 3 hours,
 * and 1 hour before a given event timestamp.
 */
public class SmsScheduler {

    private static final long[] REMINDER_OFFSETS = {
            24 * 60 * 60 * 1000L,  // 24 hours
            3 * 60 * 60 * 1000L,   // 3 hours
            60 * 60 * 1000L        // 1 hour
    };

    private static final String[] REMINDER_LABELS = {
            "tomorrow",
            "in 3 hours",
            "in 1 hour"
    };

    public static void scheduleReminder(Context context, int eventId, String eventTitle, long eventTimestamp, String phone) {
        if (phone == null || phone.isEmpty()) return;

        android.util.Log.d("SmsScheduler", "scheduleReminder called for: " + eventTitle + " phone: " + phone);
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String eventTime = timeFormat.format(new Date(eventTimestamp));

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < REMINDER_OFFSETS.length; i++) {
            long reminderTime = eventTimestamp - REMINDER_OFFSETS[i];
            if (reminderTime <= System.currentTimeMillis()) continue;

            String message = "Reminder: \"" + eventTitle + "\" is " + REMINDER_LABELS[i] + " at " + eventTime;

            Intent intent = new Intent(context, SmsReceiver.class);
            intent.putExtra("phone", phone);
            intent.putExtra("message", message);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    eventId * 10 + i,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            } catch (SecurityException e) {
                android.util.Log.e("SmsScheduler", "Error: reminder failed to set");
            }
        }
    }

    public static void cancelReminder(Context context, int eventId) {
        for (int i = 0; i < REMINDER_OFFSETS.length; i++) {
            Intent intent = new Intent(context, SmsReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    eventId * 10 + i,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
        }
    }
}