package com.pyramid.conveyance.respository.syncjob;

public interface RecordRideSyncCallback<T> {
     void onComplete(Result<T>  result);
}
