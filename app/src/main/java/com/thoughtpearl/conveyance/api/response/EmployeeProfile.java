package com.thoughtpearl.conveyance.api.response;

public class EmployeeProfile {
    String fullName;
    String employeeCode;
    String profile;
    String branch;
    String fatherName;
    String fullAddress;
    String dob;
    String vehicleName;
    String vehicleNo;
    String uid;
    boolean todaysClockIn;
    boolean todaysClockOut;

    public EmployeeProfile() {}
    public EmployeeProfile(String fullName, String employeeCode, String profile, String branch, String fatherName, String fullAddress, String dob, String vehicleName, String vehicleNo, String uid, boolean todaysAttendance) {
        this.fullName = fullName;
        this.employeeCode = employeeCode;
        this.profile = profile;
        this.branch = branch;
        this.fatherName = fatherName;
        this.fullAddress = fullAddress;
        this.dob = dob;
        this.vehicleName = vehicleName;
        this.vehicleNo = vehicleNo;
        this.uid = uid;
        this.todaysClockIn = todaysAttendance;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public boolean isTodaysClockIn() {
        return todaysClockIn;
    }

    public void setTodaysClockIn(boolean todaysClockIn) {
        this.todaysClockIn = todaysClockIn;
    }

    public boolean isTodaysClockOut() {
        return todaysClockOut;
    }

    public void setTodaysClockOut(boolean todaysClockOut) {
        this.todaysClockOut = todaysClockOut;
    }

    //  "fullName": "Aditi shah Ajmera",    "employeeId": "ea8459c7-4743-40d1-b603-0f615b229b7b",    "profile": "https://pyramidemployeetracestorage.s3.ap-south-1.amazonaws.com/69435642-55b8-4931-a56f-528fb18d98f6.png",    "branch": "Azad Nagar",    "fatherName": "Pushker Ajmera",    "dob": "1890-03-23",    "fullAddress": "56, Shodala , Jaipur , Rajasthan",    "vehicleName": "scooty",    "vehicleNo": "rj06sl2647",    "uid": "7.69E+11"
}
