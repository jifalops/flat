package com.flat.localization.signal;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Obtain WiFi beacons by scanning available networks. Scans can be run periodically or one at a time.
 * Objects that listen for changes in WiFi beacon signals should use getScanResults() when notified of a change.
 * This class implements a singleton pattern.
 */
public final class WifiBeacon extends AbstractSignal {

    public static final int EVENT_SCAN_RESULTS = 1;

    private Timer timer;
    private List<ScanResult> scanResults = new ArrayList<ScanResult>();
    public List<ScanResult> getScanResults() {
        return scanResults;
    }

    /*
     * Singleton
     */
    private WifiBeacon() {}
    private static WifiBeacon instance;
    public static WifiBeacon getInstance() {
        if(instance == null) {
            instance = new WifiBeacon();
        }
        return instance;
    }

    @Override
    public int getSignalType() {
        return Signal.TYPE_ELECTROMAGNETIC;
    }

    /**
     * @param args args[0] is a Context used to get the WifiManager and start a scan.
     *             args[1] is an interval in seconds at which scans will repeat, defaulting to 1.
     *                      If a zero is passed, only a single update will be requested.
     */
    @Override
    public void enable(Object... args) {
        Context ctx = (Context) args[0];
        final WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);

        int scanInterval = 1;
        if (args.length == 2) scanInterval = (Integer) args[1];
        scanInterval *= 1000;

        ctx.registerReceiver(scanReceiver, scanFilter);

        cancelTimer();
        if (scanInterval == 0) {
            manager.startScan();
        } else {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    manager.startScan();
                }
            }, 0, scanInterval);
        }
        enabled = true;
    }

    /**
     * @param args args[0] is a Context used to unregister the BroadcastReceiver.
     */
    @Override
    public void disable(Object... args) {
        cancelTimer();
        Context ctx = (Context) args[0];
        ctx.unregisterReceiver(scanReceiver);
        enabled = false;
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanResults = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getScanResults();

            // Disable if not recurring
            if (timer == null) {
                disable(context);
            }

            notifyListeners(EVENT_SCAN_RESULTS);
        }
    };

    private final IntentFilter scanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
}
