package com.thoughtpearl.conveyance.worker;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
/*import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;*/

import com.thoughtpearl.conveyance.respository.databaseclient.DatabaseClient;
import com.thoughtpearl.conveyance.respository.dto.UnSyncRideDto;
import com.thoughtpearl.conveyance.respository.syncjob.RecordRideSyncJob;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class RideLocationSyncWorker /*extends Worker*/ {

    /*public RideLocationSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        RecordRideSyncJob recordRideSyncJob = new RecordRideSyncJob(getApplicationContext(), true);
        Timer timer = new Timer();
        timer.schedule(recordRideSyncJob, 0);
        long startTime = System.currentTimeMillis();
        while (!recordRideSyncJob.isCompleted()) {
            if (TimeUnit.MILLISECONDS.toMinutes(SystemClock.currentThreadTimeMillis() - startTime) > 2) {
                LocationApp.logs("TRIP", "Explicitly closing the sync job");
                recordRideSyncJob.setCompleted(true);
                timer.cancel();
                break;
            }
        }

       return recordRideSyncJob.isSuccessful() ? Result.success() : Result.failure();
    }*/
}
