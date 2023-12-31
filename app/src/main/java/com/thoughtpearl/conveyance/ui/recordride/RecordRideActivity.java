/*
  Copyright 2017 Google Inc. All Rights Reserved.
  <p>
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.thoughtpearl.conveyance.ui.recordride;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.hypertrack.hyperlog.HyperLog;
import com.hypertrack.hyperlog.utils.HLDateTimeUtility;
import com.thoughtpearl.conveyance.LocationApp;
import com.thoughtpearl.conveyance.R;
import com.thoughtpearl.conveyance.api.ApiHandler;
import com.thoughtpearl.conveyance.api.SearchRideFilter;
import com.thoughtpearl.conveyance.api.SearchRideResponse;
import com.thoughtpearl.conveyance.api.response.CreateTurnOnGpsRequest;
import com.thoughtpearl.conveyance.api.response.Ride;
import com.thoughtpearl.conveyance.api.response.RideReason;
import com.thoughtpearl.conveyance.respository.databaseclient.DatabaseClient;
import com.thoughtpearl.conveyance.respository.entity.TripRecord;
import com.thoughtpearl.conveyance.respository.entity.TripRecordLocationRelation;
import com.thoughtpearl.conveyance.respository.executers.AppExecutors;
import com.thoughtpearl.conveyance.services.MyService;
import com.thoughtpearl.conveyance.ui.customcomponent.CustomDialog;
import com.thoughtpearl.conveyance.ui.navigation.BottomNavigationActivity;
import com.thoughtpearl.conveyance.utility.SphericalUtil;
import com.thoughtpearl.conveyance.utility.TrackerUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Using location settings.
 * <p/>
 * Uses the {@link com.google.android.gms.location.SettingsApi} to ensure that the device's system
 * settings are properly configured for the app's location needs. When making a request to
 * Location services, the device's system settings may be in a state that prevents the app from
 * obtaining the location data that it needs. For example, GPS or Wi-Fi scanning may be switched
 * off. The {@code SettingsApi} makes it possible to determine if a device's system settings are
 * adequate for the location request, and to optionally invoke a dialog that allows the user to
 * enable the necessary settings.
 * <p/>
 * This sample allows the user to request location updates using the ACCESS_FINE_LOCATION setting
 * (as specified in AndroidManifest.xml).
 */
public class RecordRideActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {

    private static final String TAG = RecordRideActivity.class.getSimpleName();

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000L;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 2000L;

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    private FusedLocationProviderClient client;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;
    private Location mlastLocation;
    private float totalDistance = 0.0f;
    private int seconds = 0;

    // UI Widgets.
    private Button mStartUpdatesButton;
    private Button mStopUpdatesButton;
    private TextView totalDurationTextView;
    private TextView totalDistanceTextView;

