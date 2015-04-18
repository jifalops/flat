package com.flat.aa;

import android.content.Context;

import com.flat.wifi.WifiHelper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jacob Phillips.
 */
public class BeaconAndLocalizeMode {

    ScanAndDataMode scanAndDataMode;
    WifiHelper wifiHelper;
    boolean enabled;
    Timer timer;

    private static BeaconAndLocalizeMode instance;
    public static BeaconAndLocalizeMode getInstance(Context ctx) {
        if (instance == null) {
            instance = new BeaconAndLocalizeMode(ctx.getApplicationContext());
        }
        return instance;
    }
    private BeaconAndLocalizeMode(Context ctx) {
        scanAndDataMode = ScanAndDataMode.getInstance(ctx);
        wifiHelper = WifiHelper.getInstance(ctx);

    }

    public void start() {
        if (enabled) return;
        enabled = true;

        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
               stop();
               scanAndDataMode.start();
            }
        }, 5000);
        wifiHelper.setWifiEnabled(false);
        wifiHelper.setSoftApEnabled(true);

        // Magic
        Localizer.localize();
    }

    public void stop() {
        if (!enabled) return;
        enabled = false;

        if (timer != null) {
            timer.cancel();
        }
        wifiHelper.setSoftApEnabled(false);
    }

    public boolean isEnabled() { return enabled; }
}
