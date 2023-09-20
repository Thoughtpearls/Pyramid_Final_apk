package com.thoughtpearl.conveyance.respository.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.thoughtpearl.conveyance.respository.converter.UUIDConverter;
import com.thoughtpearl.conveyance.respository.dao.TripRecordDao;
import com.thoughtpearl.conveyance.respository.entity.Location;
import com.thoughtpearl.conveyance.respository.entity.TripRecord;

// @TypeConverters(value = {LocationConverter.class})
@Database(entities = {TripRecord.class, Location.class}, version = 2, exportSchema = true)
@TypeConverters({UUIDConverter.class})
public abstract class TripDatabase extends RoomDatabase {
    public abstract TripRecordDao tripRecordDao();
}
