package com.example.sistema_riesgos.core;

import com.example.sistema_riesgos.models.ReportModel;

public class FireStrategy implements RiskStrategy {

    @Override
    public String analyzeRisk(ReportModel report) {
        String desc = report.getDescripcion() != null
                ? report.getDescripcion().toLowerCase()
                : "";

        if (desc.contains("llama") || desc.contains("fuego") || desc.contains("incendio")) {
            return "ALTO";
        }
        if (desc.contains("humo") || desc.contains("chispas")) {
            return "MEDIO";
        }
        return "BAJO";
    }
}
