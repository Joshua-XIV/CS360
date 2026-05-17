package com.example.joshuaEventApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.core.content.ContextCompat;
import android.Manifest;

/*
 * SmsReceiver.java
 *
 * BroadcastReceiver that fires when an AlarmManager alarm goes off.
 * Sends an SMS reminder to the user's phone number for the upcoming event.
 */
public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String phone = intent.getStringExtra("phone");
        String eventTitle = intent.getStringExtra("eventTitle");
        String eventTime = intent.getStringExtra("eventTime");

        if (phone == null || phone.isEmpty()) return;

        String message = "Reminder: \"" + eventTitle + "\" is tomorrow at " + eventTime;

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}