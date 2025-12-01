package com.example.sistema_riesgos.models;

public enum RiskReportState {
    PENDING,
    PROCESSED,
    ERROR;

    // Convierte un String a enum de forma segura
    public static RiskReportState fromString(String value) {
        if (value == null) return PENDING;
        try {
            return RiskReportState.valueOf(value);
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }

    // Valor para guardar/enviar (BD / API)
    public String toValue() {
        return name();
    }
}