package com.thoughtpearl.conveyance.ui.home;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.thoughtpearl.conveyance.LocationApp;
import com.thoughtpearl.conveyance.R;
import com.thoughtpearl.conveyance.api.ApiHandler;
import com.thoughtpearl.conveyance.databinding.FragmentHomeBinding;
import com.thoughtpearl.conveyance.respository.databaseclient.DatabaseClient;
import com.thoughtpearl.conveyance.respository.dto.UnSyncRideDto;
import com.thoughtpearl.conveyance.respository.entity.TripRecord;
import com.thoughtpearl.conveyance.respository.entity.TripRecordLocationRelation;
import com.thoughtpearl.conveyance.respository.executers.AppExecutors;
import com.thoughtpearl.conveyance.respository.syncjob.RecordRideSyncJob;
import com.thoughtpearl.conveyance.respository.syncjob.Result;
import com.thoughtpearl.conveyance.ui.recordride.RecordRideActivity;
import com.thoughtpearl.conveyance.utility.TrackerUtility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private boolean isClockedIn;
    private boolean isClockedOut;
    private Timer mTimer;
    Activity mActivity;
    boolean isSyncJobRunning = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(LocationApp.APP_NAME, Context.MODE_PRIVATE);
        AtomicReference<String> checkInDate = new AtomicReference<>(sharedPreferences.getString(LocationApp.CLOCK_IN, ""));
        AtomicReference<String> checkOutDate = new AtomicReference<>(sharedPreferences.getString(LocationApp.CLOCK_OUT, ""));
        AtomicReference<Boolean> isRideDisabled = new AtomicReference<>(sharedPreferences.getBoolean("rideDisabled", false));
        if (checkInDate.get().trim().length() > 0 &&  checkInDate.get().equalsIgnoreCase(TrackerUtility.getDateString(new Date()))) {
            isClockedIn = true;
        } else {
            isClockedIn = false;
        }
        if (checkOutDate.get().equalsIgnoreCase(TrackerUtility.getDateString(new Date()))) {
            isClockedOut = true;
        } else {
            isClockedOut = false;
        }

        View riderRecord = binding.recordRideBtn;
        riderRecord.setVisibility(isRideDisabled.get() ? View.GONE : View.VISIBLE);
        binding.statisticsBtn.setVisibility(isRideDisabled.get() ? View.GONE : View.VISIBLE);
        if (!(isClockedIn && isClockedOut)) {
            riderRecord.setBackgroundColor(Color.WHITE);
            riderRecord.setOnClickListener(view -> {
                if (TrackerUtility.isDeveloperModeEnabled(mActivity)) {
                    Toast.makeText(mActivity, "Please turn off developer option from settings without that record ride will not work.", Toast.LENGTH_LONG).show();
                } else if (!TrackerUtility.checkConnection(mActivity)) {
                    Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(getContext(), RecordRideActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            riderRecord.setBackgroundColor(Color.GRAY);
            riderRecord.setOnClickListener(view -> {
                Toast.makeText(mActivity, "You can not record rides once you are checkout for the day.", Toast.LENGTH_LONG).show();
            });
        }

        binding.statisticsBtn.setOnClickListener(view -> {
            if (!TrackerUtility.checkConnection(mActivity)) {
                Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
            } else {
                NavController navController = Navigation.findNavController(mActivity, R.id.nav_host_fragment_activity_bottom_navigation);
                navController.navigate(R.id.navigation_statistics);
            }
        });

        binding.attendanceBtn.setOnClickListener(view -> {
            if (!TrackerUtility.checkConnection(mActivity)) {
                Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
            } else {
                NavController navController = Navigation.findNavController(mActivity, R.id.nav_host_fragment_activity_bottom_navigation);
                navController.navigate(R.id.navigation_attendance);
            }
        });

        binding.calendarView.setFirstDayOfWeek(Calendar.MONDAY);

        if (!isRideDisabled.get()) {
            binding.calendarView.setOnDateChangeListener((calendarView, year, month, day) -> {
                String date = year + "-" + (month < 9 ? "0" + (month + 1) : (month + 1)) + "-" + day;
                showLogoutAlertDialog("Do you want to check ride details of selected date (" + date + ")", date);

                //Toast.makeText(getContext(), "i :" + year + " i1 :" +  month + " i2 :" + day, Toast.LENGTH_LONG).show();
            });
        }

        AppExecutors.getInstance().getDiskIO().execute(()->{
           DatabaseClient.getInstance(mActivity).getTripDatabase().tripRecordDao().deleteAllSyncedRides();
           int counts = DatabaseClient.getInstance(mActivity).getTripDatabase().tripRecordDao().getUnSyncRidesCount();
           mActivity.runOnUiThread(()-> {
               syncJob(counts);
           });
        });

        if (TrackerUtility.isDeveloperModeEnabled(mActivity)) {
            Toast.makeText(mActivity, "Please turn off developer options from settings without that record rides will not work", Toast.LENGTH_LONG).show();
        }

        return root;
    }

    private void syncJob(int counts) {
        String message = "Great, Your rides already synced on server";
        if (counts == 1) {
            message = "You have " + counts + " ride to sync on server";
        } else if (counts > 1) {
            message = "You have " + counts + " rides to sync on server";
        }

        TextView unSyncRideMessage  = mActivity.findViewById(R.id.unSyncRideMessage);
        if (unSyncRideMessage != null) {
            unSyncRideMessage.setText(message);
        }

        MaterialButton unSyncBtn = mActivity.findViewById(R.id.unSyncRideBtn);
        if (unSyncBtn != null) {
            unSyncBtn.setVisibility(counts > 0 ? View.VISIBLE : View.GONE);
            unSyncBtn.setOnClickListener(view -> {
                if (!isSyncJobRunning) {
                    if (!TrackerUtility.checkConnection(mActivity)) {
                        Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
                        return;
                    }
                    AtomicReference<List<UnSyncRideDto>> unSyncRideList = new AtomicReference<>();
                    unSyncRideList.set(new ArrayList<>());
                    LocationApp.showLoader(mActivity);
                    AppExecutors.getInstance().getNetworkIO().execute(()-> {
                        unSyncRideList.set(DatabaseClient.getInstance(mActivity).getTripDatabase().tripRecordDao().getUnSyncRidesList());
                        RecordRideSyncJob recordRideSyncJob = new RecordRideSyncJob(mActivity, false,false , result-> {
                             if (result instanceof Result.Success) {

                                 if (((Result.Success<Boolean>) result).data.booleanValue()) {

                                     updateRideonServer(unSyncRideList);

                                 } else {
                                     Toast.makeText(mActivity, "Error occur while syncing ride to server", Toast.LENGTH_LONG).show();
                                 }

                             } else {
                                 Toast.makeText(mActivity, "Sorry, Due to some server issue ride syncing is not working. Please try after sometime.", Toast.LENGTH_LONG).show();
                             }
                        });
                        mTimer = new Timer();
                        mTimer.schedule(recordRideSyncJob, 0);   //Schedule task
                        /*long startTime = SystemClock.currentThreadTimeMillis();
                        while (!recordRideSyncJob.isCompleted()) {
                            if (TimeUnit.MILLISECONDS.toMinutes(SystemClock.currentThreadTimeMillis() - startTime) > 2) {
                                LocationApp.logs("TRIP", "Explicitly closing the sync job");
                                recordRideSyncJob.setCompleted(true);
                                mTimer.cancel();
                                isSyncJobRunning = false;
                                break;
                            }
                        }*/

                        if (!recordRideSyncJob.isSuccessful()) {
                            LocationApp.logs("TRIP", "Sync job is not successful.");
                            return;
                        }

                        /*AppExecutors.getInstance().getDiskIO().execute(()-> {
                            unSyncRideList.get().forEach(unSyncRideDto -> {
                                AppExecutors.getInstance().getNetworkIO().execute(() -> {
                                    try {
                                        TripRecordLocationRelation relation = DatabaseClient.getInstance(mActivity).getTripDatabase().tripRecordDao().getByTripId(UUID.fromString(unSyncRideDto.getTripId()));
                                        double totalDistanceInKm = 0d;
                                        if (relation.getLocations() != null && relation.getLocations().size() > 0) {
                                            totalDistanceInKm = TrackerUtility.calculateDistanceInKilometer(relation.getLocations());
                                            totalDistanceInKm = TrackerUtility.roundOffDouble(totalDistanceInKm);
                                            Date rideDateTime = new Date(relation.getTripRecord().getStartTimestamp());
                                            String sRideDate = TrackerUtility.getDateString(rideDateTime);
                                            String sRideStartTime = TrackerUtility.getTimeString(rideDateTime);
                                            Date rideEndDateTime = TrackerUtility.convertStringToDate(sRideDate + " " + relation.getLocations().get(relation.getLocations().size() -1 ).getTimestamp(), "yyyy-MM-dd HH:mm:ss");
                                            String sRideEndTime =  TrackerUtility.getTimeString(rideEndDateTime);
                                            updateRide(totalDistanceInKm, sRideDate, sRideStartTime, sRideEndTime, relation.getTripRecord());
                                        }
                                    } catch (Exception exception) {
                                        LocationApp.logs("TRIP", "Error while updating ride :" + exception.getMessage());
                                    }
                                });
                            });
                        });*/
                    });
                } else {
                    Toast.makeText(mActivity,"Sync job is in progress..", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void updateRideonServer(AtomicReference<List<UnSyncRideDto>> unSyncRideList) {
        AppExecutors.getInstance().getDiskIO().execute(()-> {
            unSyncRideList.get().forEach(unSyncRideDto -> {
                AppExecutors.getInstance().getNetworkIO().execute(() -> {
                    try {
                        TripRecordLocationRelation relation = DatabaseClient.getInstance(mActivity).getTripDatabase().tripRecordDao().getByTripId(unSyncRideDto.getTripId());
                        double totalDistanceInKm = 0d;
                        if (relation.getLocations() != null && relation.getLocations().size() > 0) {
                            totalDistanceInKm = TrackerUtility.calculateDistanceInKilometer(relation.getLocations());
                            totalDistanceInKm = TrackerUtility.roundOffDouble(totalDistanceInKm);
                            Date rideDateTime = new Date(relation.getTripRecord().getStartTimestamp());
                            String sRideDate = TrackerUtility.getDateString(rideDateTime);
                            String sRideStartTime = TrackerUtility.getTimeString(rideDateTime);
                            Date rideEndDateTime = TrackerUtility.convertStringToDate(sRideDate + " " + relation.getLocations().get(relation.getLocations().size() -1 ).getTimestamp(), "yyyy-MM-dd HH:mm:ss");
                            String sRideEndTime =  TrackerUtility.getTimeString(rideEndDateTime);
                            updateRide(totalDistanceInKm, sRideDate, sRideStartTime, sRideEndTime, relation.getTripRecord());
                        }
                    } catch (Exception exception) {
                        LocationApp.logs("TRIP", "Error while updating ride :" + exception.getMessage());
                    }
                });
            });
        });
    }

    private void reCheckUnSyncRides() {
        int counts1 = DatabaseClient.getInstance(mActivity).getTripDatabase().tripRecordDao().getUnSyncRidesCount();
        mActivity.runOnUiThread(()-> {
            if (counts1 > 0) {
                Toast.makeText(mActivity, "Sorry something went wrong while Syncing. Please try after sometime.", Toast.LENGTH_LONG).show();
                String message1 = "Great, your ride sync is up to date";
                if (counts1 == 1) {
                    message1 = "You have " + counts1 + " ride to sync on server";
                } else if (counts1 > 1) {
                    message1 = "You have " + counts1 + " rides to sync on server";
                }
                ((TextView)mActivity.findViewById(R.id.unSyncRideMessage)).setText(message1);
            } else {
                String message1 = "Great, your ride sync is up to date";
                ((TextView)mActivity.findViewById(R.id.unSyncRideMessage)).setText(message1);
                MaterialButton unSyncRideBtn = ((MaterialButton)mActivity.findViewById(R.id.unSyncRideBtn));
                if (unSyncRideBtn != null) {
                    unSyncRideBtn.setVisibility(View.GONE);
                }
            }
            LocationApp.dismissLoader();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private void showLogoutAlertDialog(String message, String date) {
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(mActivity);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setTitle("Alert");
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
            Bundle bundle = new Bundle();
            bundle.putString("selectedDate", date);
            NavController navController = Navigation.findNavController(mActivity, R.id.nav_host_fragment_activity_bottom_navigation);
            navController.navigate(R.id.navigation_statistics, bundle);
        });
        alertDialogBuilder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
        alertDialogBuilder.show();
    }

    private void updateRide(double updatedDistanceInKm, String sRideDate, String sRideStartTime, String  sRideEndTime, TripRecord tripRecord) {
        if (true) {
            double distanceInKm = updatedDistanceInKm;
            String sRideStartDate = TrackerUtility.getDateString(new Date(tripRecord.getStartTimestamp()));
            String endTime = sRideEndTime;
            Date endRideDateTime = TrackerUtility.convertStringToDate(sRideStartDate +" " + sRideEndTime, "yyyy-MM-dd HH:mm:ss");
            tripRecord.setTotalDistance(distanceInKm);
            tripRecord.setEndTimestamp(endRideDateTime.getTime());
            tripRecord.setStatus(true);
            String startTime  = sRideStartTime;
            String startDate  = sRideDate;

            RequestBody id = RequestBody.create(MediaType.parse("text/plain"), tripRecord.getTripId().toString());
            RequestBody fileProvided = RequestBody.create(MediaType.parse("text/plain"), "false");
            //RequestBody rideStartTime = RequestBody.create(MediaType.parse("text/plain"), startTime);
            RequestBody rideEndTime = RequestBody.create(MediaType.parse("text/plain"), endTime);
            RequestBody rideDate = RequestBody.create(MediaType.parse("text/plain"), startDate);
            RequestBody rideDistance = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(distanceInKm));

            Map<String, RequestBody> bodyMap = new HashMap<>();
            bodyMap.put("id", id);
            bodyMap.put("rideDate", rideDate);
            //bodyMap.put("rideStartTime", rideStartTime);
            bodyMap.put("rideEndTime", rideEndTime);
            bodyMap.put("rideDistance", rideDistance);
            bodyMap.put("FileProvided", fileProvided);

            Call<Void> updateRideCall = ApiHandler.getClient().updateRide(LocationApp.getUserName(mActivity), LocationApp.DEVICE_ID, tripRecord.getTripId(), bodyMap);
            updateRideCall.enqueue(new Callback<Void>(){

                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    LocationApp.logs("TRIP", "Ride completed :");
                    if (response.code() == 200 || response.code() == 201) {
                        AppExecutors.getInstance().getDiskIO().execute(() -> {
                            DatabaseClient.getInstance(mActivity).getTripDatabase().tripRecordDao().updateRecord(tripRecord);
                        });
                    } else {
                        LocationApp.logs("TRIP", "Something went wrong while updating ride. Please try after some time");
                        //Toast.makeText(mActivity, "Something went wrong while updating ride. Please try after some time", Toast.LENGTH_LONG).show();
                    }

                    AppExecutors.getInstance().getDiskIO().execute(()-> {
                        reCheckUnSyncRides();
                    });

                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    LocationApp.logs("TRIP", "HomeFragment sync job : onFailure :" + t);
                    AppExecutors.getInstance().getDiskIO().execute(()-> {
                        reCheckUnSyncRides();
                    });
                }
            });
        }
    }
}

