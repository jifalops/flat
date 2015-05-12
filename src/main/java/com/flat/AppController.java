package com.flat;

import android.app.Application;
import android.content.Intent;
import android.text.TextUtils;

import com.flat.aa.ScanAndDataMode;
import com.flat.util.PersistentIntentService;
import com.flat.wifi.WifiHelper;

public class AppController extends Application {
    private static final String TAG = AppController.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    private static AppController instance;
    public static AppController getInstance() {
        return instance;
    }

    private String wifiMac;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;


//        locManager = LocalizationManager2.getInstance(this);
        ScanAndDataMode.getInstance(this).start();
    }

    public String getWifiMac() {
        if (TextUtils.isEmpty(wifiMac)) {
            WifiHelper.getInstance(this).setWifiEnabled(true);
            wifiMac = WifiHelper.getInstance(this).getMacAddress();
        }
        return wifiMac;
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
