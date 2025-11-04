package com.example.travelapp.database;

import androidx.room.TypeConverter;
import com.example.travelapp.models.LocationPoint;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class LocationListConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromLocationList(List<LocationPoint> locations) {
        if (locations == null) {
            return null;
        }
        return gson.toJson(locations);
    }

    @TypeConverter
    public static List<LocationPoint> toLocationList(String locationsString) {
        if (locationsString == null) {
            return null;
        }
        Type listType = new TypeToken<List<LocationPoint>>(){}.getType();
        return gson.fromJson(locationsString, listType);
    }
}