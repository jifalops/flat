package com.flat.aa;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.flat.AppController;
import com.flat.networkservicediscovery.NsdController;
import com.flat.networkservicediscovery.NsdServiceFilter;
import com.flat.wifi.ScanAggregator;
import com.flat.wifi.ScanReceiver;
import com.flat.wifi.WifiHelper;
import com.flat.wifi.WifiScanner;

import java.util.List;

/**
 * Created by Jacob Phillips.
 */
public class ScanAndDataMode {
    public static final String NSD_SERVICE_PREFIX = "flatloco_";

    WifiHelper wifiHelper;
    WifiScanner scanner;
    ScanAggregator aggregator;
    NsdController nsdController;
    NsdEventHandler nsdEventHandler;
    boolean enabled;
    int scanCount = 0;

    private static ScanAndDataMode instance;
    public static ScanAndDataMode getInstance(Context ctx) {
        if (instance == null) {
            instance = new ScanAndDataMode(ctx.getApplicationContext());
        }
        return instance;
    }
    private ScanAndDataMode(Context ctx) {
        wifiHelper = WifiHelper.getInstance(ctx);
        scanner = new WifiScanner(ctx);
        aggregator = new ScanAggregator();

        nsdController = new NsdController(ctx,
                AppController.getInstance().getWifiMac(), new NsdServiceFilter() {
            @Override
            public boolean isAcceptableService(NsdServiceInfo info) {
                return info.getServiceName().startsWith(NSD_SERVICE_PREFIX);
            }
        });
        nsdEventHandler = new NsdEventHandler();
    }

    public void start() {
        if (enabled) return;
        enabled = true;

        wifiHelper.setSoftApEnabled(false);
        wifiHelper.setWifiEnabled(true);

        scanner.start(new ScanReceiver() {
            @Override
            public void onScanResults(List<ScanResult> scanResults) {
                aggregator.onScanResults(scanResults);
                ++scanCount;
                if (scanCount % 10 == 0) {
                    // TODO check for new best rssi and localizationalize.
                }
            }
        });

        nsdController.registerListener(nsdEventHandler);
        nsdController.enableNsd();
    }

    public void stop() {
        if (!enabled) return;
        enabled = false;

        scanner.stop();
        nsdController.unregisterListener(nsdEventHandler);
        nsdController.disableNsd();
    }

    public boolean isEnabled() { return enabled; }
}
