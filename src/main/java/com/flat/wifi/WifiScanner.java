package com.flat.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jacob Phillips.
 */
public class WifiScanner {
    Context context;
    WifiManager manager;
    Timer timer;
    ScanReceiver callback;
    boolean enabled;

    public WifiScanner(Context ctx) {
        context = ctx.getApplicationContext();
        manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    }


    final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (callback != null) {
                callback.onScanResults(manager.getScanResults());
            }
        }
    };

    /** scanPeriod is in millis. */
    public void start(ScanReceiver callback, int scanPeriod) {
        if (enabled) return;

        enabled = true;
        this.callback = callback;
        context.registerReceiver(scanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                manager.startScan();
            }
        }, 0, scanPeriod);
    }
    public void start(ScanReceiver callback) { start(callback, 1000); }


    public void stop() {
        if (!enabled) return;

        enabled = false;
        callback = null;
        context.unregisterReceiver(scanReceiver);
        if (timer != null) {
            timer.cancel();
        }
    }

    public boolean isEnabled() { return enabled; }
}
