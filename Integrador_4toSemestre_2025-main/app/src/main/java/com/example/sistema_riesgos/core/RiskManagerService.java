package com.example.sistema_riesgos.core;

import android.content.Context;

import com.example.sistema_riesgos.data.ApiRepository;
import com.example.sistema_riesgos.data.DBHelper;
import com.example.sistema_riesgos.models.ReportModel;
import com.example.sistema_riesgos.models.RiskAnalysisResponse;
import com.example.sistema_riesgos.models.RiskReportState;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Callback;

public class RiskManagerService {

    private static RiskManagerService instance;

    private final ApiRepository apiRepository;
    private final DBHelper dbHelper;

    private final RiskStrategy fireStrategy;
    private final RiskStrategy floodStrategy;
    private final RiskStrategy windStrategy;
    private final RiskStrategy otherStrategy;

    private final List<RiskEventListener> listeners = new ArrayList<>();

    private RiskManagerService(Context context) {
        Context appContext = context.getApplicationContext();
        apiRepository = ApiRepository.getInstance(appContext);
        dbHelper = apiRepository.getDbHelper();

        fireStrategy = new FireStrategy();
        floodStrategy = new FloodStrategy();
        windStrategy = new WindStrategy();
        otherStrategy = new OtherRiskStrategy();
    }

    public static synchronized RiskManagerService getInstance(Context context) {
        if (instance == null) {
            instance = new RiskManagerService(context);
        }
        return instance;
    }

    public interface RiskEventListener {
        void onReportCreated(ReportModel report);
    }

    public void addListener(RiskEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(RiskEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Crea reporte local (SQLite) + calcula nivel de riesgo + dispara sync al
     * backend.
     */
    public long createReport(String tipo,
            String descripcion,
            double latitud,
            double longitud,
            String urlFoto,
            long idUsuarioLocal,
            double aiConfidence) {

        long nowMillis = System.currentTimeMillis();
        Date fechaSql = new Date(nowMillis);

        ReportModel report = new ReportModel();
        report.setTipo(tipo);
        report.setDescripcion(descripcion);
        report.setLatitud(latitud);
        report.setLongitud(longitud);
        report.setFecha(fechaSql);
        report.setEstadoEnum(RiskReportState.PENDING);
        report.setIdUsuario(String.valueOf(idUsuarioLocal));
        report.setUrlFoto(urlFoto);
        report.setAiConfidence(aiConfidence);

        RiskStrategy strategy = getStrategyForType(tipo);
        String nivel = strategy != null ? strategy.analyzeRisk(report) : "MEDIO";
        report.setNivel(nivel);

        long localId = dbHelper.insertRegistro(
                report.getTipo(),
                report.getDescripcion(),
                report.getLatitud(),
                report.getLongitud(),
                report.getFecha().getTime(),
                report.getEstado(),
                report.getNivel(),
                report.getUrlFoto(),
                idUsuarioLocal,
                DBHelper.SYNC_STATUS_PENDING,
                report.getAiConfidence());

        // Guardamos el id local en el modelo
        report.setLocalId(localId);

        notifyReportCreated(report);
        apiRepository.syncPendingReports();

        return localId;
    }

    public void syncPendingReports() {
        apiRepository.syncPendingReports();
    }

    public void analyzePhoto(String photoPath, Callback<RiskAnalysisResponse> callback) {
        if (photoPath == null)
            return;
        File file = new File(photoPath);
        if (file.exists()) {
            apiRepository.analyzeImage(file, callback);
        }
    }

    private RiskStrategy getStrategyForType(String tipo) {
        if (tipo == null)
            return otherStrategy;

        String t = tipo.toLowerCase();

        if (t.contains("incend")) {
            return fireStrategy;
        } else if (t.contains("inund")) {
            return floodStrategy;
        } else if (t.contains("viento")) {
            return windStrategy;
        } else {
            return otherStrategy;
        }
    }

    private void notifyReportCreated(ReportModel report) {
        for (RiskEventListener listener : listeners) {
            listener.onReportCreated(report);
        }
    }
}
