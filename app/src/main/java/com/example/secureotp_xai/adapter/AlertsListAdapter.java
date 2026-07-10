package com.example.secureotp_xai.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.secureotp_xai.database.AlertEntity;

import java.util.List;

public class AlertsListAdapter extends ArrayAdapter<AlertEntity> {

    public AlertsListAdapter(Context context, List<AlertEntity> alerts) {
        super(context, 0, alerts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            // Reuses your simple_list_item_1 so we don't break your XML layout!
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView tvText = convertView.findViewById(android.R.id.text1);
        AlertEntity alert = getItem(position);

        if (alert != null) {
            // Apply Emoji and Text
            String icon = alert.getPriority() > 0 ? "🔴 " : "🟢 ";
            tvText.setText(icon + alert.getAlertType() + " - " + alert.getAlertMsg());

            // 🔥 UNREAD VS READ STYLING 🔥
            if (!alert.isRead()) {
                tvText.setTypeface(null, Typeface.BOLD);
                // Unread messages are bright neon colors
                tvText.setTextColor(alert.getPriority() > 0 ? Color.parseColor("#EF4444") : Color.parseColor("#3B82F6"));
            } else {
                tvText.setTypeface(null, Typeface.NORMAL);
                // Read messages are subdued white/gray
                tvText.setTextColor(Color.parseColor("#E2E8F0"));
            }
        }

        return convertView;
    }
}
