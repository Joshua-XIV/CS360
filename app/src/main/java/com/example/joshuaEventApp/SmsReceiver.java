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
        String message = intent.getStringExtra("message");

        if (phone == null || phone.isEmpty() || message == null) return;

        try {
            SmsManager smsManager = context.getSystemService(SmsManager.class);
            smsManager.sendTextMessage(phone, null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}