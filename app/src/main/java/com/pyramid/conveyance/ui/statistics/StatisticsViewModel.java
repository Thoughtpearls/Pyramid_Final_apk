package com.pyramid.conveyance.ui.statistics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class StatisticsViewModel extends ViewModel {

    private final MutableLiveData<String> duration;
    private final MutableLiveData<String> distance;
    private final MutableLiveData<String> reimbursement;

    public StatisticsViewModel() {
        duration = new MutableLiveData<>();
        duration.setValue("0");
        distance = new MutableLiveData<>();
        distance.setValue("0 Km");
        reimbursement = new MutableLiveData<>();
        reimbursement.setValue("Rs 0");
    }

    public LiveData<String> getDurationText() {
        return duration;
    }
    public LiveData<String> getDistanceText() {
        return distance;
    }
    public LiveData<String> getReimbursementText() {
        return reimbursement;
    }
}