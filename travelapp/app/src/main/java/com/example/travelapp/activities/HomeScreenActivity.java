package com.example.travelapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import com.example.travelapp.R;

public class HomeScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        Button btnNewTrip = findViewById(R.id.btn_new_trip);
        Button btnViewExpenses = findViewById(R.id.btn_view_expenses);
        Button btnCarbonFootprint = findViewById(R.id.btn_carbon_footprint);
        Button btnViewMap = findViewById(R.id.btn_view_map);

        btnNewTrip.setOnClickListener(v -> {
            Intent intent = new Intent(HomeScreenActivity.this, TripTrackingActivity.class);
            startActivity(intent);
        });

        btnViewExpenses.setOnClickListener(v -> {
            Intent intent = new Intent(HomeScreenActivity.this, ExpensesActivity.class);
            startActivity(intent);
        });

        btnCarbonFootprint.setOnClickListener(v -> {
            Intent intent = new Intent(HomeScreenActivity.this, CarbonFootprintActivity.class);
            startActivity(intent);
        });

        btnViewMap.setOnClickListener(v -> {
            Intent intent = new Intent(HomeScreenActivity.this, MapsActivity.class);
            startActivity(intent);
        });
    }
}