package com.example.sistema_riesgos.models;

import com.google.gson.annotations.SerializedName;

public class UserModel {

    // ID remoto en MongoDB
    @SerializedName("id")
    private String id;

    private String nombre;
    private String correo;
    private String telefono;
    private String password;

    // ID local en SQLite (no se envía al backend)
    private transient long localId;

    public UserModel() {
    }

    // Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }
}
