package com.mpd.hospital.services;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import  android.content.Context;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mpd.hospital.ui.main.MainActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM Service";
    private static final String CHANNEL_ID = "hospital_notifications";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Mensaje recibido desde: " + remoteMessage.getFrom());

        // Verificar si el mensaje contiene una notificación
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body);
        }

        // Verificar si el mensaje contiene datos
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Datos del mensaje: " + remoteMessage.getData());
            handleDataPayload(remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM: " + token);
        guardarTokenEnPrefs(token);
        // Enviar el token a tu servidor
       // sendTokenToServer(token);
    }

    private void guardarTokenEnPrefs(String token) {
        // Obtenemos una referencia a nuestro archivo de preferencias "FCM_PREFS"
        SharedPreferences prefs = getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE);
        // Obtenemos un editor para poder escribir en él
        SharedPreferences.Editor editor = prefs.edit();
        // Guardamos el token con la clave "FCM_TOKEN"
        editor.putString("FCM_TOKEN", token);
        // Aplicamos los cambios
        editor.apply();

        Log.d(TAG, "Token guardado en SharedPreferences.");
    }

    private void showNotification(String title, String body) {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title != null ? title : "Hospital QR Scanner")
                        .setContentText(body != null ? body : "Nueva notificación")
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notificaciones Hospital";
            String description = "Canal para notificaciones del sistema hospitalario";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void handleDataPayload(java.util.Map<String, String> data) {
        // Aquí puedes manejar datos personalizados
        // Por ejemplo, si envías un código QR o una URL
        String qrCode = data.get("qr_code");
        String url = data.get("url");

        if (qrCode != null || url != null) {
            // Guardar en SharedPreferences o procesar
            Log.d(TAG, "QR Code: " + qrCode + ", URL: " + url);
        }
    }

    private void sendTokenToServer(String token) {
        // Aquí envías el token a tu servidor Node.js
        // Puedes usar Retrofit, Volley o HttpURLConnection
        Log.d(TAG, "Enviar token al servidor: " + token);

        // Ejemplo simple con tu servidor
        // POST a http://192.168.1.40:3030/api/register-token
        // Body: { "token": token, "device_id": "..." }
    }
}
