package com.pyramid.conveyance.api;

import com.pyramid.conveyance.ui.attendance.model.AttendanceDetails;

import java.util.ArrayList;

public class LeavesDetails {
    int leavesTaken;
    int leavesRemaining;
    int compOffByMonth;
    ArrayList<AttendanceDetails> holidays;
    ArrayList<AttendanceDetails> attendancesByYear;
    ArrayList<AttendanceDetails> compOffByYear;
    ArrayList<AttendanceDetails> workingDaysByYear;

    public int getCompOffByMonth() {
        return compOffByMonth;
    }

    public void setCompOffByMonth(int compOffByMonth) {
        this.compOffByMonth = compOffByMonth;
    }

    public ArrayList<AttendanceDetails> getCompOffByYear() {
        return compOffByYear;
    }

    public void setCompOffByYear(ArrayList<AttendanceDetails> compOffByYear) {
        this.compOffByYear = compOffByYear;
    }

    public LeavesDetails() {
    }

    public LeavesDetails(int leavesTaken, int leavesRemaining, int compOffByMonth, ArrayList<AttendanceDetails> holidays, ArrayList<AttendanceDetails> attendancesByYear, ArrayList<AttendanceDetails> compOffByYear, ArrayList<AttendanceDetails> workingDaysByYear) {
        this.leavesTaken = leavesTaken;
        this.leavesRemaining = leavesRemaining;
        this.compOffByMonth = compOffByMonth;
        this.holidays = holidays;
        this.attendancesByYear = attendancesByYear;
        this.compOffByYear = compOffByYear;
        this.workingDaysByYear = workingDaysByYear;
    }

    public int getLeavesTaken() {
        return leavesTaken;
    }

    public void setLeavesTaken(int leavesTaken) {
        this.leavesTaken = leavesTaken;
    }

    public int getLeavesRemaining() {
        return leavesRemaining;
    }

    public void setLeavesRemaining(int leavesRemaining) {
        this.leavesRemaining = leavesRemaining;
    }

    public ArrayList<AttendanceDetails> getHolidays() {
        return holidays;
    }

    public void setHolidays(ArrayList<AttendanceDetails> holidays) {
        this.holidays = holidays;
    }

    public ArrayList<AttendanceDetails> getAttendancesByYear() {
        return attendancesByYear;
    }

    public void setAttendancesByYear(ArrayList<AttendanceDetails> attendancesByYear) {
        this.attendancesByYear = attendancesByYear;
    }

    public ArrayList<AttendanceDetails> getWorkingDaysByYear() {
        return workingDaysByYear;
    }

    public void setWorkingDaysByYear(ArrayList<AttendanceDetails> workingDaysByYear) {
        this.workingDaysByYear = workingDaysByYear;
    }
}
