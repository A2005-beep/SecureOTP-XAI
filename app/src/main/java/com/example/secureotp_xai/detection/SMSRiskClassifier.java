package com.example.secureotp_xai.detection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SMSRiskClassifier
 *
 * Simulates a GRU/BiLSTM-based NLP pipeline for SMS threat detection.
 *
 * Pipeline layers:
 *   [Tokenizer] → [Intent Detector] → [Threat Scorer] → [Risk Label]
 *
 * Detects:
 *  - Phishing attacks (fake urgency + links)
 *  - SMS Bombing (bulk spam)
 *  - Social engineering patterns
 *  - Malicious URL embedding
 */
public class SMSRiskClassifier {

    // ─── Risk label constants ───────────────────────────────────────────────
    public static final String LABEL_SAFE     = "SAFE";
    public static final String LABEL_SPAM     = "SPAM";
    public static final String LABEL_PHISHING = "PHISHING";
    public static final String LABEL_HIGH     = "HIGH RISK";

    // ─── Threat lexicons (simulating GRU input vectors) ──────────────────────

    private static final List<String> URGENCY_KEYWORDS = Arrays.asList(
            "urgent", "immediately", "right now", "tonight", "today only",
            "last chance", "act now", "hurry", "limited time", "deadline",
            "warning", "alert", "attention", "important notice", "final notice",
            "do not ignore", "action required", "suspended", "blocked", "disconnected"
    );

    private static final List<String> FINANCIAL_THREAT_KEYWORDS = Arrays.asList(
            "pay now", "payment due", "overdue", "bill", "invoice", "pending",
            "electricity", "water", "gas", "disconnection", "service suspended",
            "account blocked", "loan", "emi", "fine", "penalty", "tax"
    );

    private static final List<String> PHISHING_KEYWORDS = Arrays.asList(
            "click here", "tap here", "open link", "visit", "http://", "https://",
            "bit.ly", "tinyurl", "goo.gl", "ow.ly", "t.co", "short.link",
            "verify now", "confirm your", "update your", "reactivate",
            "claim your", "redeem", "won", "winner", "prize", "reward"
    );

    private static final List<String> SOCIAL_ENGINEERING_KEYWORDS = Arrays.asList(
            "dear customer", "valued customer", "dear user", "dear member",
            "you have been selected", "congratulations", "lucky draw",
            "free gift", "free offer", "special offer", "exclusive offer",
            "share this", "forward this", "tell your friends",
            "do not delete", "must read"
    );

    private static final List<String> IMPERSONATION_KEYWORDS = Arrays.asList(
            "government", "police", "court", "irs", "income tax", "trai",
            "rbi", "bank of india", "your bank", "your provider",
            "amazon", "flipkart", "google", "microsoft", "apple", "paypal"
    );

    private static final List<String> SAFE_INDICATORS = Arrays.asList(
            "otp", "one time password", "verification code", "your code is",
            "do not share", "valid for", "expires in", "entered by you"
    );

    // URL pattern
    private static final Pattern URL_PATTERN =
            Pattern.compile("(https?://|www\\.|bit\\.ly|tinyurl|goo\\.gl)[^\\s]*",
                    Pattern.CASE_INSENSITIVE);

    // Phone number harvesting
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("\\b(\\+?91[\\-\\s]?)?[6-9]\\d{9}\\b");

    // ─── Main classification entry point ─────────────────────────────────────

