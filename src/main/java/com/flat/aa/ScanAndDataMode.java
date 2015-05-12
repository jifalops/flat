package com.flat.aa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.ScanResult;
import android.util.Log;

import com.flat.AppController;
import com.flat.localization.signals.interpreters.FreeSpacePathLoss;
import com.flat.networkservicediscovery.NsdController;
import com.flat.networkservicediscovery.NsdServiceFilter;
import com.flat.sockets.MyConnectionSocket;
import com.flat.sockets.MyServerSocket;
import com.flat.wifi.AggregateScanResult;
import com.flat.wifi.ScanAggregator;
import com.flat.wifi.WifiHelper;
import com.flat.wifi.WifiScanner;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Created by Jacob Phillips.
 */
public class ScanAndDataMode {
    static final String TAG = ScanAndDataMode.class.getSimpleName();
    static final String NSD_SERVICE_PREFIX = "flatloco_";

    Context context;
    WifiHelper wifiHelper;
    WifiScanner scanner;
    ScanAggregator aggregator;
    NsdController nsdController;
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
        context = ctx;
        wifiHelper = WifiHelper.getInstance(ctx);
        scanner = WifiScanner.getInstance(ctx);
        aggregator = new ScanAggregator();

        nsdController = new NsdController(ctx,
                NSD_SERVICE_PREFIX + AppController.getInstance().getWifiMac(), new NsdServiceFilter() {
            @Override
            public boolean isAcceptableService(NsdServiceInfo info) {
                return info.getServiceName().startsWith(NSD_SERVICE_PREFIX);
            }
        });

        nodeManager = NodeManager.getInstance();
    }

    public void start() {
        if (enabled) return;
        enabled = true;

        wifiHelper.setSoftApEnabled(false);
        wifiHelper.setWifiEnabled(true);

        context.registerReceiver(connChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void stop() {
        if (!enabled) return;
        enabled = false;

        scanner.unregisterListener(scanListener);
        scanner.stop();

        nsdController.unregisterListener(nsdContollerListener);
        nsdController.disableNsd();
    }

    public boolean isEnabled() { return enabled; }

    public static int makeScanLimit() {
        return Config.SCAN_MIN_SCANS + (int) (Math.random() * (Config.SCAN_MAX_SCANS - Config.SCAN_MIN_SCANS + 1));
    }

    final WifiScanner.ScanListener scanListener = new WifiScanner.ScanListener() {
        int scanLimit = makeScanLimit();
        @Override
        public void onScanResults(List<ScanResult> scanResults) {
            Log.v(TAG, "Received scan results");
            aggregator.processScanResults(scanResults);

            ++scanCount;
            if (scanCount % scanLimit == 0) {
                scanLimit = makeScanLimit();

                RangeTable rangeTable = nodeManager.getLocalNode().getRangeTable();
                for (AggregateScanResult result : aggregator.getResults()) {
                    Node n = nodeManager.getNode(result.bssid);
                    if (n == null) {
                        n = new Node(result.bssid);
                        nodeManager.addNode(n);
                    }
                    n.setSsid(result.ssid);
                    int rssi = result.effectiveRssi();
                    FreeSpacePathLoss fspl = new FreeSpacePathLoss();
                    float range = fspl.fromDbMhz(rssi, result.freq);
                    RangeTable.Entry entry = rangeTable.getEntry(result.bssid);
                    if (entry == null) {
                        entry = new RangeTable.Entry();
                    }
                    entry.algorithm = fspl.getName();
                    entry.bssid = result.bssid;
                    entry.freq = result.freq;
                    entry.range = range;
                    entry.rssi = rssi;
                    entry.ssid = result.ssid;
                    entry.time = System.currentTimeMillis();

                    rangeTable.putEntry(entry);
                }
                nodeManager.getLocalNode().setRangeTable(rangeTable);

                stop();
                BeaconAndLocalizeMode.getInstance(context).start();
            }
        }
    };

    final NsdController.NsdContollerListener nsdContollerListener = new NsdController.NsdContollerListener() {
        @Override
        public void onServiceRegistered(NsdServiceInfo info) {
            Log.v(TAG, "onServiceRegistered(): " + info.toString());
        }

        @Override
        public void onAcceptableServiceResolved(NsdServiceInfo info) {
            Log.v(TAG, "onAcceptableServiceResolved(): " + info.toString());
        }

        @Override
        public void onServerAcceptedClientSocket(MyServerSocket mss, Socket socket) {
            Log.v(TAG, "onServerAcceptedClientSocket()");
            handleNewConnection(socket);
        }

        @Override
        public void onServerFinished(MyServerSocket mss) {
            Log.v(TAG, "onServerFinished()");

        }

        @Override
        public void onServerSocketListening(MyServerSocket mss, ServerSocket socket) {
            Log.v(TAG, "listening for incoming connections on port " + socket.getLocalPort());

        }

        @Override
        public void onMessageSent(MyConnectionSocket mcs, String msg) {
            Log.v(TAG, "onMessageSent(): " + msg);

        }

        @Override
        public void onMessageReceived(MyConnectionSocket mcs, String msg) {
            Log.v(TAG, "onMessageReceived(): " + msg);
            handleReceivedMessage(mcs, msg);
        }

        @Override
        public void onClientFinished(MyConnectionSocket mcs) {
            Log.i(TAG, "lost connection to " + mcs.getAddress().getHostAddress());
            Node n = nodeManager.getNodeByConnection(mcs);
            if (n != null) n.setConnection(null);
        }

        @Override
        public void onClientSocketCreated(MyConnectionSocket mcs, Socket socket) {
            Log.v(TAG, "onClientSocketCreated()");
            handleNewConnection(socket);
        }

        void handleNewConnection(Socket socket) {
            Log.e(TAG, "##### Received connection to " + socket.getInetAddress().getHostAddress());
            nsdController.getSocketManager().send(socket.getInetAddress(), nodeManager.getLocalNode().toString());
        }

        void handleReceivedMessage(MyConnectionSocket conn, String msg) {
            Node newNode = Node.from(msg);
            Node existingNode = nodeManager.getNode(newNode.getId());
            if (existingNode == null) {
                newNode.setConnection(conn);
                nodeManager.addNode(newNode);
            } else {
                existingNode.setConnection(conn);
                existingNode.setRangeTable(newNode.getRangeTable());
                existingNode.setCoords(newNode.getCoords());
            }
        }
    };

    final BroadcastReceiver connChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (enabled && wifiHelper.isConnected()) {
                scanner.registerListener(scanListener);
                scanner.start();

                nsdController.registerListener(nsdContollerListener);
                nsdController.enableNsd();
                context.unregisterReceiver(connChangeReceiver);
            }
        }
    };
}
