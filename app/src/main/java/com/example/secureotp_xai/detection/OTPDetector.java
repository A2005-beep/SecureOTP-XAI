package com.example.secureotp_xai.detection;

import java.util.ArrayList;
import java.util.List;

public class OTPDetector {

    private static List<Long> otpTimestamps = new ArrayList<>();

    // 🔐 Detect OTP using regex
    public static boolean isOTP(String message) {
        return message != null && message.matches(".*\\b\\d{4,6}\\b.*");
    }

    // 🔥 Track OTP frequency
    public static void updateOTPCount(long timestamp) {

        otpTimestamps.add(timestamp);

        // Remove old entries (older than 60 seconds)
        long current = System.currentTimeMillis();

        otpTimestamps.removeIf(time -> (current - time) > 60000);
    }

    // 📊 Get current OTP count
    public static int getOtpCount() {
        return otpTimestamps.size();
    }
}