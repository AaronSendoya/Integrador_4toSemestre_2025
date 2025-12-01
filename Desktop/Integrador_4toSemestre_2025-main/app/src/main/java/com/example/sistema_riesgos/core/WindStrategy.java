package com.example.sistema_riesgos.core;

import com.example.sistema_riesgos.models.ReportModel;

public class WindStrategy implements RiskStrategy {

    @Override
    public String analyzeRisk(ReportModel report) {
        String desc = report.getDescripcion() != null
                ? report.getDescripcion().toLowerCase()
                : "";

        if (desc.contains("árbol caído") || desc.contains("techos volados") || desc.contains("poste caído")) {
            return "ALTO";
        }
        if (desc.contains("viento fuerte") || desc.contains("rafagas")) {
            return "MEDIO";
        }
        return "BAJO";
    }
}
