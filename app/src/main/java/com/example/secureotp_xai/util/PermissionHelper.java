package com.example.secureotp_xai.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * PermissionHelper
 *
 * Handles all SMS-related permissions including:
 * - READ_SMS (Android 13+)
 * - RECEIVE_SMS (always required)
 * - POST_NOTIFICATIONS (Android 13+)
 * - Battery optimization exemption (optional but recommended)
 *
 * Android 13+ changed permission model:
 * - Apps must request READ_SMS at runtime even if in manifest
 * - RECEIVE_SMS still works from manifest but good to check runtime
 * - POST_NOTIFICATIONS is a new runtime permission
 */
public class PermissionHelper {

    private static final String TAG = "PermissionHelper";

    // Permission request codes
    public static final int REQUEST_SMS_PERMISSIONS        = 1001;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 1002;

    // Required permissions (varies by Android version)
    private static final String[] SMS_PERMISSIONS_LEGACY = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
    };

    private static final String[] SMS_PERMISSIONS_ANDROID_13 = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            "android.permission.POST_NOTIFICATIONS"  // Android 13+
    };

    // ─── Check if all required SMS permissions are granted ────────────────────

    /**
     * Checks if app has all required SMS permissions.
     * On Android 13+, also checks notification permission.
     *
     * @return true if all permissions granted
     */
    public static boolean hasAllSmsPermissions(Context context) {
        String[] required = getRequiredPermissions();
        for (String permission : required) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Missing permission: " + permission);
                return false;
            }
        }
        return true;
    }

    // ─── Request all required SMS permissions ─────────────────────────────────

    /**
     * Requests all required SMS permissions from the user.
     * Call this from an Activity.
     */
    public static void requestSmsPermissions(Activity activity) {
        String[] required = getRequiredPermissions();
        ActivityCompat.requestPermissions(
                activity,
                required,
                REQUEST_SMS_PERMISSIONS
        );
    }

    // ─── Handle permission result callback ────────────────────────────────────

    /**
     * Call this from your Activity's onRequestPermissionsResult().
     *
     * @return true if all permissions were granted
     */
    public static boolean handlePermissionResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        if (requestCode == REQUEST_SMS_PERMISSIONS) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Permission denied: " + permissions[i]);
                    allGranted = false;
                }
            }
            return allGranted;
        }
        return false;
    }

    // ─── Check specific permissions ───────────────────────────────────────────

    public static boolean hasReceiveSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasReadSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;  // Not required before Android 13
        }
        return ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                == PackageManager.PERMISSION_GRANTED;
    }

    // ─── Battery optimization exemption ───────────────────────────────────────

    /**
     * Checks if app is exempt from battery optimization.
     * Battery optimization can kill background receivers.
     *
     * IMPORTANT: Don't abuse this. Only request if your app truly needs it.
     * SMS receivers usually work fine without exemption since they're
     * triggered by system broadcasts.
     */
    public static boolean isBatteryOptimizationDisabled(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;  // Not applicable
        }
        android.os.PowerManager pm = (android.os.PowerManager)
                context.getSystemService(Context.POWER_SERVICE);
        String packageName = context.getPackageName();
        return pm.isIgnoringBatteryOptimizations(packageName);
    }

    /**
     * Opens system settings to request battery optimization exemption.
     * User must manually grant this.
     */
    public static void requestBatteryOptimizationExemption(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));

        try {
            activity.startActivity(intent);
            Log.d(TAG, "Opened battery optimization settings");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open battery settings: " + e.getMessage());
            // Fallback: open general battery optimization settings
            openBatterySettings(activity);
        }
    }

    /**
     * Opens general battery optimization settings page.
     */
    public static void openBatterySettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open battery settings: " + e.getMessage());
        }
    }

    // ─── App settings page (for when user denies permission) ──────────────────

    /**
     * Opens app's system settings page.
     * Useful when user denies permission and needs to enable it manually.
     */
    public static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns required permissions array based on Android version.
     */
    private static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires notification permission
            return SMS_PERMISSIONS_ANDROID_13;
        } else {
            // Android 12 and below
            return SMS_PERMISSIONS_LEGACY;
        }
    }

    /**
     * Logs current permission status (for debugging).
     */
    public static void logPermissionStatus(Context context) {
        Log.d(TAG, "========== PERMISSION STATUS ==========");
        Log.d(TAG, "RECEIVE_SMS: " + hasReceiveSmsPermission(context));
        Log.d(TAG, "READ_SMS: " + hasReadSmsPermission(context));
        Log.d(TAG, "NOTIFICATIONS: " + hasNotificationPermission(context));
        Log.d(TAG, "Battery Opt Disabled: " + isBatteryOptimizationDisabled(context));
        Log.d(TAG, "======================================");
    }

    /**
     * Check if we should show permission rationale.
     * Returns true if user previously denied the permission.
     */
    public static boolean shouldShowRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }
}