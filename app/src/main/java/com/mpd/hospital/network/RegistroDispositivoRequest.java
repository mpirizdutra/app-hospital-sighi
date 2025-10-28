package com.mpd.hospital.network;


public class RegistroDispositivoRequest {
    // Los nombres de estas variables DEBEN coincidir con las claves que espera tu API
    private String usuarioId;
    private String tokenFcm;

    public RegistroDispositivoRequest(String usuarioId, String tokenFcm) {
        this.usuarioId = usuarioId;
        this.tokenFcm = tokenFcm;
    }

    // Getters y Setters (Retrofit los usa internamente)
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getTokenFcm() { return tokenFcm; }
    public void setTokenFcm(String tokenFcm) { this.tokenFcm = tokenFcm; }
}
