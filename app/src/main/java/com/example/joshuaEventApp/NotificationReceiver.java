package com.example.joshuaEventApp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/*
 * NotificationReceiver.java
 *
 * BroadcastReceiver that fires when an AlarmManager alarm goes off.
 * Posts a local push notification reminding the user of an upcoming event.
 *
 * Creates and displays a local push notification reminding the
 * user about an upcoming event.
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "event_reminders";
    private static final String CHANNEL_NAME = "Event Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Retrieves notification message and ID
        String message = intent.getStringExtra("message");
        int notificationId = intent.getIntExtra("notificationId", 0);

        if (message == null) return;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        // Creates the notification channel required
        // for Android 8.0 (API 26) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );
            channel.setDescription("Reminders for upcoming events");
            notificationManager.createNotificationChannel(channel);
        }

        // Tap notification to open app
        Intent openIntent = new Intent(context, EventDisplayActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Builds notification UI
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Event Reminder")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Displays notification
        notificationManager.notify(notificationId, builder.build());
    }
}