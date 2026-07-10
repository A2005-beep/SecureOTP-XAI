package com.example.secureotp_xai.database; // <-- Update package name if needed

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "alerts_table")
public class AlertEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String alertType; // FRAUD_OTP, SAFE_OTP, SAFE_SMS, PHISHING, SUSPICIOUS, SMS_BOMB, HIGH_RISK
    private String alertMsg;  // e.g. "Do NOT click the link"

    // 🔴 THESE WERE ACCIDENTALLY DELETED! RESTORED THEM: 🔴
    private String sender;
    private String body;
    private long timestamp;
    private int priority;

    private int riskScore;    // 0-100 risk score from RiskAnalyzer
    private String reasons;   // Explainable AI reasons (XAI)

    // Local XAI Engine fields
    private String aiExplanation;
    private int aiConfidence;
    private String aiTags;

    // Manages the read/unread UX state
    private boolean isRead;

    // 1. Empty constructor for Room (Bulletproofs the Database compilation)
    public AlertEntity() {
    }

    // 2. Parameterized constructor for your code
    @Ignore
    public AlertEntity(String alertType, String alertMsg, String sender, String body, long timestamp,
                       int priority, int riskScore, String reasons, boolean isRead,
                       String aiExplanation, int aiConfidence, String aiTags) {
        this.alertType = alertType;
        this.alertMsg = alertMsg;
        this.sender = sender;
        this.body = body;
        this.timestamp = timestamp;
        this.priority = priority;
        this.riskScore = riskScore;
        this.reasons = reasons;
        this.isRead = isRead;
        this.aiExplanation = aiExplanation;
        this.aiConfidence = aiConfidence;
        this.aiTags = aiTags;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getAlertMsg() { return alertMsg; }
    public void setAlertMsg(String alertMsg) { this.alertMsg = alertMsg; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getReasons() { return reasons; }
    public void setReasons(String reasons) { this.reasons = reasons; }

    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }

    public int getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(int aiConfidence) { this.aiConfidence = aiConfidence; }

    public String getAiTags() { return aiTags; }
    public void setAiTags(String aiTags) { this.aiTags = aiTags; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }
}