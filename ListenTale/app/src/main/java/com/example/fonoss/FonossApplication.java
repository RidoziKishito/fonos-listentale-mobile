package com.example.fonoss;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class FonossApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
