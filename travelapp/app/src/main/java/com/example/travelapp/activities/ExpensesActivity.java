package com.example.travelapp.activities;

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
import java.util.List;
import java.util.Locale;

public class ExpensesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvTotalExpenses, tvNoTrips;
    private TripExpenseAdapter adapter;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        recyclerView = findViewById(R.id.recycler_view_expenses);
        tvTotalExpenses = findViewById(R.id.tv_total_expenses);
        tvNoTrips = findViewById(R.id.tv_no_trips);

        database = AppDatabase.getInstance(this);

        adapter = new TripExpenseAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadExpenseData();
    }

    private void loadExpenseData() {
        // Load all trips
        database.tripDao().getAllTrips().observe(this, trips -> {
            if (trips == null || trips.isEmpty()) {
                tvNoTrips.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                tvTotalExpenses.setText("Total Expenses: ₹0.00");
            } else {
                tvNoTrips.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setTrips(trips);
            }
        });

        // Load total expenses
        database.tripDao().getTotalExpenses().observe(this, total -> {
            if (total != null) {
                tvTotalExpenses.setText(String.format("Total Expenses: ₹%.2f", total));
            }
        });
    }

    // RecyclerView Adapter
    private static class TripExpenseAdapter extends RecyclerView.Adapter<TripExpenseAdapter.ViewHolder> {

        private List<Trip> trips = new ArrayList<>();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        public void setTrips(List<Trip> trips) {
            this.trips = trips;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trip_expense, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Trip trip = trips.get(position);

            holder.tvRoute.setText(trip.getOrigin() + " → " + trip.getDestination());
            holder.tvDate.setText(dateFormat.format(new Date(trip.getStartTime())));
            holder.tvTransport.setText(trip.getTransportMode());
            holder.tvDistance.setText(String.format("%.1f km", trip.getDistanceKm()));

            holder.tvFuelCost.setText(String.format("Fuel: ₹%.2f", trip.getFuelCost()));
            holder.tvTollCost.setText(String.format("Toll: ₹%.2f", trip.getTollCost()));
            holder.tvTotalCost.setText(String.format("Total: ₹%.2f", trip.getTotalCost()));

            long durationMinutes = trip.getDurationMillis() / (1000 * 60);
            long hours = durationMinutes / 60;
            long minutes = durationMinutes % 60;
            holder.tvDuration.setText(String.format("Duration: %dh %dm", hours, minutes));
        }

        @Override
        public int getItemCount() {
            return trips.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoute, tvDate, tvTransport, tvDistance;
            TextView tvFuelCost, tvTollCost, tvTotalCost, tvDuration;

            ViewHolder(View itemView) {
                super(itemView);
                tvRoute = itemView.findViewById(R.id.tv_route);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvTransport = itemView.findViewById(R.id.tv_transport);
                tvDistance = itemView.findViewById(R.id.tv_distance);
                tvFuelCost = itemView.findViewById(R.id.tv_fuel_cost);
                tvTollCost = itemView.findViewById(R.id.tv_toll_cost);
                tvTotalCost = itemView.findViewById(R.id.tv_total_cost);
                tvDuration = itemView.findViewById(R.id.tv_duration);
            }
        }
    }
}