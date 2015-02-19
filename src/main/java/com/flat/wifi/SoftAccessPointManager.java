package com.flat.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Method;

/**
* This is based off of a response on Stack Overflow.
*/
public final class SoftAccessPointManager {
    private static final String TAG = SoftAccessPointManager.class.getSimpleName();
    private static final int WIFI_AP_STATE_FAILED = 14; // from WifiManager code



    private WifiManager mWifiManager;
    private Method wifiControlMethod;
    private Method wifiApConfigurationMethod;
    private Method wifiApState;
    private boolean softApEnabled;

    // Singleton
    private static SoftAccessPointManager instance;
    public static SoftAccessPointManager getInstance(Context ctx) throws NoSuchMethodException {
        if (instance == null) instance = new SoftAccessPointManager(ctx);
        return instance;
    }
    private SoftAccessPointManager(Context context) throws NoSuchMethodException {
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        wifiControlMethod = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
        wifiApConfigurationMethod = mWifiManager.getClass().getMethod("getWifiApConfiguration", null);
        wifiApState = mWifiManager.getClass().getMethod("getWifiApState");
    }


    public WifiConfiguration getConfig() {
        try {
            return (WifiConfiguration) wifiApConfigurationMethod.invoke(mWifiManager, null);
        } catch (Exception e) {
            return null;
        }
    }

    public int getState() {
        try {
            return (Integer) wifiApState.invoke(mWifiManager);
        } catch (Exception e) {
            Log.e(TAG, "Error getting soft AP state (int)", e);
            return WIFI_AP_STATE_FAILED;
        }
    }

    public boolean setSsid(String ssid) {
        WifiConfiguration config = getConfig();
        if (config != null) {
            config.SSID = ssid;
            return true;
        }
        return false;
    }

    public String getSsid() {
        WifiConfiguration config = getConfig();
        return config == null ? null : config.SSID;
    }

    public String getBssid() {
        WifiConfiguration config = getConfig();
        return config == null ? null : config.BSSID;
    }

    public boolean setSsidHidden(boolean hidden) {
        WifiConfiguration config = getConfig();
        if (config != null) {
            config.hiddenSSID = hidden;
            return true;
        }
        return false;
    }

    public boolean isSsidHidden() {
        WifiConfiguration config = getConfig();
        return config != null && config.hiddenSSID;
    }

    public boolean setEnabled(boolean enabled) {
        return setEnabled(enabled, null);
    }

    public boolean setEnabled(boolean enabled, WifiConfiguration config) {
        if (config == null) config = getConfig();

        if (enabled) mWifiManager.setWifiEnabled(false);
        try { softApEnabled = (Boolean) wifiControlMethod.invoke(mWifiManager, config, enabled); }
        catch (Exception e) {
            Log.e(TAG, "Error setting soft AP state.", e);
        }
        if (!enabled) mWifiManager.setWifiEnabled(true);
        return softApEnabled;
    }

    public boolean isEnabled() {
        return softApEnabled;
    }
}
