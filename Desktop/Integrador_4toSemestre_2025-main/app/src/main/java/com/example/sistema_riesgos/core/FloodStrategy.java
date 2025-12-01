package com.example.sistema_riesgos.core;

import com.example.sistema_riesgos.models.ReportModel;

public class FloodStrategy implements RiskStrategy {

    @Override
    public String analyzeRisk(ReportModel report) {
        String desc = report.getDescripcion() != null
                ? report.getDescripcion().toLowerCase()
                : "";

        if (desc.contains("desbord") || desc.contains("inund") || desc.contains("cubierto de agua")) {
            return "ALTO";
        }
        if (desc.contains("agua alta") || desc.contains("subiendo")) {
            return "MEDIO";
        }
        return "BAJO";
    }
}
