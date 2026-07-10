package com.example.secureotp_xai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secureotp_xai.R;
import com.example.secureotp_xai.adapter.AlertsAdapter;
import com.example.secureotp_xai.database.AlertEntity;
import com.example.secureotp_xai.viewmodel.AlertsViewModel;

import java.util.List;

public class AlertsActivity extends AppCompatActivity {

    private AlertsViewModel alertsViewModel;
    private AlertsAdapter adapter;

    // UI Elements from the new Premium Layout
    private TextView tvRiskLevel;    // Acts as the Main Status
    private TextView tvSummaryText; // Acts as the Stats Summary

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        // 1. Initialize UI with IDs from the new XML
        RecyclerView recyclerView = findViewById(R.id.recyclerAlerts);
        tvRiskLevel = findViewById(R.id.tvRiskLevel);
        tvSummaryText = findViewById(R.id.tvProbability); // We use this for the summary stats

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AlertsAdapter(alertId -> {
            alertsViewModel.markAsRead(alertId);
        });

        recyclerView.setAdapter(adapter);

        // 2. Initialize ViewModel
        alertsViewModel = new ViewModelProvider(this).get(AlertsViewModel.class);

        // 3. Observe Alerts and Calculate Summary Stats
        alertsViewModel.getAllAlerts().observe(this, alerts -> {
            if (alerts == null || alerts.isEmpty()) {
                tvRiskLevel.setText("Status: No threats detected.");
                tvSummaryText.setText("Your inbox is clean.");
                return;
            }

            adapter.submitList(alerts);
            calculateStats(alerts);
        });
    }

    /**
     * Merged Logic: Calculates the summary of attacks for the dashboard
     */
    private void calculateStats(List<AlertEntity> alerts) {
        int highRisk = 0;
        int phishing = 0;
        int spam = 0;
        int safe = 0;

        for (AlertEntity alert : alerts) {
            int score = alert.getRiskScore();
            String type = alert.getAlertType() != null ? alert.getAlertType() : "";

            if (score >= 80) {
                highRisk++;
            } else if (type.contains("FRAUD") || type.equals("PHISHING")) {
                phishing++;
            } else if (type.contains("SUSPICIOUS") || type.equals("SMS_BOMB")) {
                spam++;
            } else {
                safe++;
            }
        }

        // Update the dashboard header
        tvRiskLevel.setText("Status: " + alerts.size() + " Alerts Analyzed");

        tvSummaryText.setText(
                "🔴 High: " + highRisk +
                        "  |  🟠 Phish: " + phishing +
                        "  |  🟡 Suspicious: " + spam +
                        "  |  🟢 Safe: " + safe
        );
    }
}
