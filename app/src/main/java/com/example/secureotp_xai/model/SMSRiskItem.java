package com.example.secureotp_xai.model;

import java.util.List;

/**
 * Model class representing a single analyzed SMS risk result.
 * Used by SMSAnalysisActivity and its RecyclerView adapter.
 */
public class SMSRiskItem {

    private String sender;
    private String message;
    private String label;        // SAFE / SPAM / PHISHING / HIGH RISK
    private List<String> reasons;
    private String suggestion;
    private int threatScore;

    public SMSRiskItem(String sender, String message, String label,
                       List<String> reasons, String suggestion,
                       int threatScore) {
        this.sender      = sender;
        this.message     = message;
        this.label       = label;
        this.reasons     = reasons;
        this.suggestion  = suggestion;
        this.threatScore = threatScore;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getSender()     { return sender; }
    public String getMessage()    { return message; }
    public String getLabel()      { return label; }
    public List<String> getReasons() { return reasons; }
    public String getSuggestion() { return suggestion; }
    public int getThreatScore()   { return threatScore; }

    public boolean isHighRisk() {
        return "HIGH RISK".equals(label) || "PHISHING".equals(label);
    }
}