package com.example.secureotp_xai.ml;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * LSTMPreprocessor
 *
 * Mirrors the Python preprocessing pipeline EXACTLY so that text fed to
 * the TFLite model at inference time is identical to training-time input.
 *
 * Pipeline (must match train_lstm.py  clean_text()):
 *   1. Lowercase
 *   2. Replace URLs       → "<url>"
 *   3. Replace phone nos  → "<phone>"
 *   4. Replace OTP digits → "<otp>"
 *   5. Remove punctuation
 *   6. Tokenize on whitespace
 *   7. Map tokens to integer indices via tokenizer_config.json
 *   8. Pad / truncate to MAX_LEN
 *
 * Usage:
 *   LSTMPreprocessor prep = new LSTMPreprocessor(context);
 *   float[] input = prep.preprocess("Your OTP is 334421. Do not share.");
 */
public class LSTMPreprocessor {

    private static final String TAG     = "LSTMPreprocessor";
    private static final String ASSET   = "tokenizer_config.json";

    // ─── Regex patterns (must match Python) ──────────────────────────────────
    private static final Pattern RE_URL   = Pattern.compile(
            "(https?://|www\\.)\\S+|(bit\\.ly|tinyurl|goo\\.gl|ow\\.ly)\\S*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RE_PHONE = Pattern.compile(
            "\\+?[0-9][\\d\\-\\s]{8,}");

    private static final Pattern RE_OTP   = Pattern.compile("\\b\\d{4,8}\\b");

    private static final Pattern RE_PUNCT = Pattern.compile("[^\\w\\s]");

    private static final Pattern RE_SPACE = Pattern.compile("\\s+");

    // ─── Tokenizer vocabulary (loaded from assets) ────────────────────────────
    private Map<String, Integer> wordIndex  = new HashMap<>();
    private int vocabSize   = 5000;
    private int maxLen      = 64;
    private int oovIndex    = 1;  // index of <OOV> token

    private boolean loaded  = false;

    // ─────────────────────────────────────────────────────────────────────────

    public LSTMPreprocessor(Context context) {
        loadTokenizer(context);
    }

    // ─── Load tokenizer_config.json from assets/ ─────────────────────────────

    private void loadTokenizer(Context context) {
        try {
            InputStream is  = context.getAssets().open(ASSET);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject root  = new JSONObject(sb.toString());
            vocabSize        = root.optInt("vocab_size", 5000);
            maxLen           = root.optInt("max_len",    64);

            JSONObject wi    = root.getJSONObject("word_index");
            Iterator<String> keys = wi.keys();
            while (keys.hasNext()) {
                String word = keys.next();
                int    idx  = wi.getInt(word);
                wordIndex.put(word, idx);
            }

            // OOV token index
            oovIndex = wordIndex.containsKey("<OOV>")
                    ? wordIndex.get("<OOV>")
                    : 1;

            loaded = true;
            Log.d(TAG, "Tokenizer loaded: vocab=" + wordIndex.size()
                    + "  maxLen=" + maxLen);

        } catch (Exception e) {
            Log.e(TAG, "Failed to load tokenizer: " + e.getMessage());
            loaded = false;
        }
    }

    // ─── Main preprocessing entry point ──────────────────────────────────────

    /**
     * Converts an SMS string into a float[] of shape [1, MAX_LEN]
     * ready to be fed directly into the TFLite model.
     *
     * @param sms Raw SMS message text
     * @return    float[MAX_LEN] with token indices (0-padded)
     */
    public float[] preprocess(String sms) {
        String cleaned  = cleanText(sms);
        int[]  tokens   = tokenize(cleaned);
        return padSequence(tokens);
    }

    // ─── Step 1: Text cleaning ────────────────────────────────────────────────

    /**
     * Must produce identical output to Python  clean_text()  in train_lstm.py
     */
    String cleanText(String text) {
        text = text.toLowerCase();
        text = RE_URL.matcher(text).replaceAll(" <url> ");
        text = RE_PHONE.matcher(text).replaceAll(" <phone> ");
        text = RE_OTP.matcher(text).replaceAll(" <otp> ");
        text = RE_PUNCT.matcher(text).replaceAll(" ");
        text = RE_SPACE.matcher(text).replaceAll(" ").trim();
        return text;
    }

    // ─── Step 2: Tokenize (word → index) ─────────────────────────────────────

    private int[] tokenize(String cleanedText) {
        String[] words = cleanedText.split("\\s+");
        int[] indices  = new int[words.length];

        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (wordIndex.containsKey(w)) {
                int idx = wordIndex.get(w);
                // Clamp to vocab size (matches Keras num_words behavior)
                indices[i] = (idx < vocabSize) ? idx : oovIndex;
            } else {
                indices[i] = oovIndex;
            }
        }
        return indices;
    }

    // ─── Step 3: Pad / truncate to maxLen ────────────────────────────────────

    private float[] padSequence(int[] tokens) {
        float[] padded = new float[maxLen]; // zero-initialized = post-padding

        int copyLen = Math.min(tokens.length, maxLen);
        for (int i = 0; i < copyLen; i++) {
            padded[i] = tokens[i];
        }
        // Truncation: if tokens.length > maxLen, we just take first maxLen
        return padded;
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public int  getMaxLen()    { return maxLen;  }
    public boolean isLoaded()  { return loaded;  }
}