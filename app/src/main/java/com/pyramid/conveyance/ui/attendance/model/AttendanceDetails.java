package com.pyramid.conveyance.ui.attendance.model;

public class AttendanceDetails {
    String id;
    String startTime;
    String endTime;
    String reasonForLeave;
    String employeeAttendance;
    String date;
    String type;
    boolean onLeave;
    String description;
    String name;


    public AttendanceDetails() {
    }

    public AttendanceDetails(String id, String startTime, String endTime, String reasonForLeave, String employeeAttendance, String date, String type, boolean onLeave, String description, String name) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reasonForLeave = reasonForLeave;
        this.employeeAttendance = employeeAttendance;
        this.date = date;
        this.type = type;
        this.onLeave = onLeave;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getReasonForLeave() {
        return reasonForLeave;
    }

    public void setReasonForLeave(String reasonForLeave) {
        this.reasonForLeave = reasonForLeave;
    }

    public String getEmployeeAttendance() {
        return employeeAttendance;
    }

    public void setEmployeeAttendance(String employeeAttendance) {
        this.employeeAttendance = employeeAttendance;
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

    public boolean isOnLeave() {
        return onLeave;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOnLeave(boolean onLeave) {
        this.onLeave = onLeave;
    }

}
