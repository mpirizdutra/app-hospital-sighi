package com.mpd.hospital.ui.main;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavigationViewModel viewModel;
    private ConfigManager configManager;
   // private FCMTokenManager fcmTokenManager;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<ScanOptions> qrScanLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        configManager = new ConfigManager(this);
        viewModel = new NavigationViewModel(configManager);


        setupPermissions();
        setupQRScanner();
        setupWebView();
        setupFabQR();
        setupBackPress();

        loadHomePage();
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



    private void checkForNotificationIntent() {
        // Si la app se abrió desde una notificación
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            String qrCode = intent.getStringExtra("qr_code");
            String url = intent.getStringExtra("url");

            if (qrCode != null || url != null) {
                // Cargar URL si viene de notificación
                if (url != null && !url.isEmpty()) {
                    String fullUrl = viewModel.buildUrlFromQR(url);
                    binding.webView.loadUrl(fullUrl);
                } else if (qrCode != null) {
                    String fullUrl = viewModel.buildUrlFromQR(qrCode);
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
        WebSettings webSettings = binding.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(android.webkit.WebView view, android.webkit.WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });
    }

    private void loadHomePage() {
        String homeUrl = viewModel.getHomeUrl();
        binding.webView.loadUrl(homeUrl);
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
            Toast.makeText(this, "Cargando: " + qrData, Toast.LENGTH_SHORT).show();
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
            return true;
        } else if (id == com.mpd.hospital.R.id.action_reload) {
            binding.webView.reload();
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