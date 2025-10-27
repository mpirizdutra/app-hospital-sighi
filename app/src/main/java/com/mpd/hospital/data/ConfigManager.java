package com.mpd.hospital.data;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {

    private static final String PREFS_NAME = "AppConfig";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_SERVER_PORT = "server_port";
    private static final String DEFAULT_IP = "192.168.1.40";
    private static final String DEFAULT_PORT = "3000";

    private SharedPreferences prefs;

    public ConfigManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getServerIp() {
        return prefs.getString(KEY_SERVER_IP, DEFAULT_IP);
    }

    public String getServerPort() {
        return prefs.getString(KEY_SERVER_PORT, DEFAULT_PORT);
    }

    public void setServerIp(String ip) {
        prefs.edit().putString(KEY_SERVER_IP, ip).apply();
    }

    public void setServerPort(String port) {
        prefs.edit().putString(KEY_SERVER_PORT, port).apply();
    }

    public void resetToDefaults() {
        prefs.edit()
                .putString(KEY_SERVER_IP, DEFAULT_IP)
                .putString(KEY_SERVER_PORT, DEFAULT_PORT)
                .apply();
    }
}