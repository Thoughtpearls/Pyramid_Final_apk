package com.thoughtpearl.conveyance.respository.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.thoughtpearl.conveyance.respository.dto.UnSyncRideDto;
import com.thoughtpearl.conveyance.respository.entity.Location;
import com.thoughtpearl.conveyance.respository.entity.TripRecord;
import com.thoughtpearl.conveyance.respository.entity.TripRecordLocationRelation;

import java.util.List;

@Dao
public interface  TripRecordDao {

    @Query("SELECT * FROM TripRecord order by start_time desc")
    List<TripRecord> getAllTripRecord();

    @Transaction
    @Query("SELECT * FROM TripRecord order by start_time desc")
    List<TripRecordLocationRelation> getAllRides();

    @Query("SELECT distinct(id) FROM TripRecord")
    Long[] getTotalTrips();

    @Query("SELECT * FROM TripRecord WHERE id IN (:tripIds)")
    List<TripRecord> getTripByIds(Long[] tripIds);

    @Query("SELECT * FROM TripRecord WHERE id IN (:tripId)")
    TripRecord getTripById(Long tripId);

    @Query("SELECT * FROM TripRecord order by start_time desc limit 1")
    TripRecord getRunningTrip();

    @Query("Select id From TripRecord order by start_time desc limit 1")
    Long getLastTripId();

    @Transaction
    @Query("SELECT * FROM TripRecord WHERE id = :tripId")
    TripRecordLocationRelation getByTripId(Long tripId);

    @Query("Update TripRecord Set end_time =:endTime Where id =:id")
    int updateRecord(Long id, long endTime);

    @Update
    int updateRecord(TripRecord tripRecord);

    @Insert
    void insertAll(TripRecord... TripRecord);

    @Insert
    Long save(Location location);

    @Update
    void update(Location location);

    @Query("UPDATE location SET serversync=:syncServer WHERE locationid LIKE :locationId")
    void updateLocationById(int syncServer, Long locationId);

    @Insert
    Long save(TripRecord tripRecord);

    //SELECT id FROM table_name WHERE rowid = :rowId

    @Query("Select id From TripRecord WHERE rowid =:rowId")
    Long getLastInsertedTripId(Long rowId);

    @Insert
    void insertAll(Location... locations);

    @Delete
    void delete(TripRecord trip);

    @Query("Delete From TripRecord where id =:tripId")
    void deleteById(Long tripId);

    @Query("Delete From TripRecord")
    void deleteAllTrips();

    @Query("Delete From location")
    void deleteAllLocation();

    @Query("SELECT * FROM location WHERE tripId IN (:tripId) AND serverSync=0")
    List<Location> getUnSyncServerLocations(Long tripId);

    @Query("SELECT * FROM location WHERE serverSync=0")
    List<Location> getUnSyncServerLocations();

    @Query("SELECT count(DISTINCT(tripid)) from location where serversync=0")
    int getUnSyncRidesCount();

    @Query("SELECT DISTINCT(tripid) from location where serversync=0")
    List<UnSyncRideDto> getUnSyncRidesList();


    @Query("SELECT * FROM location WHERE tripId IN (:tripId)")
    List<Location> getLocations(String tripId);

    @Query("Delete FROM TripRecord WHERE id IN (select DISTINCT(tripid) from location where tripid not IN (select DISTINCT(tripid) from location where serversync=0))")
    void deleteAllSyncedRides();

}
