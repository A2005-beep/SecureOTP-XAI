package com.example.secureotp_xai.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.secureotp_xai.R;
import com.example.secureotp_xai.activities.AlertsActivity;

/**
 * NotificationHelper
 *
 * Handles all push notifications for fraud/spam/bomb alerts.
 * Creates notification channels, manages notification IDs,
 * and ensures PendingIntent properly opens AlertsActivity.
 *
 * Features:
 * - Android 8+ notification channels
 * - High priority for fraud alerts
 * - Sound + vibration
 * - Proper PendingIntent flags for Android 12+
 * - Auto-dismisses when tapped
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    // ── Notification channel IDs ──────────────────────────────────────────────
    private static final String CHANNEL_FRAUD_ID   = "fraud_alerts";
    private static final String CHANNEL_SPAM_ID    = "spam_alerts";
    private static final String CHANNEL_GENERAL_ID = "general_alerts";

    // ── Notification IDs (unique per type for replace behavior) ───────────────
    private static final int NOTIF_ID_FRAUD   = 1001;
    private static final int NOTIF_ID_SPAM    = 1002;
    private static final int NOTIF_ID_GENERAL = 1003;

    private final Context context;
    private final NotificationManager notificationManager;

    // ─────────────────────────────────────────────────────────────────────────

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channels on Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels();
        }
    }

    // ─── Create notification channels (Android 8+) ────────────────────────────

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        // ── High priority fraud channel ───────────────────────────────────────
        NotificationChannel fraudChannel = new NotificationChannel(
                CHANNEL_FRAUD_ID,
                "Fraud & Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        fraudChannel.setDescription("Critical fraud and phishing alerts");
        fraudChannel.enableLights(true);
        fraudChannel.setLightColor(Color.RED);
        fraudChannel.enableVibration(true);
        fraudChannel.setVibrationPattern(new long[]{0, 300, 200, 300});
        fraudChannel.setShowBadge(true);

        // ── Medium priority spam channel ──────────────────────────────────────
        NotificationChannel spamChannel = new NotificationChannel(
                CHANNEL_SPAM_ID,
                "Spam Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        spamChannel.setDescription("Spam and unwanted message alerts");
        spamChannel.enableLights(true);
        spamChannel.setLightColor(Color.YELLOW);
        spamChannel.setShowBadge(true);

        // ── Low priority general channel ──────────────────────────────────────
        NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_GENERAL_ID,
                "General Alerts",
                NotificationManager.IMPORTANCE_LOW
        );
        generalChannel.setDescription("General security notifications");
        generalChannel.setShowBadge(false);

        // Register all channels
        notificationManager.createNotificationChannel(fraudChannel);
        notificationManager.createNotificationChannel(spamChannel);
        notificationManager.createNotificationChannel(generalChannel);
    }

    // ─── Show fraud alert notification ───────────────────────────────────────

    /**
     * Shows a high-priority fraud alert notification.
     *
     * @param sender        SMS sender address
     * @param alertType     Alert type (FRAUD_OTP, SMS_BOMB, etc.)
     * @param confidencePct AI confidence percentage (0-100)
     */
    public void showFraudAlert(String sender, String alertType, int confidencePct) {
        String title = getAlertTitle(alertType);
        String body  = buildAlertBody(sender, alertType, confidencePct);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_FRAUD_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)// ← Create this icon or use existing
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)  // Dismiss when tapped
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(Color.parseColor("#C62828"));  // Red accent

        // Sound + vibration for fraud
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(soundUri);
        builder.setVibrate(new long[]{0, 300, 200, 300});

        // ── PendingIntent: opens AlertsActivity when tapped ───────────────────
        Intent intent = new Intent(context, AlertsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("alert_type", alertType);  // Pass alert type to activity

        PendingIntent pendingIntent = createPendingIntent(intent, NOTIF_ID_FRAUD);
        builder.setContentIntent(pendingIntent);

        // Show notification
        notificationManager.notify(NOTIF_ID_FRAUD, builder.build());
    }

    // ─── Show spam alert notification ────────────────────────────────────────

    public void showSpamAlert(String sender, String message) {
        String body = "Spam detected from " + sender;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SPAM_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)  // ← Create this icon or use existing
                .setContentTitle("🟡 Spam SMS Detected")
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#E65100"));  // Orange accent

        Intent intent = new Intent(context, AlertsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = createPendingIntent(intent, NOTIF_ID_SPAM);
        builder.setContentIntent(pendingIntent);

        notificationManager.notify(NOTIF_ID_SPAM, builder.build());
    }

    // ─── Show general alert ───────────────────────────────────────────────────

    public void showGeneralAlert(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);

        Intent intent = new Intent(context, AlertsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = createPendingIntent(intent, NOTIF_ID_GENERAL);
        builder.setContentIntent(pendingIntent);

        notificationManager.notify(NOTIF_ID_GENERAL, builder.build());
    }

    // ─── Helper: Create PendingIntent with correct flags for Android 12+ ─────

    /**
     * Creates a PendingIntent with proper flags for Android 12+ (API 31+).
     * FLAG_IMMUTABLE is required on Android 12+, FLAG_UPDATE_CURRENT allows replacement.
     */
    private PendingIntent createPendingIntent(Intent intent, int requestCode) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        // Android 12+ requires FLAG_IMMUTABLE or FLAG_MUTABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                flags
        );
    }

    // ─── Helper: Build alert notification body ───────────────────────────────

    private String buildAlertBody(String sender, String alertType, int confidence) {
        StringBuilder body = new StringBuilder();

        // Sender info
        if (sender != null && !sender.isEmpty()) {
            body.append("From: ").append(sender).append("\n");
        }

        // Risk level
        if (alertType.contains("FRAUD") || alertType.contains("BOMB")) {
            body.append("Risk Level: HIGH\n");
        } else if (alertType.contains("SPAM")) {
            body.append("Risk Level: MEDIUM\n");
        }

        // AI confidence
        if (confidence > 0) {
            body.append("AI Confidence: ").append(confidence).append("%\n");
        }

        body.append("\nTap to view details and take action.");
        return body.toString();
    }

    // ─── Helper: Get alert title based on type ───────────────────────────────

    private String getAlertTitle(String alertType) {
        switch (alertType.toUpperCase()) {
            case "FRAUD_OTP":
                return "⚠️ Fraud OTP Detected";
            case "SMS_BOMB":
                return "🚨 SMS Bombing Attack";
            case "PHISHING":
                return "⚠️ Phishing Attempt";
            case "SPAM":
                return "🟡 Spam Message";
            default:
                return "⚠️ Security Alert";
        }
    }

    // ─── Clear all notifications ──────────────────────────────────────────────

    public void clearAllNotifications() {
        notificationManager.cancelAll();
    }

    // ─── Clear specific notification ─────────────────────────────────────────

    public void clearNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }
}