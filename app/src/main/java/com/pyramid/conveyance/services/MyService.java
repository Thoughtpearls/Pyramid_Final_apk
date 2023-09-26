package com.pyramid.conveyance.services;

import static com.pyramid.conveyance.LocationApp.NOTIFICATION_CHANNEL_ID;
import static com.pyramid.conveyance.LocationApp.NOTIFICATION_CHANNEL_NAME;
import static com.pyramid.conveyance.LocationApp.NOTIFICATION_ID;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteConstraintException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.pyramid.conveyance.respository.entity.TripRecordLocationRelation;
import com.pyramid.conveyance.respository.executers.AppExecutors;
import com.pyramid.conveyance.utility.TrackerUtility;
import com.pyramid.conveyance.LocationApp;
import com.pyramid.conveyance.R;
import com.pyramid.conveyance.api.ApiHandler;
import com.pyramid.conveyance.api.response.CreateTurnOnGpsRequest;
import com.pyramid.conveyance.api.response.Ride;
import com.pyramid.conveyance.respository.databaseclient.DatabaseClient;
import com.pyramid.conveyance.respository.entity.TripRecord;
import com.pyramid.conveyance.respository.syncjob.PostLocationReceiver;
import com.pyramid.conveyance.respository.syncjob.RecordRideSyncCallback;
import com.pyramid.conveyance.respository.syncjob.RecordRideSyncJob;
import com.pyramid.conveyance.respository.syncjob.Result;
import com.pyramid.conveyance.ui.recordride.RecordRideActivity;

