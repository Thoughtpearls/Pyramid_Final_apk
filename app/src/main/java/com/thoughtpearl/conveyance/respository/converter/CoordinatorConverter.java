package com.thoughtpearl.conveyance.respository.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtpearl.conveyance.respository.dto.Coordinate;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class CoordinatorConverter {
    @TypeConverter
    public Coordinate toCoordinate(String coordinatesString) {
        try {
            Type listType = new TypeToken<ArrayList<Coordinate>>() {}.getType();
            return new Gson().fromJson(coordinatesString, listType);
        } catch (Exception e) {
            return null;
        }
    }

    @TypeConverter
    public String toCoordinateString(ArrayList<Coordinate> coordinates) {
        Gson gson = new Gson();
        String json = gson.toJson(coordinates);
        return json;
    }
}
