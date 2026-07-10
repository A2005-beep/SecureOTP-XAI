package com.example.secureotp_xai.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secureotp_xai.R;
import com.example.secureotp_xai.model.SMSRiskItem;

import java.util.List;

/**
 * RecyclerView Adapter for SMSAnalysisActivity.
 * Displays each SMS threat analysis as a color-coded card.
 */
public class SMSRiskAdapter extends RecyclerView.Adapter<SMSRiskAdapter.ViewHolder> {

    private final Context context;
    private final List<SMSRiskItem> items;

    public SMSRiskAdapter(Context context, List<SMSRiskItem> items) {
        this.context = context;
        this.items   = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_sms_risk, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SMSRiskItem item = items.get(position);

        // ── Label with emoji ───────────────────────────────────────────────
        String labelEmoji;
        int labelColor;
        int cardColor;
        int dividerColor;

        // NEW: Fixed labels to exactly match RiskAnalyzer & Modernized Dark Theme Colors
        switch (item.getLabel()) {
            case "FRAUD OTP":
            case "HIGH RISK":
                labelEmoji   = "🔴 HIGH RISK OTP";
                labelColor   = Color.parseColor("#FF3333"); // Neon Red
                cardColor    = Color.parseColor("#1A0505"); // Deep dark red background
                dividerColor = Color.parseColor("#FF3333");
                break;
            case "PHISHING":
                labelEmoji   = "🟠 PHISHING LINK";
                labelColor   = Color.parseColor("#FF8C00"); // Neon Orange
                cardColor    = Color.parseColor("#1A0E05"); // Deep dark orange background
                dividerColor = Color.parseColor("#FF8C00");
                break;
            case "SUSPICIOUS OTP":
            case "SPAM":
                labelEmoji   = "🟡 SPAM / SUSPICIOUS";
                labelColor   = Color.parseColor("#FFCC00"); // Neon Yellow
                cardColor    = Color.parseColor("#1A1705"); // Deep dark yellow background
                dividerColor = Color.parseColor("#FFCC00");
                break;
            case "SAFE OTP":
            case "SAFE SMS":
            default: // SAFE
                labelEmoji   = "🟢 SAFE";
                labelColor   = Color.parseColor("#00FF66"); // Neon Green
                cardColor    = Color.parseColor("#051A0A"); // Deep dark green background
                dividerColor = Color.parseColor("#00FF66");
                break;
        }

        holder.cardView.setCardBackgroundColor(cardColor);
        holder.divider.setBackgroundColor(dividerColor);
        holder.tvLabel.setText(labelEmoji);
        holder.tvLabel.setTextColor(labelColor);

        // ── Sender & message ───────────────────────────────────────────────
        holder.tvSender.setText("From: " + item.getSender());
        holder.tvSender.setTextColor(Color.parseColor("#E0E0E0")); // Light text for dark theme

        holder.tvMessage.setText(item.getMessage());
        holder.tvMessage.setTextColor(Color.parseColor("#B0B0B0")); // Slightly dimmer for body

        holder.tvSuggestion.setText("💡 " + item.getSuggestion());
        holder.tvSuggestion.setTextColor(Color.parseColor("#FFFFFF")); // Bright text for suggestion

        // ── Reasons ────────────────────────────────────────────────────────
        StringBuilder reasons = new StringBuilder();
        for (String reason : item.getReasons()) {
            reasons.append("• ").append(reason).append("\n");
        }
        holder.tvReasons.setText(reasons.toString().trim());
        holder.tvReasons.setTextColor(Color.parseColor("#A0A0A0"));

        // ── Threat score ───────────────────────────────────────────────────
        holder.tvThreatScore.setText("Threat Score: " + item.getThreatScore());
        holder.tvThreatScore.setTextColor(labelColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ─── ViewHolder ────────────────────────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        View     divider;
        TextView tvLabel, tvSender, tvMessage,
                tvReasons, tvSuggestion, tvThreatScore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView       = itemView.findViewById(R.id.cardSmsRisk);
            divider        = itemView.findViewById(R.id.viewSmsDivider);
            tvLabel        = itemView.findViewById(R.id.tvSmsLabel);
            tvSender       = itemView.findViewById(R.id.tvSmsSender);
            tvMessage      = itemView.findViewById(R.id.tvSmsMessage);
            tvReasons      = itemView.findViewById(R.id.tvSmsReasons);
            tvSuggestion   = itemView.findViewById(R.id.tvSmsSuggestion);
            tvThreatScore  = itemView.findViewById(R.id.tvSmsThreatScore);
        }
    }
}
