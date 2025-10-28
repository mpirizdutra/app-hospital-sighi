package com.mpd.hospital.network;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    /**
     * Define el endpoint para registrar un dispositivo.
     *
     * @POST especifica que es una petición POST a la ruta relativa "/api/dispositivos/registrar".
     * @Body le dice a Retrofit que el objeto 'request' debe ser serializado a JSON y enviado en el cuerpo de la petición.
     */
    @POST("/api/auth/registro-token")
    Call<Void> registrarDispositivo(@Body RegistroDispositivoRequest request);

    // Call<Void> significa que no esperamos ninguna respuesta JSON del servidor,
    // solo nos importa si la llamada fue exitosa (código 200 OK).
}