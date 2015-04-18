package com.flat.aa;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.ScanResult;
import android.util.Log;

import com.flat.AppController;
import com.flat.localization.node.NodeMessage;
import com.flat.localization.signals.interpreters.FreeSpacePathLoss;
import com.flat.networkservicediscovery.NsdController;
import com.flat.networkservicediscovery.NsdServiceFilter;
import com.flat.sockets.MyConnectionSocket;
import com.flat.sockets.MyServerSocket;
import com.flat.wifi.AggregateScanResult;
import com.flat.wifi.ScanAggregator;
import com.flat.wifi.WifiHelper;
import com.flat.wifi.WifiScanner;

import org.json.JSONException;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Created by Jacob Phillips.
 */
public class ScanAndDataMode {
    static final String TAG = ScanAndDataMode.class.getSimpleName();
    static final String NSD_SERVICE_PREFIX = "flatloco_";

    BeaconAndLocalizeMode beaconAndLocalizeMode;
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
        beaconAndLocalizeMode = BeaconAndLocalizeMode.getInstance(ctx);
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

        nodeManager = NodeManager.getInstance();
    }

    public void start() {
        if (enabled) return;
        enabled = true;

        wifiHelper.setSoftApEnabled(false);
        wifiHelper.setWifiEnabled(true);


        scanner.registerListener(scanListener);
        scanner.start();

        nsdController.registerListener(nsdContollerListener);
        nsdController.enableNsd();
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

    final WifiScanner.ScanListener scanListener = new WifiScanner.ScanListener() {
        @Override
        public void onScanResults(List<ScanResult> scanResults) {
            aggregator.processScanResults(scanResults);

            ++scanCount;
            if (scanCount % 10 == 0) {
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
                beaconAndLocalizeMode.start();
            }
        }
    };

    final NsdController.NsdContollerListener nsdContollerListener = new NsdController.NsdContollerListener() {
        @Override
        public void onServiceRegistered(NsdServiceInfo info) {

        }

        @Override
        public void onAcceptableServiceResolved(NsdServiceInfo info) {

        }

        @Override
        public void onServerAcceptedClientSocket(MyServerSocket mss, Socket socket) {
            handleNewConnection(socket);
        }

        @Override
        public void onServerFinished(MyServerSocket mss) {

        }

        @Override
        public void onServerSocketListening(MyServerSocket mss, ServerSocket socket) {

        }

        @Override
        public void onMessageSent(MyConnectionSocket mcs, String msg) {

        }

        @Override
        public void onMessageReceived(MyConnectionSocket mcs, String msg) {
            handleReceivedMessage(mcs, msg);
        }

        @Override
        public void onClientFinished(MyConnectionSocket mcs) {
//            Log.v(TAG, "lost connection to " + mcs.getAddress().getHostAddress());
            Node n = nodeManager.getNodeByConnection(mcs);
            if (n != null) n.setConnection(null);
        }

        @Override
        public void onClientSocketCreated(MyConnectionSocket mcs, Socket socket) {
            handleNewConnection(socket);
        }

        void handleNewConnection(Socket socket) {
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
}
