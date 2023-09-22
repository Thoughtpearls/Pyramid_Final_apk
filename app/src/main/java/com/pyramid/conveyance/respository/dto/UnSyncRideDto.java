package com.pyramid.conveyance.respository.dto;

public class UnSyncRideDto {
    Long tripId;
    public UnSyncRideDto(Long tripId) {
        this.tripId = tripId;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }
}
