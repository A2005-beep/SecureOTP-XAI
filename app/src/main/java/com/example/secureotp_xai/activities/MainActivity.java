package com.example.secureotp_xai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.secureotp_xai.R;
import com.example.secureotp_xai.util.PermissionHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // STEP 1 — Claude permission flow
        checkAndRequestPermissions();

        // 🔐 OTP Protection Card
        View otpCard = findViewById(R.id.cardOTP);
        if (otpCard != null) {
            otpCard.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, OTPProtectionActivity.class);
                startActivity(intent);
            });
        }

        // 💣 SMS Bomb Detection Card
        View smsCard = findViewById(R.id.cardSMS);
        if (smsCard != null) {
            smsCard.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SMSAnalysisActivity.class);
                startActivity(intent);
            });
        }

        // 📊 Dashboard Card
        View dashboardCard = findViewById(R.id.cardDashboard);
        if (dashboardCard != null) {
            dashboardCard.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AlertsActivity.class);
                startActivity(intent);
            });
        }

        // 📱 App Risk Analyzer Card
        View appsCard = findViewById(R.id.cardApps);
        if (appsCard != null) {
            appsCard.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AppRiskActivity.class);
                startActivity(intent);
            });
        }
    }

    // ---------------------------------------------------
    // Permission Handling
    // ---------------------------------------------------

    private void checkAndRequestPermissions() {

        PermissionHelper.logPermissionStatus(this);

        if (PermissionHelper.hasAllSmsPermissions(this)) {

            Log.d(TAG, "All permissions granted");

            if (!PermissionHelper.isBatteryOptimizationDisabled(this)) {
                showBatteryOptimizationDialog();
            }

            return;
        }

        showPermissionRationaleDialog();
    }

    private void showPermissionRationaleDialog() {

        new AlertDialog.Builder(this)
                .setTitle("SMS Permissions Required")
                .setMessage(
                        "SecureOTP-XAI needs SMS permissions to:\n\n" +
                                "• Detect fraud and phishing in incoming messages\n" +
                                "• Identify SMS bombing attacks\n" +
                                "• Analyze OTP messages for security\n" +
                                "• Send alerts about suspicious messages\n\n" +
                                "Messages are processed locally."
                )
                .setPositiveButton("Grant Permissions", (dialog, which) -> {
                    PermissionHelper.requestSmsPermissions(this);
                })
                .setNegativeButton("Not Now", (dialog, which) -> {
                    Toast.makeText(
                            this,
                            "App will not work without SMS permissions",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .setCancelable(false)
                .show();
    }

    private void showBatteryOptimizationDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Improve Reliability")
                .setMessage(
                        "Disable battery optimization for better SMS fraud detection.\n\n" +
                                "Optional but recommended."
                )
                .setPositiveButton("Disable", (dialog, which) -> {
                    PermissionHelper.requestBatteryOptimizationExemption(this);
                })
                .setNegativeButton("Skip", null)
                .show();
    }

    // ---------------------------------------------------
    // Permission Result
    // ---------------------------------------------------

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        boolean granted = PermissionHelper.handlePermissionResult(
                requestCode,
                permissions,
                grantResults
        );

        if (granted) {

            Log.d(TAG, "Permissions granted");

            Toast.makeText(
                    this,
                    "SMS fraud detection active!",
                    Toast.LENGTH_LONG
            ).show();

            if (!PermissionHelper.isBatteryOptimizationDisabled(this)) {
                showBatteryOptimizationDialog();
            }

        } else {

            new AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage(
                            "SecureOTP-XAI cannot function without SMS permissions.\n\n" +
                                    "Please grant permissions in Settings."
                    )
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        PermissionHelper.openAppSettings(this);
                    })
                    .setNegativeButton("Exit", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    // ---------------------------------------------------
    // Recheck on Resume
    // ---------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        if (!PermissionHelper.hasAllSmsPermissions(this)) {

            Toast.makeText(
                    this,
                    "Please grant SMS permissions for fraud detection",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}