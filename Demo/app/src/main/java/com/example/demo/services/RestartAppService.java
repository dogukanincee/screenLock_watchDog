package com.example.demo.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.example.demo.Demo;

public class RestartAppService extends Service {

    @Override
    public void onCreate() {
        Log.d("RestartAppService", "onCreate WakeUpService");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Starts main activity after handler's delay
        Log.d("RestartAppService", "onStart WakeUpService");
        if(Demo.isAppClosed){
            restartApp();
        }
        return super.onStartCommand(intent, flags, startId);
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


    @Override
    public IBinder onBind(Intent intent) {
        Log.d("RestartAppService", "onBind WakeUpService");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
 /*
        Log.d("onTaskRemoved", "onTaskRemoved called");
        super.onTaskRemoved(rootIntent);
        // Do
        // Stops foregroundService
        this.stopSelf();
    */
        Log.d("RestartAppService", "onTaskRemoved WakeUpService");
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
        Log.d("RestartAppService", "onStart WakeUpService");
        if(Demo.isAppClosed){
            restartApp();
        }
        super.onDestroy();
    }

}