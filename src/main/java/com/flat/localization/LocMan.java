package com.flat.localization;


import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.flat.localization.algorithms.Algorithm;
import com.flat.localization.algorithms.AlgorithmMatchCriteria;
import com.flat.localization.node.Node;
import com.flat.localization.node.NodeMessage;
import com.flat.localization.node.NodeRange;
import com.flat.localization.node.NodeState;
import com.flat.localization.node.RemoteNode;
import com.flat.networkservicediscovery.NsdController;
import com.flat.networkservicediscovery.NsdHelper;
import com.flat.remotelogging.CustomRequest;
import com.flat.remotelogging.RangingRequest;
import com.flat.remotelogging.VolleyController;
import com.flat.sockets.MyConnectionSocket;
import com.flat.sockets.MyServerSocket;
import com.flat.wifi.WifiHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Localization Manager
 */
public class LocMan {
    private static final String TAG = LocMan.class.getSimpleName();

    /** Minimum number of remote nodes required to do localization */
    public static final int MIN_NODES = 2;
    /** Minimum amount of time to wait for nodes to connect through NSD. */
    public static final int MIN_NSD_WAIT = 5000;
    /** Maximum amount of time to wait for expected beacons to be received. */
    public static final int MAX_BEACON_WAIT = MIN_NSD_WAIT * 3;

    public static final String NSD_SERVICE_PREFIX = "flatloco_";
    public static final String WIFI_BEACON_SSID_PREFIX = "flatloco_";


    private NodeManager nodeManager;
    public NodeManager getNodeManager() { return nodeManager; }

    private SignalManager signalManager;
    public SignalManager getSignalManager() { return signalManager; }

    private AlgorithmManager algorithmManager;
    public AlgorithmManager getAlgorithmManager() { return algorithmManager; }

    private WifiHelper wifiHelper;
    public WifiHelper getWifiHelper() { return wifiHelper; }

    private NsdController nsdController;
    public NsdController getNsdController() { return nsdController; }

    private VolleyController volleyController;
    public VolleyController getVolleyController() { return volleyController; }

    private String localNodeId;
    public String getLocalNodeId() {
        return localNodeId;
    }

    private Timer beaconTimer;
    private Timer nsdTimer;
    private Context context;


    // Singleton
    private static LocMan instance;
    public static LocMan getInstance(Context ctx) {
        if (instance == null) { instance = new LocMan(ctx); }
        return instance;
    }
    private LocMan(Context ctx) {
        if (ctx == null) return;
        context = ctx;

        wifiHelper = WifiHelper.getInstance(ctx);

        if (!wifiHelper.isWifiEnabled()) {
            wifiHelper.setWifiEnabled(true);
        }

        localNodeId = wifiHelper.getMacAddress();

        initializeManagersAndControllers();
        initializeStaticData();
    }

    private void initializeStaticData() {
        SignalManagerStaticData.initialize(signalManager, nodeManager);
        AlgorithmManagerStaticData.initialize(algorithmManager);
    }

    private void initializeManagersAndControllers() {
        nodeManager = new NodeManager(context, new Node(localNodeId));
        nodeManager.registerListener(nodeManagerListener);

        signalManager = new SignalManager(context);
//        signalManager.registerListener(signalListener);
        algorithmManager = new AlgorithmManager(context);
        nsdController = new NsdController(context, NSD_SERVICE_PREFIX + localNodeId, new NsdHelper.NsdServiceFilter() {
            @Override
            public boolean isAcceptableService(NsdServiceInfo info) {
                return info.getServiceName().startsWith(NSD_SERVICE_PREFIX);
            }
        });
        nsdController.registerListener(nsdContollerListener);

        volleyController = new VolleyController(context);
    }


    // Main power switch in AppServiceFragment
    private boolean enabled;
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        setBeaconTimerEnabled(enabled);

