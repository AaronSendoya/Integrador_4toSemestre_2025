package com.example.sistema_riesgos.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.models.ReportModel;
import com.example.sistema_riesgos.models.RiskReportState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportHistoryAdapter
        extends RecyclerView.Adapter<ReportHistoryAdapter.ReportViewHolder> {

    public interface OnReportActionListener {
        void onEdit(ReportModel report);
        void onDelete(ReportModel report);
    }

    private final List<ReportModel> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private final OnReportActionListener listener;

    public ReportHistoryAdapter(OnReportActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ReportModel> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_history, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ReportModel report = items.get(position);

        holder.tvType.setText(report.getTipo());
        holder.tvStatus.setText(report.getEstado());
        holder.tvDescription.setText(report.getDescripcion());

        if (report.getFecha() != null) {
            holder.tvDate.setText(dateFormat.format(report.getFecha()));
        } else {
            holder.tvDate.setText("-");
        }

        // --- Color del card según estado ---
        RiskReportState state = report.getEstadoEnum();
        if (state == RiskReportState.PENDING) {
            holder.container.setBackgroundResource(R.drawable.bg_report_pending);
        } else if (state == RiskReportState.PROCESSED) {
            holder.container.setBackgroundResource(R.drawable.bg_report_processed);
        } else if (state == RiskReportState.ERROR) {
            holder.container.setBackgroundResource(R.drawable.bg_report_error);
        } else {
            // fallback: gris por defecto
            holder.container.setBackgroundResource(R.drawable.rounded_card_gray);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(report);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(report);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {

        final LinearLayout container;
        final TextView tvType;
        final TextView tvStatus;
        final TextView tvDescription;
        final TextView tvDate;
        final ImageButton btnEdit;
        final ImageButton btnDelete;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.ll_report_item_container);
            tvType = itemView.findViewById(R.id.tv_item_risk_type);
            tvStatus = itemView.findViewById(R.id.tv_item_status);
            tvDescription = itemView.findViewById(R.id.tv_item_description);
            tvDate = itemView.findViewById(R.id.tv_item_date);
            btnEdit = itemView.findViewById(R.id.btn_item_edit);
            btnDelete = itemView.findViewById(R.id.btn_item_delete);
        }
    }
}
