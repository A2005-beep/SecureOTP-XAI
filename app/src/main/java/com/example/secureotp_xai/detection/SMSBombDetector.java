package com.example.secureotp_xai.detection;

import android.util.Log;
import java.util.LinkedList;
import java.util.Queue;

public class SMSBombDetector {
    private static final String TAG = "SMSBombDetector";
    private static final int BOMB_THRESHOLD = 10; // Trigger on the 11th message
    private static final long TIME_WINDOW_MS = 60 * 1000; // 1 minute window

    private static final Queue<Long> messageTimestamps = new LinkedList<>();

    public static synchronized boolean recordAndCheck(long currentTime) {
        try {
            messageTimestamps.add(currentTime);

            // Clean up old messages
            while (!messageTimestamps.isEmpty() &&
                    (currentTime - messageTimestamps.peek() > TIME_WINDOW_MS)) {
                messageTimestamps.poll();
            }

            int count = messageTimestamps.size();
            Log.d(TAG, "Current SMS Count in 60s: " + count);

            if (count > BOMB_THRESHOLD) {
                Log.w(TAG, "⚠️ BOMB ATTACK DETECTED! Count: " + count);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Detector Error", e);
        }
        return false;
    }

    public static synchronized void reset() {
        messageTimestamps.clear();
    }
}
