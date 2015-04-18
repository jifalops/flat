package com.flat.aa;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.ScanResult;

import com.flat.AppController;
import com.flat.networkservicediscovery.NsdController;
import com.flat.networkservicediscovery.NsdServiceFilter;
import com.flat.wifi.AggregateScanResult;
import com.flat.wifi.ScanAggregator;
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
    NodeManager nodeManager;
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
        scanner = WifiScanner.getInstance(ctx);
        aggregator = new ScanAggregator();

        nsdController = new NsdController(ctx,
                AppController.getInstance().getWifiMac(), new NsdServiceFilter() {
            @Override
            public boolean isAcceptableService(NsdServiceInfo info) {
                return info.getServiceName().startsWith(NSD_SERVICE_PREFIX);
            }
        });
        nsdEventHandler = new NsdEventHandler();

        nodeManager = NodeManager.getInstance();
    }

    public void start() {
        if (enabled) return;
        enabled = true;

        wifiHelper.setSoftApEnabled(false);
        wifiHelper.setWifiEnabled(true);


        scanner.registerListener(aggregator);
        scanner.registerListener(scanListener);
        scanner.start();

        nsdController.registerListener(nsdEventHandler);
        nsdController.enableNsd();
    }

    public void stop() {
        if (!enabled) return;
        enabled = false;

        scanner.unregisterListener(aggregator);
        scanner.unregisterListener(scanListener);
        scanner.stop();
        nsdController.unregisterListener(nsdEventHandler);
        nsdController.disableNsd();
    }

    public boolean isEnabled() { return enabled; }

    final WifiScanner.ScanListener scanListener = new WifiScanner.ScanListener() {
        @Override
        public void onScanResults(List<ScanResult> scanResults) {
            ++scanCount;
            if (scanCount % 10 == 0) {
                for (AggregateScanResult result : aggregator.getResults()) {
                    Node n = nodeManager.getNode(result.bssid);
                    if (n == null) {
                        n = new Node(result.bssid);
                        nodeManager.addNode(n);
                    }
                    result.
                }
            }
        }
    };
}
