package com.mpd.hospital.viewmodel;

import android.util.Log;

import com.mpd.hospital.data.ConfigManager;

import org.json.JSONException;
import org.json.JSONObject;

public class NavigationViewModel {

    private ConfigManager configManager;
    private String baseUrl;
    private String codigoQr;
    private String parametro="/buscar-equipo/";
    public NavigationViewModel(ConfigManager configManager) {
        this.configManager = configManager;
        this.baseUrl = buildBaseUrl();
        this.codigoQr="";
    }

    public void refreshConfig() {
        this.baseUrl = buildBaseUrl();
    }

    private String buildBaseUrl() {
        String ip = configManager.getServerIp();
        String port = configManager.getServerPort();
        return "http://" + ip + ":" + port;
    }

    public String getHomeUrl() {
        return baseUrl;
    }

    /**
     * Procesa el String obtenido del código QR (qrData).
     * @param qrData El String leído por el escáner QR.
     */
    private void procesarLecturaQR(String qrData) {

        this.codigoQr="";
        final String PREFIJO_BUSQUEDA = "{\"tipo\":\"";

        if (qrData == null || !qrData.startsWith(PREFIJO_BUSQUEDA)) {
            // No es nuestro formato de QR.
            mostrarMensajeAlUsuario("Código QR no reconocido para búsqueda de activo.");
            return;
        }

        // 2. PARSEO DEL JSON Y VERIFICACIÓN DE KEYS
        try {
            JSONObject json = new JSONObject(qrData);

            // Verificación de la KEY 1: "tipo"
            if (!json.has("tipo")) {
                mostrarMensajeAlUsuario("QR válido, pero falta la key 'tipo'.");
                return;
            }
            String tipo = json.getString("tipo");

            // Verificación de la KEY 2: "id_unico"
            if (!json.has("id_unico")) {
                mostrarMensajeAlUsuario("QR válido, pero falta la key 'id_unico'.");
                return;
            }
            String idUnico = json.getString("id_unico");

            if (idUnico.startsWith("sighi_") && "activo".equals(tipo)) {
                this.codigoQr=idUnico;
            } else {
                mostrarMensajeAlUsuario("ID de activo no tiene el prefijo 'sighi_'.");
            }



        } catch (JSONException e) {
            // Se ejecutará si el String empieza bien pero el resto del JSON está corrupto o mal formado.
           // Log.e("QR_PROCESS", "Error al parsear el JSON del QR: " + e.getMessage());
            mostrarMensajeAlUsuario("Error en el formato del código QR. No es un JSON válido.");
        }
    }


    /**
     * Método auxiliar para mostrar mensajes en Android (por ejemplo, con un Toast).
     */
    private void mostrarMensajeAlUsuario(String mensaje) {
        // Aquí implementas Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        Log.d("QR_APP_MSG", "Mensaje al usuario: " + mensaje);
    }

    public String buildUrlFromQR(String qrData) {

        if (qrData == null || qrData.isEmpty()) {
            return "";
        }
        procesarLecturaQR(qrData);
        if(this.codigoQr.isEmpty() || this.codigoQr.length()<=0){
            return  "";
        }



        return baseUrl + this.parametro + this.codigoQr;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}