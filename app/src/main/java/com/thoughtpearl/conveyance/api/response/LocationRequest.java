package com.thoughtpearl.conveyance.api.response;

public class LocationRequest {
   private String id;
   private Double latitude;
   private Double longitude;
   private String rideNavigationPoint;
   private String timeStamp;

    public LocationRequest(String id, Double latitude, Double longitude, String rideNavigationPoint, String timeStamp) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rideNavigationPoint = rideNavigationPoint;
        this.timeStamp = timeStamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getRideNavigationPoint() {
        return rideNavigationPoint;
    }

    public void setRideNavigationPoint(String rideNavigationPoint) {
        this.rideNavigationPoint = rideNavigationPoint;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}
