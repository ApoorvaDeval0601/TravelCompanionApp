package com.example.travelapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.example.travelapp.database.AppDatabase;
import com.example.travelapp.models.LocationPoint;
import com.example.travelapp.models.Trip;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.util.ArrayList;
import java.util.List;

public class LocationTrackingService extends Service {

    private static final String CHANNEL_ID = "TripTrackingChannel";
    private static final int NOTIFICATION_ID = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private List<LocationPoint> currentTripLocations;
    private int activeTripId;
    private Location lastLocation;
    private double totalDistance = 0.0;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        currentTripLocations = new ArrayList<>();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            activeTripId = intent.getIntExtra("TRIP_ID", -1);
            startForeground(NOTIFICATION_ID, createNotification());
            startLocationUpdates();
        }
        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 seconds
                .setMinUpdateIntervalMillis(5000) // 5 seconds
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    handleLocationUpdate(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback, Looper.getMainLooper());
        }
    }

    private void handleLocationUpdate(Location location) {
        // Calculate distance from last point
        if (lastLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    location.getLatitude(), location.getLongitude(),
                    results
            );
            totalDistance += results[0] / 1000.0; // Convert to km
        }

        lastLocation = location;

        // Store location point
        LocationPoint point = new LocationPoint(
                location.getLatitude(),
                location.getLongitude(),
                System.currentTimeMillis()
        );
        currentTripLocations.add(point);

        // Update trip in database (on background thread)
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Trip trip = db.tripDao().getActiveTrip();
            if (trip != null) {
                trip.setLocationPoints(new ArrayList<>(currentTripLocations));
                trip.setDistanceKm(totalDistance);
                db.tripDao().update(trip);
            }
        }).start();

        // Update notification with distance
        updateNotification();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Trip Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tracking Your Trip")
                .setContentText("Distance: 0.0 km")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void updateNotification() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tracking Your Trip")
                .setContentText(String.format("Distance: %.2f km", totalDistance))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Save final trip data
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Trip trip = db.tripDao().getActiveTrip();
            if (trip != null) {
                trip.setActive(false);
                trip.setEndTime(System.currentTimeMillis());
                trip.setDurationMillis(trip.getEndTime() - trip.getStartTime());
                trip.setDistanceKm(totalDistance);
                trip.setLocationPoints(currentTripLocations);

                // Calculate carbon footprint and costs
                calculateTripMetrics(trip);

                db.tripDao().update(trip);
            }
        }).start();
    }

    private void calculateTripMetrics(Trip trip) {
        // Carbon footprint (kg CO2 per km by transport mode)
        double carbonPerKm = 0;
        switch (trip.getTransportMode()) {
            case "Flight":
                carbonPerKm = 0.255;
                break;
            case "Car":
                carbonPerKm = 0.171;
                break;
            case "Bus":
                carbonPerKm = 0.089;
                break;
            case "Train":
                carbonPerKm = 0.041;
                break;
            case "Auto-rickshaw/Cab":
                carbonPerKm = 0.15;
                break;
        }
        trip.setCarbonFootprintKg(trip.getDistanceKm() * carbonPerKm);

        // Estimate fuel cost (simplified - ₹8/km for car, ₹12/km for auto)
        double fuelCost = 0;
        if (trip.getTransportMode().equals("Car")) {
            fuelCost = trip.getDistanceKm() * 8.0;
        } else if (trip.getTransportMode().equals("Auto-rickshaw/Cab")) {
            fuelCost = trip.getDistanceKm() * 12.0;
        }
        trip.setFuelCost(fuelCost);

        // Toll cost estimate (highways ~₹50 per 100km)
        double tollCost = 0;
        if (trip.getDistanceKm() > 50 &&
                (trip.getTransportMode().equals("Car") || trip.getTransportMode().equals("Bus"))) {
            tollCost = (trip.getDistanceKm() / 100.0) * 50.0;
        }
        trip.setTollCost(tollCost);

        trip.setTotalCost(fuelCost + tollCost);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}