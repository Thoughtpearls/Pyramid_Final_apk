package com.pyramid.conveyance.respository.dto;

public class Coordinate {
    public int trip_id;
    public Double lat;
    public Double log;

    public Coordinate(){

    }

    public Coordinate(int trip_id, Double lat, Double log) {
        this.trip_id = trip_id;
        this.lat = lat;
        this.log = log;
    }

    public int getTrip_id() {
        return trip_id;
    }

    public void setTrip_id(int trip_id) {
        this.trip_id = trip_id;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLog() {
        return log;
    }

    public void setLog(Double log) {
        this.log = log;
    }
}