    /**
     * Classifies an SMS message for risk level.
     *
     * @param sender  Sender address
     * @param message Full SMS body
     * @return        SMSRiskResult with label, reasons, suggestion
     */
    public SMSRiskResult classify(String sender, String message) {
        String lowerMsg    = message.toLowerCase();
        String lowerSender = (sender != null) ? sender.toLowerCase() : "";

        // ── Feature extraction (simulated token embeddings) ───────────────────
        int urgencyScore    = scoreKeywords(lowerMsg, URGENCY_KEYWORDS);
        int financialScore  = scoreKeywords(lowerMsg, FINANCIAL_THREAT_KEYWORDS);
        int phishingScore   = scoreKeywords(lowerMsg, PHISHING_KEYWORDS);
        int socialEngScore  = scoreKeywords(lowerMsg, SOCIAL_ENGINEERING_KEYWORDS);
        int impersonScore   = scoreKeywords(lowerMsg, IMPERSONATION_KEYWORDS);
        int safeScore       = scoreKeywords(lowerMsg, SAFE_INDICATORS);

        boolean hasUrl      = URL_PATTERN.matcher(lowerMsg).find();
        boolean hasPhone    = PHONE_PATTERN.matcher(lowerMsg).find();
        boolean numericSend = lowerSender.matches("\\+?[0-9\\-\\s]+");

        // ── Composite threat score (simulated GRU hidden-state weighting) ─────
        int threatScore = 0;
        threatScore += urgencyScore   * 8;
        threatScore += financialScore * 7;
        threatScore += phishingScore  * 12;
        threatScore += socialEngScore * 6;
        threatScore += impersonScore  * 9;
        threatScore -= safeScore      * 15; // safe signals reduce score
        if (hasUrl)      threatScore += 25;
        if (hasPhone && phishingScore > 0) threatScore += 10;
        if (numericSend && phishingScore > 0) threatScore += 8;

        // Keep score non-negative
        if (threatScore < 0) threatScore = 0;

        // ── Reason assembly ───────────────────────────────────────────────────
        List<String> reasons = new ArrayList<>();

        if (urgencyScore > 0) {
            reasons.add("Urgent language detected (\"" + getFirstMatch(lowerMsg, URGENCY_KEYWORDS) + "\")");
        }
        if (hasUrl) {
            reasons.add("Suspicious link embedded in message");
        }
        if (financialScore > 0) {
            reasons.add("Financial threat language detected");
        }
        if (socialEngScore > 0) {
            reasons.add("Social engineering pattern identified");
        }
        if (impersonScore > 0) {
            reasons.add("Possible impersonation of trusted entity");
        }
        if (phishingScore > 1) {
            reasons.add("Multiple phishing indicators present");
        }
        if (numericSend && threatScore > 20) {
            reasons.add("Message from unverified numeric sender");
        }

        // ── Label decision (simulated softmax output) ─────────────────────────
        String label;
        String suggestion;

        if (threatScore >= 50) {
            label = LABEL_HIGH;
            suggestion = hasUrl
                    ? "Do not click the link. This is likely a phishing attack. Delete and report immediately."
                    : "This message contains serious threat signals. Do not respond or call back. Block the sender.";
        } else if (threatScore >= 30) {
            label = LABEL_PHISHING;
            suggestion = "This message shows phishing characteristics. Verify the claim through official channels only.";
        } else if (threatScore >= 12) {
            label = LABEL_SPAM;
            suggestion = "This appears to be spam or promotional fraud. Block the sender and ignore the message.";
        } else {
            label = LABEL_SAFE;
            reasons.clear();
            reasons.add("No significant threat indicators detected");
            suggestion = "Message appears legitimate. Stay cautious and never share personal information.";
        }

        if (reasons.isEmpty()) {
            reasons.add("Message analyzed — benign content");
        }

        return new SMSRiskResult(
                sender, message, label, reasons, suggestion, threatScore
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int scoreKeywords(String text, List<String> keywords) {
        int count = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) count++;
        }
        return count;
    }

    private String getFirstMatch(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return kw;
        }
        return "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner result class
    // ─────────────────────────────────────────────────────────────────────────

    public static class SMSRiskResult {
        public final String sender;
        public final String message;
        public final String label;       // SAFE / SPAM / PHISHING / HIGH RISK
        public final List<String> reasons;
        public final String suggestion;
        public final int threatScore;

        public SMSRiskResult(String sender, String message, String label,
                             List<String> reasons, String suggestion,
                             int threatScore) {
            this.sender      = sender;
            this.message     = message;
            this.label       = label;
            this.reasons     = reasons;
            this.suggestion  = suggestion;
            this.threatScore = threatScore;
        }

        public boolean isHighRisk() {
            return LABEL_HIGH.equals(label) || LABEL_PHISHING.equals(label);
        }
    }
}