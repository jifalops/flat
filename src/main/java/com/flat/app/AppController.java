package com.flat.app;

import android.app.Application;
import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.flat.localization.CoordinateSystem;
import com.flat.localization.Node;
import com.flat.localization.NodeManager;
import com.flat.localization.NodeMessage;
import com.flat.wifi.SoftAccessPointManager;
import com.flat.localization.algorithm.AlgorithmMatchCriteria;
import com.flat.localization.algorithm.Algorithm;
import com.flat.localization.algorithm.AlgorithmManager;
import com.flat.localization.algorithm.AlgorithmManagerStaticData;
import com.flat.localization.signal.SignalManagerStaticData;
import com.flat.localization.signal.SignalManager;
import com.flat.wifi.WifiHelper;
import com.flat.remotelogging.CustomRequest;
import com.flat.remotelogging.RangingRequest;
import com.flat.remotelogging.VolleyManager;
import com.flat.networkservicediscovery.NsdController;
import com.flat.networkservicediscovery.NsdHelper;
import com.flat.sockets.MyConnectionSocket;
import com.flat.sockets.MyServerSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AppController extends Application {

	public static final String TAG = AppController.class.getSimpleName();

    public static final String NSD_SERVICE_PREFIX = "flatloco_";
    public static final String WIFI_BEACON_SSID_PREFIX = "flatloco_";

	private static AppController sInstance;
    public static synchronized AppController getInstance() {
        return sInstance;
    }

    // Main power switch in AppServiceFragment
    private boolean enabled;
    private Timer beaconTimer;
    private Timer nsdTimer;

//    private SharedPreferences prefs;

    // TODO bad etiquette
    public NodeManager nodeManager;
    public SignalManager signalManager;
    public AlgorithmManager algorithmManager;
    public NsdController nsdController;
    public VolleyManager volleyManager;
    public SoftAccessPointManager beaconController;
    public WifiManager wifiManager;

    public String id;

	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;

        acquireIdentifier();
        initializeManagersAndControllers();
        initializeStaticData();
    }

    private void acquireIdentifier() {
        // TODO check if wifi is on
        id = WifiHelper.getMacAddress(this);
    }

    private void initializeManagersAndControllers() {
        nodeManager = new NodeManager(this, new Node(id));
        nodeManager.registerListener(nodeManagerListener);

        signalManager = new SignalManager(this);
//        signalManager.registerListener(signalListener);
        algorithmManager = new AlgorithmManager(this);
        nsdController = new NsdController(this, NSD_SERVICE_PREFIX + id, new NsdHelper.NsdServiceFilter() {
            @Override
            public boolean isAcceptableService(NsdServiceInfo info) {
                return info.getServiceName().startsWith(NSD_SERVICE_PREFIX);
            }
        });
        nsdController.registerListener(nsdContollerListener);

        volleyManager = new VolleyManager(this);

        try {
            beaconController = new SoftAccessPointManager(this);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Unable to initialize beaconController.", e);
        }
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    private void initializeStaticData() {
        SignalManagerStaticData.initialize(signalManager, nodeManager);
        AlgorithmManagerStaticData.initialize(algorithmManager);
    }





    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        setBeaconTimerEnabled(enabled);

        if (enabled) {
            nsdController.enableNsd();
            // TODO why did i comment these out?
            //signalManager.enable(this);
            //algorithmManager.enable();
        } else {
            setNsdTimerEnabled(false);
            nsdController.disableNsd();
            signalManager.disable(this);
            algorithmManager.disable();
        }
    }

    private void setBeaconTimerEnabled(boolean enabled) {
        if (beaconTimer != null) {
            beaconTimer.cancel();
            beaconTimer = null; // null check used in class
        }
        if (enabled) {
            beaconTimer = new Timer();
            beaconTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (hasEnoughInfoForBeaconMode()) {
                        nsdController.disableNsd();
                        setBeaconMode(true);

                        beaconTimer.cancel();
                        beaconTimer = null;
                        setNsdTimerEnabled(true);
                    }
                }
            }, 0, (int) (10000 + Math.random() * 10000)); // repeat every 10-20 seconds
        }
    }
    private void setNsdTimerEnabled(boolean enabled) {
        if (nsdTimer != null) {
            nsdTimer.cancel();
            nsdTimer = null;
        }
        if (enabled) {
            nsdTimer = new Timer();
            nsdTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    setBeaconMode(false);
                    if (isEnabled()) nsdController.enableNsd();

                    nsdTimer.cancel();
                    nsdTimer = null;
                    setBeaconTimerEnabled(true);
                }
            }, (int) (10000 + Math.random() * 10000)); // stop after 10-20 seconds
        }
    }

