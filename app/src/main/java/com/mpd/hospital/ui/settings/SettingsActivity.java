package com.mpd.hospital.ui.settings;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


import com.mpd.hospital.data.ConfigManager;
import com.mpd.hospital.databinding.ActivitySettingsBinding;
//import com.mpd.hospital.qrscanner.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    //private ActivitySettingsBinding binding;
    private ActivitySettingsBinding binding;
    private ConfigManager configManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        configManager = new ConfigManager(this);

        loadCurrentConfig();
        setupListeners();
    }

    private void loadCurrentConfig() {
        binding.etIp.setText(configManager.getServerIp());
        binding.etPort.setText(configManager.getServerPort());
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> saveConfiguration());
        binding.btnReset.setOnClickListener(v -> resetConfiguration());
    }

    private void saveConfiguration() {
        String ip = binding.etIp.getText().toString().trim();
        String port = binding.etPort.getText().toString().trim();

        if (ip.isEmpty()) {
            Toast.makeText(this, "La IP no puede estar vacía", Toast.LENGTH_SHORT).show();
            return;
        }

        if (port.isEmpty()) {
            Toast.makeText(this, "El puerto no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        configManager.setServerIp(ip);
        configManager.setServerPort(port);

        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void resetConfiguration() {
        configManager.resetToDefaults();
        loadCurrentConfig();
        Toast.makeText(this, "Configuración restablecida", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}