package com.flat.app;

import android.app.Application;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.flat.localization.Node;
import com.flat.localization.NodeManager;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AppController extends Application {

	public static final String TAG = AppController.class.getSimpleName();

    public static final String NSD_SERVICE_PREFIX = "flatloco_";


	private static AppController sInstance;
    public static synchronized AppController getInstance() {
        return sInstance;
    }

    // Main power switch in AppServiceFragment
    private boolean enabled;

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
//        prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        controller = Controller.getInstance(this); // will populate the model.
//        model = Model.getInstance();
//        model.registerListener(modelListener);

        acquireIdentifier();
        initializeManagersAndControllers();
        initializeStaticData();

    }

    private void acquireIdentifier() {
        // TODO check if wifi is on and connected
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

        volleyController = new VolleyController(this);
    }

    private void initializeStaticData() {
        Signals.initialize(signalManager);
        LocationAlgorithms.initialize(algorithmManager);
    }





    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (enabled) {
            nsdController.enableNsd();
            signalManager.enable(this);
            algorithmManager.enable();
        } else {
            nsdController.disableNsd();
            signalManager.disable(this);
            algorithmManager.disable();
        }
    }



    private final NodeManager.NodeManagerListener nodeManagerListener = new NodeManager.NodeManagerListener() {
        @Override
        public void onNodeAdded(Node n) {
            n.registerListener(nodeListener);
        }
    };

    private final Node.NodeListener nodeListener = new Node.NodeListener() {
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