//    private final Signal.SignalListener signalListener = new Signal.SignalListener() {
//        @Override
//        public void onChange(Signal signal, int eventType) {
//            if (signal instanceof WifiBeacon) {
//                processScanResults(((WifiBeacon) signal).getScanResults());
//            }
//        }
//    };
//
//    private void processScanResults(List<ScanResult> beacons) {
//        for (ScanResult sr : beacons) {
//            if (sr.SSID.startsWith(WIFI_BEACON_SSID_PREFIX)) {
//                Node n = nodeManager.getNode(sr.BSSID);
//                if (n != null) {
//                    n.
//                }
//            }
//        }
//    }

    // TODO
    // Once the local node has shared it's mac with 2 other devices and has the macs of those same 2 devices,
    // It (they) may switch to beacon mode and perform ranging. When the ranges have been collected,
    // they will swtich back to NSD mode and share their range tables and perform localization.
    // If beacon mode (wifi AP) could be enabled without disconnecting from the current network, that'd be great.


    private boolean hasEnoughInfoForBeaconMode() {
        return  nsdController.getSocketManager().getConnections().size() >= 2
                && nodeManager.countConnectedNodes() >= 2;
    }

    private void setBeaconMode(boolean enabled) {
//        Toast.makeText(this, "Setting beacon mode: " + enabled, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Setting beacon mode: " + enabled);
        beaconController.setWifiApSsid(WIFI_BEACON_SSID_PREFIX + id);
        beaconController.setWifiApState(beaconController.getWifiApConfiguration(), enabled);
        if (enabled) {
//            signalManager.getSignals()
        } else {
            wifiManager.setWifiEnabled(true);
        }
    }

    private boolean hasEnoughInfoToExchangeRangeTables() {
        int count = 0;
        for (Node n : nodeManager.getConnectedNodes()) {
            if (n.getRangeHistorySize() > 0) ++count;
        }
        return count >= 2;
    }

    private final NodeManager.NodeManagerListener nodeManagerListener = new NodeManager.NodeManagerListener() {
        @Override
        public void onNodeAdded(Node n) {

        }

        @Override
        public void onRangePending(Node n, Node.Range r) {
            n.update(r);
            applyLocationAlgorithms();
        }

        @Override
        public void onStatePending(Node n, Node.State s) {
            n.update(s);
        }

        @Override
        public void onRangeChanged(Node n, Node.Range r) {
            Log.i(TAG, String.format("Range for %s = %s", n.getId(), r.toString()));
            CustomRequest req = new RangingRequest(RangingRequest.makeRequest(r, nodeManager.getLocalNode(), n),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.v(TAG, "Response: " + response.toString());
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Response Error: " + error.getMessage());
                }
            });

            // add the request object to the queue to be executed
            volleyManager.addToRequestQueue(req);
        }

        @Override
        public void onStateChanged(Node n, Node.State s) {
            Log.i(TAG, "State " + n.getId() + ": " + s.toString());
        }
    };



    private final NsdController.NsdContollerListener nsdContollerListener = new NsdController.NsdContollerListener() {

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
            try {
                handleReceivedMessage(mcs, NodeMessage.from(msg));
            } catch (JSONException e) {
                Log.e(TAG, "JSON exception converting received message.", e);
            }
        }

        @Override
        public void onClientFinished(MyConnectionSocket mcs) {
            Log.v(TAG, "lost connection to " + mcs.getAddress().getHostAddress());
            Node n = nodeManager.findNodeByConnection(mcs);
            if (n != null) n.setDataConnection(null);
        }

        @Override
        public void onClientSocketCreated(MyConnectionSocket mcs, Socket socket) {
            handleNewConnection(socket);
        }
    };

    private void sendNodeId(Socket socket) {
        try {
            nsdController.getSocketManager().send(socket.getInetAddress(), new NodeMessage(id).toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception while sending node id.", e);
        }
    }

    private void sendLocalRangeTable(Socket socket) {
        CoordinateSystem.RangeTable table = nodeManager.getLocalRangeTable();
        if (table == null || table.size() < 2) return;

        try {
            nsdController.getSocketManager().send(socket.getInetAddress(), new NodeMessage(id, table).toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception while sending range table.", e);
        }
    }

    private void handleNewConnection(Socket socket) {
        Toast.makeText(this, "New connection to: " + socket.getInetAddress().getHostAddress(), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Total connections: " + nsdController.getSocketManager().getConnections().size());
        sendNodeId(socket);
        sendLocalRangeTable(socket);
    }

    private void sendCoordinateSystem(MyConnectionSocket mcs) {
        CoordinateSystem coords = nodeManager.getLocalNode().getState().referenceFrame;
        if (coords == null) {
            coords = new CoordinateSystem(nodeManager.getRangeTableList());
        }
        if (coords.size() >= 3) {
            try {
                mcs.send(new NodeMessage(id, coords).toString());
            } catch (JSONException e) {
                Log.e(TAG, "JSON exception while sending coordinate system.", e);
            }
        }
    }

    private void handleReceivedMessage(MyConnectionSocket mcs, NodeMessage nm) {
        Node n = nodeManager.getNode(nm.fromId);
        if (n == null) {
            n = new Node(nm.fromId);
            nodeManager.addNode(n);
        } else if (n.getDataConnection() != null) {
            Log.d(TAG, "Node " + n.getId() + " already has data connection (closed=" + mcs.getSocket().isClosed() + ", finished=" + mcs.isFinished() + ").");
        }
        n.setDataConnection(mcs);

        switch (nm.type) {
            case NodeMessage.TYPE_ID:
                Log.v(TAG, "received node ID from " + nm.fromId + "@" + mcs.getAddress().getHostAddress());
                Log.d(TAG, "Total nodes with data connections: " + nodeManager.countConnectedNodes());
                break;
            case NodeMessage.TYPE_RANGE_TABLE:
                Log.v(TAG, "received range table from " + nm.fromId + "@" + mcs.getAddress().getHostAddress());
                n.setRangeTable(nm.rangeTable);
                sendCoordinateSystem(mcs);
                break;
            case NodeMessage.TYPE_COORDINATE_SYSTEM:
                Log.v(TAG, "received coordinate system from " + nm.fromId + "@" + mcs.getAddress().getHostAddress());
                Node.State state = new Node.State();
                state.referenceFrame = nm.coordinateSystem;
                n.update(state);
                break;
        }
    }


    private void applyLocationAlgorithms() {
        /*
         * When a range has been obtained by processing a signal (except the internal sensors),
         * notify the registered LocationAlgorithms, which will compare known information about external nodes with
         * the LA's filter, to see if it is able to estimate a new state/position for this node.
         */
        List<Node.State> states = new ArrayList<Node.State>();
        List<Node> nodes = new ArrayList<Node>(nodeManager.getNodes(false));
        AlgorithmMatchCriteria criteria;

        for (Algorithm la : algorithmManager.getAlgorithms()) {
            if (!la.isEnabled()) continue;
            criteria = algorithmManager.getCriteria(la);

            List<Node> filteredNodes = criteria.filter(nodes);
            if (filteredNodes.size() > 0) {
                states.add(la.applyTo(nodeManager.getLocalNode(), filteredNodes));
            }
        }
        for (Node.State s : states) {
            nodeManager.getLocalNode().addPending(s);
        }
    }


}
