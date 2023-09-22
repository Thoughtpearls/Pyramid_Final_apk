package com.pyramid.conveyance.api.response;

public class LoginRequest {

    public LoginRequest(String userName, String password, String deviceId) {
        this.userName = userName;
        this.password = password;
        this.deviceId = deviceId;
    }

    String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    String password;
    String deviceId;

}
