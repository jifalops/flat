package com.flat.aa;

import android.content.Context;

import com.flat.wifi.WifiHelper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jacob Phillips.
 */
public class BeaconAndLocalizeMode {

    Context context;
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
        context = ctx;
        wifiHelper = WifiHelper.getInstance(ctx);

    }

    public static int makeBeaconPeriod() {
        return Config.BEACON_PERIOD_MIN_MS + (int) (Math.random() * (Config.BEACON_PERIOD_MAX_MS - Config.BEACON_PERIOD_MIN_MS + 1));
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
               ScanAndDataMode.getInstance(context).start();
            }
        }, makeBeaconPeriod());
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
