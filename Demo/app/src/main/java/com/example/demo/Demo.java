package com.example.demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.demo.services.NotificationService;
import com.example.demo.services.RestartAppService;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Demo extends AppCompatActivity {

    //Buttons for lock the screen and enable the app as device administrator
    Button lockButton;
    static final int RESULT_ENABLE = 1;
    DevicePolicyManager devicePolicyManager;
    ComponentName componentName;
    Activity activity = this;
    public static boolean isAppClosed;
    Intent restartAppServiceIntent;
    boolean isScreenLocked;

    @RequiresApi(api = Build.VERSION_CODES.O_MR1)
    @SuppressLint({"SetTextI18n", "InvalidWakeLockTag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ProcessPhoenix.isPhoenixProcess(this)) {
            return;
        }
        isAppClosed = false;
        //Locate button on xml
        lockButton = findViewById(R.id.sleepButton);
        //Create a policy manager and a system service for admin authorization
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(Demo.this, Controller.class);
        isScreenLocked = false;

        boolean active = devicePolicyManager.isAdminActive(componentName);

        if (active) {
            //App has admin authorization
            lockButton.setVisibility(View.VISIBLE);
        } else {
            //App does not have admin authorization
            lockButton.setVisibility(View.GONE);
            enableAdminAuthorization();
        }

        restartAppServiceIntent = new Intent(Demo.this, RestartAppService.class);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            activity.startForegroundService(new Intent(Demo.this, NotificationService.class));
        } else {
            startService(new Intent(Demo.this, NotificationService.class));
        }

        //onClickListener for locking the screen and waking up the screen
        lockButton.setOnClickListener(v -> {
                    // Lock the screen
                    devicePolicyManager.lockNow();
                    isScreenLocked = true;
                    // Unlock and turn on screen
                    turnScreenOnThroughKeyguard(Demo.this);
                }
        );
    }

    private void enableAdminAuthorization() {
        boolean active1 = devicePolicyManager.isAdminActive(componentName);
        if (active1) {
            //App is not authorized and lock button is not visible, thus locking error without
            // permission is prevented
            devicePolicyManager.removeActiveAdmin(componentName);
            lockButton.setVisibility(View.GONE);
        } else {
            //Create an intent to add app as device admin and start its activity
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Please enable the app as device administrator");
            startActivityForResult(intent, RESULT_ENABLE);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RESULT_ENABLE) {
            if (resultCode == Activity.RESULT_OK) {
                //Permission is granted and the app has admin authorization now.
                //LockButton is now visible and the device can be locked and waken up
                lockButton.setVisibility(View.VISIBLE);
            } else {
                //If a problem occurs send a toast with fail message
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static void turnScreenOnThroughKeyguard(@NonNull Activity activity) {
        // Create a handle and wait for 10 seconds to handle
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            // Create powerManager and wakelock
            userPowerManagerWakeup(activity);
            // Create flags
            useWindowFlags(activity);
            // Turn the screen on
            useActivityScreenMethods(activity);
        }, 10000); // 10 seconds

    }

    private static void userPowerManagerWakeup(@NonNull Activity activity) {
        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakelock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, " ");
        wakelock.acquire(1);
    }

    // Add Flags to the activity which will be waken up
    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    private static void useWindowFlags(@NonNull Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private static void useActivityScreenMethods(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                activity.setTurnScreenOn(true);
                activity.setShowWhenLocked(true);
            } catch (NoSuchMethodError e) {
                Log.e("enable", "Enable setTurnScreenOn and setShowWhenLocked is not present on device!");
            }
        }
    }

    //TODO: When app is closed by clicking yes in alertDialog,
    // App is deleted from recent apps and restarts with delay.
    // However, it does not restart app if user removes app
    // from recent app without using alert dialog.

    // If user choose yes, kill the app. Otherwise, cancel the dialog and do nothing
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Demo.this);
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setMessage("Do you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> finishAndRemoveTask())
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O_MR1)
    @Override
    protected void onResume() {
        isAppClosed = false;
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        isAppClosed = true;
        Log.d("RestartAppService", "onDestroy MainActivity");
        // runRestartAppService();
        triggerRebirth(this);
        super.onDestroy();
    }

    public static void triggerRebirth(Context context) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            Intent intent = new Intent(context, Demo.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
            Runtime.getRuntime().exit(0);
        }, 1 * 10000); // 5 minutes timer

    }

    private void runRestartAppService() {
        Log.d("RestartAppService", "openWakeUpService method runs on MainActivity");
        startService(restartAppServiceIntent);
    }
}