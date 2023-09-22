package com.pyramid.conveyance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.widget.Toast;

import com.pyramid.conveyance.utility.TrackerUtility;

public class GpsStatusChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        /*if (intent.getAction().matches("android.location.GPS_ENABLED_CHANGE")) {
            boolean enabled = intent.getBooleanExtra("enabled",false);
            Toast.makeText(context, "GPS : status " + TrackerUtility.isPermissionGranted(context),
                    Toast.LENGTH_SHORT).show();
        }*/
    }
}
