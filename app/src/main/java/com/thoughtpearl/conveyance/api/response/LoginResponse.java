package com.thoughtpearl.conveyance.api.response;

import java.util.HashMap;
import java.util.Map;

public class LoginResponse {
    private boolean rideDisabled;

    public boolean isRideDisabled() {
        return rideDisabled;
    }

    public void setRideDisabled(boolean rideDisabled) {
        this.rideDisabled = rideDisabled;
    }
}
