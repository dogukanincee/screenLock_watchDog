package com.example.demo;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class Controller extends DeviceAdminReceiver {

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        //If permission is granted by the user, send a toast to a user informing
        // the app is now an admin
        Toast.makeText(context, "Device Admin: Enabled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        //If permission is revoked by the user, send a toast to a user informing
        // the app is not an admin anymore
        Toast.makeText(context, "Device Admin: Disabled", Toast.LENGTH_LONG).show();
    }
}
