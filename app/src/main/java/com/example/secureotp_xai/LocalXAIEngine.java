package com.example.secureotp_xai;
import com.example.secureotp_xai.analyzer.RiskAnalyzer;
/**
 * LocalXAIEngine — Explainable AI (XAI) Module
 *
 * Generates plain-language explanations, a confidence score, and
 * threat tags entirely on-device. No internet required.
 *
 * Design Rule: Every explanation must be understandable by someone
 * who has never heard the word "phishing" before.
 */
public class LocalXAIEngine {
    // ─────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────
    /**
     * Generates a full XAI result from the analysis pipeline output.
     *
     * @param result          The RiskResult produced by RiskAnalyzer
     * @param sender          The SMS sender address
     * @param body            The SMS message body
     * @param lstmProbability Raw LSTM fraud probability [0.0 – 1.0]
     * @param isBomb          Whether this was flagged as an SMS bomb attack
     * @return XAIResult containing explanation, confidence, and tags
     */
    public static XAIResult explain(
            RiskAnalyzer.RiskResult result,
            String sender,
            String body,
            float lstmProbability,
            boolean isBomb) {
        String lowerBody = body == null ? "" : body.toLowerCase();
        // Detect features used for explanation building
        boolean hasLink      = lowerBody.contains("http") || lowerBody.contains("www.");
        boolean hasBitly     = lowerBody.contains("bit.ly") || lowerBody.contains("tinyurl")
                || lowerBody.contains("cutt.ly");
        boolean hasUrgency   = lowerBody.contains("urgent") || lowerBody.contains("immediately")
                || lowerBody.contains("blocked") || lowerBody.contains("suspended")
                || lowerBody.contains("verify now") || lowerBody.contains("last chance")
                || lowerBody.contains("final warning");
        boolean hasOtp       = lowerBody.contains("otp") || lowerBody.contains("one time")
                || lowerBody.contains("verification code");
        boolean hasPrize     = lowerBody.contains("won") || lowerBody.contains("prize")
                || lowerBody.contains("reward") || lowerBody.contains("lottery")
                || lowerBody.contains("congratulations") || lowerBody.contains("claim");
        boolean hasShareOtp  = lowerBody.contains("share your otp") || lowerBody.contains("enter your otp")
                || lowerBody.contains("give your otp") || lowerBody.contains("provide otp");
        boolean hasPersonal  = lowerBody.contains("aadhaar") || lowerBody.contains("account number")
                || lowerBody.contains("cvv") || lowerBody.contains("card number")
                || lowerBody.contains("password") || lowerBody.contains("pin");
        boolean hasImpersonation = (lowerBody.contains("from sbi") || lowerBody.contains("from hdfc")
                || lowerBody.contains("from axis") || lowerBody.contains("from icici")
                || lowerBody.contains("calling from"));
        int confidence = computeConfidence(result.riskScore, lstmProbability);
        String tags    = buildTags(result.label, hasLink, hasBitly, hasUrgency,
                hasOtp, hasPrize, hasPersonal, isBomb);
        String explanation = buildExplanation(
                result, sender, lstmProbability,
                isBomb, hasLink, hasBitly, hasUrgency,
                hasOtp, hasPrize, hasShareOtp, hasPersonal, hasImpersonation,
                confidence
        );
        return new XAIResult(explanation, confidence, tags);
    }
    // ─────────────────────────────────────────────────────────
    // EXPLANATION BUILDER — plain language, per alert type
    // ─────────────────────────────────────────────────────────
    private static String buildExplanation(
            RiskAnalyzer.RiskResult result,
            String sender,
            float lstmProbability,
            boolean isBomb,
            boolean hasLink,
            boolean hasBitly,
            boolean hasUrgency,
            boolean hasOtp,
            boolean hasPrize,
            boolean hasShareOtp,
            boolean hasPersonal,
            boolean hasImpersonation,
            int confidence) {
        // ── SMS BOMB ──────────────────────────────────────────
        if (isBomb) {
            return "⚠️ Your phone is being flooded with many messages at once. "
                    + "This is called an SMS Bomb attack — someone is sending you "
                    + "lots of messages quickly, possibly to hide an important alert "
                    + "(like a bank transaction you didn't approve) in the noise. "
                    + "Do not click on anything. Check your bank account directly from your bank's official app.";
        }
        switch (result.label) {
            // ── FRAUD OTP ─────────────────────────────────────
            case "FRAUD_OTP": {
                StringBuilder sb = new StringBuilder();
                sb.append("🚨 This message looks like a scam. ");
                if (hasShareOtp) {
                    sb.append("It is asking you to share your OTP — no real bank or company will ever ask for your OTP. ");
                }
                if (hasBitly) {
                    sb.append("It also contains a short link (like bit.ly) which scammers use to hide dangerous websites. ");
                } else if (hasLink) {
                    sb.append("It contains a link from an unknown website. ");
                }
                if (hasUrgency) {
                    sb.append("The message uses scary or urgent words to rush you into acting without thinking. ");
                }
                sb.append("Our AI checked this message and found it to be ")
                        .append(confidence).append("% likely to be fraud. ")
                        .append("\n\n✅ What to do: Delete this message. Do not call the number, click the link, or share your OTP.");
                return sb.toString();
            }
            // ── PHISHING ──────────────────────────────────────
            case "PHISHING": {
                StringBuilder sb = new StringBuilder();
                sb.append("🎣 This message is trying to trick you into clicking a dangerous link. ");
                if (hasBitly) {
                    sb.append("It uses a disguised link (bit.ly or similar) to hide where it will actually take you. "
                            + "Real banks and companies always use their official website address. ");
                } else if (hasLink) {
                    sb.append("The link in this message does not belong to any trusted company we recognise. ");
                }
                if (hasImpersonation) {
                    sb.append("The message pretends to be from a bank or company, but the sender is not verified. ");
                }
                if (hasPrize) {
                    sb.append("It promises a prize or reward to lure you in — this is a classic trick. ");
                }
                sb.append("Our AI is ").append(confidence).append("% confident this is a phishing attempt.")
                        .append("\n\n✅ What to do: Do not click any links. Block the sender if possible.");
                return sb.toString();
            }
            // ── SUSPICIOUS ────────────────────────────────────
            case "SUSPICIOUS": {
                StringBuilder sb = new StringBuilder();
                sb.append("⚠️ This message looks a bit unusual. We are not 100% sure it is a scam, but there are some warning signs. ");
                if (hasUrgency) {
                    sb.append("It uses urgent or alarming words like 'blocked' or 'verify now' to make you act quickly. ");
                }
                if (hasPersonal) {
                    sb.append("It seems to be asking for personal details like your Aadhaar, account number, or card info. "
                            + "No legitimate company needs this over SMS. ");
                }
                if (hasLink && !hasBitly) {
                    sb.append("It contains a link from an unrecognised website. ");
                }
                if (hasPrize) {
                    sb.append("It mentions winning a prize or reward, which is often used to trick people. ");
                }
                sb.append("Our AI gave this message a suspicion score of ").append(result.riskScore).append("/100.")
                        .append("\n\n✅ What to do: Do not reply or click anything. "
                                + "If it claims to be from your bank, open your bank's official app to verify.");
                return sb.toString();
            }
            // ── SAFE OTP ──────────────────────────────────────
            case "SAFE_OTP": {
                int lstmPercent = Math.round(lstmProbability * 100);
                return "✅ This looks like a genuine OTP message. "
                        + "The sender appears to be a trusted source, and the message follows the "
                        + "standard format used by real banks and services. "
                        + "Our AI checked the message and found only " + lstmPercent + "% chance of fraud. "
                        + "\n\nReminder: Never share your OTP with anyone — not even someone claiming to be from your bank.";
            }
            // ── SAFE SMS ──────────────────────────────────────
            case "SAFE_SMS": {
                return "✅ This message appears to be safe. "
                        + "We found no signs of fraud, suspicious links, or unusual requests. "
                        + "It looks like a normal message from a legitimate source. "
                        + "\n\nStay safe: Always be careful before clicking links or sharing personal information.";
            }
            // ── SMS BOMB (fallback label) ─────────────────────
            case "SMS_BOMB": {
                return "⚠️ Your phone received many messages in a very short time. "
                        + "This kind of attack is meant to confuse you and distract you "
                        + "from a real fraud happening on your account. "
                        + "\n\n✅ What to do: Ignore these messages. Open your bank app directly and check for any transactions you did not make.";
            }
            // ── DEFAULT FALLBACK ──────────────────────────────
            default: {
                return "ℹ️ This message was scanned and no serious threats were found. "
                        + "As always, never share your OTP, password, or personal details with anyone over SMS.";
            }
        }
    }
    // ─────────────────────────────────────────────────────────
    // CONFIDENCE SCORE — mathematically derived, not hardcoded
    // ─────────────────────────────────────────────────────────
    /**
     * Blends the rule-based riskScore with the LSTM probability
     * to produce a single 0–100 confidence value.
     *
     * Weight: 60% rule score + 40% LSTM probability.
     */
    private static int computeConfidence(int riskScore, float lstmProbability) {
        float lstmPercent = lstmProbability * 100f;
        float blended = (riskScore * 0.6f) + (lstmPercent * 0.4f);
        return Math.min(100, Math.max(0, Math.round(blended)));
    }
    // ─────────────────────────────────────────────────────────
    // THREAT TAGS — for quick-scan badges in the UI
    // ─────────────────────────────────────────────────────────
    private static String buildTags(
            String label,
            boolean hasLink,
            boolean hasBitly,
            boolean hasUrgency,
            boolean hasOtp,
            boolean hasPrize,
            boolean hasPersonal,
            boolean isBomb) {
        StringBuilder tags = new StringBuilder();
        if (isBomb)      appendTag(tags, "SMS BOMB");
        if (hasBitly)    appendTag(tags, "HIDDEN LINK");
        else if (hasLink) appendTag(tags, "UNKNOWN LINK");
        if (hasUrgency)  appendTag(tags, "URGENCY TACTIC");
        if (hasOtp)      appendTag(tags, "OTP REQUEST");
        if (hasPrize)    appendTag(tags, "PRIZE BAIT");
        if (hasPersonal) appendTag(tags, "DATA THEFT");
        // Always include the verdict as a tag
        switch (label) {
            case "FRAUD_OTP":  appendTag(tags, "FRAUD"); break;
            case "PHISHING":   appendTag(tags, "PHISHING"); break;
            case "SUSPICIOUS": appendTag(tags, "SUSPICIOUS"); break;
            case "SAFE_OTP":   appendTag(tags, "SAFE OTP"); break;
            case "SAFE_SMS":   appendTag(tags, "SAFE"); break;
            case "SMS_BOMB":   appendTag(tags, "ATTACK"); break;
        }
        return tags.length() > 0 ? tags.toString() : "SCANNED";
    }
    private static void appendTag(StringBuilder sb, String tag) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(tag);
    }
    // ─────────────────────────────────────────────────────────
    // RESULT WRAPPER
    // ─────────────────────────────────────────────────────────
    public static class XAIResult {
        public final String explanation;
        public final int    confidence;
        public final String tags;
        public XAIResult(String explanation, int confidence, String tags) {
            this.explanation = explanation;
            this.confidence  = confidence;
            this.tags        = tags;
        }
    }
}
