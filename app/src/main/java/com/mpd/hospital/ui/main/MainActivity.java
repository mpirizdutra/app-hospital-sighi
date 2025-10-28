package com.mpd.hospital.ui.main;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mpd.hospital.data.ConfigManager;
import com.mpd.hospital.databinding.ActivityMainBinding;
import com.mpd.hospital.ui.settings.SettingsActivity;
import com.mpd.hospital.viewmodel.NavigationViewModel;
import com.mpd.hospital.network.ApiService;
import com.mpd.hospital.network.RegistroDispositivoRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private ApiService apiService;
    private ActivityMainBinding binding;
    private NavigationViewModel viewModel;
    private ConfigManager configManager;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<ScanOptions> qrScanLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
       // WebView.setWebContentsDebuggingEnabled(true);

        configManager = new ConfigManager(this);
        viewModel = new NavigationViewModel(configManager);
        // Configurar Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getHomPage())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Crear una implementación de nuestra interfaz ApiService
        apiService = retrofit.create(ApiService.class);


        setupPermissions();
        setupQRScanner();
        setupWebView();
        setupFabQR();
        setupBackPress();

        loadHomePage();
    }



    private void setupPermissions() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Notificaciones habilitadas", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Notificaciones deshabilitadas", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void setupQRScanner() {
        qrScanLauncher = registerForActivityResult(
                new ScanContract(),
                result -> {
                    if (result.getContents() != null) {
                        handleQRCode(result.getContents());
                    } else {
                        Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void enviarDatosAlServidor(String userId, String fcmToken) {
        // 1. Crear el objeto que enviaremos en el cuerpo de la petición
        RegistroDispositivoRequest requestBody = new RegistroDispositivoRequest(userId, fcmToken);

        // 2. Crear la llamada a la API usando la interfaz
        Call<Void> call = apiService.registrarDispositivo(requestBody);

        Log.d("enviarDatosAlServidor", "Enviando datos al servidor...");

        // 3. Ejecutar la llamada de forma asíncrona (en un hilo secundario)
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Este método se ejecuta cuando el servidor responde
                if (response.isSuccessful()) {
                    // Código 200-299: ¡Éxito!
                    Log.d("enviarDatosAlServidor", "Dispositivo registrado en el servidor exitosamente. Código: " + response.code());
                } else {
                    // El servidor respondió con un error (ej: 400, 404, 500)
                    Log.e("enviarDatosAlServidor", "Error al registrar en el servidor. Código: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Este método se ejecuta si hubo un error de red (ej: sin internet, URL incorrecta)
                Log.e("enviarDatosAlServidor", "Fallo en la llamada de red: " + t.getMessage());
            }
        });
    }


    private void checkForNotificationIntent() {
        // Si la app se abrió desde una notificación
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            //String qrCode = intent.getStringExtra("qr_code");
            String url = intent.getStringExtra("url");

            if ( url != null) {
                // Cargar URL si viene de notificación
                if (url != null && !url.isEmpty()) {
                    String fullUrl = viewModel.buildUrlFromQR(url);
                    binding.webView.loadUrl(fullUrl);
                }
            }
        }
    }

    private void setupFabQR() {
        binding.fabQR.setOnClickListener(v -> startQRScanner());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Cuando la app ya está abierta y llega una notificación
        checkForNotificationIntent();
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }


private void setupWebView() {
    binding.swipeRefreshLayout.setOnRefreshListener(() -> {
        Log.d("SwipeRefresh", "Recarga manual iniciada por el usuario.");
        // Accedemos a la webview también a través del binding
        binding.webView.reload();
    });

    // 4. Configurar el cliente de la WebView para detener el spinner
    binding.webView.setWebViewClient(new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // Si el spinner está activo, lo detenemos.
            if (binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        }
    });
    WebSettings webSettings = binding.webView.getSettings();

    webSettings.setJavaScriptEnabled(true);
    webSettings.setDomStorageEnabled(true);
    webSettings.setLoadWithOverviewMode(true);
    webSettings.setUseWideViewPort(true);
    webSettings.setBuiltInZoomControls(true);
    webSettings.setDisplayZoomControls(false);
    webSettings.setAllowFileAccess(true);


    // 💡 interfaz JavaScript
    binding.webView.addJavascriptInterface(new WebAppInterface(this), "Android");

    binding.webView.setWebViewClient(new CustomWebViewClient(){
        // Se llama CADA VEZ que una nueva página está A PUNTO de empezar a cargarse.
        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // Aunque el usuario no haya deslizado, si empieza una carga nueva,
            // es buena idea mostrar el spinner para dar feedback visual.
            if (!binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(true);
            }
        }

        // Se llama cuando la página (y todas sus redirecciones) ha TERMINADO de cargarse.
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // Ahora sí, cuando todo ha terminado, ocultamos el spinner.
            if (binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        }


    });





}

    public class WebAppInterface {


        private final MainActivity activity;
        WebAppInterface(MainActivity activity) {
            this.activity = activity;
        }

        // Esta función es la que llama el JavaScript: Android.reloadOriginalUrl()
        @android.webkit.JavascriptInterface
        public void reloadOriginalUrl() {
            // Ejecutar en el hilo principal (UI thread)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Llama al método de recarga que definiremos en MainActivity
                    activity.reloadAndShowProgress();
                }
            });
        }

        /**
         * Este método es llamado por JavaScript después de un login exitoso.
         * Recibe el ID del usuario desde la página web.
         * La anotación @JavascriptInterface es OBLIGATORIA.
         *
         * @param userId El ID del usuario que viene como un String desde la web.
         */
        @android.webkit.JavascriptInterface
        public void recibirUsuarioId(String userId) {

            Log.d("WebAppInterface", "ID de usuario recibido desde la web: " + userId);

            // Llamamos a un método en MainActivity para que se encargue de la lógica.
            // Pasarle el `userId` a la Activity principal es una buena práctica
            // para mantener esta clase limpia y centrada solo en la interfaz.
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.asociarUsuarioConToken(userId);
                }
            });
        }


    }


    /**
     * Este es el método que es llamado desde WebAppInterface.
     * Su trabajo es coordinar el envío del userId y el token FCM al servidor.
     *
     * @param userId El ID del usuario que viene de la web.
     */
    public void asociarUsuarioConToken(String userId) {
        Log.d("asociarUsuarioConToken", "Iniciando asociación de usuario " + userId + " con token FCM.");

        // 1. Obtener el token FCM desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE);
        String fcmToken = prefs.getString("FCM_TOKEN", null);

        // 2. Comprobar que tenemos ambos datos
        if (userId != null && !userId.isEmpty() && fcmToken != null && !fcmToken.isEmpty()) {

            Log.d("MainActivityIdToken", "Datos listos para enviar al servidor:");
            Log.d("MainActivityIdToken", " - User ID: " + userId);
            Log.d("MainActivityIdToken", " - FCM Token: " + fcmToken);

            // =============================================================
            // AQUÍ ES DONDE HAREMOS LA LLAMADA AL SERVIDOR CON RETROFIT
            // POR AHORA, SOLO MOSTRAMOS LOS LOGS PARA CONFIRMAR
            // =============================================================
            // TODO: Implementar la llamada de red con Retrofit
             enviarDatosAlServidor(userId, fcmToken);


        } else {
            // Si falta alguno de los datos, lo indicamos en los logs.
            Log.w("MainActivity", "Faltan datos para asociar. No se puede enviar al servidor.");
            if (userId == null || userId.isEmpty()) {
                Log.w("MainActivity", " - User ID es nulo o vacío.");
            }
            if (fcmToken == null || fcmToken.isEmpty()) {
                Log.w("MainActivity", " - FCM Token es nulo o no se ha guardado aún.");
            }
        }
    }

    public void reloadAndShowProgress() {
        // 1. Mostrar la barra de progreso ANTES de la recarga
        // Esto es opcional si el WebChromeClient ya lo hace, pero da una respuesta inmediata


        // 2. Llamar a tu método existente para cargar la URL (obtenida del ViewModel)
        loadHomePage();

        // 3. Ocultar la pantalla de error (si la tuvieras visible en el XML)
        // (Asegúrate de que este layout de error exista en tu XML)
//         if (binding.errorLayout != null) {
//             binding.errorLayout.setVisibility(View.GONE);
//         }
    }


    private class CustomWebViewClient extends WebViewClient {

        private boolean connectionError = false;
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // En tu caso, quieres que la WebView cargue la URL internamente
            view.loadUrl(request.getUrl().toString());
            return true; // Indicamos que hemos manejado la carga
        }
        // Método para la carga de página exitosa/fallida
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // Si no hubo error de conexión, muestra la WebView y oculta el Layout de error (si lo tuvieras)
            if (!connectionError) {
                // Oculta el layout de error si existe
            }
        }

        // Método para interceptar errores de carga (API 23+)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);

            // CÓDIGOS DE ERROR RELEVANTES para problemas de red:
            // ERROR_HOST_LOOKUP: No se puede encontrar el servidor (sin conexión/DNS)
            // ERROR_TIMEOUT: La conexión tardó demasiado (tu ERR_CONNECTION_TIMED_OUT)
            // ERROR_CONNECT: Fallo al intentar conectar

            int errorCode = error.getErrorCode();

            if (request.isForMainFrame() &&
                    (errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
                            errorCode == WebViewClient.ERROR_TIMEOUT ||
                            errorCode == WebViewClient.ERROR_CONNECT)) {

                //  Establecer bandera de error
                connectionError = true;

                //  Evitar que se cargue la página de error por defecto
                //  y en su lugar, cargar tu página local de error
                view.loadUrl("file:///android_asset/pagina_sin_conexion.html");
                //view.loadUrl("file:///sin_web.html");
                // O si tienes un layout de error en el XML:
                // binding.webView.setVisibility(View.GONE);
                // binding.errorLayout.setVisibility(View.VISIBLE);

                // Puedes loguear el error si lo necesitas
                //Log.e("WebViewError", "Error de conexión: " + error.getDescription());
            }
        }
    }

    private void loadHomePage() {
        String homeUrl = viewModel.getHomeUrl();
        binding.webView.loadUrl(homeUrl);
    }

    private String getHomPage(){
        return viewModel.getHomeUrl();
    }

    private void startQRScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Escanea el código QR");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(false);

        qrScanLauncher.launch(options);
    }

    private void handleQRCode(String qrData) {
        String url = viewModel.buildUrlFromQR(qrData);
        if(!url.isEmpty()){
            //binding.webView.loadUrl(url);
            Toast.makeText(this, "Cargando: " + url, Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Qr corrupto: " + qrData  , Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.mpd.hospital.R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == com.mpd.hospital.R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;

        } else if (id == com.mpd.hospital.R.id.action_home) {
            loadHomePage();
            Toast.makeText(this, "Home: " + getHomPage(), Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshConfig();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}