package com.example.travelapp.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import com.example.travelapp.R;
import com.example.travelapp.database.AppDatabase;
import com.example.travelapp.models.LocationPoint;
import com.example.travelapp.models.Trip;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private AppDatabase database;
    private Button btnShowAllTrips, btnShowLatestTrip, btnClearMap;
    private List<Trip> allTrips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        database = AppDatabase.getInstance(this);

        // Initialize buttons
        btnShowAllTrips = findViewById(R.id.btn_show_all_trips);
        btnShowLatestTrip = findViewById(R.id.btn_show_latest_trip);
        btnClearMap = findViewById(R.id.btn_clear_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Button click listeners
        btnShowAllTrips.setOnClickListener(v -> showAllTripsOnMap());
        btnShowLatestTrip.setOnClickListener(v -> showLatestTripOnMap());
        btnClearMap.setOnClickListener(v -> clearMapAndShowDefault());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable traffic layer
        mMap.setTrafficEnabled(true);

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Default view: Mumbai
        showDefaultLocation();

        // Load trips from database
        loadTripsFromDatabase();
    }

    private void showDefaultLocation() {
        LatLng mumbai = new LatLng(19.0760, 72.8777);
        mMap.addMarker(new MarkerOptions()
                .position(mumbai)
                .title("Mumbai")
                .snippet("Default Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mumbai, 6));
    }

    private void loadTripsFromDatabase() {
        new Thread(() -> {
            List<Trip> trips = database.tripDao().getAllTrips().getValue();
            if (trips != null) {
                allTrips = trips;
                runOnUiThread(() -> {
                    if (!allTrips.isEmpty()) {
                        Toast.makeText(this,
                                allTrips.size() + " trips found in database",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();

        // Also observe LiveData for real-time updates
        database.tripDao().getAllTrips().observe(this, trips -> {
            if (trips != null) {
                allTrips = trips;
            }
        });
    }

    private void showAllTripsOnMap() {
        if (allTrips.isEmpty()) {
            Toast.makeText(this, "No trips to display", Toast.LENGTH_SHORT).show();
            return;
        }

        mMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidPoints = false;

        int colorIndex = 0;
        int[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN};

        for (Trip trip : allTrips) {
            if (trip.getLocationPoints() != null && !trip.getLocationPoints().isEmpty()) {
                hasValidPoints = true;
                drawTripRoute(trip, colors[colorIndex % colors.length], boundsBuilder);
                colorIndex++;
            } else {
                // For trips without GPS data, show origin/destination markers
                addCityMarkers(trip, boundsBuilder);
            }
        }

        if (hasValidPoints) {
            // Zoom to show all trips
            try {
                LatLngBounds bounds = boundsBuilder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            } catch (IllegalStateException e) {
                // If bounds are empty, show default location
                showDefaultLocation();
            }
        } else {
            Toast.makeText(this, "No GPS data available for trips", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLatestTripOnMap() {
        if (allTrips.isEmpty()) {
            Toast.makeText(this, "No trips to display", Toast.LENGTH_SHORT).show();
            return;
        }

        mMap.clear();
        Trip latestTrip = allTrips.get(0); // Already sorted by start time DESC

        if (latestTrip.getLocationPoints() != null && !latestTrip.getLocationPoints().isEmpty()) {
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            drawTripRoute(latestTrip, Color.BLUE, boundsBuilder);

            try {
                LatLngBounds bounds = boundsBuilder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
            } catch (IllegalStateException e) {
                showDefaultLocation();
            }

            Toast.makeText(this,
                    "Showing: " + latestTrip.getOrigin() + " → " + latestTrip.getDestination(),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No GPS data for latest trip", Toast.LENGTH_SHORT).show();
            addCityMarkers(latestTrip, null);
        }
    }

    private void drawTripRoute(Trip trip, int color, LatLngBounds.Builder boundsBuilder) {
        List<LocationPoint> points = trip.getLocationPoints();
        if (points == null || points.isEmpty()) return;

        // Create polyline for the route
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(color)
                .width(8f)
                .geodesic(true);

        // Add all GPS points to polyline
        for (LocationPoint point : points) {
            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
            polylineOptions.add(latLng);
            if (boundsBuilder != null) {
                boundsBuilder.include(latLng);
            }
        }

        mMap.addPolyline(polylineOptions);

        // Add start marker (green)
        LocationPoint startPoint = points.get(0);
        LatLng startLatLng = new LatLng(startPoint.getLatitude(), startPoint.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(startLatLng)
                .title("Start: " + trip.getOrigin())
                .snippet(trip.getTransportMode() + " • " + String.format("%.1f km", trip.getDistanceKm()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Add end marker (red)
        LocationPoint endPoint = points.get(points.size() - 1);
        LatLng endLatLng = new LatLng(endPoint.getLatitude(), endPoint.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(endLatLng)
                .title("End: " + trip.getDestination())
                .snippet(String.format("₹%.2f", trip.getTotalCost()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void addCityMarkers(Trip trip, LatLngBounds.Builder boundsBuilder) {
        // If no GPS data, show approximate city locations
        // You can enhance this by geocoding the city names
        String infoText = trip.getTransportMode() + " • " +
                String.format("%.1f km • ₹%.2f", trip.getDistanceKm(), trip.getTotalCost());

        // Add a single marker for the trip (you can improve with geocoding)
        LatLng location = new LatLng(19.0760, 72.8777); // Default location
        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(trip.getOrigin() + " → " + trip.getDestination())
                .snippet(infoText)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        if (boundsBuilder != null) {
            boundsBuilder.include(location);
        }
    }

    private void clearMapAndShowDefault() {
        mMap.clear();
        showDefaultLocation();
        Toast.makeText(this, "Map cleared", Toast.LENGTH_SHORT).show();
    }
}