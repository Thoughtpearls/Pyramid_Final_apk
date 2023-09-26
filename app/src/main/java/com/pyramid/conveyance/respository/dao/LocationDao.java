package com.pyramid.conveyance.respository.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.pyramid.conveyance.respository.entity.Location;

import java.util.List;

@Dao
public interface LocationDao {
    @Query("SELECT * FROM location WHERE tripId = :tripId order by SUBSTR(timestamp, 1, 2) || SUBSTR(timestamp, 4, 2) || SUBSTR(timestamp, 7, 2) asc")
    List<Location> getAllLocationsByTripId(Long tripId);
}
