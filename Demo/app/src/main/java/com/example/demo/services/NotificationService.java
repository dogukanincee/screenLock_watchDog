package com.example.demo.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.demo.Demo;
import com.example.demo.R;

public class NotificationService extends Service {


    public void onCreate() {
        super.onCreate();
        setNotificationChannel();
    }

    private void setNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {

            // Create a notification channel for android 8+
            String NOTIFICATION_CHANNEL_ID = "example.demo";
            String channelName = "Demo";

            // Initialize the channel
            NotificationChannel notificationChannel = new NotificationChannel
                    (NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // Create a notification manager and add into notification channel
            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);

            // Create a pending intent for notification to start main activity
            PendingIntent pendingIntent = PendingIntent.getActivity(NotificationService.this,
                    0,
                    new Intent(NotificationService.this, Demo.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Create the notification's properties
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                    (this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Demo App Notification") // Set its title
                    .setContentText("Application is running on background") // Set its context
                    .setPriority(NotificationManager.IMPORTANCE_HIGH) // Set its priority
                    .setCategory(Notification.CATEGORY_SERVICE) // Set its category
                    .setContentIntent(pendingIntent); // Set its intent when clicked

            Notification notification = notificationBuilder.build(); // Build the notification
            startForeground(1, notification); // Call foreground

            if(Demo.isAppClosed){
                restartApp();
            }
        } else {
            stopSelf();
        }
    }

    public void restartApp() {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            Intent intentMain = new Intent(this, Demo.class);
            intentMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Demo.isAppClosed=false;
            startActivity(intentMain);
        }, 1 * 10000); // 5 minutes timer
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        PendingIntent restartServicePendingIntent = PendingIntent.getService
                (getApplicationContext(), 1, restartServiceIntent,
                        PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        alarmService.set
                (AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1,
                        restartServicePendingIntent);
        super.onTaskRemoved(rootIntent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("RestartAppService", "onStart WakeUpService");
        setNotificationChannel();
        if(Demo.isAppClosed){
            restartApp();
        }
    }
}