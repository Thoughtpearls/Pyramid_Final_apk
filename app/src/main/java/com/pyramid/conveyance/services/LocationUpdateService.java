package com.pyramid.conveyance.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.pyramid.conveyance.LocationApp;
import com.pyramid.conveyance.R;
import com.pyramid.conveyance.respository.database.TripDatabase;
import com.pyramid.conveyance.respository.entity.TripRecord;
import com.pyramid.conveyance.ui.recordride.RecordRideActivity;

import java.util.Random;

/**
 * Starts location updates on background and publish LocationUpdateEvent upon
 * each new location result.
 */
public class LocationUpdateService extends Service {

    //region data
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;
    private static final int SERVICE_LOCATION_REQUEST_CODE  = 11;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private TripDatabase db;

    //endregion

    //onCreate
    @Override
    public void onCreate() {
        super.onCreate();
        initData();
        db = Room.databaseBuilder(getApplicationContext(),
                TripDatabase.class, "tripTracker").build();
    }


    //Location Callback
    private LocationCallback  locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location currentLocation = locationResult.getLastLocation();
            LocationApp.logs("Locations", currentLocation.getLatitude() + "," + currentLocation.getLongitude());
            //Share/Publish Location
            TripRecord tripRecord = new TripRecord();
            Random random = new Random();
            tripRecord.tripId = random.nextLong();
            tripRecord.startTimestamp = System.currentTimeMillis();
            //tripRecord.latlong = currentLocation.getLatitude() + "," + currentLocation.getLongitude();
            db.tripRecordDao().insertAll(tripRecord);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        prepareForegroundNotification();
        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(this.locationRequest,
                this.locationCallback, Looper.myLooper());
    }

    private void prepareForegroundNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                     "1",
                    "location",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
        Intent notificationIntent = new Intent(this, RecordRideActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                SERVICE_LOCATION_REQUEST_CODE,
                notificationIntent, PendingIntent.FLAG_MUTABLE);

        Notification notification = new NotificationCompat.Builder(this, "1")
                .setContentTitle(getString(com.pyramid.conveyance.R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initData() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this.getApplicationContext());

    }
}
