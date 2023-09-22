package com.pyramid.conveyance.api.response;

import java.util.List;

public class RideDetailsResponse {
    Double startLatitude;
    Double startLongitude;
    Double endLatitude;
    Double endLongitude;
    Double reimbursementAmount;
    String totalTime;
    Double distanceTravelled;
    String rideDate;
    String rideTime;
    String purpose;
    List<LocationRequest> rideLocationDTOList;

    public RideDetailsResponse(){}

    public RideDetailsResponse(Double startLatitude, Double startLongitude, Double endLatitude, Double endLongitude, Double reimbursementAmount, String totalTime, Double distanceTravelled, String rideDate, String rideTime, String purpose, List<LocationRequest> rideLocationDTOList) {
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        this.reimbursementAmount = reimbursementAmount;
        this.totalTime = totalTime;
        this.distanceTravelled = distanceTravelled;
        this.rideDate = rideDate;
        this.rideTime = rideTime;
        this.purpose = purpose;
        this.rideLocationDTOList = rideLocationDTOList;
    }

    public Double getStartLatitude() {
        return startLatitude;
    }

    public void setStartLatitude(Double startLatitude) {
        this.startLatitude = startLatitude;
    }

    public Double getStartLongitude() {
        return startLongitude;
    }

    public void setStartLongitude(Double startLongitude) {
        this.startLongitude = startLongitude;
    }

    public Double getEndLatitude() {
        return endLatitude;
    }

    public void setEndLatitude(Double endLatitude) {
        this.endLatitude = endLatitude;
    }

    public Double getEndLongitude() {
        return endLongitude;
    }

    public void setEndLongitude(Double endLongitude) {
        this.endLongitude = endLongitude;
    }

    public Double getReimbursementAmount() {
        return reimbursementAmount;
    }

    public void setReimbursementAmount(Double reimbursementAmount) {
        this.reimbursementAmount = reimbursementAmount;
    }

    public String getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(String totalTime) {
        this.totalTime = totalTime;
    }

    public Double getDistanceTravelled() {
        return distanceTravelled;
    }

    public void setDistanceTravelled(Double distanceTravelled) {
        this.distanceTravelled = distanceTravelled;
    }

    public String getRideDate() {
        return rideDate;
    }

    public void setRideDate(String rideDate) {
        this.rideDate = rideDate;
    }

    public String getRideTime() {
        return rideTime;
    }

    public void setRideTime(String rideTime) {
        this.rideTime = rideTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public List<LocationRequest> getRideLocationDTOList() {
        return rideLocationDTOList;
    }

    public void setRideLocationDTOList(List<LocationRequest> rideLocationDTOList) {
        this.rideLocationDTOList = rideLocationDTOList;
    }
}
