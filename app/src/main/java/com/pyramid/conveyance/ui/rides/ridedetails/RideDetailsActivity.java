package com.pyramid.conveyance.ui.rides.ridedetails;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pyramid.conveyance.LocationApp;
import com.pyramid.conveyance.R;
import com.pyramid.conveyance.api.ApiHandler;
import com.pyramid.conveyance.api.response.LocationRequest;
import com.pyramid.conveyance.api.response.RideDetailsResponse;
import com.pyramid.conveyance.respository.databaseclient.DatabaseClient;
import com.pyramid.conveyance.respository.entity.Location;
import com.pyramid.conveyance.respository.entity.TripRecord;
import com.pyramid.conveyance.respository.entity.TripRecordLocationRelation;
import com.pyramid.conveyance.respository.executers.AppExecutors;
import com.pyramid.conveyance.services.MyService;
import com.pyramid.conveyance.ui.statistics.StatisticsFragment;
import com.pyramid.conveyance.utility.TrackerUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView rideAmountTextView;
    private TextView rideDateTextView;
    private TextView rideDurationTextView;
    private TextView rideDistanceTextView;
    private TextView ridePurposeTextView;
    private Button completeRideButton;
    private GoogleMap mMap;
    private MapView mapView;
    private RideDetailsResponse rideDetailsResponse;
    private Long rideId;
    private boolean isInCompleteRide;
    private Boolean isFromStatisticScreen;
    private static final int REQUEST_CODE = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_details);

        rideId = getIntent().getExtras().getLong("rideId");
        isInCompleteRide = getIntent().getExtras().getBoolean("isInCompleteRide", false);
        isFromStatisticScreen = getIntent().getExtras().getBoolean("isFromStatisticScreen", false);
        rideAmountTextView = findViewById(R.id.rideAmount);
        rideAmountTextView.setText("");
        rideDateTextView = findViewById(R.id.rideDate);
        rideDateTextView.setText("");
        rideDurationTextView = findViewById(R.id.rideDuration);
        rideDurationTextView.setText("");
        rideDistanceTextView = findViewById(R.id.rideDistance);
        rideDistanceTextView.setText("");
        ridePurposeTextView = findViewById(R.id.ridePurpose);
        ridePurposeTextView.setText("");
        completeRideButton = findViewById(R.id.completeRide);
        completeRideButton.setVisibility(View.GONE);
        if (isInCompleteRide) {
            setListenerToUpdateRideBtn();
        }
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        mapView.setEnabled(true);
        initToolbar();
    }

    private void setListenerToUpdateRideBtn() {
        completeRideButton.setOnClickListener(view -> {
            if (mMap != null) {
                mMap.snapshot(bitmap -> {
                    File screenshot = TrackerUtility.takeScreen(getApplicationContext(), mapView, bitmap);
                    if (!TrackerUtility.checkConnection(getApplicationContext())) {
                        Toast.makeText(getApplicationContext(), "Please check your network connection", Toast.LENGTH_LONG).show();
                    } else {
                        updateInCompleteRide(screenshot.getAbsolutePath());
                    }
                });
            }
        });
    }

    private void fetchRideDetails() {
       Dialog dialog = LocationApp.showLoader(RideDetailsActivity.this);
        AppExecutors.getInstance().getDiskIO().execute(()-> {
           Call<RideDetailsResponse> rideCall = ApiHandler.getClient().getRideDetails(LocationApp.getUserName(this), LocationApp.DEVICE_ID, rideId);
           rideCall.enqueue(new Callback<RideDetailsResponse>() {
               @Override
               public void onResponse(Call<RideDetailsResponse> call, Response<RideDetailsResponse> response) {
                   if (response.code() == 200) {
                       rideDetailsResponse = response.body();
                       String sStartDate = rideDetailsResponse.getRideDate()+ " " + rideDetailsResponse.getRideTime();
                       Date startDate = TrackerUtility.convertStringToDate(sStartDate);

                       rideDateTextView.setText(TrackerUtility.getDateString(startDate, "dd MMM yyyy"));
                       if (rideDetailsResponse.getReimbursementAmount() != null) {
                           rideAmountTextView.setText("Rs " + rideDetailsResponse.getReimbursementAmount().toString());
                       } else {
                           rideAmountTextView.setText("Waiting");
                       }
                       ridePurposeTextView.setText(rideDetailsResponse.getPurpose());
                       if (rideDetailsResponse.getDistanceTravelled() != null) {
                           rideDistanceTextView.setText(TrackerUtility.roundOffDoubleToString(rideDetailsResponse.getDistanceTravelled()) + " Km");
                       } else {
                           rideDistanceTextView.setText("0 Km");
                       }
                       rideDurationTextView.setText(rideDetailsResponse.getTotalTime());
                       /*if (isListEmptyOrNull(rideDetailsResponse.getRideLocationDTOList())) {
                           completeRideButton.setVisibility(View.GONE);
                       } else {*/

                     Log.d("TRIP", "distance in Meter" + TrackerUtility.calculateDistanceInMeter(rideDetailsResponse.getRideLocationDTOList()));
                       if (isInCompleteRide) {
                           completeRideButton.setVisibility(View.VISIBLE);
                       } else {
                           AppExecutors.getInstance().getDiskIO().execute(()->{
                               int unSyncRideLocationCount = DatabaseClient.getInstance(RideDetailsActivity.this).getTripDatabase().tripRecordDao().getUnSyncServerLocations(rideId).size();
                               AppExecutors.getInstance().getMainThread().execute(()->{
                                    double distance = TrackerUtility.roundOffDouble(TrackerUtility.calculateDistanceInMeter(rideDetailsResponse.getRideLocationDTOList())/1000f);
                                   if (/*(!isListEmptyOrNull(rideDetailsResponse.getRideLocationDTOList()) && distance > rideDetailsResponse.getDistanceTravelled()) ||*/ unSyncRideLocationCount > 0) {
                                       completeRideButton.setText("Update Ride");
                                       completeRideButton.setVisibility(View.VISIBLE);
                                       setListenerToUpdateRideBtn();
                                   } else {
                                       completeRideButton.setVisibility(View.GONE);
                                   }
                               });
                           });
                       }
                       //}
                       reDrawTravelPathOnMap(rideDetailsResponse.getRideLocationDTOList());
                       zoomToSeeWholeTrack(rideDetailsResponse.getRideLocationDTOList());

                   } else {
                       Toast.makeText(RideDetailsActivity.this, "Failed to fetch ride details please try after sometime.", Toast.LENGTH_SHORT).show();
                   }

                   if (dialog != null && dialog.isShowing()) {
                       dialog.dismiss();
                   }
               }

               @Override
               public void onFailure(Call<RideDetailsResponse> call, Throwable t) {
                   Toast.makeText(RideDetailsActivity.this, "Failed to fetch ride details please try after sometime.", Toast.LENGTH_SHORT).show();
                   if (dialog != null && dialog.isShowing()) {
                       dialog.dismiss();
                   }
               }
           });
        });
    }

    @SuppressLint("RestrictedApi")
    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            this.finishActivity(RESULT_OK);
        });
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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        if (rideDetailsResponse != null) {
            reDrawTravelPathOnMap(rideDetailsResponse.getRideLocationDTOList());
            zoomToSeeWholeTrack(rideDetailsResponse.getRideLocationDTOList());
        }
    }

    private void zoomToSeeWholeTrack(List<LocationRequest> locationList) {
        if (isListEmptyOrNull(locationList)) {
            return;
        }
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (LocationRequest locationRequest : locationList) {
            builder.include(new LatLng(locationRequest.getLatitude(), locationRequest.getLongitude()));
        }
        int widthPixels = getResources().getDisplayMetrics().widthPixels;
        int heightPixels = (int) (getResources().getDisplayMetrics().heightPixels * 0.70f);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), widthPixels, heightPixels, (int) (heightPixels * 0.05f)));
    }

    public void reDrawTravelPathOnMap(List<LocationRequest> locationList) {
        if (isListEmptyOrNull(locationList)) {
            return;
        }
        PolylineOptions polylineOptions = new PolylineOptions();
        LatLng dest = null;
        if (locationList != null && locationList.size() > 0) {
            LatLng start = new LatLng(locationList.get(0).getLatitude(), locationList.get(0).getLongitude());
            for (LocationRequest locationRequest : locationList) {
                dest = new LatLng(locationRequest.getLatitude(), locationRequest.getLongitude());
                polylineOptions.add(dest);
            }

            MarkerOptions startMarkerOptions = new MarkerOptions();
            startMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            startMarkerOptions.title("Start point");
            startMarkerOptions.position(start);
            mMap.addMarker(startMarkerOptions);

            MarkerOptions endMarkerOptions = new MarkerOptions();
            endMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            endMarkerOptions.title("End point");
            endMarkerOptions.position(dest);
            mMap.addMarker(endMarkerOptions);
        }

        polylineOptions.color(LocationApp.POLYLINE_COLOR);
        polylineOptions.width(LocationApp.POLYLINE_WIDTH);
        polylineOptions.geodesic(true);
        mMap.addPolyline(polylineOptions);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        if (dest != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dest, LocationApp.ZOOM_LEVEL));
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!TrackerUtility.checkConnection(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "Please check your network connection", Toast.LENGTH_LONG).show();
        } else {
            fetchRideDetails();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }

