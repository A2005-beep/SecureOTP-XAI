package com.example.secureotp_xai.adapter; // <-- Change this if your project package is different!

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secureotp_xai.R; // <-- Change this if your project package is different!
import com.example.secureotp_xai.database.AlertEntity;

public class AlertsAdapter extends ListAdapter<AlertEntity, AlertsAdapter.AlertViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int alertId);
    }

    private final OnItemClickListener listener;

    public AlertsAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<AlertEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<AlertEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull AlertEntity oldItem, @NonNull AlertEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AlertEntity oldItem, @NonNull AlertEntity newItem) {
            return oldItem.isRead() == newItem.isRead() &&
                    oldItem.getAlertMsg().equals(newItem.getAlertMsg());
        }
    };

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_otp_result, parent, false);
        return new AlertViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        AlertEntity currentAlert = getItem(position);

        holder.tvSender.setText("From: " + currentAlert.getSender());
        holder.tvMessage.setText(currentAlert.getBody());
        holder.tvLabel.setText(currentAlert.getAlertType());
        holder.tvSuggestion.setText(currentAlert.getAlertMsg());

        int accentColor;
        int accentBgColor;
        int suggestionBgColor;

        String alertType = currentAlert.getAlertType();
        if (alertType == null) alertType = "";

        switch (alertType) {
            case "FRAUD_OTP":
            case "SMS_BOMB":
            case "HIGH_RISK":
            case "PHISHING":
                accentColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_red);
                accentBgColor = Color.parseColor("#1AEF4444");
                suggestionBgColor = Color.parseColor("#1AEF4444");
                break;
            case "SUSPICIOUS":
                accentColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_amber);
                accentBgColor = Color.parseColor("#1AF59E0B");
                suggestionBgColor = Color.parseColor("#1AF59E0B");
                break;
            case "SAFE_OTP":
            case "SAFE_SMS":
            default:
                accentColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_green);
                accentBgColor = Color.parseColor("#1A10B981");
                suggestionBgColor = Color.parseColor("#1A10B981");
                break;
        }

        holder.tvLabel.setTextColor(accentColor);
        holder.tvLabel.setBackgroundColor(accentBgColor);
        holder.viewDivider.setBackgroundColor(accentColor);

        holder.tvSuggestion.setText(currentAlert.getAlertMsg());
        holder.tvSuggestion.setTextColor(accentColor);
        if (holder.suggestionContainer != null) {
            holder.suggestionContainer.setBackgroundColor(suggestionBgColor);
        }

        if (holder.tvReasons != null) {
            holder.tvReasons.setText(currentAlert.getReasons());
            holder.tvReasons.setTextColor(accentColor);
        }

        if (holder.tvAiInsight != null) {
            String aiMsg = currentAlert.getAiExplanation();
            if (aiMsg != null && !aiMsg.isEmpty() && !aiMsg.equals("Analysis pending...")) {
                holder.aiContainer.setVisibility(View.VISIBLE);
                holder.tvAiInsight.setText(aiMsg);

                if (holder.pbAiConfidence != null) {
                    int confidence = currentAlert.getAiConfidence();
                    holder.pbAiConfidence.setProgress(confidence);

                    int pbColor;
                    if (confidence > 70) {
                        pbColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_red);
                    } else if (confidence > 40) {
                        pbColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_amber);
                    } else {
                        pbColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_green);
                    }
                    holder.pbAiConfidence.getProgressDrawable().setTint(pbColor);
                }
            } else {
                holder.aiContainer.setVisibility(View.GONE);
            }
        }

        if (holder.tvRiskScore != null) {
            int score = currentAlert.getRiskScore();
            holder.tvRiskScore.setText("Risk: " + score);

            int scoreColor;
            if (score <= 30) {
                scoreColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_green);
            } else if (score <= 59) {
                scoreColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_amber);
            } else {
                scoreColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_red);
            }
            holder.tvRiskScore.setTextColor(scoreColor);
        }

        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView cardView = (com.google.android.material.card.MaterialCardView) holder.itemView;
            if (!currentAlert.isRead()) {
                holder.tvSender.setTypeface(null, Typeface.BOLD);
                holder.tvMessage.setTypeface(null, Typeface.BOLD);
                cardView.setCardBackgroundColor(accentBgColor);
            } else {
                holder.tvSender.setTypeface(null, Typeface.NORMAL);
                holder.tvMessage.setTypeface(null, Typeface.NORMAL);
                cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.cyber_surface));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && !currentAlert.isRead()) {
                listener.onItemClick(currentAlert.getId());
            }
        });
    }

    class AlertViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSender;
        private TextView tvMessage;
        private TextView tvLabel;
        private TextView tvSuggestion;
        private TextView tvReasons;
        private TextView tvRiskScore;
        private TextView tvAiInsight;
        private ProgressBar pbAiConfidence;
        private View viewDivider;
        private View suggestionContainer;
        private View aiContainer;

        public AlertViewHolder(View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvOtpSender);
            tvMessage = itemView.findViewById(R.id.tvOtpMessage);
            tvLabel = itemView.findViewById(R.id.tvOtpLabel);
            tvSuggestion = itemView.findViewById(R.id.tvOtpSuggestion);
            tvReasons = itemView.findViewById(R.id.tvOtpReasons);
            tvRiskScore = itemView.findViewById(R.id.tvOtpRiskScore);
            tvAiInsight = itemView.findViewById(R.id.tvAiInsight);
            pbAiConfidence = itemView.findViewById(R.id.pbAiConfidence);
            aiContainer = itemView.findViewById(R.id.layoutAiInsight);
            viewDivider = itemView.findViewById(R.id.viewOtpDivider);
            if (tvSuggestion != null && tvSuggestion.getParent() instanceof View) {
                suggestionContainer = (View) tvSuggestion.getParent();
            }
        }
    }
}