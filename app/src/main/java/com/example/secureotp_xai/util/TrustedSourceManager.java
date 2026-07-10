package com.example.secureotp_xai.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class TrustedSourceManager {

    // Whitelist of trusted banking and application senders
    private static final Set<String> TRUSTED_SENDERS = new HashSet<>(Arrays.asList(
            "HDFCBK", "ICICIB", "SBIINB", "AXISBK", "KOTAKB",
            "PAYTM", "PHONEPE", "GPAY", "AMAZON", "FLPKRT", "INSTAGRAM", "FACEBK"
    ));

    // Expanded Whitelist of known official domains
    private static final Set<String> TRUSTED_DOMAINS = new HashSet<>(Arrays.asList(
            "instagram.com", "amazon.in", "amazon.com", "paytm.com", "google.com",
            "facebook.com", "hdfcbank.com", "sbi.co.in", "pinterest.com", "pintrest.com",
            "youtube.com", "youtu.be", "twitter.com", "x.com", "linkedin.com",
            "flipkart.com", "myntra.com", "zomato.com", "swiggy.com", "netflix.com",
            "gnits.ac.in", "jntuh.ac.in", "osmania.ac.in"
    ));
    public static boolean isAcademicOrGov(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains(".ac.in") || lower.contains(".edu") || lower.contains(".gov");
    }

    // Regex for suspicious link shorteners and IP addresses
    private static final Pattern SUSPICIOUS_DOMAINS = Pattern.compile(".*(bit\\.ly|tinyurl\\.com|t\\.co|ngrok\\.io|cutt\\.ly).*");
    private static final Pattern IP_ADDRESS_URL = Pattern.compile(".*\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b.*");

    // NEW: Generic URL Detector (Catches ANY link)
    private static final Pattern GENERIC_URL = Pattern.compile("(https?://|www\\.)[^\\s]+", Pattern.CASE_INSENSITIVE);

    public static boolean isTrustedSender(String sender) {
        if (sender == null) return false;
        String normalized = sender.toUpperCase().replaceAll("[^A-Z]", "");
        for (String trusted : TRUSTED_SENDERS) {
            if (normalized.contains(trusted)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsTrustedDomain(String message) {
        if (message == null) return false;
        String lowerMsg = message.toLowerCase();
        for (String domain : TRUSTED_DOMAINS) {
            if (lowerMsg.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsSuspiciousLink(String message) {
        if (message == null) return false;
        String lowerMsg = message.toLowerCase();
        if (SUSPICIOUS_DOMAINS.matcher(lowerMsg).matches()) return true;
        if (IP_ADDRESS_URL.matcher(lowerMsg).matches()) return true;
        return false;
    }

    // NEW: Detects if the message has ANY link at all
    public static boolean containsAnyLink(String message) {
        if (message == null) return false;
        return GENERIC_URL.matcher(message).find();
    }
}
