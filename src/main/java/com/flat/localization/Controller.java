package com.flat.localization;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.Sensor;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;

import com.flat.localization.ranging.FreeSpacePathLoss;
import com.flat.localization.ranging.LinearAcceleration;
import com.flat.localization.ranging.RotationVector;
import com.flat.localization.ranging.SignalProcessor;
import com.flat.localization.scheme.LocationAlgorithm;
import com.flat.localization.scheme.MinMax;
import com.flat.localization.scheme.Trilateration;
import com.flat.localization.signal.AndroidSensor;
import com.flat.localization.signal.BluetoothBeacon;
import com.flat.localization.signal.Signal;
import com.flat.localization.signal.WifiBeacon;
import com.flat.localization.util.Calc;
import com.flat.localization.util.Const;
import com.flat.localization.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO bad embedded references
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
        initialize();
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
        return r.signal + r.algorithm;
    }
    private String getKey(Signal sig, Node.State st) {
        return sig.getName() + st.algorithm;
    }


    private void initialize() {
        model.registerListener(this);

        /*
         * ===========================
         * Signal Processing (ranging)
         * ===========================
         */

        List<SignalProcessor> signalProcessors;


        /*
         * Linear acceleration (internal sensor)
         */
        final AndroidSensor accelSignal = new AndroidSensor("LinAccel", Sensor.TYPE_LINEAR_ACCELERATION);

        // boilerplate
        final LinearAcceleration la = new LinearAcceleration();
        signalProcessors = new ArrayList<SignalProcessor>(1);
        signalProcessors.add(la);
        model.put(accelSignal, signalProcessors);

        // signal change listener
        accelSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.algorithm = la.getName();
                state.time = accelSignal.getTimestamp();
                String key = getKey(accelSignal, state);
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:
                        long last = extras.getLong(key);
                        extras.putLong(key, state.time);
                        state.pos = la.integrate(accelSignal.getValues(), state.time - last);
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
        signalProcessors = new ArrayList<SignalProcessor>(1);
        signalProcessors.add(rv);
        model.put(rotSignal, signalProcessors);

        // signal change listener
        rotSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.algorithm = rv.getName();
                state.time = rotSignal.getTimestamp();
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:
                        float[] rot = null;
                        if (me.getStateHistorySize() > 0) {
                            // null angle is okay
                            rot = me.getState().angle;
                        }

                        // new angle incorporates previous angle
                        rot = rv.rotateBy(rot, rotSignal.getValues());

                        state.angle = new float[]{
                                rot[0] * Const.RAD_TO_DEG,
                                rot[1] * Const.RAD_TO_DEG,
                                rot[2] * Const.RAD_TO_DEG
                        };

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
        signalProcessors = new ArrayList<SignalProcessor>(1);
        signalProcessors.add(fspl);
        model.put(btSignal, signalProcessors);

        // signal change listener
        btSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.Range range = new Node.Range();
                range.signal = btSignal.getName();
                range.algorithm = fspl.getName();
                range.time = System.nanoTime();
                switch (eventType) {
                    case BluetoothBeacon.EVENT_DEVICE_DISCOVERED:
                        BluetoothDevice btdevice = btSignal.getMostRecentDevice();
                        short rssi = btSignal.getScanResults().get(btdevice);
                        // TODO access true frequency
                        range.dist = fspl.fromDbMhz((double) rssi, 2400.0);

                        // TODO using BT mac instead of wifi
                        String mac = btdevice.getAddress();
                        if (model.get(mac) == null) {
                            model.add(new Node(mac));
                        }
                        model.get(mac).addPending(range);
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
        signalProcessors = new ArrayList<SignalProcessor>(1);
        signalProcessors.add(fspl2);
        model.put(wifiSignal, signalProcessors);

        // signal change listener
        wifiSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                switch (eventType) {
                    case WifiBeacon.EVENT_SCAN_RESULTS:
                        for (ScanResult sr : wifiSignal.getScanResults()) {
                            Node.Range range = new Node.Range();
                            range.signal = wifiSignal.getName();
                            range.algorithm = fspl2.getName();
                            range.time = sr.timestamp;
                            range.dist = fspl2.fromDbMhz(sr.level, sr.frequency);
                            if (model.get(sr.BSSID) == null) {
                                model.add(new Node(sr.BSSID));
                            }
                            model.get(sr.BSSID).addPending(range);
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

        Model.AlgorithmMatchCriteria criteria;
        Model.NodeMatchCriteria nmc;


        /*
         * MinMax
         */
        final MinMax minmax = new MinMax();
        criteria = new Model.AlgorithmMatchCriteria();
        nmc = new Model.NodeMatchCriteria();
        nmc.rangePendingCountMin = 1;
        nmc.rangePendingCountMax = Integer.MAX_VALUE;
        criteria.nodeRequirements.add(nmc);
        model.put(minmax, criteria);



        /*
         * Trilateration
         */
        final Trilateration trilat = new Trilateration();
        criteria = new Model.AlgorithmMatchCriteria();

        // Anchor 1 = (0, 0)
        nmc = new Model.NodeMatchCriteria();
        nmc.posMin = new double[] {0, 0, Double.MIN_VALUE};
        nmc.posMax = new double[] {0, 0, Double.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        // Anchor 2 = (x, 0)
        nmc = new Model.NodeMatchCriteria();
        nmc.posMin = new double[] {Double.MIN_VALUE, 0, Double.MIN_VALUE};
        nmc.posMax = new double[] {Double.MAX_VALUE, 0, Double.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        // Anchor 3 = (x, y)
        nmc = new Model.NodeMatchCriteria();
        nmc.posMin = new double[] {Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE};
        nmc.posMax = new double[] {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        model.put(minmax, criteria);
    }

    // TODO periodic LA updates by checking pending states





    public void runAlgorithms() {


        for (Node n : model.nodes.values()) {

        }
    }

    private void processNode(Node n) {
        for (Node.Range r : n.getPendingRanges()) {

        }

        for (Node.State s : n.getPendingStates()) {

        }
    }


    @Override
    public void onNodeAdded(Node n) {
        n.registerListener(this);
    }

    @Override
    public void onNodeRemoved(Node n) {
        n.unregisterListener(this);
    }

    @Override
    public void onRangePending(Node n) {
        List<Node.State> states = new ArrayList<Node.State>();
        for (Map.Entry<LocationAlgorithm, Model.AlgorithmMatchCriteria> entry : model.algorithms.entrySet()) {
            states.add(entry.getKey().applyTo(me, entry.getValue().filter(new ArrayList<Node>(model.nodes.values()))));
        }
        for (Node.State s : states) {
            Log.e(TAG, s.toString());
        }

    }

    @Override
    public void onStatePending(Node n) {

    }

    @Override
    public void onRangeChanged(Node n) {

    }

    @Override
    public void onStateChanged(Node n) {

    }
}
