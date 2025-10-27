package com.mpd.hospital;
import android.app.Application;
import com.google.firebase.FirebaseApp;
public class HospitalApp extends  Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
