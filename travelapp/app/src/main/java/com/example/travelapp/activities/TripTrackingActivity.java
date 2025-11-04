package com.example.travelapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.travelapp.BuildConfig;
import com.example.travelapp.R;
import com.example.travelapp.database.AppDatabase;
import com.example.travelapp.models.Trip;
import com.example.travelapp.network.DirectionsResponse;
import com.example.travelapp.network.GoogleMapsApiService;
import com.example.travelapp.services.LocationTrackingService;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TripTrackingActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private TextInputEditText etOrigin, etDestination, etCompanions;
    private Spinner spinnerTransport;
    private Button btnSuggest, btnStartTrip;
    private ProgressBar progressBar;
    private ArrayAdapter<CharSequence> spinnerAdapter;
    private GoogleMapsApiService apiService;

    private Trip plannedTrip;
    private boolean isTrackingActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triptracking);

        initializeViews();
        setupRetrofit();
        setupSpinner();
        checkForActiveTrip();

        btnSuggest.setOnClickListener(v -> fetchDistanceAndSuggestTransport());
        btnStartTrip.setOnClickListener(v -> handleStartStopTrip());
    }

    private void initializeViews() {
        etOrigin = findViewById(R.id.et_origin);
        etDestination = findViewById(R.id.et_destination);
        etCompanions = findViewById(R.id.et_companions);
        spinnerTransport = findViewById(R.id.spinner_transport_mode);
        btnSuggest = findViewById(R.id.btn_suggest_transport);
        btnStartTrip = findViewById(R.id.btn_save_trip);
        progressBar = findViewById(R.id.progressBar);

        btnStartTrip.setText("Start Trip");
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(GoogleMapsApiService.class);
    }

    private void setupSpinner() {
        spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.transport_modes, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransport.setAdapter(spinnerAdapter);
    }

    private void checkForActiveTrip() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Trip activeTrip = db.tripDao().getActiveTrip();

            runOnUiThread(() -> {
                if (activeTrip != null) {
                    isTrackingActive = true;
                    plannedTrip = activeTrip;
                    btnStartTrip.setText("Stop Trip");
                    btnStartTrip.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                    disableInputs();
                }
            });
        }).start();
    }

    private void fetchDistanceAndSuggestTransport() {
        String origin = etOrigin.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();

        if (TextUtils.isEmpty(origin) || TextUtils.isEmpty(destination)) {
            Toast.makeText(this, "Please enter both origin and destination", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSuggest.setEnabled(false);

        apiService.getDirections(origin, destination, BuildConfig.MAPS_API_KEY)
                .enqueue(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSuggest.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null &&
                                !response.body().getRoutes().isEmpty()) {
                            int distanceInMeters = response.body().getRoutes().get(0)
                                    .getLegs().get(0).getDistance().getValue();
                            double distanceInKm = distanceInMeters / 1000.0;

                            updateSuggestion(distanceInKm);
                        } else {
                            Toast.makeText(TripTrackingActivity.this,
                                    "Could not find route. Check city names.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSuggest.setEnabled(true);
                        Toast.makeText(TripTrackingActivity.this,
                                "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateSuggestion(double distanceInKm) {
        String companionsStr = etCompanions.getText().toString().trim();
        int companions = TextUtils.isEmpty(companionsStr) ? 0 : Integer.parseInt(companionsStr);

        String suggestedMode = getSuggestedTransport(distanceInKm, companions);

        int spinnerPosition = spinnerAdapter.getPosition(suggestedMode);
        if (spinnerPosition >= 0) {
            spinnerTransport.setSelection(spinnerPosition);
            Toast.makeText(this, "Suggested: " + suggestedMode +
                            " (~" + String.format("%.1f", distanceInKm) + " km)",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String getSuggestedTransport(double distanceInKm, int companions) {
        if (distanceInKm > 1000) {
            return "Flight";
        } else if (distanceInKm > 300) {
            return "Train";
        } else if (distanceInKm > 50) {
            return (companions >= 2) ? "Car" : "Bus";
        } else {
            return (companions >= 2) ? "Car" : "Auto-rickshaw/Cab";
        }
    }

    private void handleStartStopTrip() {
        if (isTrackingActive) {
            showStopTripDialog();
        } else {
            startTrip();
        }
    }

    private void startTrip() {
        String origin = etOrigin.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();

        if (TextUtils.isEmpty(origin) || TextUtils.isEmpty(destination)) {
            Toast.makeText(this, "Please enter origin and destination", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        // Create trip object
        plannedTrip = new Trip();
        plannedTrip.setOrigin(origin);
        plannedTrip.setDestination(destination);
        plannedTrip.setTransportMode(spinnerTransport.getSelectedItem().toString());

        String companionsStr = etCompanions.getText().toString().trim();
        plannedTrip.setCompanions(TextUtils.isEmpty(companionsStr) ? 0 : Integer.parseInt(companionsStr));

        plannedTrip.setStartTime(System.currentTimeMillis());
        plannedTrip.setActive(true);

        // Save to database and start tracking service
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long tripId = db.tripDao().insert(plannedTrip);

            runOnUiThread(() -> {
                // Start location tracking service
                Intent serviceIntent = new Intent(this, LocationTrackingService.class);
                serviceIntent.putExtra("TRIP_ID", (int) tripId);
                ContextCompat.startForegroundService(this, serviceIntent);

                isTrackingActive = true;
                btnStartTrip.setText("Stop Trip");
                btnStartTrip.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                disableInputs();

                Toast.makeText(this, "Trip started! GPS tracking active.", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void showStopTripDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Trip")
                .setMessage("Are you sure you want to end this trip? Your data will be saved.")
                .setPositiveButton("End Trip", (dialog, which) -> stopTrip())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void stopTrip() {
        // Stop the location tracking service
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);

        isTrackingActive = false;
        btnStartTrip.setText("Start Trip");
        btnStartTrip.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
        enableInputs();

        Toast.makeText(this, "Trip ended and saved!", Toast.LENGTH_SHORT).show();

        // Clear inputs
        etOrigin.setText("");
        etDestination.setText("");
        etCompanions.setText("");
        spinnerTransport.setSelection(0);
    }

    private void disableInputs() {
        etOrigin.setEnabled(false);
        etDestination.setEnabled(false);
        etCompanions.setEnabled(false);
        spinnerTransport.setEnabled(false);
        btnSuggest.setEnabled(false);
    }

    private void enableInputs() {
        etOrigin.setEnabled(true);
        etDestination.setEnabled(true);
        etCompanions.setEnabled(true);
        spinnerTransport.setEnabled(true);
        btnSuggest.setEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTrip();
            } else {
                Toast.makeText(this, "Location permission is required for trip tracking",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}