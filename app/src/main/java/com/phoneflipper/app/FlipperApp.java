package com.phoneflipper.app;

import android.app.Application;
import android.content.Context;

import com.phoneflipper.app.core.LocaleManager;

public class FlipperApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrap(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
