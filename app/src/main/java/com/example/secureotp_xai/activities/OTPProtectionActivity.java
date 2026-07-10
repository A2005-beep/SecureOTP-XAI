package com.example.secureotp_xai.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secureotp_xai.R;
import com.example.secureotp_xai.adapter.OTPResultAdapter;
import com.example.secureotp_xai.analyzer.RiskAnalyzer;
import com.example.secureotp_xai.ml.LSTMInferenceEngine;
import com.example.secureotp_xai.model.OTPAnalysisItem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class OTPProtectionActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_SMS = 101;

    private static final Pattern OTP_FILTER = Pattern.compile(
            "(?i)(\\b(otp|one.?time|passcode|verification code|pin|code is|your code|use code|enter code)\\b|\\b\\d{4,8}\\b)"
    );

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvSummary;

    private final List<OTPAnalysisItem> results = new ArrayList<>();
    private OTPResultAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_protection);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("OTP Protection");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerOtpResults);
        progressBar = findViewById(R.id.progressOtp);
        tvStatus = findViewById(R.id.tvOtpStatus);
        tvSummary = findViewById(R.id.tvOtpSummary);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OTPResultAdapter(this, results);
        recyclerView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSION_REQUEST_SMS);
        } else {
            startOTPScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startOTPScan();
            } else {
                tvStatus.setText("SMS permission denied.");
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void startOTPScan() {
        tvStatus.setText("Scanning inbox...");
        progressBar.setVisibility(View.VISIBLE);
        new OTPScanTask().execute();
    }

    @SuppressWarnings("deprecation")
    private class OTPScanTask extends AsyncTask<Void, Void, List<OTPAnalysisItem>> {

        @Override
        protected List<OTPAnalysisItem> doInBackground(Void... voids) {
            List<OTPAnalysisItem> scanResults = new ArrayList<>();

            LSTMInferenceEngine lstmEngine = null;
            try {
                lstmEngine = new LSTMInferenceEngine(getApplicationContext());
            } catch (Exception e) {
                lstmEngine = null;
            }

            try {
                Uri inboxUri = Uri.parse("content://sms/inbox");
                Cursor cursor = getContentResolver().query(inboxUri, new String[]{"address", "body"}, null, null, "date DESC");

                if (cursor != null) {
                    int addrIdx = cursor.getColumnIndex("address");
                    int bodyIdx = cursor.getColumnIndex("body");

                    while (cursor.moveToNext()) {
                        String sender = cursor.getString(addrIdx);
                        String body = cursor.getString(bodyIdx);

                        if (sender == null || body == null) continue;

                        if (!OTP_FILTER.matcher(body).find()) continue;

                        float aiProb = 0.50f;
                        if (lstmEngine != null) {
                            try {
                                aiProb = lstmEngine.predictFraudProbability(body);
                            } catch (Exception e) {}
                        }

                        RiskAnalyzer.RiskResult res = RiskAnalyzer.analyzeRisk(sender, body, aiProb);

                        if (!res.isOtp && res.riskScore < 40) continue;

                        int safeRiskScore = Math.min(res.riskScore, 100);
                        String uiLabel = res.label;

                        if (uiLabel.equals("PHISHING") || uiLabel.equals("SPAM")) {
                            uiLabel = "FRAUD OTP";
                        }

                        // 🟢 FIXED CONSTRUCTOR CALL
                        scanResults.add(new OTPAnalysisItem(
                                sender,
                                body,
                                uiLabel,
                                "SMS Inbox",
                                res.reasons,
                                res.suggestion,
                                safeRiskScore,
                                "Historical Analysis Complete",
                                100
                        ));

                        if (scanResults.size() >= 50) break;
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (scanResults.isEmpty()) {
                scanResults.addAll(getDemoData(lstmEngine));
            }

            return scanResults;
        }

        @Override
        protected void onPostExecute(List<OTPAnalysisItem> scanResults) {
            progressBar.setVisibility(View.GONE);
            results.clear();
            results.addAll(scanResults);
            adapter.notifyDataSetChanged();

            if (!results.isEmpty()) {
                Toast.makeText(OTPProtectionActivity.this, "Loaded " + results.size() + " messages", Toast.LENGTH_LONG).show();
            }

            if (results.isEmpty()) {
                tvStatus.setText("No messages found.");
                tvSummary.setVisibility(View.GONE);
            } else {
                long fraudCount = results.stream().filter(OTPAnalysisItem::isFraud).count();
                tvStatus.setText("Scan complete");
                tvSummary.setVisibility(View.VISIBLE);
                tvSummary.setText("Safe: " + (results.size() - fraudCount) + " | Fraud: " + fraudCount);
            }
        }
    }

    private List<OTPAnalysisItem> getDemoData(LSTMInferenceEngine lstmEngine) {
        List<OTPAnalysisItem> demo = new ArrayList<>();
        String[][] samples = {
                {"HDFCBK", "Your OTP is 748291"},
                {"+919876543210", "URGENT click link enter OTP 554321"},
                {"PAYTMB", "Your Paytm OTP is 334421"}
        };

        for (String[] s : samples) {
            float prob = 0.5f;
            if (lstmEngine != null) {
                try {
                    prob = lstmEngine.predictFraudProbability(s[1]);
                } catch (Exception e) {}
            }

            RiskAnalyzer.RiskResult res = RiskAnalyzer.analyzeRisk(s[0], s[1], prob);
            int safeRiskScore = Math.min(res.riskScore, 100);

            String uiLabel = res.label;
            if (uiLabel.equals("PHISHING") || uiLabel.equals("SPAM")) uiLabel = "FRAUD OTP";

            // 🟢 FIXED CONSTRUCTOR CALL FOR DEMO DATA
            demo.add(new OTPAnalysisItem(
                    s[0], s[1], uiLabel, "Demo Data",
                    res.reasons,
                    res.suggestion, safeRiskScore,
                    "Local XAI Simulation",
                    95
            ));
        }
        return demo;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}