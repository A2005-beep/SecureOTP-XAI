package com.example.secureotp_xai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.secureotp_xai.R;
import com.example.secureotp_xai.model.AppModel;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private List<AppModel> appList;

    public AppAdapter(List<AppModel> appList) {
        this.appList = appList;
    }

    // 🔹 ViewHolder class
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvRisk, tvReason;

        public ViewHolder(View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvAppName);
            tvRisk = itemView.findViewById(R.id.tvRisk);
            tvReason = itemView.findViewById(R.id.tvReason);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        AppModel app = appList.get(position);

        holder.tvName.setText(app.getName());
        holder.tvReason.setText("Reason: " + app.getReason());

        // 🎨 Risk Color Logic
        String risk = app.getRisk();

        if (risk.equalsIgnoreCase("HIGH")) {
            holder.tvRisk.setText("🔴 HIGH RISK");
        } else if (risk.equalsIgnoreCase("MEDIUM")) {
            holder.tvRisk.setText("🟡 MEDIUM RISK");
        } else {
            holder.tvRisk.setText("🟢 SAFE");
        }
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }
}