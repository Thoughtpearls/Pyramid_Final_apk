package com.thoughtpearl.conveyance.ui.login;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private String displayName;
    private boolean rideDisabled;
    //... other data fields that may be accessible to the UI

    LoggedInUserView(String displayName, boolean rideDisabled) {
        this.displayName = displayName;
        this.rideDisabled = rideDisabled;
    }

    String getDisplayName() {
        return displayName;
    }

    boolean isRideDisabled() { return rideDisabled;}
}