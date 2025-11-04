package com.example.travelapp.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.travelapp.database.LocationListConverter;
import java.util.List;

@Entity(tableName = "trips")
@TypeConverters(LocationListConverter.class)
public class Trip {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String origin;
    private String destination;
    private String transportMode;
    private int companions;

    private double distanceKm;
    private long startTime;
    private long endTime;
    private long durationMillis;

    private double tollCost;
    private double fuelCost;
    private double totalCost;

    private double carbonFootprintKg;

    private List<LocationPoint> locationPoints;

    private boolean isActive;

    // Constructor
    public Trip() {
        this.isActive = false;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public int getCompanions() {
        return companions;
    }

    public void setCompanions(int companions) {
        this.companions = companions;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public double getTollCost() {
        return tollCost;
    }

    public void setTollCost(double tollCost) {
        this.tollCost = tollCost;
    }

    public double getFuelCost() {
        return fuelCost;
    }

    public void setFuelCost(double fuelCost) {
        this.fuelCost = fuelCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getCarbonFootprintKg() {
        return carbonFootprintKg;
    }

    public void setCarbonFootprintKg(double carbonFootprintKg) {
        this.carbonFootprintKg = carbonFootprintKg;
    }

    public List<LocationPoint> getLocationPoints() {
        return locationPoints;
    }

    public void setLocationPoints(List<LocationPoint> locationPoints) {
        this.locationPoints = locationPoints;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}