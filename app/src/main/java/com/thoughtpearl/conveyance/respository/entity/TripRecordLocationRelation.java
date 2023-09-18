package com.thoughtpearl.conveyance.respository.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.thoughtpearl.conveyance.respository.entity.Location;
import com.thoughtpearl.conveyance.respository.entity.TripRecord;

import java.util.List;

//@Entity(tableName = "trip_location_relation", primaryKeys = {"tripId","locationId"})
/*@Entity(foreignKeys = {@ForeignKey(entity = TripRecord.class,
                parentColumns = "id",
                childColumns = "tripId",
                onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Location.class,
        parentColumns = "locationId",
        childColumns = "locationId",
        onDelete = ForeignKey.CASCADE)
})*/
public class TripRecordLocationRelation {
    @Embedded
    public TripRecord tripRecord;
    @Relation(
            parentColumn = "id",
            entityColumn = "tripId"
    )
    public List<Location> locations;
    public TripRecordLocationRelation(){}

    public TripRecord getTripRecord() {
        return tripRecord;
    }

    public void setTripRecord(TripRecord tripRecord) {
        this.tripRecord = tripRecord;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }
}
