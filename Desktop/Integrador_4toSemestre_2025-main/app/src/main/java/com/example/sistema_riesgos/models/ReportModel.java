package com.example.sistema_riesgos.models;

import java.util.Date;

public class ReportModel {

    // Id generado por MongoDB
    private String id;

    // Id local en SQLite (tabla registros)
    private long localId;

    private String tipo;
    private String descripcion;
    private double latitud;
    private double longitud;
    private Date fecha;
    private String estado;   // PENDING / PROCESSED / ERROR
    private String nivel;    // BAJO / MEDIO / ALTO
    private String urlFoto;
    private String idUsuario;

    public ReportModel() {
    }

    public ReportModel(String tipo,
                       String descripcion,
                       double latitud,
                       double longitud,
                       Date fecha,
                       String estado,
                       String nivel,
                       String urlFoto,
                       String idUsuario) {
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.fecha = fecha;
        this.estado = estado;
        this.nivel = nivel;
        this.urlFoto = urlFoto;
        this.idUsuario = idUsuario;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Atajo para trabajar con el enum
    public RiskReportState getEstadoEnum() {
        return RiskReportState.fromString(estado);
    }

    public void setEstadoEnum(RiskReportState state) {
        this.estado = state != null ? state.toValue() : null;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getUrlFoto() {
        return urlFoto;
    }

    public void setUrlFoto(String urlFoto) {
        this.urlFoto = urlFoto;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }
}
