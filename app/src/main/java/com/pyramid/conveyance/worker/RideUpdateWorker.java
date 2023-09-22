package com.pyramid.conveyance.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
/*import androidx.work.Worker;
import androidx.work.WorkerParameters;*/

import com.pyramid.conveyance.LocationApp;
import com.pyramid.conveyance.api.ApiHandler;
import com.pyramid.conveyance.respository.databaseclient.DatabaseClient;
import com.pyramid.conveyance.respository.entity.TripRecord;
import com.pyramid.conveyance.respository.executers.AppExecutors;
import com.pyramid.conveyance.utility.TrackerUtility;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideUpdateWorker /*extends Worker */{
    /*TripRecord tripRecord;
    String imagePath;
    Double totalDistance;
    String rideId;
    String sRidePurpose;
    String sRideStartTime;
    String sRideDate;
    public RideUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams, TripRecord tripRecord) {
        super(context, workerParams);
        this.tripRecord = tripRecord;
        rideId = workerParams.getInputData().getString("rideId");
        sRidePurpose = workerParams.getInputData().getString("ridePurpose");
        sRideStartTime = workerParams.getInputData().getString("rideStartTime");
        sRideDate = workerParams.getInputData().getString("rideDate");
        imagePath = workerParams.getInputData().getString("imagePath");
        totalDistance = workerParams.getInputData().getDouble("totalDistance", 0d);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppExecutors.getInstance().getDiskIO().execute(()-> {
            if (tripRecord != null) {
                long endTime = System.currentTimeMillis();
                tripRecord.setEndTimestamp(endTime);
                tripRecord.setTotalDistance(totalDistance);//
                tripRecord.setStatus(false);
                DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().tripRecordDao().updateRecord(tripRecord);
                File file = new File(imagePath);
                LocationApp.logs("TRIP", "image path :" + imagePath + "file exists :" + file.exists());

                RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
                RequestBody id = RequestBody.create(MediaType.parse("text/plain"), rideId);
                RequestBody ridePurpose = RequestBody.create(MediaType.parse("text/plain"), sRidePurpose);
                RequestBody rideStartTime = RequestBody.create(MediaType.parse("text/plain"), sRideStartTime);
                RequestBody rideEndTime = RequestBody.create(MediaType.parse("text/plain"), TrackerUtility.getTimeString(new Date(endTime)));
                RequestBody rideDate = RequestBody.create(MediaType.parse("text/plain"), sRideDate);
                RequestBody rideDistance = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(tripRecord.getTotalDistance()));

                Map<String, RequestBody> bodyMap = new HashMap<>();
                bodyMap.put("file\"; filename=\"pp.png\" ", fileBody);
                bodyMap.put("id", id);
                bodyMap.put("rideDate", rideDate);
                bodyMap.put("rideStartTime", rideStartTime);
                bodyMap.put("rideEndTime", rideEndTime);
                bodyMap.put("rideDistance", rideDistance);
                bodyMap.put("ridePurpose", ridePurpose);

                Call<Void> updateRideCall = ApiHandler.getClient().updateRide(LocationApp.getUserName(getApplicationContext()), LocationApp.DEVICE_ID, rideId, bodyMap);
                updateRideCall.enqueue(new Callback<Void>(){

                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        LocationApp.logs("TRIP", "Ride completed :");
                        if (response.code() == 200 || response.code() == 201) {
                            tripRecord.setStatus(true);
                            AppExecutors.getInstance().getDiskIO().execute(() -> {
                                DatabaseClient.getInstance(getApplicationContext()).getTripDatabase().tripRecordDao().updateRecord(tripRecord);
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        LocationApp.logs("TRIP", "Ride not completed :" + t);
                    }
                });
            }
        });
        return null;
    } */
}
