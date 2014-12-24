package com.flat.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flat.util.app.PersistentIntentService;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class AppService extends PersistentIntentService {
    private static final String TAG = AppService.class.getSimpleName();



    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }
    };

    public void readSharedPreferences() {

    }
}