        if (enabled) {
            nsdController.enableNsd();
            //signalManager.enable(this);
            //algorithmManager.enable();
        } else {
            setNsdTimerEnabled(false);
            nsdController.disableNsd();
            signalManager.disable(context);
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

    // TODO
    // Once the local node has shared it's mac with 2 other devices and has the macs of those same 2 devices,
    // It (they) may switch to beacon mode and perform ranging. When the ranges have been collected,
    // they will switch back to NSD mode and share their range tables and perform localization.
    // If beacon mode (wifi AP) could be enabled without disconnecting from the current network, that'd be great.


    private boolean hasEnoughInfoForBeaconMode() {
        return  nsdController.getSocketManager().getConnections().size() >= 2
                && nodeManager.countConnectedNodes() >= 2;
        // TODO check node connected nodes.
    }

    private void setBeaconMode(boolean enabled) {
//        Toast.makeText(this, "Setting beacon mode: " + enabled, Toast.LENGTH_SHORT).show();
        if (!wifiHelper.getSoftApManager().setSsid(WIFI_BEACON_SSID_PREFIX + localNodeId)) {
            Log.e(TAG, "Setting SSID for beacon mode failed.");
        }
        if (!wifiHelper.setSoftApEnabled(enabled)) {
            Log.e(TAG, "Error setting soft AP.");
        }
    }

    private boolean hasEnoughInfoToExchangeRangeTables() {
        int count = 0;
        for (RemoteNode n : nodeManager.getConnectedNodes()) {
            if (n.getRangeHistorySize() > 0) ++count;
        }
        return count >= 2;
    }

    private final NodeManager.NodeManagerListener nodeManagerListener = new NodeManager.NodeManagerListener() {
        @Override
        public void onNodeAdded(RemoteNode n) {

        }

        @Override
        public void onRangePending(RemoteNode n, NodeRange r) {
            n.update(r);
            applyLocationAlgorithms();
        }

        @Override
        public void onStatePending(Node n, NodeState s) {
            n.update(s);
        }

        @Override
        public void onRangeChanged(RemoteNode n, NodeRange r) {
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
            volleyController.addToRequestQueue(req);
        }

        @Override
        public void onStateChanged(Node n, NodeState s) {
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
            RemoteNode n = nodeManager.findNodeByConnection(mcs);
            if (n != null) n.setDataConnection(null);
        }

        @Override
        public void onClientSocketCreated(MyConnectionSocket mcs, Socket socket) {
            handleNewConnection(socket);
        }
    };

    private void sendNodeId(Socket socket) {
        try {
            nsdController.getSocketManager().send(socket.getInetAddress(), new NodeMessage(localNodeId).toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception while sending node id.", e);
        }
    }

    private void sendLocalRangeTable(Socket socket) {
        CoordinateSystem.RangeTable table = nodeManager.getLocalRangeTable();
        if (table == null || table.size() < 2) return;

        try {
            nsdController.getSocketManager().send(socket.getInetAddress(), new NodeMessage(localNodeId, table).toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception while sending range table.", e);
        }
    }

    private void handleNewConnection(Socket socket) {
        Toast.makeText(context, "New connection to: " + socket.getInetAddress().getHostAddress(), Toast.LENGTH_SHORT).show();
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
                mcs.send(new NodeMessage(localNodeId, coords).toString());
            } catch (JSONException e) {
                Log.e(TAG, "JSON exception while sending coordinate system.", e);
            }
        }
    }

    private void handleReceivedMessage(MyConnectionSocket mcs, NodeMessage nm) {
        RemoteNode n = nodeManager.getNode(nm.fromId);
        if (n == null) {
            n = new RemoteNode(nm.fromId);
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
                NodeState state = new NodeState();
                state.referenceFrame = nm.coordinateSystem;
                n.update(state);
                break;
            case NodeMessage.TYPE_CONNECTED_NODES:
                Log.v(TAG, "received connected node list from " + nm.fromId + "@" + mcs.getAddress().getHostAddress());
                n.setConnectedNodes(nm.connectedNodes);
                break;
        }
    }


    private void applyLocationAlgorithms() {
        /*
         * When a range has been obtained by processing a signal (except the internal sensors),
         * notify the registered LocationAlgorithms, which will compare known information about external nodes with
         * the LA's filter, to see if it is able to estimate a new state/position for this node.
         */
        List<NodeState> states = new ArrayList<NodeState>();
        List<RemoteNode> nodes = new ArrayList<RemoteNode>(nodeManager.getNodes());
        AlgorithmMatchCriteria criteria;

        for (Algorithm la : algorithmManager.getAlgorithms()) {
            if (!la.isEnabled()) continue;
            criteria = algorithmManager.getCriteria(la);

            List<Node> filteredNodes = criteria.filter(nodes);
            if (filteredNodes.size() > 0) {
                states.add(la.applyTo(nodeManager.getLocalNode(), filteredNodes));
            }
        }
        for (NodeState s : states) {
            nodeManager.getLocalNode().addPending(s);
        }
    }
}
