package com.example.secureotp_xai.analyzer; // <-- Change this if your project package is different!

import com.example.secureotp_xai.util.TrustedSourceManager; // <-- Change this if your project package is different!

/**
 * RiskAnalyzer — Hybrid AI + Rule-Based Decision Engine
 *
 * Produces a RiskResult with a LABEL, riskScore (0-100), reasons, and suggestion.
 */
public class RiskAnalyzer {

    // ── Score ceilings per label (enforced in normalization) ──
    private static final int SAFE_SMS_MAX_SCORE   = 25;
    private static final int SAFE_OTP_MAX_SCORE   = 40;
    private static final int SUSPICIOUS_MIN_SCORE = 30;
    private static final int SUSPICIOUS_MAX_SCORE = 60;
    private static final int PHISHING_MIN_SCORE   = 50;
    private static final int FRAUD_MIN_SCORE      = 70;
    private static final int HIGH_RISK_MIN_SCORE  = 90;

    /**
     * Main analysis entry point.
     *
     * @param sender              SMS sender address / shortcode
     * @param message             Full SMS body
     * @param lstmFraudProbability Raw output from the LSTM model [0.0 – 1.0]
     * @return Fully consistent RiskResult (label ↔ riskScore guaranteed)
     */
    public static RiskResult analyzeRisk(String sender, String message, float lstmFraudProbability) {

        // ── Contextual feature extraction ──
        boolean isTrustedSender  = TrustedSourceManager.isTrustedSender(sender);
        boolean hasTrustedDomain = TrustedSourceManager.containsTrustedDomain(message);
        boolean hasSuspiciousLink = TrustedSourceManager.containsSuspiciousLink(message);
        boolean hasAnyLink       = TrustedSourceManager.containsAnyLink(message);

        String lowerMessage = message.toLowerCase();

        // ── Core OTP / Safety Context ──
        boolean isOTP = lowerMessage.contains("otp")
                || lowerMessage.contains("one time")
                || lowerMessage.contains("verification code")
                || lowerMessage.contains("one-time");
        boolean hasSafeContext = lowerMessage.contains("do not share")
                || lowerMessage.contains("valid for")
                || lowerMessage.contains("expires in");

        // ── Urgency / Threatening Language ──
        boolean hasUrgency = lowerMessage.contains("urgent")
                || lowerMessage.contains("immediately")
                || lowerMessage.contains("account blocked")
                || lowerMessage.contains("suspended")
                || lowerMessage.contains("verify now")
                || lowerMessage.contains("last chance")
                || lowerMessage.contains("final warning")
                || lowerMessage.contains("don't ignore")
                || lowerMessage.contains("action required");

        // ── NEW PATTERN 1: OTP Share Request ──
        boolean hasOtpShareRequest = lowerMessage.contains("share your otp")
                || lowerMessage.contains("enter your otp")
                || lowerMessage.contains("give your otp")
                || lowerMessage.contains("provide otp")
                || lowerMessage.contains("send otp")
                || lowerMessage.contains("tell us your otp");

        // ── NEW PATTERN 2: Prize / Lottery Bait ──
        boolean hasPrizeBait = lowerMessage.contains("you have won")
                || lowerMessage.contains("you won")
                || lowerMessage.contains("claim your prize")
                || lowerMessage.contains("claim your reward")
                || lowerMessage.contains("free reward")
                || lowerMessage.contains("lottery")
                || lowerMessage.contains("lucky winner")
                || lowerMessage.contains("cash prize");

        // ── NEW PATTERN 3: Impersonation ──
        boolean hasImpersonation = !isTrustedSender && (
                lowerMessage.contains("from sbi")
                        || lowerMessage.contains("from hdfc")
                        || lowerMessage.contains("from axis bank")
                        || lowerMessage.contains("from icici")
                        || lowerMessage.contains("calling from bank")
                        || lowerMessage.contains("rbi official")
                        || lowerMessage.contains("income tax department")
                        || lowerMessage.contains("trai"));

        // ── NEW PATTERN 4: Personal Information Request ──
        boolean hasPersonalInfoRequest = lowerMessage.contains("aadhaar")
                || lowerMessage.contains("account number")
                || lowerMessage.contains(" cvv")
                || lowerMessage.contains("card number")
                || lowerMessage.contains("debit card")
                || lowerMessage.contains("credit card")
                || lowerMessage.contains("net banking password")
                || lowerMessage.contains("atm pin");

        // ── NEW PATTERN 5: Emotional Manipulation ──
        boolean hasEmotionalManipulation = lowerMessage.contains("your account will be closed")
                || lowerMessage.contains("arrest")
                || lowerMessage.contains("legal action")
                || lowerMessage.contains("police")
                || lowerMessage.contains("court notice")
                || lowerMessage.contains("penalty")
                || lowerMessage.contains("case filed");

        int rawScore = 0;
        StringBuilder reasons = new StringBuilder();
        boolean trustOverrideApplied = false;

        // ─────────────────────────────────────────────────
        // STEP 1 — FAST PATH: Obvious safe OTP from trusted sender
        // ─────────────────────────────────────────────────
        if (hasSafeContext && isTrustedSender && !hasSuspiciousLink) {
            String label = isOTP ? "SAFE_OTP" : "SAFE_SMS";
            int score = isOTP ? 5 : 0;
            return new RiskResult(
                    label, score,
                    "• Official OTP detected from a trusted sender.\n• No suspicious indicators found.",
                    "💡 Safe to use. This is a legitimate message.",
                    isOTP
            );
        }

        // ─────────────────────────────────────────────────
        // STEP 2 — AI Contribution (calibrated)
        // ─────────────────────────────────────────────────
        float effectiveAIProbability = lstmFraudProbability;
        if (isTrustedSender) {
            effectiveAIProbability = lstmFraudProbability * 0.3f;
            if (lstmFraudProbability > 0.5f) {
                reasons.append("• The AI noticed some patterns, but the sender is trusted so risk is reduced.\n");
                trustOverrideApplied = true;
            }
        }

        if (effectiveAIProbability > 0.70f) {
            rawScore += 50;
            reasons.append("• The AI found a high chance (")
                    .append(String.format("%.0f%%", lstmFraudProbability * 100))
                    .append(") that this message is fraudulent.\n");
        } else if (effectiveAIProbability > 0.40f) {
            rawScore += 25;
            reasons.append("• The AI found some suspicious characteristics in this message.\n");
        } else if (effectiveAIProbability > 0.20f) {
            rawScore += 10;
            reasons.append("• The AI noticed a slight irregularity in this message.\n");
        }

        // ─────────────────────────────────────────────────
        // STEP 3 — Sender Trust Adjustment
        // ─────────────────────────────────────────────────
        if (isTrustedSender) {
            rawScore -= 30;
            reasons.append("• This message came from a known, trusted source.\n");
            trustOverrideApplied = true;
        } else {
            rawScore += 15;
            reasons.append("• The sender is unknown and has not been verified.\n");
        }

        // ─────────────────────────────────────────────────
        // STEP 4 — URL / Link Analysis
        // ─────────────────────────────────────────────────
        if (hasSuspiciousLink) {
            rawScore += 55;
            reasons.append("• Contains a disguised link — scammers hide dangerous websites behind short links.\n");
        } else if (hasAnyLink && !hasTrustedDomain) {
            rawScore += 40;
            reasons.append("• Contains a link from an unrecognised website.\n");
        } else if (hasAnyLink && hasTrustedDomain) {
            rawScore -= 20;
            reasons.append("• The link points to a verified, official website.\n");
            trustOverrideApplied = true;
        }

        // ─────────────────────────────────────────────────
        // STEP 5 — Behavioral / Contextual Signals
        // ─────────────────────────────────────────────────
        if (hasUrgency && !isTrustedSender) {
            rawScore += 20;
            reasons.append("• Uses urgent or alarming words to pressure you into acting fast.\n");
        }
        if (hasEmotionalManipulation && !isTrustedSender) {
            rawScore += 25;
            reasons.append("• Uses threats like arrest, legal action, or account closure to create fear.\n");
        }

        if (isOTP) {
            if (!isTrustedSender && hasSuspiciousLink) {
                rawScore += 25;
                reasons.append("• Very dangerous: An OTP is requested alongside a suspicious hidden link.\n");
            } else if (!isTrustedSender) {
                rawScore += 10;
                reasons.append("• An OTP was sent from an unknown number — always double-check before using it.\n");
            } else if (isTrustedSender && !hasSafeContext) {
                reasons.append("• From a verified sender, but missing the usual 'Do not share' safety reminder.\n");
            }
        }

        if (hasOtpShareRequest) {
            rawScore += 30;
            reasons.append("• Asking you to share or give your OTP — no real company ever does this.\n");
        }

        if (hasPrizeBait) {
            rawScore += 20;
            reasons.append("• Promises a prize or reward — a common trick to make you click a link.\n");
        }

        if (hasImpersonation) {
            rawScore += 25;
            reasons.append("• Pretends to be from a bank or government body, but the sender is not verified.\n");
        }

        if (hasPersonalInfoRequest) {
            rawScore += 30;
            reasons.append("• Asks for sensitive details like card number, Aadhaar, or PIN — this is a red flag.\n");
        }

        // ─────────────────────────────────────────────────
        // STEP 6 — Clamp raw score to [0, 100]
        // ─────────────────────────────────────────────────
        rawScore = Math.max(0, Math.min(100, rawScore));

        // ─────────────────────────────────────────────────
        // STEP 7 — Label Assignment (based on raw score)
        // ─────────────────────────────────────────────────
        String label;
        String suggestion;

        if (rawScore >= 70) {
            label = hasSuspiciousLink ? "PHISHING" : "FRAUD_OTP";
            suggestion = "🚨 Do NOT click any links or share OTPs! This message is likely fraudulent.";
        } else if (rawScore >= 40) {
            label = "SUSPICIOUS";
            suggestion = "⚠️ Proceed with caution. Verify the sender independently.";
        } else {
            if (isOTP) {
                label = "SAFE_OTP";
                suggestion = "💡 Safe to use. Legitimate OTP detected.";
            } else {
                label = "SAFE_SMS";
                suggestion = "💡 Safe message. No threats detected.";
            }
        }

        // ─────────────────────────────────────────────────
        // STEP 8 — FINAL NORMALIZATION
        // ─────────────────────────────────────────────────
        rawScore = normalizeScoreForLabel(label, rawScore);

        // ─────────────────────────────────────────────────
        // STEP 9 — Confidence Correction Annotation
        // ─────────────────────────────────────────────────
        if (trustOverrideApplied && rawScore <= SAFE_OTP_MAX_SCORE) {
            reasons.append("✅ Risk was lowered because the sender is a verified, trusted source.\n");
        }

        if (reasons.length() == 0) {
            reasons.append("• No phishing patterns found.");
        }

        return new RiskResult(label, rawScore, reasons.toString().trim(), suggestion, isOTP);
    }

