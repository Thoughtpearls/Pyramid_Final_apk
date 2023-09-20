package com.thoughtpearl.conveyance.worker;

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