import java.io.File;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyService extends LifecycleService implements LocationListener {

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 60000; // 10 seconds, in milliseconds
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 10000; // 1 second, in milliseconds
    public static final String START_SERVICE = "Start";
    public static final String STOP_SERVICE = "Stop";

    public static final String RESUME_SERVICE = "Resume";
    private final MyServiceBinder binder = new MyServiceBinder();
    private static boolean useGPSLocationForApp = true; // Use locationManager for getting GPS services
    private Context context;
    public static MutableLiveData<Boolean> isTrackingOn = new MutableLiveData<>();
    private FusedLocationProviderClient client;
    private LocationCallback locationCallback;
    private LocationRequest mLocationRequest;
    private NotificationCompat.Builder builder;
    private static Long tripId;
    private static Ride ride;
    private DatabaseClient databaseClient;
    private Location mLastLocation;
    public static MutableLiveData<Location> mCurrentLocation = new MutableLiveData<>();
    private ArrayList<com.pyramid.conveyance.respository.entity.Location> locationList = new ArrayList<>();
    public static MutableLiveData<TripRecord> runningTripRecord = new MutableLiveData<>();
    public static MutableLiveData<ArrayList<com.pyramid.conveyance.respository.entity.Location>> locationListData = new MutableLiveData<>();
    public static MutableLiveData<Double> totalDistance = new MutableLiveData<>();
    public static MutableLiveData<String> timerCount = new MutableLiveData<>();
    public LocationManager locationManager;
    public static final int notify = 60000;  //interval between two services(Here Service run every 5 seconds)
    int count = 0;  //number of times service is display
    private Handler mHandler = new Handler();   //run on another Thread to avoid crash
    //private Timer mTimer = null;    //timer handling
    private Handler timerHandler;
    //private int seconds = 0;
    private Runnable timerRunnable;
    private CustomLocationFilter customLocationFilter;
    float currentSpeed = 0.0f; // meters/second
    long runStartTimeInMillis;
    double manualDistance = 0d;
    PendingIntent locationUpdateBroadcaster;
    //GpsStatusChangeReceiver gpsStatusChangeReceiver;

    public MyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        databaseClient = DatabaseClient.getInstance(context);
        mCurrentLocation.setValue(null);
        client = LocationServices.getFusedLocationProviderClient(context);
        setupInitialValues();
        setupLocationListener();
        setupTimerTask();
    }


    private void setupLocationManagerListener() {

        locationManager = (LocationManager) getSystemService(context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); //setAccuracyは内部では、https://stackoverflow.com/a/17874592/1709287の用にHorizontalAccuracyの設定に変換されている。
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAltitudeRequired(false);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(true);
        criteria.setBearingRequired(false);

        //API level 9 and up
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        //criteria.setBearingAccuracy(Criteria.ACCURACY_HIGH);
        //criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);

        Integer gpsFreqInMillis = 5000;
        Integer gpsFreqInDistance = 5;  // in meters

        locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationApp.logs("setup location requestLocationUpdates");
        locationManager.requestLocationUpdates(gpsFreqInMillis, gpsFreqInDistance, criteria, this, null);
    }

    public void showToastMessage(String message) {
        if (!LocationApp.isAppInBackground()) {
            Toast.makeText(MyService.this, message, Toast.LENGTH_LONG).show();
        }
    }


    private void startTimer() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTrackingOn.getValue() != null && isTrackingOn.getValue().booleanValue()) {

                    //seconds = (System.currentTimeMillis() - runningTripRecord.getValue().getStartTimestamp())/1000l;
                    long currentTimestamp = System.currentTimeMillis();
                    long tempSecs = TimeUnit.MILLISECONDS.toSeconds(currentTimestamp - (runningTripRecord.getValue() != null ? runningTripRecord.getValue().getStartTimestamp() : 0 ));
                    //long minutes = TimeUnit.MILLISECONDS.toMinutes(currentTimestamp - runningTripRecord.getValue().getStartTimestamp());
                    //long hours = TimeUnit.MILLISECONDS.toHours(currentTimestamp - runningTripRecord.getValue().getStartTimestamp());
                    int hours = (int) (tempSecs / 3600);
                    int minutes = (int) (tempSecs % 3600) / 60;
                    int secs = (int) (tempSecs % 60);

                    // Format the seconds into hours, minutes,
                    // and seconds.
                    String time
                            = String
                            .format(Locale.getDefault(),
                                    "%02d:%02d:%02d", hours,
                                    minutes, secs);

                    //LocationApp.logs("TRIP", "timer  value :" + time);
                    timerCount.setValue(time);
                    updateNotificationManager(time);
                    // Set the text view text.
                    //seconds++;
                } else {
                    //seconds = 0;
                }
                // Post the code again
                // with a delay of 1 second.
                timerHandler.postDelayed(this, 1000);

            }
        };
        timerHandler.post(timerRunnable);
    }

    private void setupTimerTask() {
        /*if (mTimer != null) // Cancel if already existed
            mTimer.cancel();
        else
            mTimer = new Timer();   //recreate new
       mTimer.scheduleAtFixedRate(new RecordRideSyncJob(MyService.this, true, result -> {

        }), 0, notify);*/   //Schedule task
    }

    private void setupLocationListener() {
        locationCallback = new LocationCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                updateNotificationManager(timerCount.getValue());
                if (locationResult.getLocations() != null) {
                    locationResult.getLocations().forEach(location -> {
                        if (location != null && (location.isFromMockProvider() || TrackerUtility.isDeveloperModeEnabled(context))) {
                            showToastMessage("Please turn off developer option from settings. Without that ride will not be recorded.");
                            return;
                        }

                        boolean isAcceptedLocation = filterAndAddLocation(location);
                        LocationApp.logs("Ride Id :  " + ride.getId() + " " + location.getLatitude() +" : " +location.getLongitude() + " isAcceptedLocation : " + isAcceptedLocation);

                        isAcceptedLocation = isAcceptedLocation(location, isAcceptedLocation);

                        if (isAcceptedLocation) {
                            if (isTrackingOn.getValue() != null && isTrackingOn.getValue().booleanValue()) {
                                updateLocationAndDistance(location);
                                updateNotificationManager(timerCount.getValue());
                            }
                            mCurrentLocation.postValue(location);
                        }
                    });
                }
            }

            @SuppressLint("MissingPermission")
            public boolean isUsableLocation(Location location) {
                float lastLocationAccuracy = client.getLastLocation() != null && client.getLastLocation().isComplete() ? client.getLastLocation().getResult().getAccuracy() : 20;
                LocationApp.logs("TRIP", "lastLocationAccuracy :" + lastLocationAccuracy);
                if (location != null) {
                    LocationApp.logs("TRIP", "location.hasAccuracy():" + location.hasAccuracy());
                    LocationApp.logs("TRIP", "location.getAccuracy(): " + location.getAccuracy());
                    LocationApp.logs("TRIP", "location.getSpeed():" + location.getSpeed());
                    LocationApp.logs("TRIP", "location.getAccuracy() <= lastLocationAccuracy:" + (location.getAccuracy() <= lastLocationAccuracy));
                }
                return location != null && location.hasAccuracy() && location.getAccuracy() <= lastLocationAccuracy && location.getSpeed() > 0;
            }


            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };
    }

    private boolean isAcceptedLocation(Location location, boolean isAcceptedLocation) {
        mLastLocation = mCurrentLocation.getValue();
        if (location == null || mLastLocation == null) {
            return isAcceptedLocation;
        }
        double distance = location.distanceTo(mLastLocation);
        LocationApp.logs("TRIP", "pos 1 lat :" + location.getLatitude() + " long" + location.getLongitude());
        LocationApp.logs("TRIP", "pos 2 lat :" + mLastLocation.getLatitude() + " long" + mLastLocation.getLongitude());
        LocationApp.logs("TRIP", "Distance difference : " + distance);

        manualDistance += TrackerUtility.calculateDistance(location.getLatitude(), location.getLongitude(), mLastLocation.getLatitude(), mLastLocation.getLongitude(), "M");
        LocationApp.logs("TRIP", "Distance difference manualDistance : " + manualDistance);

        if (distance < 3 || (distance > 2000 && TimeUnit.MILLISECONDS.toMinutes(location.getTime() - mLastLocation.getTime()) < 2)) { //5 meter per second
            isAcceptedLocation = false;
        }
        return isAcceptedLocation;
    }

    private boolean updateLocationAndDistance(Location location) {
        mLastLocation = mCurrentLocation.getValue();
        if (mLastLocation != null && mLastLocation.hasSpeed()) {
            double distance = location.distanceTo(mLastLocation);//TrackerUtility.calculateDistance(location.getLatitude(), location.getLongitude(), mLastLocation.getLatitude(), mLastLocation.getLongitude(), "M");//
            LocationApp.logs("TRIP", "pos 1 lat :" + location.getLatitude() + " long" + location.getLongitude());
            LocationApp.logs("TRIP", "pos 2 lat :" + mLastLocation.getLatitude() + " long" + mLastLocation.getLongitude());
            LocationApp.logs("TRIP", "Distance difference : " + distance);

            manualDistance += TrackerUtility.calculateDistance(location.getLatitude(), location.getLongitude(), mLastLocation.getLatitude(), mLastLocation.getLongitude(), "M");
            LocationApp.logs("TRIP", "Distance difference manualDistance : " + manualDistance);
             // 300 meters in 300 seconds
            if (distance < 3 || (distance > 2000 && TimeUnit.MILLISECONDS.toMinutes(location.getTime() - mLastLocation.getTime()) < 2)) { //5 meter per second
                return true;
            }

            totalDistance.postValue(totalDistance.getValue() + (distance / 1000f));
        }
        mCurrentLocation.postValue(location);
        AtomicReference<TripRecord> tripRecord = new AtomicReference<>(runningTripRecord.getValue());

        if (tripRecord.get() == null) {
            AppExecutors.getInstance().getDiskIO().execute(() -> {
                TripRecordLocationRelation relation = DatabaseClient.getInstance(context).getTripDatabase().tripRecordDao().getByTripId(ride.getId());
                tripRecord.set(relation.getTripRecord());
                AppExecutors.getInstance().getMainThread().execute(() -> runningTripRecord.setValue(relation.getTripRecord()));
            });
        }

        if (tripRecord.get() == null) {
            tripRecord.set(new TripRecord());
            tripRecord.get().setTripId(tripId);
            tripRecord.get().setRidePurposeId(ride.getRidePurpose());
            tripRecord.get().setDeviceId(TrackerUtility.getDeviceId(context));
            long startTime;
            try {
                startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(ride.getRideDate() + " " + ride.getRideStartTime()).getTime();
            } catch (ParseException parseException) {
                startTime = System.currentTimeMillis();
            }
            tripRecord.get().setStartTimestamp(startTime);
            tripRecord.get().setTotalDistance(totalDistance.getValue().floatValue());
            TripRecord finalTripRecord1 = tripRecord.get();
            AppExecutors.getInstance().getDiskIO().execute(() -> {
                TripRecordLocationRelation relation = DatabaseClient.getInstance(context).getTripDatabase().tripRecordDao().getByTripId(ride.getId());
                if (relation == null || relation.tripRecord == null) {
                    long insertRowId = databaseClient.getTripDatabase().tripRecordDao().save(finalTripRecord1);
                }
                //tripId = databaseClient.getTripDatabase().tripRecordDao().getLastInsertedTripId(insertRowId);
                finalTripRecord1.setTripId(tripId);
                runningTripRecord.postValue(finalTripRecord1);

                String timeStamp = TrackerUtility.getTimeString(new Date());
                com.pyramid.conveyance.respository.entity.Location locationTemp = new com.pyramid.conveyance.respository.entity.Location(new Random().nextLong(), location.getLatitude(), location.getLongitude(), false, tripId, timeStamp);
                locationList.add(locationTemp);
                AppExecutors.getInstance().getMainThread().execute(() -> {
                    locationListData.postValue(locationList);
                });

                AppExecutors.getInstance().getDiskIO().execute(() -> {
                    databaseClient.getTripDatabase().tripRecordDao().save(locationTemp);
                });


                /*
                new Handler(getApplicationContext().getMainLooper()).post(() -> {
                    String timeStamp = TrackerUtility.getTimeString(new Date());
                    com.pyramid.conveyance.response.api.conveyance.LocationRequest request = new com.pyramid.conveyance.response.api.conveyance.LocationRequest("", location.getLatitude(), location.getLongitude(), tripId.toString(), timeStamp);
                    Call<String> createLocationCall = ApiHandler.getClient().createLocation(LocationApp.USER_NAME, LocationApp.DEVICE_ID, request);
                    createLocationCall.enqueue( new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            if (response.code() == 201) {
                                com.pyramid.conveyance.entity.respository.conveyance.Location locationTemp = new com.pyramid.conveyance.entity.respository.conveyance.Location(UUID.fromString(response.body().toString()), location.getLatitude(), location.getLongitude(), false, tripId);
                                locationList.add(locationTemp);
                                AppExecutors.getInstance().getMainThread().execute(()->{
                                    locationListData.postValue(locationList);
                                });
                                AppExecutors.getInstance().getDiskIO().execute(()->{
                                    databaseClient.getTripDatabase().tripRecordDao().save(locationTemp);
                                });
                            } else {
                                Toast.makeText(getApplicationContext(), "Location data sync failed :" + response.errorBody(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), "Location data sync failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                });*/

            });
        } else {

            String timeStamp = TrackerUtility.getTimeString(new Date());
            if (tripId == null) {
                tripId = runningTripRecord.getValue().getTripId();
            }
            com.pyramid.conveyance.respository.entity.Location locationTemp = new com.pyramid.conveyance.respository.entity.Location(new Random().nextLong(), location.getLatitude(), location.getLongitude(), false, tripId, timeStamp);
            locationList.add(locationTemp);
            locationListData.postValue(locationList);
            AppExecutors.getInstance().getDiskIO().execute(() -> {
                try {

                    TripRecordLocationRelation relation = DatabaseClient.getInstance(context).getTripDatabase().tripRecordDao().getByTripId(ride.getId());
                    if (relation == null || relation.tripRecord == null) {
                       databaseClient.getTripDatabase().tripRecordDao().save(tripRecord.get());
                    }

                    databaseClient.getTripDatabase().tripRecordDao().save(locationTemp);
                } catch (SQLiteConstraintException sqLiteConstraintException) {
                    LocationApp.logs("TRIP", sqLiteConstraintException.getMessage());

                    AppExecutors.getInstance().getNetworkIO().execute(() -> {
                        com.pyramid.conveyance.api.response.LocationRequest request = new com.pyramid.conveyance.api.response.LocationRequest("", location.getLatitude(), location.getLongitude(), tripId.toString(), locationTemp.getTimestamp());
                        ArrayList<com.pyramid.conveyance.api.response.LocationRequest> locationRequests = new ArrayList<>();
                        locationRequests.add(request);
                        Call<List<String>> createLocationCall = ApiHandler.getClient().createLocation(LocationApp.USER_NAME, LocationApp.DEVICE_ID, locationRequests);
                        createLocationCall.enqueue( new Callback<List<String>>() {
                            @Override
                            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                                if (response.code() == 201) {
                                    response.body().forEach(uuid -> {
                                        com.pyramid.conveyance.respository.entity.Location locationTemp = new com.pyramid.conveyance.respository.entity.Location(Long.parseLong(uuid), location.getLatitude(), location.getLongitude(), true, tripId, request.getTimeStamp());
                                        AppExecutors.getInstance().getDiskIO().execute(()-> {
                                            databaseClient.getTripDatabase().tripRecordDao().save(locationTemp);
                                        });
                                    });

                                } else {
                                    LocationApp.logs("TRIP", "Location data sync failed response.code() " + response.code() + " erorr :" + response.errorBody());
                                    showToastMessage("Location data sync failed :" + response.errorBody());
                                }
                            }

                            @Override
                            public void onFailure(Call<List<String>> call, Throwable t) {
                                LocationApp.logs("TRIP", "Location data sync failed : Exception: " + t.getMessage());
                                LocationApp.logs(t);
                                showToastMessage("Location data sync failed :" + t.getMessage());
                            }
                        });

                    });
                }
            });
        }

        return false;
    }

    private void updateNotificationManager(String duration) {
        //LocationApp.logs("TRIP", "Lat:" + location.getLatitude() + " Long :" + location.getLongitude());
        //Toast.makeText(context, "Lat:" +location.getLatitude() + " Long :" +location.getLongitude(), Toast.LENGTH_SHORT).show();
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);
        formatter.setRoundingMode(RoundingMode.HALF_UP);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        String message = "Distance : " + formatter.format(totalDistance.getValue() != null ? totalDistance.getValue() : 0.0) + " km";
        if (duration != null) {
            message = message + " Duration : " + duration;
        } else {
            message = message + " Duration : 00:00:00";
        }
        builder.setContentText(message);
        builder.setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager);
        }
        //notificationManager.notify(1, builder.build());
        startForeground(NOTIFICATION_ID, builder.build());

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(NotificationManager notificationManager) {
        notificationManager.createNotificationChannel(new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            switch (intent.getAction()) {
                case START_SERVICE:
                    //context.registerReceiver(gpsStatusChangeReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
                    PostLocationReceiver.locationOff = false;
                    runStartTimeInMillis = (long) (SystemClock.elapsedRealtimeNanos() / 1000000);
                    manualDistance = 0d;
                    Bundle bundle = intent.getExtras();
                    ride = bundle.getParcelable("ride");
                    useGPSLocationForApp = bundle.getBoolean("isUseGps", true);

                    tripId = ride.getId();

                    AppExecutors.getInstance().getDiskIO().execute(() -> {
                        TripRecord tripRecord = new TripRecord();
                        tripRecord.setTripId(ride.getId());
                        long startTime;
                        try {
                            startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(ride.getRideDate() + " " + ride.getRideStartTime()).getTime();
                        } catch (ParseException parseException) {
                            startTime = System.currentTimeMillis();
                        }
                        tripRecord.setStartTimestamp(startTime);
                        tripRecord.setTotalDistance(0);
                        tripRecord.setDeviceId(TrackerUtility.getDeviceId(context));
                        tripRecord.setStatus(false);
                        tripRecord.setRidePurposeId(ride.getRidePurpose());
                        DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().tripRecordDao().save(tripRecord);
                        //tripId = DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().tripRecordDao().getLastInsertedTripId(rowId);
                        //tripRecord.setTripId(tripId);
                        AppExecutors.getInstance().getMainThread().execute(() -> {
                            runningTripRecord.postValue(tripRecord);
                        });
                        //setupTimerTask();
                    });
                    start();
                    //seconds = 0;
                    startTimer();
                    if (isTrackingOn == null) {
                        isTrackingOn = new MutableLiveData<>();
                    }
                    isTrackingOn.postValue(true);
                    break;
                case STOP_SERVICE:
                    //context.unregisterReceiver(gpsStatusChangeReceiver);
                    PostLocationReceiver.locationOff = false;
                    if (isTrackingOn == null) {
                        isTrackingOn = new MutableLiveData<>();
                    }
                    isTrackingOn.postValue(false);
                    String imagePath = intent.getExtras().get("screenshot_path").toString();
                    LocationApp.logs("TRIP", "STOP ride request : imagePath :" + imagePath);
                    stop(imagePath);
                    //seconds = 0;
                    if (timerHandler != null) {
                        timerHandler.removeCallbacks(timerRunnable);
                    }
                    break;

                case RESUME_SERVICE:
                    resumeService();
                    //seconds = 0;
                    //startTimer();
                default:
            }
        }
        return START_STICKY;
        //return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return useGPSLocationForApp ? binder : null;
    }

    private void stop(String imagePath) {
        if (useGPSLocationForApp) {
            //LocationManager
            stopUpdatingLocation();
        } else {
            //FusedLocation
            client.removeLocationUpdates(locationCallback);
        }

        removeNotification();

       /*WorkRequest rideLocationSyncWorkerRequest =
                new OneTimeWorkRequest.Builder(RideLocationSyncWorker.class)
                        .build();
       WorkManager
               .getInstance(getApplicationContext())
               .beginWith(((OneTimeWorkRequest) rideLocationSyncWorkerRequest))
               .enqueue();*/

        //Send unsync location to server before updating ride.


        AppExecutors.getInstance().getNetworkIO().execute(() -> {

            RecordRideSyncCallback recordRideSyncCallback = result -> {
                if (result instanceof Result.Success) {
                    if (((Result.Success<Boolean>) result).data.booleanValue()) {
                        updateRide(imagePath);
                    } else {
                        showToastMessage("Failed to sync ride locations.");
                    }
                } else {
                    showToastMessage("Error occur while location synchronization");
                }
                AppExecutors.getInstance().getMainThread().execute(() -> {
                    LocationApp.dismissLoader();
                });
            };

            RecordRideSyncJob recordRideSyncJob = new RecordRideSyncJob(MyService.this, true, true, recordRideSyncCallback);
            Timer tempTimer = new Timer();
            tempTimer.schedule(recordRideSyncJob, 0);
        });

        /*long startTime = System.currentTimeMillis();

        while(!recordRideSyncJob.isCompleted()) {
            new Handler(Looper.getMainLooper()).postDelayed(()-> {
                if (TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime) > 2) {
                    LocationApp.logs("TRIP", "Explicitly closing the sync job");
                    recordRideSyncJob.setCompleted(true);
                    tempTimer.cancel();
                }
            }, 6000);

        }*/
        //updateRide(imagePath);
    }

    private void updateRide(String imagePath) {
        AppExecutors.getInstance().getDiskIO().execute(() -> {
            TripRecord tripRecord = runningTripRecord.getValue();
            if (tripRecord != null) {
                long endTime = System.currentTimeMillis();
                tripRecord.setEndTimestamp(endTime);
                tripRecord.setTotalDistance(totalDistance.getValue() != null ? totalDistance.getValue() : 0);
                tripRecord.setStatus(false);
                DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().tripRecordDao().updateRecord(tripRecord);
                ride.setRideEndTime(TrackerUtility.getTimeString(new Date(endTime)));
                double totalDistanceValue = tripRecord.getTotalDistance();
                if (totalDistanceValue == 0) {
                    totalDistanceValue = totalDistance.getValue();
                }
                if (totalDistanceValue == 0) {
                    List<com.pyramid.conveyance.respository.entity.Location> tripLocations = DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().locationDao().getAllLocationsByTripId(ride.getId());
                    totalDistanceValue = TrackerUtility.calculateDistanceInKilometer(tripLocations);
                }
                ride.setRideDistance(totalDistanceValue);
                File file = new File(imagePath);
                LocationApp.logs("TRIP", "image path :" + imagePath + "file exists :" + file.exists());

                RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
                RequestBody id = RequestBody.create(MediaType.parse("text/plain"), ride.getId().toString());
                RequestBody ridePurpose = RequestBody.create(MediaType.parse("text/plain"), ride.getRidePurpose());
                RequestBody rideStartTime = RequestBody.create(MediaType.parse("text/plain"), ride.getRideStartTime());
                RequestBody rideEndTime = RequestBody.create(MediaType.parse("text/plain"), ride.getRideEndTime());
                RequestBody rideDate = RequestBody.create(MediaType.parse("text/plain"), ride.getRideDate());
                RequestBody rideDistance = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(ride.getRideDistance()));

                Map<String, RequestBody> bodyMap = new HashMap<>();
                bodyMap.put("file\"; filename=\"pp.png\" ", fileBody);
                bodyMap.put("id", id);
                bodyMap.put("rideDate", rideDate);
                bodyMap.put("rideStartTime", rideStartTime);
                bodyMap.put("rideEndTime", rideEndTime);
                bodyMap.put("rideDistance", rideDistance);
                bodyMap.put("ridePurpose", ridePurpose);

                Call<Void> updateRideCall = ApiHandler.getClient().updateRide(LocationApp.getUserName(this), LocationApp.DEVICE_ID, ride.getId(), bodyMap);
                updateRideCall.enqueue(new Callback<Void>() {

                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        LocationApp.logs("TRIP", "Ride completed :" + response.code());
                        if (response.code() == 200 || response.code() == 201) {
                            tripRecord.setStatus(true);
                            AppExecutors.getInstance().getDiskIO().execute(() -> {
                                DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().tripRecordDao().updateRecord(tripRecord);
                            });
                            showToastMessage("Ride Recorded Successfully");
                        }
                        AppExecutors.getInstance().getMainThread().execute(() -> {
                            LocationApp.dismissLoader();
                        });
                        runningTripRecord.setValue(tripRecord);
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        LocationApp.logs("TRIP", "Ride not completed :" + t);
                        LocationApp.logs(t);
                        AppExecutors.getInstance().getMainThread().execute(() -> {
                            LocationApp.dismissLoader();
                        });
                    }
                });
            }
        });
    }

    private void removeNotification() {
        stopForeground(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        NotificationManagerCompat.from(this).cancel(LocationApp.NOTIFICATION_ID);
    }

    @SuppressLint("MissingPermission")
    private void start() {
        LocationApp.logs("started service :");
        setupInitialValues();
        Intent notificationIntent = new Intent(this, RecordRideActivity.class);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                11,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);
        startForeground(NOTIFICATION_ID, builder.build());

        LocationApp.logs("started service : useGPSLocationForApp : " + useGPSLocationForApp);
        if (useGPSLocationForApp) {
            //context.registerReceiver(gpsStatusChangeReceiver,  new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
            //LocationServiceManager
            setupLocationManagerListener();
        } else {
            //FusedLocationClient
            client.requestLocationUpdates(mLocationRequest,
                    locationCallback, Looper.myLooper());
        }
        sendLocationDataToServerPeriodically();
    }

    private void resumeService() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(10f);

        Intent notificationIntent = new Intent(this, RecordRideActivity.class);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                11,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);
        startForeground(NOTIFICATION_ID, builder.build());

        if (useGPSLocationForApp) {
            //context.registerReceiver(gpsStatusChangeReceiver,  new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
            //LocationServiceManager
            setupLocationManagerListener();
        } else {
            //FusedLocationClient
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
            client.requestLocationUpdates(mLocationRequest,
                    locationCallback, Looper.myLooper());
        }
        sendLocationDataToServerPeriodically();
    }

    private void sendLocationDataToServerPeriodically() {
        //getting the alarm manager
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //creating a new intent specifying the broadcast receiver
        Intent intentLR = new Intent(this, PostLocationReceiver.class);

        locationUpdateBroadcaster = PendingIntent.getBroadcast(this, 0, intentLR,
                PendingIntent.FLAG_IMMUTABLE);

        final Calendar time = Calendar.getInstance();
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);

        if (am != null) {
            am.setRepeating(AlarmManager.RTC, time.getTime().getTime(), 60000, locationUpdateBroadcaster);
        }

        /*if (android.os.Build.VERSION.SDK_INT >= 23) {
            assert am != null;
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    60000, locationUpdateBroadcaster);
        } else if (Build.VERSION.SDK_INT >= 19) {
            if (am != null) {
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis(), 60000, locationUpdateBroadcaster);
            }
        } else {
            if (am != null) {
                am.setRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis(), 60000, locationUpdateBroadcaster);
            }
        }*/
        LocationApp.logs("TRIP", "Alert set for every one minute");
    }

    private void setupInitialValues() {
        mLastLocation = null;
        manualDistance = 0d;
        mCurrentLocation.postValue(null);
        totalDistance.postValue(0d);
        currentSpeed = 0.0f; // meters/second
        runStartTimeInMillis = 0;
        customLocationFilter = new CustomLocationFilter(3);
        //locationListData = new MutableLiveData<>();
        locationListData.postValue(new ArrayList<>());

        builder = new NotificationCompat.Builder(this, "location");
        builder.setContentTitle("Recording Ride");
        builder.setContentText("Distance : 0.0Km  Duration : 00:00:00");
        builder.setSmallIcon(R.drawable.ic_baseline_directions_bike_24);
        builder.setOngoing(true);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(10f);
        //gpsStatusChangeReceiver = new GpsStatusChangeReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        //unregisterReceiver(gpsStatusChangeReceiver);
        //mTimer.cancel();
    }

    /*
    //class DatabaseServerSyncJob for handling task
    class RecordRideSyncJob extends TimerTask {
        int finishTaskCount = 0;
        @Override
        public void run() {
            // run on another thread
            LocationApp.logs("TRIP", "Timer job is running every one minute : " + TrackerUtility.getTimeString(new Date()));
            mHandler.post(() -> {
                // display toast
                AtomicReference<List<com.pyramid.conveyance.respository.entity.Location>> unSyncList = new AtomicReference<>(new ArrayList<>());

                AppExecutors.getInstance().getDiskIO().execute(()->{
                    unSyncList.set(databaseClient.getTripDatabase().tripRecordDao().getUnSyncServerLocations());
                    if (unSyncList.get().size() > 0) {
                        updateLocationsOnServer(unSyncList.get());
                        LocationApp.logs("TRIP", "UPDATING RECORDS..");
                        finishTaskCount = 0;
                    } else {
                        if (finishTaskCount++ >= 5) {
                            cancel();
                        }
                    }
                });
            });

        }

    } */

    public void updateLocationsOnServer(List<com.pyramid.conveyance.respository.entity.Location> unSyncedLocations) {
        updateLocationsOnServer(unSyncedLocations, 0);
    }
    public void updateLocationsOnServer(List<com.pyramid.conveyance.respository.entity.Location> unSyncedLocations, int retryAttemptCount) {
        if (!TrackerUtility.checkConnection(getApplicationContext())) {
            showToastMessage("Please check your network connection");
        } else {
            ArrayList<com.pyramid.conveyance.api.response.LocationRequest> locationRequests = new ArrayList<>();
            unSyncedLocations.forEach(location -> {
                com.pyramid.conveyance.api.response.LocationRequest request = new com.pyramid.conveyance.api.response.LocationRequest("", location.getLatitude(), location.getLongitude(), location.getTripId().toString(), location.getTimestamp());
                locationRequests.add(request);
            });

            Call<List<String>> createLocationCall = ApiHandler.getClient().createLocation(LocationApp.getUserName(this), LocationApp.DEVICE_ID, locationRequests);
            createLocationCall.enqueue(new Callback<List<String>>() {
                @Override
                public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                    LocationApp.logs("update locations response.code() : " + response.code());
                    if (response.code() == 201) {
                        AppExecutors.getInstance().getDiskIO().execute(() -> {
                            unSyncedLocations.forEach(location -> {
                                location.serverSync = true;
                                databaseClient.getTripDatabase().tripRecordDao().update(location);
                            });

                        });

                        /*com.pyramid.conveyance.entity.respository.conveyance.Location locationTemp = new com.pyramid.conveyance.entity.respository.conveyance.Location(UUID.fromString(response.body().toString()), location.getLatitude(), location.getLongitude(), false, tripId);
                        locationList.add(locationTemp);
                        AppExecutors.getInstance().getMainThread().execute(()->{
                            locationListData.postValue(locationList);
                        });
                        AppExecutors.getInstance().getDiskIO().execute(()->{
                            databaseClient.getTripDatabase().tripRecordDao().save(locationTemp);
                        });*/
                    } else {
                        if (retryAttemptCount < 1) {
                            updateLocationsOnServer(unSyncedLocations, 1);
                        }
                        showToastMessage("Location data sync failed :" + response.errorBody());
                    }
                }

                @Override
                public void onFailure(Call<List<String>> call, Throwable t) {
                    if (retryAttemptCount < 1) {
                        updateLocationsOnServer(unSyncedLocations, 1);
                    }
                    showToastMessage("Location data sync failed");
                    LocationApp.logs("Location data sync failed : ");
                    LocationApp.logs(t);
                }
            });
        }
    }

    @SuppressLint("NewApi")
    private long getLocationAge(Location newLocation){
        long locationAge;
        if(android.os.Build.VERSION.SDK_INT >= 17) {
            long currentTimeInMilli = (long)(SystemClock.elapsedRealtimeNanos() / 1000000);
            long locationTimeInMilli = (long)(newLocation.getElapsedRealtimeNanos() / 1000000);
            locationAge = currentTimeInMilli - locationTimeInMilli;
        }else{
            locationAge = System.currentTimeMillis() - newLocation.getTime();
        }
        return locationAge;
    }

    private boolean filterAndAddLocation(Location location) {

        long age = getLocationAge(location);

        /*if(age > 15 * 1000){ //more than 5 seconds
            LocationApp.logs("TRIP", "Location is old");
            return false;
        }*/

        if (location.getAccuracy() <= 0) {
            LocationApp.logs("TRIP", "Latitidue and longitude values are invalid.");
            return false;
        }

        //setAccuracy(newLocation.getAccuracy());
        float horizontalAccuracy = location.getAccuracy();
        if(horizontalAccuracy > 20) { //10meter filter
            LocationApp.logs("TRIP", "Accuracy is too low.");
            return false;
        }

        /* Kalman Filter */
        float Qvalue;

        long locationTimeInMillis = (long)(location.getElapsedRealtimeNanos() / 1000000);
        long elapsedTimeInMillis = locationTimeInMillis - runStartTimeInMillis;

        if(currentSpeed == 0.0f) {
            Qvalue = 3.0f; //3 meters per second
        } else {
            Qvalue = currentSpeed; // meters per second
        }

        customLocationFilter.Process(location.getLatitude(), location.getLongitude(), location.getAccuracy(), elapsedTimeInMillis, Qvalue);
        double predictedLat = customLocationFilter.get_lat();
        double predictedLng = customLocationFilter.get_lng();

        Location predictedLocation = new Location("");//provider name is unecessary
        predictedLocation.setLatitude(predictedLat);//your coords of course
        predictedLocation.setLongitude(predictedLng);
        float predictedDeltaInMeters =  predictedLocation.distanceTo(location);
        LocationApp.logs(" " + predictedDeltaInMeters);
        if (predictedDeltaInMeters > 60) {
            LocationApp.logs("TRIP", "custom Filter detects mal GPS, we should probably remove this from track");
            customLocationFilter.consecutiveRejectCount += 1;

            if(customLocationFilter.consecutiveRejectCount > 3) {
                customLocationFilter = new CustomLocationFilter(3); //reset Kalman Filter if it rejects more than 3 times in raw.
            }
            return false;
        } else {
            customLocationFilter.consecutiveRejectCount = 0;
        }

       /* *//* Notifiy predicted location to UI *//*
        Intent intent = new Intent("PredictLocation");
        intent.putExtra("location", predictedLocation);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);*/

        LocationApp.logs("TRIP", "Location quality is good enough.");
        currentSpeed = location.getSpeed();
        //locationList.add(location);
        return true;
    }

    private boolean filterAndAddLocationLocationManager(Location location) {

        long age = getLocationAge(location);

        if (age > 15 * 1000) { //more than 5 seconds
            LocationApp.logs("TRIP", "Location is old :" + age);
            return false;
        }

        if (location.getAccuracy() <= 0) {
            LocationApp.logs("TRIP", "Latitude and longitude values are invalid." + location.getAccuracy());
            return false;
        }

        //setAccuracy(newLocation.getAccuracy());
        float horizontalAccuracy = location.getAccuracy();
        if(horizontalAccuracy > 30) { //10meter filter
            LocationApp.logs("TRIP", "Accuracy is too low. horizontalAccuracy :" + horizontalAccuracy);
            return false;
        }

        /* Kalman Filter */
        float Qvalue;

        long locationTimeInMillis = (long)(location.getElapsedRealtimeNanos() / 1000000);
        long elapsedTimeInMillis = locationTimeInMillis - runStartTimeInMillis;

        if(currentSpeed == 0.0f) {
            Qvalue = 3.0f; //3 meters per second
        } else {
            Qvalue = currentSpeed; // meters per second
        }

        customLocationFilter.Process(location.getLatitude(), location.getLongitude(), location.getAccuracy(), elapsedTimeInMillis, Qvalue);
        double predictedLat = customLocationFilter.get_lat();
        double predictedLng = customLocationFilter.get_lng();

        Location predictedLocation = new Location("");//provider name is unecessary
        predictedLocation.setLatitude(predictedLat);//your coords of course
        predictedLocation.setLongitude(predictedLng);
        float predictedDeltaInMeters =  predictedLocation.distanceTo(location);

        if (predictedDeltaInMeters > 60) {
            LocationApp.logs("TRIP", "custom Filter detects mal GPS, we should probably remove this from track");
            customLocationFilter.consecutiveRejectCount += 1;

            if (customLocationFilter.consecutiveRejectCount > 3) {
                customLocationFilter = new CustomLocationFilter(3); //reset Kalman Filter if it rejects more than 3 times in raw.
            }
            return false;
        } else {
            customLocationFilter.consecutiveRejectCount = 0;
        }

        /* *//* Notifiy predicted location to UI *//*
        Intent intent = new Intent("PredictLocation");
        intent.putExtra("location", predictedLocation);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);*/

        LocationApp.logs("TRIP", "Location quality is good enough.");
        currentSpeed = location.getSpeed();
        LocationApp.logs("TRIP", "current Speed :" + currentSpeed);
        //locationList.add(location);
        return true;
    }

    //This is where we detect the app is being killed, thus stop service.
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        LocationApp.logs(String.valueOf("Terminated background location service:" + ride != null ? ride.getId() : " null"));
        this.stopUpdatingLocation();
        stopSelf();

        /*PendingIntent service = PendingIntent.getService(
                getApplicationContext(),
                1001,
                new Intent(getApplicationContext(), MyService.class),
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, service);*/
    }

    public void stopUpdatingLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.removeUpdates(this);

        if (locationUpdateBroadcaster != null) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(context.ALARM_SERVICE);
            alarmManager.cancel(locationUpdateBroadcaster);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                //notifyLocationProviderStatusUpdated(false);
                LocationApp.logs("TRIP", "GPS is out of service");
            } else {
                LocationApp.logs("TRIP", "GPS is on service");
                //notifyLocationProviderStatusUpdated(true);
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (useGPSLocationForApp) {
            boolean isValidLocation = filterAndAddLocationLocationManager(location);
            LocationApp.logs("TRIP", "onLocationChanged : lat " + location.getLatitude() + " long : " + location.getLongitude() + "isAccurate :" + isValidLocation);

            if (isTrackingOn != null && isTrackingOn.getValue() != null && isTrackingOn.getValue()) {
                updateNotificationManager(timerCount.getValue());
                if (location != null) {
                    if (location != null && (location.isFromMockProvider() || TrackerUtility.isDeveloperModeEnabled(context))) {
                        showToastMessage("Please turn off developer option from settings. Without that ride will not be recorded.");
                        return;
                    }
                    LocationApp.logs( "onLocationChanged : isValidLocation :" + isValidLocation);

                     isValidLocation = isAcceptedLocation(location, isValidLocation);

                    if (isValidLocation) {
                        if (isTrackingOn.getValue() != null && isTrackingOn.getValue().booleanValue()) {
                            LocationApp.logs( "onLocationChanged : tracking is On ");
                            updateLocationAndDistance(location);
                            updateNotificationManager(timerCount.getValue());
                        }
                        mCurrentLocation.postValue(location);
                    }
                }
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        LocationApp.logs("TRIP", "onProviderEnabled : " + provider);
        /*LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            sentLocationTurnOffNotification(this, false);
        }*/
    }

    @Override
    public void onProviderDisabled(String provider) {
        LocationApp.logs("TRIP", "onProviderDisabled : " + provider);
        /*LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            sentLocationTurnOffNotification(this, true);
        }*/
    }


    public class MyServiceBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
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