//        if (!checkPermissions()) {
//            requestPermissions();
//        } else {
//            //fetchRideDetails();
//        }
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

    public void updateInCompleteRide(String imagePath) {
        Dialog dialog = LocationApp.showLoader(this);
        AppExecutors.getInstance().getDiskIO().execute(()->{

            List<Location> unSyncLocations = DatabaseClient.getInstance(RideDetailsActivity.this).getTripDatabase().tripRecordDao().getUnSyncServerLocations(rideId);

            if (unSyncLocations.size() > 0) {
                AtomicReference<List<Location>> unSyncList = new AtomicReference<>(new ArrayList<>());

                AppExecutors.getInstance().getDiskIO().execute(() -> {
                    unSyncList.set(unSyncLocations);
                    if (unSyncList.get().size() > 0) {
                        updateLocationsOnServer(unSyncList.get(), imagePath, dialog);
                        LocationApp.logs("TRIP", "UPDATING RECORDS..");
                    }
                });
            } else {
                updateRide(imagePath, dialog, 0d,"");
            }
        });
    }

    private void updateRide(String imagePath, Dialog dialog, double updatedDistanceInKm, String  updateTime) {
        TripRecord tripRecord = new TripRecord();
        tripRecord.setTripId(rideId);
        if (rideDetailsResponse != null) {

            File file = new File(imagePath);
            LocationApp.logs("TRIP", "image path :" + imagePath + "file exists :" + file.exists());
            double totalDistance = 0;
            double distanceInKm = 0;
            String sDate = rideDetailsResponse.getRideDate();
            String endTime;
            if (!isListEmptyOrNull(rideDetailsResponse.getRideLocationDTOList())) {
                totalDistance = TrackerUtility.calculateDistanceInMeter(rideDetailsResponse.getRideLocationDTOList());
                double tempInKm = (totalDistance / 1000f);
                distanceInKm = tempInKm > updatedDistanceInKm ? tempInKm : updatedDistanceInKm;
                distanceInKm = TrackerUtility.roundOffDouble(distanceInKm);
                LocationRequest locationRequest = rideDetailsResponse.getRideLocationDTOList().get(rideDetailsResponse.getRideLocationDTOList().size() - 1);
                sDate = sDate + " " + locationRequest.getTimeStamp();
                endTime = locationRequest.getTimeStamp();
            } else {
                sDate = sDate + " 00:00:00";
                endTime = rideDetailsResponse.getRideTime();
            }
            Date date = TrackerUtility.convertStringToDate(sDate , "yyyy-MM-dd HH:mm:ss");
            if (updateTime != null && updateTime.trim().length() > 0) {
                Date updateDate = TrackerUtility.convertStringToDate(rideDetailsResponse.getRideDate() + " " + updateTime);
                date = date.getTime() > updateDate.getTime() ? date : updateDate;
            }
            tripRecord.setTotalDistance(distanceInKm);
            tripRecord.setEndTimestamp(date.getTime());
            tripRecord.setStatus(true);

            RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
            RequestBody id = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(rideId));
            //RequestBody ridePurpose = RequestBody.create(MediaType.parse("text/plain"), rideDetailsResponse.getPurpose());
//            RequestBody rideStartTime = RequestBody.create(MediaType.parse("text/plain"), rideDetailsResponse.getRideTime());
            RequestBody rideEndTime = RequestBody.create(MediaType.parse("text/plain"), endTime);
            RequestBody rideDate = RequestBody.create(MediaType.parse("text/plain"), rideDetailsResponse.getRideDate());
            RequestBody rideDistance = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(distanceInKm));

            Map<String, RequestBody> bodyMap = new HashMap<>();
            bodyMap.put("file\"; filename=\"pp.png\" ", fileBody);
            bodyMap.put("id", id);
            bodyMap.put("rideDate", rideDate);
            //bodyMap.put("rideStartTime", rideStartTime);
            bodyMap.put("rideEndTime", rideEndTime);
            bodyMap.put("rideDistance", rideDistance);
            //bodyMap.put("ridePurpose", ridePurpose);

            Call<Void> updateRideCall = ApiHandler.getClient().updateRide(LocationApp.getUserName(this), LocationApp.DEVICE_ID, rideId, bodyMap);
            updateRideCall.enqueue(new Callback<Void>(){

                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    LocationApp.logs("TRIP", "Ride completed :");
                    if (response.code() == 200 || response.code() == 201) {
                        //tripRecord.setStatus(true);
                        StatisticsFragment.isRideListRefreshRequired = isFromStatisticScreen;
                        AppExecutors.getInstance().getDiskIO().execute(() ->
                            DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().tripRecordDao().updateRecord(tripRecord)
                        );
                        isInCompleteRide = false;
                        completeRideButton.setVisibility(View.GONE);
                        if (MyService.isTrackingOn !=null && Boolean.TRUE.equals(MyService.isTrackingOn.getValue())) {
                           if  (MyService.runningTripRecord != null) {
                                TripRecord tripRecord1 = MyService.runningTripRecord.getValue();
                                if (rideId.equals(tripRecord1.getTripId())) {
                                        MyService.isTrackingOn.setValue(false);
                                        tripRecord1.setStatus(true);
                                        MyService.runningTripRecord.setValue(tripRecord1);
                                        Intent intent = new Intent(RideDetailsActivity.this, MyService.class);
                                        intent.setAction(MyService.STOP_SERVICE);
                                        intent.putExtra("screenshot_path", imagePath);
                                        startService(intent);
                                        NotificationManagerCompat.from(RideDetailsActivity.this).cancel(LocationApp.NOTIFICATION_ID);
                                }
                           }
                        }

                        Toast.makeText(getApplicationContext(), "Ride Updated Successfully", Toast.LENGTH_LONG).show();
                        fetchRideDetails();
                    } else {
                        Toast.makeText(RideDetailsActivity.this, "Something went wrong while updating ride. Please try after some time", Toast.LENGTH_LONG).show();
                    }

                    AppExecutors.getInstance().getMainThread().execute(()->{
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    });
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    LocationApp.logs("TRIP", "Ride not completed :" + t);
                    Toast.makeText(RideDetailsActivity.this, "D" +
                            "Something went wrong while updating ride. Please try after some time", Toast.LENGTH_LONG).show();
                    AppExecutors.getInstance().getMainThread().execute(()->{
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    });
                }
            });
        }
    }

    public boolean isListEmptyOrNull(List locations) {
        return locations == null || locations.size() == 0;
    }

    /**
     * Return the current state of the permissions needed.
     */
