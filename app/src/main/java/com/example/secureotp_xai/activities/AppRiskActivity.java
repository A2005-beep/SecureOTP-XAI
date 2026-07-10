package com.example.secureotp_xai.activities;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secureotp_xai.R;
import com.example.secureotp_xai.adapter.AppAdapter;
import com.example.secureotp_xai.model.AppModel;

import java.util.ArrayList;
import java.util.List;

public class AppRiskActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<AppModel> appList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_risk);

        recyclerView = findViewById(R.id.recyclerApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        appList = new ArrayList<>();

        analyzeApps();

        recyclerView.setAdapter(new AppAdapter(appList));
    }

    private void analyzeApps() {

        PackageManager pm = getPackageManager();
        List<PackageInfo> packages;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packages = pm.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS)
            );
        } else {
            packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        }

        for (PackageInfo pkg : packages) {

            if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;

            int riskScore = 0;
            String reason = "";

            String[] permissions = pkg.requestedPermissions;

            if (permissions != null) {
                for (String perm : permissions) {

                    if (perm.contains("READ_SMS") || perm.contains("RECEIVE_SMS")) {
                        riskScore += 5;
                        reason += "SMS ";
                    }
                    if (perm.contains("READ_CONTACTS")) {
                        riskScore += 3;
                        reason += "CONTACTS ";
                    }
                    if (perm.contains("INTERNET")) {
                        riskScore += 1;
                        reason += "INTERNET ";
                    }
                }
            }

            String riskLevel;

            if (riskScore > 5) riskLevel = "HIGH";
            else if (riskScore > 2) riskLevel = "MEDIUM";
            else riskLevel = "SAFE";

            appList.add(new AppModel(
                    pkg.applicationInfo.loadLabel(pm).toString(),
                    riskLevel,
                    reason
            ));
        }
    }
}