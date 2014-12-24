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

    public void readSharedPreferences() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    }
}
