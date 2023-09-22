package com.pyramid.conveyance.data.model;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private String userId;
    private String displayName;
    private boolean rideDisabled;

    public LoggedInUser(String userId, String displayName, boolean rideDisabled) {
        this.userId = userId;
        this.displayName = displayName;
        this.rideDisabled = rideDisabled;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRideDisabled() {
        return rideDisabled;
    }

    public void setRideDisabled(boolean rideDisabled) {
        this.rideDisabled = rideDisabled;
    }
}