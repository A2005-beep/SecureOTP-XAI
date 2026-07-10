package com.example.secureotp_xai.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secureotp_xai.R;
import com.example.secureotp_xai.model.OTPAnalysisItem;

import java.util.List;

public class OTPResultAdapter extends RecyclerView.Adapter<OTPResultAdapter.ViewHolder> {

    private Context context;
    private List<OTPAnalysisItem> list;

    public OTPResultAdapter(Context context, List<OTPAnalysisItem> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_otp_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OTPAnalysisItem item = list.get(position);

        // Standardize the label to Uppercase to prevent spelling mismatches
        String label = (item.getLabel() != null) ? item.getLabel().toUpperCase() : "SAFE OTP";

        holder.tvLabel.setVisibility(View.VISIBLE);
        holder.tvLabel.setText(label);
        holder.tvSender.setText("From: " + item.getSender());
        holder.tvMessage.setText(item.getMessage());
        holder.tvSource.setText("Source: " + item.getSourceType());
        holder.tvSuggestion.setText("💡 " + item.getSuggestion());
        holder.tvRisk.setText("Risk Score: " + item.getRiskScore() + "%");

        // 1. 🔍 SET LOCAL XAI REASONS
        if (holder.tvReasons != null) {
            String reasons = item.getReasons();
            holder.tvReasons.setText(reasons != null ? reasons : "No threats detected.");
            holder.tvReasons.setTextColor(Color.parseColor("#FF8800"));
        }

        // 2. 🧠 SET RELEVANCE AI INSIGHT
        if (holder.tvAiInsight != null) {
            String aiMsg = item.getAiExplanation();
            if (aiMsg != null && !aiMsg.isEmpty() && !aiMsg.contains("pending") && !aiMsg.contains("progress")) {
                holder.aiContainer.setVisibility(View.VISIBLE);
                holder.tvAiInsight.setText("🧠 AI Insight: " + aiMsg);
                if (holder.pbAiConfidence != null) {
                    holder.pbAiConfidence.setProgress(item.getAiConfidence());
                }
            } else {
                holder.aiContainer.setVisibility(View.GONE);
            }
        }

        // === 🎨 FIXED COLOR LOGIC & DARK THEME UPGRADE ===
        // We prioritize SAFE checks to ensure Green font for safe messages
        if (label.contains("SAFE") || item.getRiskScore() < 40) {
            // 🟢 NEON GREEN - For Safe Messages
            holder.card.setCardBackgroundColor(Color.parseColor("#051A0A")); // Dark Green Bg
            holder.tvRisk.setTextColor(Color.parseColor("#00FF66"));        // Neon Green Font
            holder.tvLabel.setTextColor(Color.parseColor("#00FF66"));       // Neon Green Label
        } else {
            // 🔴 NEON RED - For Fraud/Phishing/Threats
            holder.card.setCardBackgroundColor(Color.parseColor("#1A0505")); // Dark Red Bg
            holder.tvRisk.setTextColor(Color.parseColor("#FF3333"));        // Neon Red Font
            holder.tvLabel.setTextColor(Color.parseColor("#FF3333"));       // Neon Red Label
        }

        // Static Text Colors
        holder.tvSender.setTextColor(Color.parseColor("#E0E0E0"));
        holder.tvMessage.setTextColor(Color.parseColor("#B0B0B0"));
        holder.tvSuggestion.setTextColor(Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvLabel, tvSender, tvMessage, tvSource, tvSuggestion, tvRisk, tvReasons, tvAiInsight;
        ProgressBar pbAiConfidence;
        View aiContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardOtpResult);
            tvLabel = itemView.findViewById(R.id.tvOtpLabel);
            tvSender = itemView.findViewById(R.id.tvOtpSender);
            tvMessage = itemView.findViewById(R.id.tvOtpMessage);
            tvSource = itemView.findViewById(R.id.tvOtpSource);
            tvSuggestion = itemView.findViewById(R.id.tvOtpSuggestion);
            tvRisk = itemView.findViewById(R.id.tvOtpRiskScore);

            // AI Insight Fields
            tvReasons = itemView.findViewById(R.id.tvOtpReasons);
            tvAiInsight = itemView.findViewById(R.id.tvAiInsight);
            pbAiConfidence = itemView.findViewById(R.id.pbAiConfidence);
            aiContainer = itemView.findViewById(R.id.layoutAiInsight);
        }
    }
}