//    private boolean checkPermissions() {
//        int permissionState = ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.CAMERA);
//        int permissionStateWriteFile = ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        int permissionStateReadImageFile = ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.READ_MEDIA_IMAGES);
//        int permissionStateReadExternalStorageFile = ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.READ_EXTERNAL_STORAGE);
//        int permissionStateFineLocation = ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION);
//        int permissionStateCourseLocation = ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_COARSE_LOCATION);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            return ((permissionState == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateReadImageFile == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateReadExternalStorageFile == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateFineLocation == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateCourseLocation == PackageManager.PERMISSION_GRANTED));
//
//        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
//            return ((permissionState == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateWriteFile == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateReadExternalStorageFile == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateFineLocation == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateCourseLocation == PackageManager.PERMISSION_GRANTED));
//        } else {
//            return ((permissionState == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateFineLocation == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateReadExternalStorageFile == PackageManager.PERMISSION_GRANTED) &&
//                    (permissionStateCourseLocation == PackageManager.PERMISSION_GRANTED));
//        }
//    }

//    private void requestPermissions() {
//        boolean shouldProvideRationale = false;
//        if(!isFinishing()) {
//            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                shouldProvideRationale =
//                        ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.READ_MEDIA_IMAGES)  && ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.READ_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.ACCESS_COARSE_LOCATION);
//            } else if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
//                shouldProvideRationale =
//                        ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.WRITE_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.ACCESS_COARSE_LOCATION);
//            } else {
//                shouldProvideRationale =
//                        ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                Manifest.permission.ACCESS_COARSE_LOCATION);
//            }
//
//            if (shouldProvideRationale) {
//                Log.i("TRIP", "Displaying permission rationale to provide additional context.");
//
//                String[] permissions;
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                    permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE,  Manifest.permission.READ_MEDIA_IMAGES};
//                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
//                } else {
//                    permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
//                }
//                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
//            } else {
//                Log.i("TRIP", "Requesting permission");
//                String[] permissions;
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                    permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE,  Manifest.permission.READ_MEDIA_IMAGES};
//                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
//                } else {
//                    permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
//                }
//                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
//            }
//        }
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        Log.i("TRIP", "onRequestPermissionResult");
//        if (requestCode == REQUEST_CODE) {
//            if (grantResults.length <= 0) {
//                Log.i("TRIP", "User interaction was cancelled.");
//            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
////                fetchRideDetails();
//            } else {
//                showSnackbar(R.string.permission_denied_explanation,
//                        R.string.settings, view -> {
//                            // Build intent that displays the App settings screen.
//                            Intent intent = new Intent();
//                            intent.setAction(
//                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                            Uri uri = Uri.fromParts("package",
//                                    "com.pyramid.conveyance", null);
//                            intent.setData(uri);
//                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
//                        });
//            }
//        }
//    }

