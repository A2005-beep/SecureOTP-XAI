package com.example.secureotp_xai.model;

/**
 * Model class for analyzed OTP results.
 */
public class OTPAnalysisItem {

    private String sender;
    private String message;
    private String label;
    private String sourceType;
    private String reasons;
    private String suggestion;
    private int riskScore;
    private String aiExplanation;
    private int aiConfidence;

    public OTPAnalysisItem(String sender, String message, String label,
                           String sourceType, String reasons,
                           String suggestion, int riskScore,
                           String aiExplanation, int aiConfidence) {
        this.sender = sender;
        this.message = message;
        this.label = label;
        this.sourceType = sourceType;
        this.reasons = reasons;
        this.suggestion = suggestion;
        this.riskScore = riskScore;
        this.aiExplanation = aiExplanation;
        this.aiConfidence = aiConfidence;
    }

    // --- Accessor Methods ---
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public String getLabel() { return label; }
    public String getSourceType() { return sourceType; }
    public String getReasons() { return reasons; }
    public String getSuggestion() { return suggestion; }
    public int getRiskScore() { return riskScore; }
    public String getAiExplanation() { return aiExplanation; }
    public int getAiConfidence() { return aiConfidence; }

    public boolean isFraud() {
        return "FRAUD OTP".equals(label) || "PHISHING".equals(label);
    }
    // --- SETTERS (Optional, but helpful for AI updates) ---
    public void setSender(String sender) { this.sender = sender; }
    public void setMessage(String message) { this.message = message; }
    public void setLabel(String label) { this.label = label; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setReasons(String reasons) { this.reasons = reasons; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
    public void setAiConfidence(int aiConfidence) { this.aiConfidence = aiConfidence; }

}
