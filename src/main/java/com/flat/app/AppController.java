package com.flat.app;

import android.app.Application;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.flat.localization.Node;
import com.flat.localization.NodeManager;
import com.flat.localization.NodeMessage;
import com.flat.localization.algorithm.Criteria;
import com.flat.localization.algorithm.LocationAlgorithm;
import com.flat.localization.algorithm.LocationAlgorithmManager;
import com.flat.localization.data.LocationAlgorithms;
import com.flat.localization.data.Signals;
import com.flat.localization.signal.SignalManager;
import com.flat.localization.util.Util;
import com.flat.loggingrequests.CustomRequest;
import com.flat.loggingrequests.RangingRequest;
import com.flat.loggingrequests.VolleyController;
import com.flat.nsd.NsdController;
import com.flat.nsd.NsdHelper;
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


	private static AppController sInstance;
    public static synchronized AppController getInstance() {
        return sInstance;
    }

    // Main power switch in AppServiceFragment
    private boolean enabled;
    private Timer timer;

//    private SharedPreferences prefs;

    // TODO these probably shouldn't be public
    public NodeManager nodeManager;
    public SignalManager signalManager;
    public LocationAlgorithmManager algorithmManager;
    public NsdController nsdController;
    public VolleyController volleyController;

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
        id = Util.getWifiMac(this);
    }

    private void initializeManagersAndControllers() {
        nodeManager = new NodeManager(this, new Node(id));
        nodeManager.registerListener(nodeManagerListener);

        signalManager = new SignalManager(this);
        algorithmManager = new LocationAlgorithmManager(this);
        nsdController = new NsdController(this, NSD_SERVICE_PREFIX + id, new NsdHelper.NsdServiceFilter() {
            @Override
            public boolean isAcceptableService(NsdServiceInfo info) {
                return info.getServiceName().startsWith(NSD_SERVICE_PREFIX);
            }
        });
        nsdController.registerListener(nsdContollerListener);

        volleyController = new VolleyController(this);
    }

    private void initializeStaticData() {
        Signals.initialize(signalManager, nodeManager);
        LocationAlgorithms.initialize(algorithmManager);
    }





    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (timer != null) {
            timer.cancel();
        }

        if (enabled) {
            nsdController.enableNsd();
            //signalManager.enable(this);
            //algorithmManager.enable();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    checkConnections();
                }
            }, 0, 5000);
        } else {
            nsdController.disableNsd();
            signalManager.disable(this);
            algorithmManager.disable();
        }
    }

    // TODO
    // Once the local node has shared it's mac with 2 other devices and has the macs of those same 2 devices,
    // It (they) may switch to beacon mode and perform ranging. When the ranges have been collected,
    // they will swtich back to NSD mode and share their range tables and perform localization.
    // If beacon mode (wifi AP) could be enabled without disconnecting from the current network, that'd be great.




    private void checkConnections() {
        if (nsdController.getSocketManager().getConnections().size() >= 2) {
            setBeaconMode(true);
        }
    }

    private void setBeaconMode(boolean enabled) {
        if (enabled) {
            
        } else {

        }
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
            volleyController.addToRequestQueue(req);
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
            sendNodeId(socket);
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
            sendNodeId(socket);
        }
    };

    private void sendNodeId(Socket socket) {
        try {
            nsdController.getSocketManager().send(socket.getInetAddress(), new NodeMessage(id).toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception while sending node id.", e);
        }
    }

    private void handleReceivedMessage(MyConnectionSocket mcs, NodeMessage nm) {
        switch (nm.type) {
            case NodeMessage.TYPE_ID:
                Log.v(TAG, "received node ID from " + nm.fromId + "@" + mcs.getAddress().getHostAddress());
                Node n = nodeManager.getNode(nm.fromId);
                if (n == null) {
                    n = new Node(nm.fromId);
                    nodeManager.addNode(n);
                }
                n.setDataConnection(mcs);
                break;
            case NodeMessage.TYPE_RANGE_TABLE:
                Log.v(TAG, "received range table from " + nm.fromId + "@" + mcs.getAddress().getHostAddress());
                break;
            case NodeMessage.TYPE_COORDINATE_SYSTEM:
                Log.v(TAG, "received coordinate system from " + nm.fromId + "@" + mcs.getAddress().getHostAddress());
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
        Criteria.AlgorithmMatchCriteria criteria;

        for (LocationAlgorithm la : algorithmManager.getAlgorithms()) {
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
