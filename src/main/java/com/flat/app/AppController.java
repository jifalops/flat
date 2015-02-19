package com.flat.app;

import android.app.Application;

import com.flat.localization.LocalizationManager2;

public class AppController extends Application {
    private static final String TAG = AppController.class.getSimpleName();

	private static AppController sInstance;
    public static synchronized AppController getInstance() {
        return sInstance;
    }

    private LocalizationManager2 locManager;

	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;
        locManager = LocalizationManager2.getInstance(this);
    }
}
