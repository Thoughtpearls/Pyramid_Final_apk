package com.thoughtpearl.conveyance.api.response;

import android.os.Parcel;
import android.os.Parcelable;

public class Ride implements Parcelable {
        private Long id;
        private String rideDate;
        private String rideStartTime;
        private String rideEndTime;
        private Double rideCost;
        private Double rideDistance;
        private String ridePurpose;
        private Double sanctionDistance;
        private String employeeRide;
        private boolean deleted;
        private Double reimbursementCost;

        public Ride() {}
        public Ride(Long id, String rideDate, String rideStartTime, String rideEndTime, Double rideCost, Double rideDistance, String reason, Double sanctionDistance, String employeeRide, boolean deleted, Double reimbursementCost) {
                this.id = id;
                this.rideDate = rideDate;
                this.rideStartTime = rideStartTime;
                this.rideEndTime = rideEndTime;
                this.rideCost = rideCost;
                this.rideDistance = rideDistance;
                this.ridePurpose = reason;
                this.sanctionDistance = sanctionDistance;
                this.employeeRide = employeeRide;
                this.deleted = deleted;
                this.reimbursementCost = reimbursementCost;
        }

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public String getRideDate() {
                return rideDate;
        }

        public void setRideDate(String rideDate) {
                this.rideDate = rideDate;
        }

        public String getRideStartTime() {
                return rideStartTime;
        }

        public void setRideStartTime(String rideStartTime) {
                this.rideStartTime = rideStartTime;
        }

        public String getRideEndTime() {
                return rideEndTime;
        }

        public void setRideEndTime(String rideEndTime) {
                this.rideEndTime = rideEndTime;
        }

        public Double getRideCost() {
                return rideCost;
        }

        public void setRideCost(Double rideCost) {
                this.rideCost = rideCost;
        }

        public Double getRideDistance() {
                return rideDistance;
        }

        public void setRideDistance(Double rideDistance) {
                this.rideDistance = rideDistance;
        }

        public String getRidePurpose() {
                return ridePurpose;
        }

        public void setRidePurpose(String ridePurpose) {
                this.ridePurpose = ridePurpose;
        }

        public Double getSanctionDistance() {
                return sanctionDistance;
        }

        public void setSanctionDistance(Double sanctionDistance) {
                this.sanctionDistance = sanctionDistance;
        }

        public String getEmployeeRide() {
                return employeeRide;
        }

        public void setEmployeeRide(String employeeRide) {
                this.employeeRide = employeeRide;
        }

        public boolean isDeleted() {
                return deleted;
        }

        public void setDeleted(boolean deleted) {
                this.deleted = deleted;
        }

        public Double getReimbursementCost() {
                return reimbursementCost;
        }

        public void setReimbursementCost(Double reimbursementCost) {
                this.reimbursementCost = reimbursementCost;
        }

        // Parcelling part
        public Ride(Parcel in) {
                String[] data = new String[11];
                in.readStringArray(data);
                // the order needs to be the same as in writeToParcel() method
                this.id =  Long.parseLong(data[0]);
                this.rideDate = data[1];
                this.rideStartTime = data[2];
                this.rideEndTime = data[3];
                this.rideCost = data[4].equals("null") ? 0 : Double.parseDouble(data[4]);
                this.rideDistance =  data[5].equals("null") ? 0 : Double.parseDouble(data[5]);
                this.ridePurpose = data[6];
                this.sanctionDistance = data[7].equals("null") ? 0 : Double.parseDouble(data[7]);
                this.employeeRide = data[8];
                this.deleted = data[9].equals("null") ? false : Boolean.parseBoolean(data[9]);
                this.reimbursementCost = data[10].equals("null") ? 0 : Double.parseDouble(data[7]);
        }

        @Override
        public int describeContents() {
                return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
                parcel.writeStringArray(new String[] {this.id.toString(),
                        this.rideDate,
                        this.rideStartTime,
                        this.rideEndTime,
                        String.valueOf(this.rideCost),
                        String.valueOf(this.rideDistance),
                        this.ridePurpose,
                        String.valueOf(this.sanctionDistance),
                        this.employeeRide,
                        String.valueOf(this.deleted),
                        String.valueOf(this.reimbursementCost)});
        }

        public static final Creator<Ride> CREATOR = new Creator<Ride>() {
                @Override
                public Ride createFromParcel(Parcel in) {
                        return new Ride(in);
                }

                @Override
                public Ride[] newArray(int size) {
                        return new Ride[size];
                }
        };
}
