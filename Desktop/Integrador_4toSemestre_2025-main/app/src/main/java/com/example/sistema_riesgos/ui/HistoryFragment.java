package com.example.sistema_riesgos.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.core.RiskManagerService;
import com.example.sistema_riesgos.models.ReportModel;

public class HistoryFragment extends Fragment {

    private SharedRiskViewModel viewModel;
    private ReportHistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.rv_report_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

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

        recyclerView.setAdapter(adapter);

        viewModel.getReports().observe(getViewLifecycleOwner(), reports ->
                adapter.submitList(reports));

        // Botón de refresco / sincronización forzada
        Button btnForceSync = view.findViewById(R.id.btn_force_sync);
        btnForceSync.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Sincronizando reportes pendientes...",
                    Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                try {
                    // Forzamos la subida de pendientes al backend
                    RiskManagerService
                            .getInstance(requireContext())
                            .syncPendingReports();

                    // Volvemos a cargar listas (local + remoto)
                    viewModel.loadLocalReports();
                    viewModel.loadRemoteReports();

                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Sincronización forzada finalizada.",
                                    Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Error al sincronizar: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        // 1) Carga historial local (SQLite) -> siempre persistente
        viewModel.loadLocalReports();

        // 2) Intenta fusionar con backend (si hay internet)
        viewModel.loadRemoteReports();
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
