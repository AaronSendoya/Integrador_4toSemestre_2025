package com.example.sistema_riesgos.core;

import com.example.sistema_riesgos.models.ReportModel;

public interface RiskStrategy {
    // Devuelve nivel de riesgo: BAJO / MEDIO / ALTO
    String analyzeRisk(ReportModel report);
}
