package com.example.sistema_riesgos.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.models.ReportModel;
import com.example.sistema_riesgos.models.RiskReportState;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {

    private static final int REQ_LOCATION_ALERTS = 3001;
    // Radio máximo para considerar una alerta "cercana" (20 km)
    private static final float MAX_DISTANCE_METERS = 20_000f;
    // Ventana de tiempo: sólo alertas de las últimas 24 horas
    private static final long MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L;

    private TextView tvAlertsTitle;
    private TextView tvAlertsEmptyMessage;
    private RecyclerView rvAlerts;

    private SharedRiskViewModel viewModel;
    private ReportHistoryAdapter adapter;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0;
    private double currentLon = 0.0;
    private boolean hasLocation = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAlertsTitle = view.findViewById(R.id.tvAlertstitle);
        tvAlertsEmptyMessage = view.findViewById(R.id.tvAlertsemptymessage);
        rvAlerts = view.findViewById(R.id.rvAlerts);

        rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        viewModel = new ViewModelProvider(requireActivity())
                .get(SharedRiskViewModel.class);

        adapter = new ReportHistoryAdapter(new ReportHistoryAdapter.OnReportActionListener() {
            @Override
            public void onEdit(ReportModel report) {
                showEditDialog(report);
            }

            @Override
            public void onDelete(ReportModel report) {
                showDeleteDialog(report);
            }
        });
        rvAlerts.setAdapter(adapter);

// Asegúrate también de llamar a:
        viewModel.loadLocalReports();
        viewModel.loadRemoteReports();
    }

    private void requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                currentLat = location.getLatitude();
                                currentLon = location.getLongitude();
                                hasLocation = true;
                            } else {
                                hasLocation = false;
                            }
                        }
                    });
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION_ALERTS
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION_ALERTS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationIfNeeded();
            } else {
                // Sin ubicación: seguiremos filtrando sólo por estado + tiempo
                hasLocation = false;
            }
        }
    }

    /**
     * Devuelve solo las alertas:
     * - con estado PENDING
     * - recientes (<= 24h)
     * - y, si tenemos ubicación, dentro de MAX_DISTANCE_METERS
     */
    private List<ReportModel> filterActiveAlerts(@Nullable List<ReportModel> reports) {
        List<ReportModel> result = new ArrayList<>();
        if (reports == null || reports.isEmpty()) {
            return result;
        }

        long now = System.currentTimeMillis();

        for (ReportModel r : reports) {
            if (r == null) continue;

            // 1) Estado PENDING
            if (!RiskReportState.PENDING.name().equalsIgnoreCase(r.getEstado())) {
                continue;
            }

            // 2) Tiene fecha y es reciente
            if (r.getFecha() == null) continue;
            long age = now - r.getFecha().getTime();
            if (age > MAX_AGE_MILLIS) continue;

            // 3) Si tenemos ubicación, filtramos por distancia
            if (hasLocation) {
                float[] results = new float[1];
                Location.distanceBetween(
                        currentLat, currentLon,
                        r.getLatitud(), r.getLongitud(),
                        results
                );
                float distanceMeters = results[0];
                if (distanceMeters > MAX_DISTANCE_METERS) {
                    continue;
                }
            }

            result.add(r);
        }

        return result;
    }

    private void updateUi(List<ReportModel> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            tvAlertsEmptyMessage.setVisibility(View.VISIBLE);
            rvAlerts.setVisibility(View.GONE);
            adapter.submitList(new ArrayList<>());
        } else {
            tvAlertsEmptyMessage.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.VISIBLE);
            adapter.submitList(alerts);
        }
    }

    private void showEditDialog(ReportModel report) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_report, null, false);

        EditText et = dialogView.findViewById(R.id.et_edit_description);
        et.setText(report.getDescripcion());

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_edit_title)
                .setView(dialogView)
                .setPositiveButton(R.string.dialog_edit_save, (d, which) -> {
                    String newDesc = et.getText().toString().trim();
                    if (!newDesc.isEmpty()) {
                        viewModel.updateReportDescription(report, newDesc);
                    }
                })
                .setNegativeButton(R.string.dialog_edit_cancel, null)
                .show();
    }

    private void showDeleteDialog(ReportModel report) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.dialog_delete_yes, (d, which) ->
                        viewModel.deleteReport(report))
                .setNegativeButton(R.string.dialog_delete_no, null)
                .show();
    }
}
