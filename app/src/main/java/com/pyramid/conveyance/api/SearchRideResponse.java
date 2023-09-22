package com.pyramid.conveyance.api;

import com.pyramid.conveyance.api.response.Ride;

import java.util.List;

public class SearchRideResponse {
    String rideDuration;
    float distanceTravelled;
    float reimbursementAmount;
    List<Ride> rideDTOList;

    public SearchRideResponse(){
    }

    public SearchRideResponse(String rideDuration, float distanceTravelled, float reimbursementAmount, List<Ride> rideDTOList) {
        this.rideDuration = rideDuration;
        this.distanceTravelled = distanceTravelled;
        this.reimbursementAmount = reimbursementAmount;
        this.rideDTOList = rideDTOList;
    }

    public String getRideDuration() {
        return rideDuration;
    }

    public void setRideDuration(String rideDuration) {
        this.rideDuration = rideDuration;
    }

    public float getDistanceTravelled() {
        return distanceTravelled;
    }

    public void setDistanceTravelled(float distanceTravelled) {
        this.distanceTravelled = distanceTravelled;
    }

    public float getReimbursementAmount() {
        return reimbursementAmount;
    }

    public void setReimbursementAmount(float reimbursementAmount) {
        this.reimbursementAmount = reimbursementAmount;
    }

    public List<Ride> getRideDTOList() {
        return rideDTOList;
    }

    public void setRideDTOList(List<Ride> rideDTOList) {
        this.rideDTOList = rideDTOList;
    }
}
