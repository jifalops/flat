package com.flat.localization;

import android.content.Context;
import android.hardware.Sensor;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.flat.localization.scheme.MinMax;
import com.flat.localization.scheme.Trilateration;
import com.flat.localization.scheme.InternalSensor;
import com.flat.localization.ranging.SignalStrength;
import com.flat.localization.signal.AndroidSensor;
import com.flat.localization.signal.BluetoothBeacon;
import com.flat.localization.signal.Signal;
import com.flat.localization.signal.WifiBeacon;

/**
 * The Controller controls the progression of a {@code Node}'s {@code Node.State} by incorporating
 * various {@code Signal}s. A signal undergoes {@code Ranging} to be converted into a linear distance,
 * or in the case of the gyroscope, a new orientation. Then, a {@code LocationAlgorithm} uses one or more ranges
 * to calculate a new node state. The Controller may accept or reject this as the nodes new state.
 */
public final class Controller {
    final Node me;
    final Model model = Model.getInstance();
    final Bundle extras = new Bundle();

    /*
     * Singleton
     */
    private Controller(Context ctx) {
        me = new Node(Controller.getWifiMac(ctx));
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

    private String getKey(Signal sig, Node.State s) {
        return sig.getName() + s.algorithm;
    }

    private void initialize() {
        final AndroidSensor accel = new AndroidSensor("LinAccel", Sensor.TYPE_LINEAR_ACCELERATION);
        accel.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.algorithm = InternalSensor.LINEAR_ACCELERATION;
                state.time = accel.getTimestamp();
                String key = getKey(accel, state);
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:
                        long last = extras.getLong(key);
                        extras.putLong(key, state.time);
                        state.pos = InternalSensor.linearAcceleration(accel.getValues(), state.time - last);

                        me.addPending(state);
                        break;
                    case AndroidSensor.EVENT_ACCURACY_CHANGE:

                        break;
                }
            }
        });
        model.signals.put(accel, InternalSensor.LINEAR_ACCELERATION);



        final AndroidSensor rotv = new AndroidSensor("RotVect", Sensor.TYPE_ROTATION_VECTOR);
        rotv.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.algorithm = InternalSensor.ROTATION_VECTOR;
                state.time = rotv.getTimestamp();
                String key = getKey(rotv, state);
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:
                        // Try to get current rotation vector if it exists
                        float[] rot = extras.getFloatArray(key);
                        rot = InternalSensor.rotationVector(rot, rotv.getValues());
                        extras.putFloatArray(key, rot);

                        state.angle = new double[]{
                                rot[0] * InternalSensor.RAD_TO_DEG,
                                rot[1] * InternalSensor.RAD_TO_DEG,
                                rot[2] * InternalSensor.RAD_TO_DEG
                        };

                        me.addPending(state);
                        break;
                    case AndroidSensor.EVENT_ACCURACY_CHANGE:

                        break;
                }
            }
        });
        model.signals.put(rotv, InternalSensor.ROTATION_VECTOR);



        final BluetoothBeacon bt = BluetoothBeacon.getInstance();
        bt.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.Range range = new Node.Range();
                range.signal = bt.getName();
                range.algorithm = SignalStrength.FREE_SPACE_PATH_LOSS;
                range.time = System.nanoTime();
                switch (eventType) {
                    case BluetoothBeacon.EVENT_DEVICE_DISCOVERED:
                        short rssi = bt.getScanResults().get(bt.getMostRecentDevice());
                        range.dist = SignalStrength.freeSpacePathLoss((double) rssi, 2400.0);

                        // TODO npe, using BT mac
                        String mac = bt.getMostRecentDevice().getAddress();
                        if (model.nodes.get(mac) == null) {
                            model.nodes.put(mac, new Node(mac));
                        }
                        model.nodes.get(mac).addPending(range);
                        break;
                }
            }
        });
        model.signals.put(bt, SignalStrength.FREE_SPACE_PATH_LOSS);


        final WifiBeacon wifi = WifiBeacon.getInstance();
        wifi.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                switch (eventType) {
                    case WifiBeacon.EVENT_SCAN_RESULTS:
                        for (ScanResult sr : wifi.getScanResults()) {
                            Node.Range range = new Node.Range();
                            range.signal = wifi.getName();
                            range.algorithm = SignalStrength.FREE_SPACE_PATH_LOSS;
                            range.time = sr.timestamp;
                            range.dist = SignalStrength.freeSpacePathLoss(sr.level, sr.frequency);
                            if (model.nodes.get(sr.BSSID) == null) {
                                model.nodes.put(sr.BSSID, new Node(sr.BSSID));
                            }
                            model.nodes.get(sr.BSSID).addPending(range);
                        }
                        break;
                }
            }
        });
        // TODO allow multiple path loss algorithms simultaneously.
        model.signals.put(wifi, SignalStrength.FREE_SPACE_PATH_LOSS);




        /*
         * Location Algorithms
         */
        model.algorithms.put(new MinMax(), true);
        model.algorithms.put(new Trilateration(), true);



    }

    // TODO periodic LA updates by checking pending states














    static String getWifiMac(Context ctx) {
        WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        return info.getMacAddress();
    }

    static double[] add (double[] a, double[] b) {
        double[] sum = new double[a.length];
        for (int i=0; i<a.length; ++i) {
            sum[i] = a[i] + b[i];
        }
        return sum;
    }
}
