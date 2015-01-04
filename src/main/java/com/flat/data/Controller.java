package com.flat.data;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.flat.app.AppController;
import com.flat.localization.algorithm.Criteria;
import com.flat.localization.algorithm.LocationAlgorithm;
import com.flat.localization.node.Node;
import com.flat.localization.signal.Signal;
import com.flat.localization.util.Util;
import com.flat.loggingrequests.CustomRequest;
import com.flat.loggingrequests.RangingRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO bad embedded references in this comment
 * The Controller controls the progression of a {@code Node}'s {@code Node.State} by incorporating
 * various {@code Signal}s. A signal undergoes {@code Ranging} to be converted into a linear distance,
 * or in the case of the gyroscope, a new orientation. Then, a {@code LocationAlgorithm} uses one or more ranges
 * to calculate a new node state. The Controller may accept or reject this as the nodes new state.
 */
public final class Controller implements Model.ModelListener, Node.NodeListener {
    private static final String TAG = Controller.class.getSimpleName();

    final Node me;
    final Model model = Model.getInstance();
    final Bundle extras = new Bundle();

    /*
     * Singleton
     */
    private Controller(Context ctx) {
        me = new Node(Util.getWifiMac(ctx));
        populateModel();
        me.registerListener(this);
    }
    private static Controller instance;
    private final static Object syncObj = new Object();
    public static Controller getInstance(Context ctx) {
        synchronized (syncObj) {
            if (instance == null) {
                instance = new Controller(ctx);
            }
        }
        return instance;
    }


    private String getKey(Node.Range r) {
        return r.signal + r.interpreter;
    }
    private String getKey(Signal sig, Node.State st) {
        return sig.getName() + st.algorithm;
    }


    private void populateModel() {
        model.registerListener(this);








    }

    @Override
    public void onNodeAdded(Node n) {
        n.registerListener(this);
        Log.i(TAG, "Node count: " + model.getNodeCount());
    }

    @Override
    public void onRangePending(Node n, Node.Range r) {
        n.update(r);
        applyLocationAlgorithms();
    }

    private void applyLocationAlgorithms() {
        /*
         * When a range has been obtained by processing a signal (except the internal sensors),
         * notify the registered LocationAlgorithms, which will compare known information about external nodes with
         * the LA's filter, to see if it is able to estimate a new state/position for this node.
         */
        List<Node.State> states = new ArrayList<Node.State>();
        List<Node> nodes = new ArrayList<Node>(model.getNodesCopy());
        LocationAlgorithm la;
        Criteria.AlgorithmMatchCriteria criteria;

        for (Map.Entry<LocationAlgorithm, Criteria.AlgorithmMatchCriteria> entry : model.algorithms.entrySet()) {
            la = entry.getKey();
            if (!la.isEnabled()) continue;
            criteria = entry.getValue();

            List<Node> filteredNodes = criteria.filter(nodes);
            if (filteredNodes.size() > 0) {
                states.add(la.applyTo(me, filteredNodes));
            }
        }
        for (Node.State s : states) {
            me.addPending(s);
        }
    }

    @Override
    public void onStatePending(Node n, Node.State s) {
//        Log.i(TAG, "Pending state for " + n.getId() + ": " + s.toString());
        n.update(s);
    }

    @Override
    public void onRangeChanged(Node n, Node.Range r) {
        Log.i(TAG, String.format("Range for %s = %s", n.getId(), r.toString()));

//        JSONObject json = new JSONObject();
//        try {
//            json.put("a", "1");
//            json.put("b", "x");
//            json.put("c3","asdfoim");
//        } catch (JSONException e) {
//            Log.e(TAG, "Error forming json object");
//        }
//
//
//
//        RangingRequest rr = new RangingRequest(r, me, n, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject jsonObject) {
//                Log.e(TAG, "Response: " + jsonObject.toString());
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError volleyError) {
//                Log.d(TAG, "Response Error: " + volleyError.toString());
//            }
//        });
//        rr.queue();

        // Post params to be sent to the server

        CustomRequest req = new RangingRequest(RangingRequest.makeRequest(r, me, n),
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
        AppController.getInstance().addToRequestQueue(req);
    }

    @Override
    public void onStateChanged(Node n, Node.State s) {
        Log.i(TAG, "State " + n.getId() + ": " + s.toString());

    }
}