//    /**
//     * Shows a {@link Snackbar}.
//     *
//     * @param mainTextStringId The id for the string resource for the Snackbar text.
//     * @param actionStringId   The text of the action item.
//     * @param listener         The listener associated with the Snackbar action.
//     */
//    private void showSnackbar(final int mainTextStringId, final int actionStringId,
//                              View.OnClickListener listener) {
//        Snackbar.make(findViewById(android.R.id.content),
//                        getString(mainTextStringId),
//                        Snackbar.LENGTH_INDEFINITE)
//                .setAction(getString(actionStringId), listener).show();
//    }

    public void updateLocationsOnServer(List<Location> unSyncedLocations, String imagePath, Dialog dialog) {
        updateLocationsOnServer(unSyncedLocations, 0, imagePath, dialog);
    }
    public void updateLocationsOnServer(List<Location> unSyncedLocations, int retryAttemptCount, String imagePath, Dialog dialog) {
        if (!TrackerUtility.checkConnection(RideDetailsActivity.this)) {
            showToastMessage("Please check your network connection");
        } else {
            ArrayList<LocationRequest> locationRequests = new ArrayList<>();
            unSyncedLocations.forEach(location -> {
                LocationRequest request = new LocationRequest("", location.getLatitude(), location.getLongitude(), location.getTripId().toString(), location.getTimestamp());
                locationRequests.add(request);
            });

            Call<List<String>> createLocationCall = ApiHandler.getClient().createLocation(LocationApp.getUserName(RideDetailsActivity.this), LocationApp.DEVICE_ID, locationRequests);
            createLocationCall.enqueue(new Callback<List<String>>() {
                @Override
                public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                    if (response.code() == 201) {
                        AppExecutors.getInstance().getDiskIO().execute(() -> {
                            unSyncedLocations.forEach(location -> {
                                location.serverSync = true;
                                DatabaseClient.getInstance(RideDetailsActivity.this).getTripDatabase().tripRecordDao().update(location);
                            });

                            double totalDistance = 0d;
                            String updatedTime = "";
                            if (unSyncedLocations.size() > 0) {
                                TripRecordLocationRelation recordLocationRelation = DatabaseClient.getInstance(RideDetailsActivity.this).getTripDatabase().tripRecordDao().getByTripId(rideId);
                                if (recordLocationRelation.getLocations() != null && recordLocationRelation.getLocations().size() > 0 ) {
                                    totalDistance = TrackerUtility.calculateDistanceInKilometer(recordLocationRelation.getLocations());
                                }
                                updatedTime = recordLocationRelation.getLocations().get(recordLocationRelation.getLocations().size() - 1).getTimestamp();
                            }

                            updateRide(imagePath, dialog, totalDistance, updatedTime);
                        });
                    } else {
                        if (retryAttemptCount < 1) {
                            updateLocationsOnServer(unSyncedLocations, 1, imagePath, dialog);
                        }
                        showToastMessage("Ride data sync failed :" + response.errorBody());
                    }
                }

                @Override
                public void onFailure(Call<List<String>> call, Throwable t) {
                    if (retryAttemptCount < 1) {
                        updateLocationsOnServer(unSyncedLocations, 1, imagePath, dialog);
                    }
                    showToastMessage("Ride data sync failed.");
                }
            });
        }
    }

    public void showToastMessage(String message) {
        if (!LocationApp.isAppInBackground()) {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast toast = Toast.makeText(RideDetailsActivity.this, message, Toast.LENGTH_LONG);
                toast.show();
            });
        }
    }

    @Override
    protected void onDestroy() {
        // Release references to UI elements
        rideAmountTextView = null;
        rideDateTextView = null;
        rideDurationTextView = null;
        rideDistanceTextView = null;
        ridePurposeTextView = null;
        completeRideButton = null;

        // Release the Google Map and MapView resources
        if (mapView != null) {
            mapView.onDestroy();
        }

        // Remove the mapView from its parent to prevent potential leaks
        if (mapView != null && mapView.getParent() != null) {
            ((ViewGroup) mapView.getParent()).removeView(mapView);
        }

        // Release any other references or resources specific to your activity
        rideDetailsResponse = null;
        rideId = null;

        super.onDestroy();
    }

}