    // ─────────────────────────────────────────────────────────
    // NORMALIZATION — Enforces strict label ↔ score invariant
    // ─────────────────────────────────────────────────────────
    private static int normalizeScoreForLabel(String label, int rawScore) {
        switch (label) {
            case "SAFE_SMS":
                return Math.min(rawScore, SAFE_SMS_MAX_SCORE);
            case "SAFE_OTP":
                return Math.min(rawScore, SAFE_OTP_MAX_SCORE);
            case "SUSPICIOUS":
                return Math.max(SUSPICIOUS_MIN_SCORE, Math.min(rawScore, SUSPICIOUS_MAX_SCORE));
            case "PHISHING":
                return Math.max(PHISHING_MIN_SCORE, rawScore);
            case "FRAUD_OTP":
                return Math.max(FRAUD_MIN_SCORE, rawScore);
            case "HIGH_RISK":
                return Math.max(HIGH_RISK_MIN_SCORE, rawScore);
            default:
                return rawScore;
        }
    }

    // ═══════════════════════════════════════════════════════
    // RiskResult — Immutable output of the analysis pipeline
    // ═══════════════════════════════════════════════════════
    public static class RiskResult {
        public final String label;
        public final int riskScore;
        public final String reasons;
        public final String suggestion;
        public final boolean isOtp;

        public RiskResult(String label, int riskScore, String reasons, String suggestion, boolean isOtp) {
            this.label = label;
            this.riskScore = riskScore;
            this.reasons = reasons;
            this.suggestion = suggestion;
            this.isOtp = isOtp;
        }

        @Override
        public String toString() {
            return "RiskResult{" +
                    "label='" + label + '\'' +
                    ", riskScore=" + riskScore +
                    ", isOtp=" + isOtp +
                    ", suggestion='" + suggestion + '\'' +
                    '}';
        }
    }
}