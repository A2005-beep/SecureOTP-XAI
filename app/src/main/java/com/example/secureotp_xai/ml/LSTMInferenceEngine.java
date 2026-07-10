package com.example.secureotp_xai.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * LSTMInferenceEngine
 *
 * Upgraded to expose raw probabilities for the Hybrid Decision Engine (RiskAnalyzer).
 */
public class LSTMInferenceEngine {

    private static final String TAG            = "LSTMEngine";
    private static final String MODEL_QUANT    = "otp_lstm_quant.tflite";
    private static final String MODEL_FLOAT    = "otp_lstm_model.tflite";

    // Classification threshold (probability > THRESHOLD → FRAUD)
    private static final float FRAUD_THRESHOLD  = 0.35f;
    private static final float HIGH_CONF        = 0.80f;
    private static final float LOW_CONF         = 0.60f;

    private Interpreter         tflite;
    private LSTMPreprocessor    preprocessor; // Assumes you have this class
    private boolean             ready = false;
    private int                 maxLen;

    // ─────────────────────────────────────────────────────────────────────────

    public LSTMInferenceEngine(Context context) {
        preprocessor = new LSTMPreprocessor(context);
        maxLen       = preprocessor.getMaxLen();

        // Try quantized first, fall back to float32
        if (!loadModel(context, MODEL_QUANT)) {
            Log.w(TAG, "Quantized model not found, trying float32...");
            loadModel(context, MODEL_FLOAT);
        }
    }

    // ─── Model loading ────────────────────────────────────────────────────────