    private GoogleMap mMap;
    private MapView mapView;
    private Runnable timerRunnable;
    private Handler timerHandler;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private Boolean mRequestingLocationUpdates;
    private DatabaseClient databaseClient;
    private boolean isServiceRunning = false;
    private TripRecord runningTripRecord;
    private List<com.thoughtpearl.conveyance.respository.entity.Location> locationList = new ArrayList<>();
    private ArrayList<RideReason> ridePurposeList = new ArrayList<>();
    private boolean isStopRideTriggered;

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_activity);
        LocationApp.logs("Record Activity : onCreate");
        // Locate the UI widgets.
        mStartUpdatesButton = findViewById(R.id.start_updates_button);
        mStartUpdatesButton.setOnClickListener(view -> {
            try {

                if (HyperLog.hasPendingDeviceLogs()) {
//                    HashMap<String, String> headers = new HashMap<>();
//                    headers.put("userName", LocationApp.getUserName(this));
//                    headers.put("deviceId", LocationApp.DEVICE_ID);
                    String fileName = LocationApp.getUserName(this) +"_" + HLDateTimeUtility.getCurrentTime();
                    File file = HyperLog.getDeviceLogsInFile(this);
                    Map<String, RequestBody> bodyMap = new HashMap<>();

                    RequestBody filePart = RequestBody.create(MediaType.parse("text/plain"), file);
                    bodyMap.put("file\"; filename=\"" + fileName + "\".txt", filePart);

                    Call<Void> uploadLogs = ApiHandler.getClient().uploadLogs(LocationApp.getUserName(this), LocationApp.DEVICE_ID, bodyMap);
                    uploadLogs.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                LocationApp.logs("TRIP", "logs has been sent : " + response.body());
                                HyperLog.deleteLogs();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            LocationApp.logs("TRIP","failure : " + t.getMessage());
                        }
                    });
                }

               /* HyperLog.pushLogs(this, headers,false, new HLCallback() {
                    @Override
                    public void onSuccess(@NonNull Object response) {
                        Log.d("TRIP", "logs sent successfully to server");
                    }

                    @Override
                    public void onError(@NonNull HLErrorResponse HLErrorResponse) {
                        Log.d("TRIP", "failed to sent logs" + HLErrorResponse.getErrorMessage());
                    }
                });*/
                mStartUpdatesButton.setEnabled(false);
                startUpdatesButtonHandler(view);
            } catch (Exception exception) {
                LocationApp.logs("exception in startUpdatesButtonHandler :");
                LocationApp.logs(exception);
            }
        });
        mStopUpdatesButton = findViewById(R.id.stop_updates_button);
        mStopUpdatesButton.setOnClickListener(view -> {
            try {
                mStopUpdatesButton.setEnabled(false);
                stopUpdatesButtonHandler(view);
            } catch (Exception exception) {
                LocationApp.logs("exception in startUpdatesButtonHandler :");
                LocationApp.logs(exception);
            }
        });
        totalDurationTextView = findViewById(R.id.total_duration);
        totalDistanceTextView = findViewById(R.id.total_distance);
        totalDistanceTextView.setText("" + totalDistance);
        mRequestingLocationUpdates = false;
        mSettingsClient = LocationServices.getSettingsClient(this);
        this.client = new FusedLocationProviderClient(this);
        buildLocationSettingsRequest();
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        mapView.setEnabled(true);
        databaseClient = DatabaseClient.getInstance(getApplicationContext());
        initToolbar();

        if (!TrackerUtility.checkConnection(getApplicationContext())) {
            Toast.makeText(RecordRideActivity.this, "Please check your network connection", Toast.LENGTH_LONG).show();
            return;
        } else {
            getRideReasonList(LocationApp.getUserName(this), LocationApp.DEVICE_ID);
        }
        subscribeToObservers();
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    Location location = locationResult.getLocations().get(0);
                    moveCameraToUser(new LatLng(location.getLatitude(), location.getLongitude()));
                    if (TrackerUtility.checkPlayServicesAvailable(RecordRideActivity.this)) {
                        client.removeLocationUpdates(mLocationCallback);
                    }
                }
            }
        };
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
    }

    private void subscribeToObservers() {
        MyService.isTrackingOn.observe(this, aBoolean -> {
            LocationApp.logs("TRIP", "isTrackingOn : Service is running  : " + aBoolean);
            isServiceRunning = aBoolean.booleanValue();
            mRequestingLocationUpdates = isServiceRunning;
            updateUI();
        });

        MyService.totalDistance.observe(this, aTotalDistance -> {
            LocationApp.logs("TRIP", "totalDistance: Service is running  : aTotalDistance :" + aTotalDistance.floatValue());
            totalDistance = aTotalDistance.floatValue();
            if (totalDistance > 0) {
                totalDistanceTextView.setText(TrackerUtility.roundOffDoubleToString((double) totalDistance));
            } else {
                totalDistanceTextView.setText("0.00");
            }
        });

        MyService.timerCount.observe(this, timerCount -> {
            if (timerCount == null) {
                timerCount = "00:00:00";
            }
            totalDurationTextView.setText(timerCount);
        });

        MyService.mCurrentLocation.observe(this, location -> {
            if (location == null) {
                return;
            }

            mlastLocation = mCurrentLocation;
            mCurrentLocation = location;
            updateLocationUI();

            LocationApp.logs("TRIP", "mCurrentLocation: Service is running  : Lat :" + location.getLatitude() + " Lng: " + location.getLongitude());
        });

        MyService.runningTripRecord.observe(this, tripRecord -> {
            LocationApp.logs("TRIP", "runningTripRecord: Service is running  : TripId :" + tripRecord.getTripId());
            runningTripRecord = tripRecord;
            if (runningTripRecord != null && runningTripRecord.isStatus() && isStopRideTriggered) {
                AppExecutors.getInstance().getMainThread().execute(() -> {
                    LocationApp.dismissLoader();
                    showFinishRideDetailPopUp();
                    isStopRideTriggered = false;
                });
            }
        });

        MyService.locationListData.observe(this, locations -> {
            LocationApp.logs("TRIP", "locationListData: Service is running  : location update :" + locations);
            locationList = locations;
        });
    }

    private void showFinishRideDetailPopUp() {
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(this);
        alertDialogBuilder.setView(R.layout.complete_ride_layout);
        AlertDialog alertDialog = alertDialogBuilder.show();
        ((TextView)alertDialog.findViewById(R.id.rideDateTextView)).setText(TrackerUtility.getDateString(new Date()));
        ((TextView)alertDialog.findViewById(R.id.rideAmountTextView)).setText(runningTripRecord.getReimbursementCost() != null && runningTripRecord.getReimbursementCost().trim().length() > 0 ? "Rs " +runningTripRecord.getReimbursementCost() : "Rs 0.0");
        ((TextView)alertDialog.findViewById(R.id.distanceTravelledTextView)).setText(TrackerUtility.roundOffDouble(runningTripRecord.getTotalDistance()) + " Km");
        ((TextView)alertDialog.findViewById(R.id.rideDurationTextView)).setText(MyService.timerCount.getValue());
        Optional<RideReason> rideReasonSearch = ridePurposeList.stream().filter(rideReason -> rideReason.getId().equalsIgnoreCase(runningTripRecord.getRidePurposeId())).findFirst();
        if (rideReasonSearch.isPresent()) {
            ((TextView)alertDialog.findViewById(R.id.rideReasonTextView)).setText(rideReasonSearch.get().getPurpose());
        } else {
            ((TextView)alertDialog.findViewById(R.id.rideReasonTextView)).setText("");
        }

        alertDialog.findViewById(R.id.okAlertButton).setOnClickListener(view -> {
            reInitialize();
            alertDialog.dismiss();
        });

        alertDialog.setOnDismissListener(dialogInterface -> {
            reInitialize();
        });
    }

    private void reInitialize() {
        mMap.clear();
        totalDistance = 0.0f;
        locationList = new ArrayList<>();
        runningTripRecord = new TripRecord();
        MyService.totalDistance.setValue(0d);
        MyService.timerCount.setValue("00:00:00");
    }

    private void getRideReasonList(String username, String deviceId) {
        Call<List<RideReason>> ridePurpose = ApiHandler.getClient().getRidePurpose(username, deviceId);
        ridePurpose.enqueue(new Callback<List<RideReason>>() {
            @Override
            public void onResponse(Call<List<RideReason>> call, Response<List<RideReason>> response) {
                if (response.code() == 200) {
                    ridePurposeList = (ArrayList<RideReason>) response.body();
                } else {
                    ridePurposeList = new ArrayList<>();
                }
            }

            @Override
            public void onFailure(Call<List<RideReason>> call, Throwable t) {
                ridePurposeList = new ArrayList<>();
            }
        });
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> this.finishActivity(RESULT_CANCELED));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    private void checkingAndUpdateRunningTripRecords() {
       //runningTripRecord = MyService.runningTripRecord.getValue();
       mRequestingLocationUpdates = (MyService.isTrackingOn.getValue() == null ? false : MyService.isTrackingOn.getValue());
        if (mRequestingLocationUpdates) {
                AppExecutors.getInstance().getDiskIO().execute(() -> {
                    runningTripRecord = DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().tripRecordDao().getRunningTrip();
                    TripRecordLocationRelation locationRelation = null;
                    if (runningTripRecord != null && !runningTripRecord.isStatus()) {
                        //mRequestingLocationUpdates = true;
                        long diff = System.currentTimeMillis() - runningTripRecord.getStartTimestamp();
                        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
                        startTimer((int)seconds);
                        locationRelation = DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().tripRecordDao().getByTripId(runningTripRecord.getTripId());
                        locationList = locationRelation.locations;
                        if (!TrackerUtility.isMyServiceRunning(MyService.class, this)) {
                            LocationApp.logs("TRIP", "Restarting background service :");
                            //MyService.runningTripRecord.setValue(runningTripRecord);
                            //MyService.totalDistance.setValue(runningTripRecord.totalDistance);
                            //MyService.locationListData.setValue((ArrayList<com.thoughtpearl.conveyance.respository.entity.Location>) locationList);
                            //Toast.makeText(this, "Please close the current ride and start it again as background service terminated by device.", Toast.LENGTH_LONG).show();
                            Bundle rideBundle = new Bundle();
                            //rideBundle.putParcelable("ride", ride);
                            Intent intent = new Intent(getApplicationContext(), MyService.class);
                            intent.setAction(MyService.RESUME_SERVICE);
                            intent.putExtras(rideBundle);
                            ContextCompat.startForegroundService(this, intent);
                        }
                    }

                    TripRecordLocationRelation finalLocationRelation = locationRelation;
                    runOnUiThread(() -> {
                        setButtonsEnabledState();
                        reDrawTravelPathOnMap(finalLocationRelation != null ? finalLocationRelation.getLocations() : new ArrayList<>());
                    });
                });
        } else {
            updateUI();
        }
    }

    private void zoomToSeeWholeTrack() {
        if (isListEmptyOrNull(locationList)) {
            return;
        }
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (com.thoughtpearl.conveyance.respository.entity.Location location : locationList) {
            builder.include(new LatLng(location.getLatitude(), location.getLongitude()));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), mapView.getWidth(), mapView.getHeight(), (int) (mapView.getHeight() * 0.05f)));
    }

    public boolean isListEmptyOrNull(List locations) {
        return locations == null || locations.size() == 0;
    }

    private void moveCameraToUser() {
        if (!isListEmptyOrNull(locationList)) {
            com.thoughtpearl.conveyance.respository.entity.Location location = locationList.get(locationList.size() - 1);
            moveCameraToUser(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    private void moveCameraToUser(LatLng latLng) {
        if (latLng != null && mMap != null ) {
            try {
                animateToMeters(1000, latLng);
            /* mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            latLng,
                            LocationApp.ZOOM_LEVEL
                    )
            );*/
            } catch (Exception e) {
                LocationApp.logs("TRIP", "error in moving map :" + e.getMessage());
            }
        }
    }

    /**
     * Uses a {@link LocationSettingsRequest.Builder} to build
     * a {@link LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
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
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        mMap.getUiSettings().setCompassEnabled(true);
                        mMap.getUiSettings().setZoomControlsEnabled(true);
                        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);

                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        updateUI();
                        break;
                }
                break;
        }
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        LocationApp.logs("TRIP", "startUpdatesButtonHandler clicked");
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!TrackerUtility.checkConnection(getApplicationContext())) {
            Toast.makeText(RecordRideActivity.this, "Please check your network connection", Toast.LENGTH_LONG).show();
            mStartUpdatesButton.setEnabled(true);
            return;
        } else if (TrackerUtility.isDeveloperModeEnabled(this)) {
            Toast.makeText(RecordRideActivity.this, "Please turn off developer option from settings before starting ride", Toast.LENGTH_LONG).show();
            mStartUpdatesButton.setEnabled(true);
            return;
        } else if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(RecordRideActivity.this, "Please turn on device location.", Toast.LENGTH_LONG).show();
            mStartUpdatesButton.setEnabled(true);
            return;
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences(LocationApp.APP_NAME, Context.MODE_PRIVATE);
            String checkInDate = sharedPreferences.getString(LocationApp.CLOCK_IN, "");
            if (!checkInDate.equalsIgnoreCase(TrackerUtility.getDateString(new Date()))) {
                Toast.makeText(RecordRideActivity.this, "Please mark attendance before starting ride", Toast.LENGTH_LONG).show();
                mStartUpdatesButton.setEnabled(true);
                return;
            }
            if (ridePurposeList.size() == 0) {
                Toast.makeText(RecordRideActivity.this, "Cannot start ride without selecting reason", Toast.LENGTH_SHORT).show();
                mStartUpdatesButton.setEnabled(true);
                return;
            }
            fetchTodaysRides();
        }
    }

    public void startRide(Ride ride, boolean isUseGps) {
        mMap.clear();
        totalDistance = 0.0f;
        locationList = new ArrayList<>();
        runningTripRecord = new TripRecord();
        startTimer();

        AppExecutors.getInstance().getDiskIO().execute(() -> {

            Long tripId = databaseClient.getTripDatabase().tripRecordDao().getLastTripId();
            LocationApp.logs("TRIP", "TripId:" + tripId);
            LocationApp.logs("TRIP", "TripCount:" + databaseClient.getTripDatabase().tripRecordDao().getTotalTrips().length);

            runOnUiThread(() -> {
                Bundle rideBundle = new Bundle();
                rideBundle.putParcelable("ride", ride);
                rideBundle.putBoolean("isUseGps", isUseGps);
                Intent intent = new Intent(getApplicationContext(), MyService.class);
                intent.setAction(MyService.START_SERVICE);
                intent.putExtras(rideBundle);
                ContextCompat.startForegroundService(this, intent);
            });
        });

        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    private void startTimer() {
        startTimer(0);
    }

    private void startTimer(int iSeconds) {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
         timerHandler = new Handler();
        seconds = iSeconds;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning) {
                    int hours = seconds / 3600;
                    int minutes = (seconds % 3600) / 60;
                    int secs = seconds % 60;

                    // Format the seconds into hours, minutes,
                    // and seconds.
                    String time
                            = String
                            .format(Locale.getDefault(),
                                    "%d:%02d:%02d", hours,
                                    minutes, secs);

                    //LocationApp.logs"TRIP", "timer  value :" + time);
                    // Set the text view text.
                    runOnUiThread(() -> {
                        //totalDurationTextView = findViewById(R.id.total_duration);
                        //totalDurationTextView.setText(time);
                    });
                    seconds++;
                } else {
                    seconds = 0;
                }
                // Post the code again
                // with a delay of 1 second.
                timerHandler.postDelayed(this, 1000);

            }
        };
        timerHandler.post(timerRunnable);
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates.
     */
    public void stopUpdatesButtonHandler(View view) {
        isStopRideTriggered = true;
        LocationApp.logs("TRIP", "stopUpdatesButtonHandler : clicked");
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        if (!TrackerUtility.checkConnection(this)) {
            Toast.makeText(this, "Please check your network connection", Toast.LENGTH_LONG).show();
        } else {
            if (timerHandler != null && isServiceRunning) {
                timerHandler.removeCallbacks(timerRunnable);
            }
        /*if (mMap != null) {
            mMap.clear();
            reDrawTravelPathOnMap(locationList);
        }*/
            zoomToSeeWholeTrack();

            new Handler().postDelayed(() -> {
                if (mMap != null) {
                    mMap.snapshot(bitmap -> {
                        File screenshot = TrackerUtility.takeScreen(getApplicationContext(), mapView, bitmap);
                        Intent intent = new Intent(RecordRideActivity.this, MyService.class);
                        intent.setAction(MyService.STOP_SERVICE);
                        intent.putExtra("screenshot_path", screenshot.getAbsolutePath());
                        //LocationApp.showLoader(this);
                        startService(intent);
                        stopLocationUpdates();
                        NotificationManagerCompat.from(this).cancel(LocationApp.NOTIFICATION_ID);
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            sentLocationTurnOffNotification(this);
                        }
                    });
                }
            }, 3000);
        }
    }


    private static void sentLocationTurnOffNotification(Context context) {
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
        message.append(" turned off Location on ");
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

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        if (mLocationSettingsRequest == null) {
            buildLocationSettingsRequest();
        }
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, locationSettingsResponse -> {
                    Log.i(TAG, "All location settings are satisfied.");
                    updateUI();
                })
                .addOnFailureListener(this, e -> {
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                    "location settings ");
                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the
                                // result in onActivityResult().
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(RecordRideActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sie) {
                                Log.i(TAG, "PendingIntent unable to execute request.");
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            String errorMessage = "Location settings are inadequate, and cannot be " +
                                    "fixed here. Fix in Settings.";
                            Log.e(TAG, errorMessage);
                            Toast.makeText(RecordRideActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            mRequestingLocationUpdates = false;
                    }
                    updateUI();
                });
    }

    /**
     * Updates all UI fields.
     */
    private void updateUI() {
        setButtonsEnabledState();
        updateLocationUI();
    }

    /**
     * Disables both buttons when functionality is disabled due to insuffucient location settings.
     * Otherwise ensures that only one button is enabled at any time. The Start Updates button is
     * enabled if the user is not requesting location updates. The Stop Updates button is enabled
     * if the user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStartUpdatesButton.setVisibility(View.GONE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mStopUpdatesButton.setEnabled(true);
                    mStopUpdatesButton.setVisibility(View.VISIBLE);
                }
            }, 3000);

        } else {
            mStartUpdatesButton.setEnabled(true);
            mStartUpdatesButton.setVisibility(View.VISIBLE);
            mStopUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    private void updateLocationUI() {
        if (locationList != null) {
            if (locationList != null && locationList.size() > 1) {
                if (mlastLocation == null) {
                    return;
                }
                LatLng origin;
                LatLng dest;
                if (true /*mCurrentLocation == null*/)
                {
                    com.thoughtpearl.conveyance.respository.entity.Location preLastLocation = locationList.get(locationList.size() - 2);
                    com.thoughtpearl.conveyance.respository.entity.Location lastLocation = locationList.get(locationList.size() - 1);
                    origin = new LatLng(preLastLocation.getLatitude(), preLastLocation.getLongitude());
                    dest = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                } /*else {
                    origin = new LatLng(mlastLocation.getLatitude(), mlastLocation.getLongitude());
                    dest = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                }*/

                if (locationList.size() < 3 && !runningTripRecord.isStatus()) {
                    com.thoughtpearl.conveyance.respository.entity.Location start = locationList.get(0);
                    MarkerOptions startMarkerOptions = new MarkerOptions();
                    startMarkerOptions.title("Start point");
                    startMarkerOptions.position(new LatLng(start.getLatitude(), start.getLongitude()));
                    mMap.addMarker(startMarkerOptions);
                }

                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.add(origin, dest);
                polylineOptions.color(LocationApp.POLYLINE_COLOR);
                polylineOptions.width(LocationApp.POLYLINE_WIDTH);
                polylineOptions.geodesic(true);
                mMap.addPolyline(polylineOptions);
                mMap.getMaxZoomLevel();
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setAllGesturesEnabled(true);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dest,
                        LocationApp.ZOOM_LEVEL));
            }
        }
    }

    public void reDrawTravelPathOnMap(List<com.thoughtpearl.conveyance.respository.entity.Location> locationList) {
        PolylineOptions polylineOptions = new PolylineOptions();
        LatLng dest = null;
        if (!isListEmptyOrNull(locationList)) {
            LatLng start = new LatLng(locationList.get(0).getLatitude(), locationList.get(0).getLongitude());
            for (com.thoughtpearl.conveyance.respository.entity.Location location : locationList) {
                dest = new LatLng(location.getLatitude(), location.getLongitude());
                polylineOptions.add(dest);
            }

            if (start != null && isServiceRunning && locationList.size() == 1 ) {
                MarkerOptions startMarkerOptions = new MarkerOptions();
                startMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                startMarkerOptions.title("Start point");
                startMarkerOptions.position(start);
                mMap.addMarker(startMarkerOptions);
            }

            /*if (dest != null && !isServiceRunning) {
                MarkerOptions endMarkerOptions = new MarkerOptions();
                endMarkerOptions.title("End point");
                endMarkerOptions.position(dest);
                mMap.addMarker(endMarkerOptions);
            }*/
        }

        polylineOptions.color(LocationApp.POLYLINE_COLOR);
        polylineOptions.width(LocationApp.POLYLINE_WIDTH);
        polylineOptions.geodesic(true);
        mMap.addPolyline(polylineOptions);
        mMap.getMaxZoomLevel();
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        moveCameraToUser(dest);
    }

    private void centerMapOnMyLocation() {

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
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
        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            moveCameraToUser(latLng);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (timerHandler != null && isServiceRunning) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            LocationApp.logs(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        /*if (locationList != null && locationList.size() > 1) {
            com.thoughtpearl.conveyance.respository.entity.Location lastLocation = locationList.get(locationList.size() - 1);
            LatLng lastPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 13f));
        }*/

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (checkPermissions()) {
            checkingAndUpdateRunningTripRecords();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
        //updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mapView != null) {
            mapView.onSaveInstanceState(savedInstanceState);
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(mainTextStringId),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int backgroundPermission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        int notificationPermission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return permissionState == PackageManager.PERMISSION_GRANTED
                    && backgroundPermission == PackageManager.PERMISSION_GRANTED
                    && notificationPermission == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return permissionState == PackageManager.PERMISSION_GRANTED
                    && backgroundPermission == PackageManager.PERMISSION_GRANTED;
        } else {
            return permissionState == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) &&
                        ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.POST_NOTIFICATIONS);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, view -> {
                        // Request permission
                        String[] permission;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.POST_NOTIFICATIONS };
                        } else {
                            permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        }
                        ActivityCompat.requestPermissions(RecordRideActivity.this, permission,
                                REQUEST_PERMISSIONS_REQUEST_CODE);
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            String[] permission;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.POST_NOTIFICATIONS};
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
            } else {
                permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            }
            ActivityCompat.requestPermissions(RecordRideActivity.this, permission,
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    "com.thoughtpearl.conveyance", null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
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
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        reDrawTravelPathOnMap(locationList);
        //centerMapOnMyLocation();
        //animateToMeters(100);
        if (TrackerUtility.checkPlayServicesAvailable(this)) {
            client.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } else {
            long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 5 meters
            long MIN_TIME_BW_UPDATES = 1000 * 15; // 15 seconds ok, 5 seconds really fast, 30s slow
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_DISTANCE_CHANGE_FOR_UPDATES, MIN_TIME_BW_UPDATES, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        if (location != null) {
                            moveCameraToUser(new LatLng(location.getLatitude(), location.getLongitude()));
                            locationManager.removeUpdates(this);
                        }
                    }
                });
            }
        }

        if (mRequestingLocationUpdates) {
            mStopUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setVisibility(View.VISIBLE);
        }

    }

    private void animateToMeters(int meters,  LatLng point){
        int mapHeightInDP = mapView.getHeight();
        Resources r = getResources();
        int mapSideInPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mapHeightInDP, r.getDisplayMetrics());

        LatLngBounds latLngBounds = calculateBounds(point, meters);
        if(latLngBounds != null) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, mapSideInPixels, mapSideInPixels, (int)(mapHeightInDP * 0.05f));
            if(mMap != null)
                mMap.animateCamera(cameraUpdate);
        }
    }

    private LatLngBounds calculateBounds(LatLng center, double radius) {
        return new LatLngBounds.Builder().
                include(SphericalUtil.computeOffset(center, radius, 0)).
                include(SphericalUtil.computeOffset(center, radius, 90)).
                include(SphericalUtil.computeOffset(center, radius, 180)).
                include(SphericalUtil.computeOffset(center, radius, 270)).build();
    }

    @Override
    public void onMapLoaded() {

    }

    private void fetchTodaysRides() {
        LocationApp.logs("Fetch Today's Rides");
        AppExecutors.getInstance().getNetworkIO().execute(() -> {
            Date today = Calendar.getInstance().getTime();
            //today = TrackerUtility.convertStringToDate("2022-12-27");
            SearchRideFilter filter = new SearchRideFilter(TrackerUtility.getDateString(today), TrackerUtility.getDateString(today));
            Call<SearchRideResponse> searchRideStatisticsCall = ApiHandler.getClient().searchRideStatistics(LocationApp.getUserName(this), LocationApp.DEVICE_ID, filter);
            searchRideStatisticsCall.enqueue(new Callback<SearchRideResponse>() {
                @Override
                public void onResponse(Call<SearchRideResponse> call, Response<SearchRideResponse> response) {
                    LocationApp.logs("Fetch Today's Rides : response " + response.code());
                    if (response.code() == 200) {
                        List<Ride> tripRecordList = response.body().getRideDTOList();
                        if (tripRecordList != null) {
                            int incompleteRidesCount = tripRecordList.stream().filter(ride -> ride.getRideEndTime() ==null).collect(Collectors.toList()).size();
                            if (incompleteRidesCount > 0) {
                                AppExecutors.getInstance().getMainThread().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(RecordRideActivity.this);
                                        alertDialogBuilder.setCancelable(true);
                                        alertDialogBuilder.setTitle("Alert");
                                        alertDialogBuilder.setMessage("Before starting new ride you need to finish your incomplete or running rides.");
                                        alertDialogBuilder.setPositiveButton("Goto Rides", (dialogInterface, i) -> {
                                            Intent intent = new Intent(getApplicationContext(), BottomNavigationActivity.class);
                                            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                            intent.putExtra("navigationId", R.id.navigation_ridedetails);
                                            startActivity(intent);
                                            finish();
                                        });
                                        alertDialogBuilder.setNegativeButton("CANCEL", (dialogInterface, i) -> {
                                            dialogInterface.dismiss();
                                            mStartUpdatesButton.setEnabled(true);
                                        });
                                        if (!isFinishing()) {
                                            alertDialogBuilder.show();
                                        }
                                    }
                                });

                            } else {
                                AppExecutors.getInstance().getMainThread().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        showRidePurposeDialog();
                                    }
                                });
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call<SearchRideResponse> call, Throwable t) {
                    Toast.makeText(RecordRideActivity.this, "Ride can not start at the moment. Please try after sometime.", Toast.LENGTH_LONG).show();
                    mStartUpdatesButton.setEnabled(true);
                }
            });
            //}
        });
    }

    boolean isRidePurposeOkButtonClicked = false;
    public void showRidePurposeDialog() {
        if(!RecordRideActivity.this.isFinishing()) {
            CustomDialog dialog = new CustomDialog(this, ridePurposeList);
            dialog.show(dialogInterface -> {
                if (dialog.isCancelled()) {
                    mStartUpdatesButton.setEnabled(true);
                    return;
                }
                LocationApp.logs("TRIP", "showRidePurposeDialog button clicked");
                if (isRidePurposeOkButtonClicked) {
                    LocationApp.logs("TRIP", "showRidePurposeDialog button already clicked");
                    return;
                }

                isRidePurposeOkButtonClicked = true;
                Date myDate = new Date();
                String startDate = TrackerUtility.getDateString(myDate);
                String startTime = TrackerUtility.getTimeString(myDate);
                String ridePurposeId = ridePurposeList.get(dialog.getSelectedPosition()).getId();
                Ride ride = new Ride();
                ride.setRideDate(startDate);
                ride.setEmployeeRide(LocationApp.getEmployeeProfile().getEmployeeCode());
                ride.setRideStartTime(startTime);
                ride.setRidePurpose(ridePurposeId);
                ride.setRideDistance(0d);

                Map<String, String> params = new HashMap<>();
                params.put("RideDate", ride.getRideDate());
                params.put("ridePurpose", ride.getRidePurpose());
                params.put("rideStartTime", ride.getRideStartTime());

                if (!TrackerUtility.checkConnection(getApplicationContext())) {
                    Toast.makeText(this, "Please check your network connection", Toast.LENGTH_LONG).show();
                    isRidePurposeOkButtonClicked = false;
                    mStartUpdatesButton.setEnabled(true);
                    return;
                } else {

                    Call<String> createRideCall = ApiHandler.getClient().createRide(LocationApp.getUserName(this), LocationApp.DEVICE_ID ,params);
                    createRideCall.enqueue(new Callback<String>() {

                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            if (response.code() == 201) {
                                ride.setId(Long.parseLong(response.body()));
                                startRide(ride, dialog.isUseGps());
                            } else {
                                String message = "Can not start ride at the moment. Please try after some time";
                                if (response.code() == 401) {
                                    message = "Can not start ride at the moment. Your account is blocked";
                                }
                                mStartUpdatesButton.setEnabled(true);
                                Toast.makeText(RecordRideActivity.this, message, Toast.LENGTH_LONG).show();

                            }
                            isRidePurposeOkButtonClicked = false;
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(RecordRideActivity.this, "Issue in starting Ride. Please try after sometime.", Toast.LENGTH_LONG).show();
                            mStartUpdatesButton.setEnabled(true);
                            isRidePurposeOkButtonClicked = false;
                        }
                    });
                }
            });
        }
    }
}
