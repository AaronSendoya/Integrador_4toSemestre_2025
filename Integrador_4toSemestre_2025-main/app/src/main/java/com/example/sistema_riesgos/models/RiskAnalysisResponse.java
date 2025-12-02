package com.example.sistema_riesgos.models;

public class RiskAnalysisResponse {
    private String riskType;
    private String severity;
    private double confidence;

    public RiskAnalysisResponse() {
    }

    public String getRiskType() {
        return riskType;
    }

    public void setRiskType(String riskType) {
        this.riskType = riskType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
