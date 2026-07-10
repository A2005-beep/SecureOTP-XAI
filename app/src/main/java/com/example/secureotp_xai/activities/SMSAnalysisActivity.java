package com.example.secureotp_xai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secureotp_xai.R;
import com.example.secureotp_xai.adapter.SMSRiskAdapter;
import com.example.secureotp_xai.database.AlertEntity;
import com.example.secureotp_xai.model.SMSRiskItem;
import com.example.secureotp_xai.viewmodel.AlertsViewModel;

import java.util.ArrayList;
import java.util.List;

public class SMSAnalysisActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvSummary;

    private final List<SMSRiskItem> results = new ArrayList<>();
    private SMSRiskAdapter adapter;
    private AlertsViewModel alertsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_analysis);

        if(getSupportActionBar()!=null){
            getSupportActionBar().setTitle("SMS Bomb & Phishing Detection");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView=findViewById(R.id.recyclerSmsResults);
        progressBar=findViewById(R.id.progressSms);
        tvStatus=findViewById(R.id.tvSmsStatus);
        tvSummary=findViewById(R.id.tvSmsSummary);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SMSRiskAdapter(this, results);
        recyclerView.setAdapter(adapter);

        // 1. Initialize ViewModel to connect to Room Database
        alertsViewModel = new ViewModelProvider(this).get(AlertsViewModel.class);

        // Load messages dynamically!
        loadScannedMessages();
    }

    private void loadScannedMessages(){

        progressBar.setVisibility(View.VISIBLE);

        // 2. Observe the database instead of using the old AlertManager
        // This will automatically refresh the screen whenever a new SMS arrives!
        alertsViewModel.getAllAlerts().observe(this, alerts -> {

            results.clear();

            long highRisk = 0;
            long phishing = 0;
            long spam = 0;
            long safe = 0;

            for(AlertEntity a : alerts){

                String label = a.getAlertType();   // ✅ REAL label
                int score = a.getRiskScore();      // ✅ REAL score

                if (label.equals("HIGH_RISK")) highRisk++;
                else if (label.equals("PHISHING")) phishing++;
                else if (label.equals("SUSPICIOUS")) spam++;
                else safe++;

                List<String> reasons = new ArrayList<>();
                reasons.add(a.getAlertMsg());

                results.add(
                        new SMSRiskItem(
                                a.getSender(),
                                a.getBody(),
                                label,
                                reasons,
                                a.getAlertMsg(),
                                score   // ✅ REAL score
                        )
                );
            }

            // show highest threat score first
            results.sort((a,b) -> b.getThreatScore() - a.getThreatScore());

            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);

            if(results.isEmpty()){
                tvStatus.setText("No scanned SMS found yet.");
                tvSummary.setVisibility(View.GONE);
            } else {
                tvStatus.setText("Analysis complete — " + results.size() + " scanned message(s)");
                tvSummary.setVisibility(View.VISIBLE);
                tvSummary.setText(
                        "🔴 High Risk: " + highRisk +
                                "  🟠 Phishing: " + phishing +
                                "  🟡 Spam: " + spam +
                                "  🟢 Safe: " + safe
                );
            }
        });
    }

    // You can remove onResume() override because LiveData automatically handles
    // refreshing the screen when you return to this Activity!

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
