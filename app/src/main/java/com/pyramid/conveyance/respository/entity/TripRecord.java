package com.pyramid.conveyance.respository.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class TripRecord {
    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    public Long tripId;

    @ColumnInfo(name = "start_time")
    public long startTimestamp;

    @ColumnInfo(name = "end_time")
    public long endTimestamp;

    @ColumnInfo(name = "total_distance")
    public double totalDistance;

    @ColumnInfo(name = "status")
    public boolean status;

    @ColumnInfo(name = "deviceId")
    public String deviceId;

    @ColumnInfo(name= "purposeId")
    public String ridePurposeId;

    @ColumnInfo(name= "reimbursementCost")
    public String reimbursementCost;

    @ColumnInfo(name = "sanctionDistance")
    public String sanctionDistance;

    public String getSanctionDistance() {
        return sanctionDistance;
    }

    public void setSanctionDistance(String sanctionDistance) {
        this.sanctionDistance = sanctionDistance;
    }

    public String getReimbursementCost() {
        return reimbursementCost;
    }

    public void setReimbursementCost(String reimbursementCost) {
        this.reimbursementCost = reimbursementCost;
    }

    public String getRidePurposeId() {
        return ridePurposeId;
    }

    public void setRidePurposeId(String ridePurposeId) {
        this.ridePurposeId = ridePurposeId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

}
