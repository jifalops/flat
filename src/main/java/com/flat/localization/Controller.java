package com.flat.localization;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.Sensor;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.flat.app.AppController;
import com.flat.localization.coordinatesystem.LocationAlgorithm;
import com.flat.localization.coordinatesystem.MinMax;
import com.flat.localization.coordinatesystem.Trilateration;
import com.flat.localization.node.Node;
import com.flat.localization.signal.AndroidSensor;
import com.flat.localization.signal.BluetoothBeacon;
import com.flat.localization.signal.Signal;
import com.flat.localization.signal.WifiBeacon;
import com.flat.localization.signal.rangingandprocessing.FreeSpacePathLoss;
import com.flat.localization.signal.rangingandprocessing.LinearAcceleration;
import com.flat.localization.signal.rangingandprocessing.RotationVector;
import com.flat.localization.signal.rangingandprocessing.SignalInterpreter;
import com.flat.localization.util.Calc;
import com.flat.localization.util.Conv;
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

        /*
         * ===========================
         * Signal Processing (ranging)
         * ===========================
         */

        List<SignalInterpreter> signalProcessors;


        /*
         * Linear acceleration (internal sensor)
         */
        final AndroidSensor accelSignal = new AndroidSensor("LinAccel", Sensor.TYPE_LINEAR_ACCELERATION);

        // boilerplate
        final LinearAcceleration la = new LinearAcceleration();
        signalProcessors = new ArrayList<SignalInterpreter>(1);
        signalProcessors.add(la);
        model.addSignal(accelSignal, signalProcessors);

        // signal change listener
        accelSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.algorithm = la.getName();
                state.time = System.currentTimeMillis(); //accelSignal.getTimestamp();
                String key = getKey(accelSignal, state);
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:
                        long last = extras.getLong(key);
                        double diff = state.time - last;
                        if (last == 0 || diff > 1E9) diff = 0;
                        diff = diff / 1E9;

                        extras.putLong(key, state.time);
                        state.pos = la.integrate(accelSignal.getValues(), diff);
                        // incorporate current position into new state
                        state.pos = Calc.vectorSum(me.getState().pos, state.pos);
                        me.addPending(state);
                        break;
                    case AndroidSensor.EVENT_ACCURACY_CHANGE:

                        break;
                }
            }
        });


        /*
         * Rotation vector (internal sensor)
         */
        final AndroidSensor rotSignal = new AndroidSensor("RotVect", Sensor.TYPE_ROTATION_VECTOR);

        // boilerplate
        final RotationVector rv = new RotationVector();
        signalProcessors = new ArrayList<SignalInterpreter>(1);
        signalProcessors.add(rv);
        model.addSignal(rotSignal, signalProcessors);

        // signal change listener
        rotSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.algorithm = rv.getName();
                state.time = System.currentTimeMillis(); //rotSignal.getTimestamp();
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:
                        float[] angle = rotSignal.getValues();
                        rv.toWorldOrientation(angle);
                        Conv.rad2deg(angle);
                        state.angle = angle;

                        me.addPending(state);
                        break;
                    case AndroidSensor.EVENT_ACCURACY_CHANGE:

                        break;
                }
            }
        });




        /*
         * Bluetooth beacon
         */
        final BluetoothBeacon btSignal = BluetoothBeacon.getInstance();

        // boilerplate
        final FreeSpacePathLoss fspl = new FreeSpacePathLoss();
        signalProcessors = new ArrayList<SignalInterpreter>(1);
        signalProcessors.add(fspl);
        model.addSignal(btSignal, signalProcessors);

        // signal change listener
        btSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.Range range = new Node.Range();
                range.signal = btSignal.getName();
                range.interpreter = fspl.getName();
                range.time = System.currentTimeMillis();
                switch (eventType) {
                    case BluetoothBeacon.EVENT_DEVICE_DISCOVERED:
                        BluetoothDevice btdevice = btSignal.getMostRecentDevice();
                        short rssi = btSignal.getScanResults().get(btdevice);
                        // TODO access true frequency
                        range.range = fspl.fromDbMhz(rssi, 2400.0f);

                        // TODO using BT mac instead of wifi
                        String mac = btdevice.getAddress();
                        if (model.getNode(mac) == null) {
                            model.addNode(new Node(mac));
                        }
                        model.getNode(mac).addPending(range);
                        break;
                }
            }
        });



        /*
         * Wifi beacon
         */
        final WifiBeacon wifiSignal = WifiBeacon.getInstance();

        // boilerplate
        // TODO no need for multiple fspl instances
        final FreeSpacePathLoss fspl2 = new FreeSpacePathLoss();
        signalProcessors = new ArrayList<SignalInterpreter>(1);
        signalProcessors.add(fspl2);
        model.addSignal(wifiSignal, signalProcessors);

        // signal change listener
        wifiSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                switch (eventType) {
                    case WifiBeacon.EVENT_SCAN_RESULTS:
                        for (ScanResult sr : wifiSignal.getScanResults()) {
                            Node.Range range = new Node.Range();
                            range.signal = wifiSignal.getName();
                            range.interpreter = fspl2.getName();
                            range.time = System.currentTimeMillis(); //sr.timestamp;
                            range.range = fspl2.fromDbMhz(sr.level, sr.frequency);
                            if (model.getNode(sr.BSSID) == null) {
                                model.addNode(new Node(sr.BSSID));
                            }
                            model.getNode(sr.BSSID).addPending(range);
                        }
                        break;
                }
            }
        });





        /*
         * ===================
         * Location Algorithms
         * ===================
         */

        Criteria.AlgorithmMatchCriteria criteria;
        Criteria.NodeMatchCriteria nmc;


        /*
         * MinMax
         */
        final MinMax minmax = new MinMax();
        criteria = new Criteria.AlgorithmMatchCriteria();
        nmc = new Criteria.NodeMatchCriteria();
        nmc.rangePendingCountMin = 1;
        nmc.rangePendingCountMax = Integer.MAX_VALUE;
        criteria.nodeRequirements.add(nmc);
        model.addAlgorithm(minmax, criteria);



        /*
         * Trilateration
         */
        final Trilateration trilat = new Trilateration();
        criteria = new Criteria.AlgorithmMatchCriteria();

        // Anchor 1 = (0, 0)
        nmc = new Criteria.NodeMatchCriteria();
        nmc.posMin = new float[] {0, 0, Float.MIN_VALUE};
        nmc.posMax = new float[] {0, 0, Float.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        // Anchor 2 = (x, 0)
        nmc = new Criteria.NodeMatchCriteria();
        nmc.posMin = new float[] {Float.MIN_VALUE, 0, Float.MIN_VALUE};
        nmc.posMax = new float[] {Float.MAX_VALUE, 0, Float.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        // Anchor 3 = (x, y)
        nmc = new Criteria.NodeMatchCriteria();
        nmc.posMin = new float[] {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
        nmc.posMax = new float[] {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        model.addAlgorithm(trilat, criteria);
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
