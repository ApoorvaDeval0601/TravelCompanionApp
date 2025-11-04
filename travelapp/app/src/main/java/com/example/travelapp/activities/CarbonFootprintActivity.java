package com.example.travelapp.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.travelapp.R;
import com.example.travelapp.database.AppDatabase;
import com.example.travelapp.models.Trip;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CarbonFootprintActivity extends AppCompatActivity {

    private TextView tvTotalCarbon, tvEquivalent, tvByMode, tvNoTrips;
    private RecyclerView recyclerView;
    private CarbonAdapter adapter;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carbon_footprint);

        tvTotalCarbon = findViewById(R.id.tv_total_carbon);
        tvEquivalent = findViewById(R.id.tv_carbon_equivalent);
        tvByMode = findViewById(R.id.tv_carbon_by_mode);
        tvNoTrips = findViewById(R.id.tv_no_trips_carbon);
        recyclerView = findViewById(R.id.recycler_view_carbon);

        database = AppDatabase.getInstance(this);

        adapter = new CarbonAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadCarbonData();
    }

    private void loadCarbonData() {
        database.tripDao().getAllTrips().observe(this, trips -> {
            if (trips == null || trips.isEmpty()) {
                tvNoTrips.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                tvTotalCarbon.setText("Total CO₂: 0.0 kg");
                tvEquivalent.setText("");
                tvByMode.setText("");
            } else {
                tvNoTrips.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setTrips(trips);
                calculateTotals(trips);
            }
        });
    }

    private void calculateTotals(List<Trip> trips) {
        double totalCarbon = 0;
        Map<String, Double> carbonByMode = new HashMap<>();

        for (Trip trip : trips) {
            totalCarbon += trip.getCarbonFootprintKg();

            String mode = trip.getTransportMode();
            carbonByMode.put(mode, carbonByMode.getOrDefault(mode, 0.0) + trip.getCarbonFootprintKg());
        }

        tvTotalCarbon.setText(String.format("Total CO₂: %.2f kg", totalCarbon));

        // Equivalents
        double treesNeeded = totalCarbon / 21.77; // One tree absorbs ~21.77 kg CO2/year
        tvEquivalent.setText(String.format("Equivalent to %.1f trees needed to offset per year", treesNeeded));

        // Breakdown by mode
        StringBuilder breakdown = new StringBuilder("Carbon by Transport:\n");
        for (Map.Entry<String, Double> entry : carbonByMode.entrySet()) {
            breakdown.append(String.format("  %s: %.2f kg\n", entry.getKey(), entry.getValue()));
        }
        tvByMode.setText(breakdown.toString());
    }

    private static class CarbonAdapter extends RecyclerView.Adapter<CarbonAdapter.ViewHolder> {

        private List<Trip> trips = new ArrayList<>();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        public void setTrips(List<Trip> trips) {
            this.trips = trips;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_carbon_trip, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Trip trip = trips.get(position);

            holder.tvRoute.setText(trip.getOrigin() + " → " + trip.getDestination());
            holder.tvDate.setText(dateFormat.format(new Date(trip.getStartTime())));
            holder.tvTransport.setText(trip.getTransportMode());
            holder.tvCarbon.setText(String.format("%.2f kg CO₂", trip.getCarbonFootprintKg()));

            // Color code by impact
            double carbon = trip.getCarbonFootprintKg();
            if (carbon < 10) {
                holder.tvCarbon.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else if (carbon < 50) {
                holder.tvCarbon.setTextColor(Color.parseColor("#FF9800")); // Orange
            } else {
                holder.tvCarbon.setTextColor(Color.parseColor("#F44336")); // Red
            }
        }

        @Override
        public int getItemCount() {
            return trips.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoute, tvDate, tvTransport, tvCarbon;

            ViewHolder(View itemView) {
                super(itemView);
                tvRoute = itemView.findViewById(R.id.tv_route_carbon);
                tvDate = itemView.findViewById(R.id.tv_date_carbon);
                tvTransport = itemView.findViewById(R.id.tv_transport_carbon);
                tvCarbon = itemView.findViewById(R.id.tv_carbon_value);
            }
        }
    }
}