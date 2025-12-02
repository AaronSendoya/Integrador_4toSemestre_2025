package com.example.sistema_riesgos.ui;

import android.location.Location;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.models.ReportModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private final List<ReportModel> items = new ArrayList<>();

    // Ubicación de referencia para calcular distancias
    private double refLat = 0.0;
    private double refLon = 0.0;
    private boolean hasLocation = false;

    public void setReferenceLocation(double lat, double lon, boolean hasLocation) {
        this.refLat = lat;
        this.refLon = lon;
        this.hasLocation = hasLocation;
        notifyDataSetChanged();
    }

    public void submitList(List<ReportModel> alerts) {
        items.clear();
        if (alerts != null) {
            items.addAll(alerts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        ReportModel report = items.get(position);
        holder.bind(report, refLat, refLon, hasLocation);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {

        private final View levelIndicator;
        private final TextView tvType;
        private final TextView tvLevel;
        private final TextView tvDescription;
        private final TextView tvDistance;
        private final TextView tvTime;

        AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            levelIndicator = itemView.findViewById(R.id.view_alert_level_indicator);
            tvType = itemView.findViewById(R.id.tv_alert_type);
            tvLevel = itemView.findViewById(R.id.tv_alert_level);
            tvDescription = itemView.findViewById(R.id.tv_alert_description);
            tvDistance = itemView.findViewById(R.id.tv_alert_distance);
            tvTime = itemView.findViewById(R.id.tv_alert_time);
        }

        void bind(ReportModel report,
                  double refLat,
                  double refLon,
                  boolean hasLocation) {

            String tipo = report.getTipo() != null ? report.getTipo() : "Riesgo";
            String nivel = report.getNivel() != null ? report.getNivel() : "DESCONOCIDO";
            String desc = report.getDescripcion() != null ? report.getDescripcion() : "";

            tvType.setText(tipo);
            tvLevel.setText(nivel.toUpperCase(Locale.getDefault()));
            tvDescription.setText(desc);

            // Distancia
            if (hasLocation && report.getLatitud() != 0.0 && report.getLongitud() != 0.0) {
                float[] results = new float[1];
                Location.distanceBetween(
                        refLat, refLon,
                        report.getLatitud(), report.getLongitud(),
                        results
                );
                float km = results[0] / 1000f;
                tvDistance.setText(
                        String.format(Locale.getDefault(),
                                "A %.1f km de tu ubicación", km)
                );
            } else {
                tvDistance.setText(itemView.getContext()
                        .getString(R.string.alerts_distance_unknown));
            }

            // Tiempo relativo
            if (report.getFecha() != null) {
                CharSequence relTime = DateUtils.getRelativeTimeSpanString(
                        report.getFecha().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                );
                tvTime.setText(relTime);
            } else {
                tvTime.setText("--");
            }

            // (Opcional) podrías cambiar colores según nivel aquí más adelante
            // por ahora dejamos los drawables tal cual.
        }
    }
}
