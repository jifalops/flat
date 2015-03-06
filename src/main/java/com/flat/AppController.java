package com.flat;

import android.app.Application;
import android.content.Intent;

import com.flat.localization.LocMan;
import com.flat.util.PersistentIntentService;

public class AppController extends Application {
    private static final String TAG = AppController.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    private static AppController instance;
    public static synchronized AppController getInstance() {
        return instance;
    }

    private LocMan locManager;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
        locManager = LocMan.getInstance(this);


    }


    /**
     * {@link com.flat.app.MainFragment} binds to and can start this service
     * to keep it running in the background.
     */
    public static class AppService extends PersistentIntentService {
        @Override
        protected void onHandleIntent(Intent intent) {

        }
    }


}
