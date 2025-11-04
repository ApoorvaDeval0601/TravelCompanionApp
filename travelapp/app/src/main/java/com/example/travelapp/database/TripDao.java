package com.example.travelapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.travelapp.models.Trip;
import java.util.List;

@Dao
public interface TripDao {

    @Insert
    long insert(Trip trip);

    @Update
    void update(Trip trip);

    @Delete
    void delete(Trip trip);

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    LiveData<List<Trip>> getAllTrips();

    @Query("SELECT * FROM trips WHERE id = :tripId")
    LiveData<Trip> getTripById(int tripId);

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    Trip getActiveTrip();

    @Query("SELECT SUM(totalCost) FROM trips")
    LiveData<Double> getTotalExpenses();

    @Query("SELECT SUM(carbonFootprintKg) FROM trips")
    LiveData<Double> getTotalCarbonFootprint();

    @Query("SELECT * FROM trips WHERE startTime >= :startDate AND startTime <= :endDate")
    LiveData<List<Trip>> getTripsBetweenDates(long startDate, long endDate);
}