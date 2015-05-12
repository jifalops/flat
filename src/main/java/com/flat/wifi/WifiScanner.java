package com.flat.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.flat.aa.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jacob Phillips.
 */
public class WifiScanner {
    Context context;
    WifiManager manager;
    Timer timer;
    boolean enabled;

    private static WifiScanner instance;
    public static WifiScanner getInstance(Context ctx) {
        if (instance == null) {
            instance = new WifiScanner(ctx.getApplicationContext());
        }
        return instance;
    }
    private WifiScanner(Context ctx) {
        context = ctx;
        manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    }


    final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> results = manager.getScanResults();
            for (ScanListener l : listeners) {
                l.onScanResults(results);
            }
        }
    };

    public void start() {
        if (enabled) return;
        enabled = true;

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
        }, 0, Config.SCAN_PERIOD_MS);
    }


    public void stop() {
        if (!enabled) return;
        enabled = false;

        context.unregisterReceiver(scanReceiver);
        if (timer != null) {
            timer.cancel();
        }
    }

    public boolean isEnabled() { return enabled; }


    /**
     * Allow other objects to react to node events.
     */
    public interface ScanListener {
        void onScanResults(List<ScanResult> scanResults);
    }
    private final List<ScanListener> listeners = new ArrayList<ScanListener>(1);
    public boolean registerListener(ScanListener l) {
        return !listeners.contains(l) && listeners.add(l);
    }
    public boolean unregisterListener(ScanListener l) {
        return listeners.remove(l);
    }
}
