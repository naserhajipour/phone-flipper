package com.phoneflipper.app.core;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

/** Applies the user's language choice to every Activity context (per-app locale). */
public final class LocaleManager {

    private LocaleManager() {
    }

    public static Context wrap(Context base) {
        String lang = Prefs.language(base);
        if (Prefs.LANG_SYSTEM.equals(lang)) {
            return base;
        }
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        Context wrapped = base.createConfigurationContext(config);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wrapped.getResources().getConfiguration().setLocale(locale);
        }
        return wrapped;
    }

    public static boolean isPersian(Context c) {
        return "fa".equals(c.getResources().getConfiguration().getLocales().get(0).getLanguage());
    }
}
