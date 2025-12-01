package com.example.sistema_riesgos.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sistema_riesgos.data.ApiRepository;
import com.example.sistema_riesgos.models.ReportModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel compartido para la lista de reportes
 * (HomeFragment, HistoryFragment, AlertsFragment, etc).
 */
public class SharedRiskViewModel extends AndroidViewModel {

    private final ApiRepository apiRepository;
    private final MutableLiveData<List<ReportModel>> reportsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    public SharedRiskViewModel(@NonNull Application application) {
        super(application);
        apiRepository = ApiRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<List<ReportModel>> getReports() {
        return reportsLiveData;
    }

    // -------------------------------------------------------------------------
    // Cargar datos locales (SQLite)
    // -------------------------------------------------------------------------

    public void loadLocalReports() {
        new Thread(() -> {
            List<ReportModel> local = apiRepository.getLocalReports();
            reportsLiveData.postValue(local);
        }).start();
    }

    // -------------------------------------------------------------------------
    // Cargar datos remotos (MongoDB) y fusionar
    // -------------------------------------------------------------------------

    public void loadRemoteReports() {
        apiRepository.getAllRemoteReports().enqueue(new Callback<List<ReportModel>>() {
            @Override
            public void onResponse(Call<List<ReportModel>> call,
                                   Response<List<ReportModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ReportModel> remote = response.body();
                    List<ReportModel> current = reportsLiveData.getValue();
                    List<ReportModel> merged = mergeReports(current, remote);
                    reportsLiveData.postValue(merged);
                }
            }

            @Override
            public void onFailure(Call<List<ReportModel>> call, Throwable t) {
                // Aquí podrías loguear el error si quieres
            }
        });
    }

    private List<ReportModel> mergeReports(List<ReportModel> base, List<ReportModel> extra) {
        List<ReportModel> result = new ArrayList<>();
        if (base != null) {
            result.addAll(base);
        }

        if (extra != null) {
            for (ReportModel r : extra) {
                boolean exists = false;
                String remoteId = r.getId();
                if (remoteId != null) {
                    for (ReportModel existing : result) {
                        if (remoteId.equals(existing.getId())) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Sincronización y acciones sobre reportes
    // -------------------------------------------------------------------------

    /** Lanza la sincronización de reportes pendientes (SQLite -> Mongo). */
    public void syncPendingReports() {
        apiRepository.syncPendingReports();
    }

    /** Envía un reporte actualizado al backend. */
    public void updateRemoteReport(ReportModel report) {
        apiRepository.updateRemoteReport(report);
    }

    /** Elimina un reporte remoto (si tiene id remoto). */
    public void deleteRemoteReport(ReportModel report) {
        apiRepository.deleteRemoteReport(report);
    }

    // ---------- MÉTODOS QUE USA AlertsFragment / HistoryFragment ------------

    /**
     * Actualiza la descripción de un reporte:
     * - actualiza en memoria (LiveData)
     * - actualiza en SQLite
     * - si tiene id remoto, manda PUT al backend
     */
    public void updateReportDescription(ReportModel report, String newDescription) {
        if (report == null || newDescription == null) return;

        // 1) actualizar el modelo
        report.setDescripcion(newDescription);

        // 2) actualizar en SQLite
        apiRepository.updateLocalReport(report);

        // 3) actualizar la lista en memoria
        List<ReportModel> current = reportsLiveData.getValue();
        if (current != null) {
            for (int i = 0; i < current.size(); i++) {
                ReportModel r = current.get(i);
                if (r.getLocalId() == report.getLocalId()) {
                    r.setDescripcion(newDescription);
                    break;
                }
            }
            reportsLiveData.setValue(new ArrayList<>(current));
        }

        // 4) actualizar en el backend (si ya está sincronizado)
        apiRepository.updateRemoteReport(report);
    }

    /**
     * Elimina un reporte:
     * - lo borra en SQLite
     * - si tiene id remoto, lo borra en backend
     * - actualiza LiveData
     */
    public void deleteReport(ReportModel report) {
        if (report == null) return;

        long localId = report.getLocalId();

        // 1) borrar en SQLite
        apiRepository.deleteLocalReport(localId);

        // 2) borrar remoto (si corresponde)
        apiRepository.deleteRemoteReport(report);

        // 3) actualizar la lista en memoria
        List<ReportModel> current = reportsLiveData.getValue();
        if (current != null) {
            List<ReportModel> updated = new ArrayList<>();
            for (ReportModel r : current) {
                if (r.getLocalId() != localId) {
                    updated.add(r);
                }
            }
            reportsLiveData.setValue(updated);
        }
    }
}
