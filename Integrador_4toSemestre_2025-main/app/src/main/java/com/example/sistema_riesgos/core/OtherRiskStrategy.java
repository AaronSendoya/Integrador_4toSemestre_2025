package com.example.sistema_riesgos.core;

import com.example.sistema_riesgos.models.ReportModel;

public class OtherRiskStrategy implements RiskStrategy {

    @Override
    public String analyzeRisk(ReportModel report) {
        String desc = report.getDescripcion() != null
                ? report.getDescripcion().toLowerCase()
                : "";

        if (desc.contains("evacuación") || desc.contains("explos")) {
            return "ALTO";
        }
        if (desc.contains("daño menor") || desc.contains("revisión")) {
            return "MEDIO";
        }
        return "BAJO";
    }
}
