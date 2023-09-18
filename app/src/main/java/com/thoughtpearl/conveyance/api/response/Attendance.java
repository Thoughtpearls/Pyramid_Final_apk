package com.thoughtpearl.conveyance.api.response;

public class Attendance {
    String time;
    String date;
    String type;
    String longitude;
    String latitude;
    String reasonForLeave;

    public Attendance(){
    }

    public Attendance(String time, String date, String type, String longitude, String latitude, String reasonForLeave) {
        this.time = time;
        this.date = date;
        this.type = type;
        this.longitude = longitude;
        this.latitude = latitude;
        this.reasonForLeave = reasonForLeave;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getReasonForLeave() {
        return reasonForLeave;
    }

    public void setReasonForLeave(String reasonForLeave) {
        this.reasonForLeave = reasonForLeave;
    }
}