    private boolean loadModel(Context context, String assetName) {
        try {
            MappedByteBuffer buffer = loadModelBuffer(context, assetName);

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2);
            options.setUseXNNPACK(true);

            tflite = new Interpreter(buffer, options);

            ready = true;

            Log.d(TAG,"Model loaded: " + assetName);

            return true;

        } catch (Exception e) {

            Log.e(TAG,"Model load failed: " + e.getMessage(), e);

            ready = false;

            return false;
        }
    }
    private MappedByteBuffer loadModelBuffer(Context context, String assetName)
            throws IOException {
        AssetFileDescriptor afd = context.getAssets().openFd(assetName);
        FileInputStream fis     = new FileInputStream(afd.getFileDescriptor());
        FileChannel channel     = fis.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY,
                afd.getStartOffset(), afd.getDeclaredLength());
    }
    // ─── NEW HYBRID LOGIC METHOD ──────────────────────────────────────────────
    /**
     * Extracts ONLY the raw fraud probability from the LSTM model.
     * This is used by the new Hybrid Decision Engine (RiskAnalyzer)
     * so it can combine the AI score with Whitelist/Trust checks.
     */
    public float predictFraudProbability(String smsText) {
        if (!ready || !preprocessor.isLoaded() || smsText == null || smsText.trim().isEmpty()) {
            return 0.50f; // Neutral fallback if model isn't ready
        }

        try {
            // Step 1: Preprocess (must match Python pipeline)
            float[] sequence = preprocessor.preprocess(smsText);

            // Step 2: Reshape to [1, MAX_LEN] (model expects batch dimension)
            float[][] input  = new float[1][maxLen];
            input[0]         = sequence;

            // Step 3: Output buffer — model outputs sigmoid scalar [1, 1]
            float[][] output = new float[1][1];

            // Step 4: Run inference
            tflite.run(input, output);

            return output[0][0];

        } catch (Exception e) {
            Log.e(TAG, "Inference probability error: " + e.getMessage());
            return 0.50f; // Neutral fallback on error
        }
    }

    // ─── Inference (Legacy/Standard) ──────────────────────────────────────────

    /**
     * Full pipeline: text → preprocess → TFLite → Result
     *
     * @param smsText Raw SMS message string
     * @return        Result with label, confidence, and explanations
     */
    public Result predict(String smsText) {
        if (!ready || !preprocessor.isLoaded()) {
            return fallbackResult(smsText);
        }

        try {
            // Re-use our new method to get the raw float
            float fraudProb = predictFraudProbability(smsText);
            float safeProb  = 1.0f - fraudProb;

            // Step 5: Classify
            boolean isFraud  = fraudProb >= FRAUD_THRESHOLD;
            String  label    = isFraud ? "FRAUD OTP" : "SAFE OTP";
            float   confidence = isFraud ? fraudProb : safeProb;

            // Step 6: Generate XAI reasons
            List<String> reasons = buildReasons(
                    smsText, isFraud, fraudProb, preprocessor.cleanText(smsText)
            );

            return new Result(label, confidence, fraudProb, reasons, true);

        } catch (Exception e) {
            Log.e(TAG, "Inference error: " + e.getMessage());
            return fallbackResult(smsText);
        }
    }

    // ─── XAI Reason Builder ───────────────────────────────────────────────────
    // Provides human-readable explanations by inspecting the raw text for
    // features that the LSTM would have weighted heavily (approximates SHAP).

    private List<String> buildReasons(String raw, boolean isFraud,
                                      float fraudProb, String cleaned) {
        List<String> reasons = new ArrayList<>();
        String lower = raw.toLowerCase();

        if (isFraud) {
            if (lower.contains("http") || lower.contains("bit.ly")
                    || lower.contains("tinyurl") || lower.contains("www.")) {
                reasons.add("Suspicious URL detected — phishing signal");
            }
            if (lower.matches(".*\\b(urgent|immediately|right now|tonight|expire)\\b.*")) {
                reasons.add("Urgency language detected — social engineering pattern");
            }
            if (lower.matches(".*\\b(won|winner|prize|congratulations|lucky|reward|gift|free)\\b.*")) {
                reasons.add("Prize/reward language — common fraud tactic");
            }
            if (lower.matches(".*\\b(share|disclose|tell|give).*(otp|code|pin|password)\\b.*")) {
                reasons.add("Request to share OTP — legitimate services never ask this");
            }
            if (lower.matches(".*\\b(block|suspend|disconnect|deactivate|cancel)\\b.*")) {
                reasons.add("Threat of service disruption — scare tactic");
            }
            if (lower.matches(".*\\+?[0-9][\\d\\-\\s]{8,}.*")) {
                reasons.add("Unknown phone number embedded in message");
            }
            if (fraudProb > HIGH_CONF) {
                reasons.add(String.format("LSTM model: %.0f%% fraud probability", fraudProb * 100));
            }
            if (reasons.isEmpty()) {
                reasons.add("Unusual OTP context detected by AI model");
                reasons.add(String.format("Model confidence: %.0f%%", fraudProb * 100));
            }
        } else {
            if (lower.matches(".*\\b(hdfc|sbi|icici|axis|kotak|paytm|phonepe|gpay)\\b.*")) {
                reasons.add("Recognized trusted sender pattern");
            }
            if (lower.contains("do not share") || lower.contains("never share")) {
                reasons.add("Contains legitimate privacy warning");
            }
            if (lower.matches(".*valid for \\d+.*")) {
                reasons.add("Standard OTP expiry notice present");
            }
            if (reasons.isEmpty()) {
                reasons.add("Message matches legitimate OTP pattern");
                reasons.add(String.format("Model confidence: %.0f%%", (1f - fraudProb) * 100));
            }
        }

        return reasons;
    }

    // ─── Fallback (if model fails to load) ───────────────────────────────────

    private Result fallbackResult(String smsText) {
        Log.w(TAG, "Using rule-based fallback (model not ready)");
        String lower = smsText.toLowerCase();
        boolean suspect = lower.contains("http") || lower.contains("bit.ly")
                || lower.contains("urgent") || lower.contains("prize")
                || lower.contains("won") || lower.contains("click");

        List<String> reasons = new ArrayList<>();
        reasons.add("Rule-based analysis (AI model unavailable)");
        if (suspect) reasons.add("Suspicious keywords detected");

        return new Result(
                suspect ? "FRAUD OTP" : "SAFE OTP",
                0.60f, suspect ? 0.60f : 0.40f,
                reasons, false
        );
    }
    // ─── Cleanup ─────────────────────────────────────────────────────────────
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
    public boolean isReady() { return ready; }
    //  Result class
    public static class Result {
        /** "SAFE OTP"  or  "FRAUD OTP" */
        public final String       label;

        /** Confidence in the predicted label: 0.0 – 1.0 */
        public final float        confidence;

        /** Raw sigmoid output (probability this is fraud): 0.0 – 1.0 */
        public final float        fraudProbability;

        /** Human-readable explanation of the decision */
        public final List<String> reasons;

        /** True if inference came from LSTM, false if rule-based fallback */
        public final boolean      usedLSTM;

        Result(String label, float confidence, float fraudProbability,
               List<String> reasons, boolean usedLSTM) {
            this.label            = label;
            this.confidence       = confidence;
            this.fraudProbability = fraudProbability;
            this.reasons          = reasons;
            this.usedLSTM         = usedLSTM;
        }

        public boolean isFraud()    { return "FRAUD OTP".equals(label); }
        public int     confidencePct() { return Math.round(confidence * 100); }

        /** Confidence tier for UI coloring */
        public String confidenceTier() {
            if (confidence >= 0.85f) return "HIGH";
            if (confidence >= 0.65f) return "MEDIUM";
            return "LOW";
        }
    }
}
