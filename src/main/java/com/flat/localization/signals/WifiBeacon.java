package com.flat.localization.signals;


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

    private boolean enabled;

    private Timer timer;
    private List<ScanResult> scanResults = new ArrayList<ScanResult>();

    private synchronized void setScanResults(List<ScanResult> results) {
        scanResults = results;
    }
    public synchronized List<ScanResult> getScanResults() {
        return scanResults;
    }

    /*
     * Simple Singleton
     */
    private WifiBeacon() { super("WiFi-beacon"); }
    private static final WifiBeacon instance = new WifiBeacon();
    public static WifiBeacon getInstance() { return instance; }

    /**
     * @param ctx used to get the WifiManager and start a scan.
     * @param interval is an interval in seconds at which scans will repeat, defaulting to 1.
     *                  If a zero is passed, only a single update will be requested.
     */
    public void enable(Context ctx, int interval) {
        if (isEnabled()) return;
        final WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);

        ctx.getApplicationContext().registerReceiver(scanReceiver, scanFilter);

        cancelTimer();
        if (interval == 0) {
            manager.startScan();
        } else {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    manager.startScan();
                }
            }, 0, interval);
        }
        enabled = true;
    }
    @Override
    public void enable(Context ctx) { enable(ctx, 1000); }


    @Override
    public void disable(Context ctx) {
        if (!isEnabled()) return;
        cancelTimer();
        ctx.getApplicationContext().unregisterReceiver(scanReceiver);
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
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
            // Disable if not recurring
            if (timer == null) {
                disable(context);
            }
            setScanResults(((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getScanResults());
            notifyListeners(EVENT_SCAN_RESULTS);
        }
    };

    private final IntentFilter scanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
}
