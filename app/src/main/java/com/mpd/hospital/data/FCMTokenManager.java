package com.mpd.hospital.data;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMTokenManager {

    private static final String TAG = "FCMTokenManager";
    private static final String PREFS_NAME = "FCMPrefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_TOKEN_SENT = "token_sent_to_server";

    private SharedPreferences prefs;
    private Context context;

    public FCMTokenManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void initializeFCM() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Error al obtener token FCM", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    saveToken(token);
                    Log.d(TAG, "Token FCM obtenido: " + token);

                    // Enviar token al servidor si no se ha enviado
                    if (!isTokenSentToServer()) {
                        sendTokenToServer(token);
                    }
                });
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    public void markTokenAsSent() {
        prefs.edit().putBoolean(KEY_TOKEN_SENT, true).apply();
    }

    public boolean isTokenSentToServer() {
        return prefs.getBoolean(KEY_TOKEN_SENT, false);
    }

    public void sendTokenToServer(String token) {
        // Aquí implementarías la llamada HTTP a tu servidor Node.js
        // Por ahora solo lo marcamos como enviado
        Log.d(TAG, "Enviando token al servidor: " + token);

        // TODO: Implementar llamada HTTP
        // Ejemplo de endpoint: POST /api/fcm/register
        // Body: { "token": token, "device_id": getDeviceId() }

        // Después de enviar exitosamente:
        markTokenAsSent();
    }

    private String getDeviceId() {
        // Obtener un ID único del dispositivo
        return android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
        );
    }
}
