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
 * Utility class responsible for scheduling and cancelling
 * event reminder alarms using AlarmManager.
 *
 * Creates both SMS reminders and local push notifications
 * at multiple intervals before an event:
 * - 24 hours before
 * - 3 hours before
 * - 1 hour before
 */
public class SmsScheduler {

    // Reminders in milliseconds
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

    // Schedules all SMS and notification reminders for a specific event
    public static void scheduleReminder(Context context, int eventId, String eventTitle, long eventTimestamp, String phone) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String eventTime = timeFormat.format(new Date(eventTimestamp));

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Creates alarms for each reminder interval
        for (int i = 0; i < REMINDER_OFFSETS.length; i++) {
            long reminderTime = eventTimestamp - REMINDER_OFFSETS[i];
            if (reminderTime <= System.currentTimeMillis()) continue;

            String message = "Reminder: \"" + eventTitle + "\" is " + REMINDER_LABELS[i] + " at " + eventTime;

            // Schedule SMS
            if (phone != null && !phone.isEmpty()) {
                Intent smsIntent = new Intent(context, SmsReceiver.class);
                smsIntent.putExtra("phone", phone);
                smsIntent.putExtra("message", message);

                PendingIntent smsPendingIntent = PendingIntent.getBroadcast(
                        context,
                        eventId * 10 + i,
                        smsIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    android.util.Log.d("SmsScheduler", "Setting SMS alarm " + i + " diff ms: " + (reminderTime - System.currentTimeMillis()));
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, smsPendingIntent);
                } catch (SecurityException e) {
                    android.util.Log.e("SmsScheduler", "SecurityException: " + e.getMessage());
                }
            }

            // Schedule push notification
            Intent notifIntent = new Intent(context, NotificationReceiver.class);
            notifIntent.putExtra("message", message);
            notifIntent.putExtra("notificationId", eventId * 10 + i);

            PendingIntent notifPendingIntent = PendingIntent.getBroadcast(
                    context,
                    eventId * 100 + i,
                    notifIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, notifPendingIntent);
            } catch (SecurityException e) {
                android.util.Log.e("SmsScheduler", "SecurityException: " + e.getMessage());
            }
        }
    }

    // Cancels all scheduled SMS and notification reminders for a specific event
    public static void cancelReminder(Context context, int eventId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < REMINDER_OFFSETS.length; i++) {
            // Cancel SMS alarms
            Intent smsIntent = new Intent(context, SmsReceiver.class);
            PendingIntent smsPendingIntent = PendingIntent.getBroadcast(
                    context,
                    eventId * 10 + i,
                    smsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(smsPendingIntent);

            // Cancel notification alarms
            Intent notifIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent notifPendingIntent = PendingIntent.getBroadcast(
                    context,
                    eventId * 100 + i,
                    notifIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(notifPendingIntent);
        }
    }
}