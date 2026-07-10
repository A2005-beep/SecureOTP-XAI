package com.example.secureotp_xai.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.secureotp_xai.analyzer.RiskAnalyzer;
import com.example.secureotp_xai.database.AlertEntity;
import com.example.secureotp_xai.database.AppDatabase;
import com.example.secureotp_xai.detection.SMSBombDetector;
import com.example.secureotp_xai.ml.LSTMInferenceEngine;
import com.example.secureotp_xai.util.NotificationHelper;
import com.example.secureotp_xai.LocalXAIEngine;

public class SMSReceiver extends BroadcastReceiver {

    private static final String TAG = "SMSReceiver";
    private static LSTMInferenceEngine lstmEngine;

    private static final int PRIORITY_LOW  = 0;
    private static final int PRIORITY_HIGH = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action) &&
                !Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) return;

        try {
            Bundle bundle = intent.getExtras();
            if (bundle == null) return;
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null) return;
            SmsMessage[] messages = new SmsMessage[pdus.length];
            String format = bundle.getString("format");

            for (int i = 0; i < pdus.length; i++) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
            }
            if (messages == null || messages.length == 0) return;

            String sender = messages[0].getDisplayOriginatingAddress();
            StringBuilder bodyBuilder = new StringBuilder();
            for (SmsMessage m : messages) {
                if (m != null && m.getMessageBody() != null) bodyBuilder.append(m.getMessageBody());
            }

            final String finalSender = sender;
            final String finalBody = bodyBuilder.toString();

            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
                    NotificationHelper notifier = new NotificationHelper(context.getApplicationContext());

                    if (lstmEngine == null) {
                        try { lstmEngine = new LSTMInferenceEngine(context.getApplicationContext()); }
                        catch (Exception e) { lstmEngine = null; }
                    }

                    boolean isBomb = SMSBombDetector.recordAndCheck(System.currentTimeMillis());

                    // FIXED SCOPE: Declared at the top of the thread so they can be saved properly
                    String alertType = "SAFE_OTP";
                    String alertMsg = "Safe SMS";
                    String reasons = "No threats detected.";
                    int priority = PRIORITY_LOW;
                    int finalRiskScore = 0;
                    String aiExplanation = "Analysis pending...";
                    int aiConfidence = 0;
                    String aiTags = "Scanning...";

                    if (isBomb) {
                        alertType = "SMS_BOMB";
                        alertMsg = "SMS Bomb Attack Detected!";
                        reasons = "• Many messages arrived at once in a very short time.";
                        priority = PRIORITY_HIGH;
                        finalRiskScore = 95;

                        LocalXAIEngine.XAIResult bombXai = LocalXAIEngine.explain(
                                new RiskAnalyzer.RiskResult("SMS_BOMB", 95, reasons, "", false),
                                finalSender, finalBody, 1.0f, true
                        );
                        aiExplanation = bombXai.explanation;
                        aiConfidence  = bombXai.confidence;
                        aiTags        = bombXai.tags;
                    } else {
                        float aiProbability = 0.50f;
                        if (lstmEngine != null) {
                            try { aiProbability = lstmEngine.predictFraudProbability(finalBody); }
                            catch (Exception e) { Log.e(TAG, "Prediction fail " + e.getMessage()); }
                        }

                        RiskAnalyzer.RiskResult result = RiskAnalyzer.analyzeRisk(finalSender, finalBody, aiProbability);
                        finalRiskScore = result.riskScore;
                        reasons = result.reasons;

                        LocalXAIEngine.XAIResult xai = LocalXAIEngine.explain(
                                result, finalSender, finalBody, aiProbability, false
                        );
                        aiExplanation = xai.explanation;
                        aiConfidence  = xai.confidence;
                        aiTags        = xai.tags;

                        switch (result.label) {
                            case "FRAUD_OTP":
                            case "PHISHING":
                            case "SUSPICIOUS":
                                alertType = result.label;
                                alertMsg = result.suggestion;
                                priority = PRIORITY_HIGH;
                                break;
                            case "SAFE_OTP":
                            case "SAFE_SMS":
                            default:
                                alertType = result.label.equals("SAFE_OTP") ? "SAFE_OTP" : "SAFE_SMS";
                                alertMsg = aiExplanation;
                                priority = PRIORITY_LOW;
                                break;
                        }
                    }

                    AlertEntity alertEntity = new AlertEntity(
                            alertType, alertMsg, finalSender, finalBody, System.currentTimeMillis(),
                            priority, finalRiskScore, reasons, false,
                            aiExplanation, aiConfidence, aiTags
                    );

                    db.alertDao().insertAlert(alertEntity);
                    notifier.showFraudAlert(finalSender, alertType, finalRiskScore);

                } catch (Exception e) {
                    Log.e(TAG, "BG THREAD CRASH", e);
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Receiver crash", e);
        }
    }
}