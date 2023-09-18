package com.thoughtpearl.conveyance.respository.syncjob;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.util.Log;

import com.thoughtpearl.conveyance.LocationApp;
import com.thoughtpearl.conveyance.api.ApiHandler;
import com.thoughtpearl.conveyance.api.response.CreateTurnOnGpsRequest;
import com.thoughtpearl.conveyance.utility.TrackerUtility;

import java.util.Date;
import java.util.Timer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostLocationReceiver extends BroadcastReceiver {
    public static boolean locationOff = false;
    public static boolean isLocationTurnOffRequestSent = false;
    public static boolean isLocationTurnOnRequestSent = false;
    @Override
    public void onReceive(Context context, Intent intent) {
        LocationApp.logs("TRIP", "Location update received on broadcast receiver :" + new Date().toString());
        LocationApp.logs("TRIP", "TrackerUtility.isGpsEnabled(context) : " + TrackerUtility.isGpsEnabled(context));

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (!locationOff) {
                LocationApp.logs("TRIP", "Location turnoff request sent :" + new Date().toString());
                locationOff = true;
                isLocationTurnOffRequestSent = true;
                sentLocationTurnOffNotification(context, true);
            }
        } else {
            if (isLocationTurnOffRequestSent && !isLocationTurnOnRequestSent) {
                LocationApp.logs("TRIP", "Location turnOn request sent :" + new Date().toString());
                isLocationTurnOnRequestSent = true;
                sentLocationTurnOffNotification(context, false);
            }
            locationOff = false;
        }

        RecordRideSyncJob recordRideSyncJob = new RecordRideSyncJob(context, true, false, result -> {
            if(result instanceof  Result.Success) {
                if (((Result.Success<Boolean>) result).data.booleanValue()) {
                    LocationApp.logs("TRIP", " Success True : ride location sync successfully");
                } else {
                    LocationApp.logs("TRIP", " Success False : ride location sync failed from broadcast receiver:");
                }
            } else {
                LocationApp.logs("TRIP", "Error block : ride location sync failed from broadcast receiver:");
            }
        });
        Timer tempTimer = new Timer();
        tempTimer.schedule(recordRideSyncJob, 0);
    }

    private static void sentLocationTurnOffNotification(Context context, boolean isLocationTurnOff) {
        String employeeFullName = LocationApp.getEmployeeProfile().getFullName();
        String employeeCode = LocationApp.getEmployeeProfile().getEmployeeCode();

        SharedPreferences sharedPreferences = context.getSharedPreferences(LocationApp.APP_NAME, Context.MODE_PRIVATE);
        if (employeeFullName == null) {
            employeeFullName = sharedPreferences.getString("employeeFullName", LocationApp.getUserName(context));
        }

        if (employeeCode == null) {
            employeeCode = sharedPreferences.getString("employeeCode", LocationApp.getUserName(context));
        }

        Date date = new Date();
        StringBuilder message = new StringBuilder();
        message.append(employeeFullName);
        message.append("(");
        message.append(employeeCode);
        message.append(")");
        if (isLocationTurnOff) {
            message.append(" turned off Location on ");
        } else {
            message.append(" turned On Location on ");
        }
        message.append(TrackerUtility.getDateString(date, "yyyy-MM-dd"));
        message.append(" at ");
        message.append(TrackerUtility.getDateString(date, "HH:mm:ss"));

        Call<String> gpsTurnOfRequest = ApiHandler.getClient().createLocationTurnOffNotification(LocationApp.getUserName(context), LocationApp.DEVICE_ID,
                new CreateTurnOnGpsRequest("LOCATION_OFF", message.toString()));
        gpsTurnOfRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                LocationApp.logs("TRIP", " createLocationTurnOffNotification+( " + response.toString() + ") : gps turn Off request send successfully.");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                LocationApp.logs("TRIP", " createLocationTurnOffNotification : gps turn Off : onFailure - " + t.getMessage());
            }
        });
    }
}
