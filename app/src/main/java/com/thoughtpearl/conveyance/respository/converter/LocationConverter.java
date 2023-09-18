package com.thoughtpearl.conveyance.respository.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtpearl.conveyance.respository.entity.Location;

import java.lang.reflect.Type;
import java.util.ArrayList;

class LocationConverter {
    @TypeConverter
    public Location toLocation(String locationString) {
        try {
            Type listType = new TypeToken<ArrayList<Location>>() {}.getType();
            return new Gson().fromJson(locationString, listType);
        } catch (Exception e) {
            return null;
        }
    }

    @TypeConverter
    public String toLocationString(ArrayList<Location> locations) {
        Gson gson = new Gson();
        String json = gson.toJson(locations);
        return json;
    }

   /* @TypeConverters(LocationConverter::class)
    abstract class YourDatabase : RoomDatabase() {
        // your code
    }*/
}
