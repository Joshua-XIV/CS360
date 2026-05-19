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
 * Reminder times are controlled by UserPreferencesManager.
 */
public class SmsScheduler {
    private static final int MAX_REMINDER_SLOTS = 6;

    // Schedules all SMS and notification reminders for a specific event
    public static void scheduleReminder(Context context, int eventId, String eventTitle, long eventTimestamp, String phone) {
        UserPreferencesManager preferencesManager = new UserPreferencesManager(context);

        boolean smsEnabled = preferencesManager.isSmsEnabled();
        boolean notificationsEnabled = preferencesManager.isNotificationsEnabled();

        if (!smsEnabled && !notificationsEnabled) {
            return;
        }

        long[] reminderOffsets = preferencesManager.getReminderOffsetsAsLongArray();

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String eventTime = timeFormat.format(new Date(eventTimestamp));

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Creates alarms for each reminder interval
        for (int i = 0; i < reminderOffsets.length; i++) {
            long reminderTime = eventTimestamp - reminderOffsets[i];
            if (reminderTime <= System.currentTimeMillis()) continue;

            String reminderLabel = UserPreferencesManager.getReminderLabel(reminderOffsets[i]);
            String message = "Reminder: \"" + eventTitle + "\" is " + reminderLabel + " at " + eventTime;

            if (smsEnabled && phone != null && !phone.trim().isEmpty()) {
                scheduleSmsAlarm(context, alarmManager, eventId, i, reminderTime, phone, message);
            }

            if (notificationsEnabled) {
                scheduleNotificationAlarm(context, alarmManager, eventId, i, reminderTime, message);
            }
        }
    }

    // Cancels all scheduled SMS and notification reminders for a specific event
    public static void cancelReminder(Context context, int eventId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < MAX_REMINDER_SLOTS; i++) {
            cancelSmsAlarm(context, alarmManager, eventId, i);
            cancelNotificationAlarm(context, alarmManager, eventId, i);
        }
    }

    // Schedules SMS reminder
    private static void scheduleSmsAlarm(Context context, AlarmManager alarmManager, int eventId,
            int reminderIndex, long reminderTime, String phone, String message) {
        Intent smsIntent = new Intent(context, SmsReceiver.class);
        smsIntent.putExtra("phone", phone);
        smsIntent.putExtra("message", message);

        PendingIntent smsPendingIntent = PendingIntent.getBroadcast(
                context,
                getSmsRequestCode(eventId, reminderIndex),
                smsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    smsPendingIntent
            );
        } catch (SecurityException e) {
            android.util.Log.e("SmsScheduler", "SMS alarm SecurityException: " + e.getMessage());
        }
    }

    // Schedules Notification reminder
    private static void scheduleNotificationAlarm(Context context, AlarmManager alarmManager,
            int eventId, int reminderIndex, long reminderTime, String message) {
        Intent notifIntent = new Intent(context, NotificationReceiver.class);
        notifIntent.putExtra("message", message);
        notifIntent.putExtra("notificationId", getNotificationRequestCode(eventId, reminderIndex));

        PendingIntent notifPendingIntent = PendingIntent.getBroadcast(
                context,
                getNotificationRequestCode(eventId, reminderIndex),
                notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    notifPendingIntent
            );
        } catch (SecurityException e) {
            android.util.Log.e("SmsScheduler", "Notification alarm SecurityException: " + e.getMessage());
        }
    }

    // Cancels SMS reminders
    private static void cancelSmsAlarm(Context context, AlarmManager alarmManager,
            int eventId, int reminderIndex) {
        Intent smsIntent = new Intent(context, SmsReceiver.class);

        PendingIntent smsPendingIntent = PendingIntent.getBroadcast(
                context,
                getSmsRequestCode(eventId, reminderIndex),
                smsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(smsPendingIntent);
        smsPendingIntent.cancel();
    }

    // Cancel Notification reminders
    private static void cancelNotificationAlarm(Context context, AlarmManager alarmManager,
            int eventId, int reminderIndex) {
        Intent notifIntent = new Intent(context, NotificationReceiver.class);

        PendingIntent notifPendingIntent = PendingIntent.getBroadcast(
                context,
                getNotificationRequestCode(eventId, reminderIndex),
                notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(notifPendingIntent);
        notifPendingIntent.cancel();
    }

    private static int getSmsRequestCode(int eventId, int reminderIndex) {
        return eventId * 10 + reminderIndex;
    }

    private static int getNotificationRequestCode(int eventId, int reminderIndex) {
        return eventId * 100 + reminderIndex;
    }
}