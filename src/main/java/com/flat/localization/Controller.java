package com.flat.localization;

import android.content.Context;
import android.hardware.Sensor;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Pair;

import com.flat.internal.Internal;
import com.flat.localization.algorithm.MinMax;
import com.flat.localization.ranging.InternalSensor;
import com.flat.localization.ranging.SignalStrength;
import com.flat.localization.signal.AndroidLocation;
import com.flat.localization.signal.AndroidSensor;
import com.flat.localization.signal.BluetoothBeacon;
import com.flat.localization.signal.Cellular;
import com.flat.localization.signal.Signal;
import com.flat.localization.signal.WifiBeacon;

import java.util.HashMap;
import java.util.Map;

/**
 * A Localization Algorithm (LA) controls the progression of a {@code Node}'s {@code Node.State} by incorporating
 * various {@code Signal}s. A signal undergoes {@code Ranging} to be converted into a linear distance,
 * or in the case of the gyroscope, a new orientation. Then, a {@code Scheme} uses one or more ranges
 * to calculate a new node state. The LA may accept or reject this as the nodes new state (coordinates).
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




    private final Signal.Listener signalListener = new Signal.Listener() {
        @Override
        public void onChange(Signal signal, int eventType) {
            if (signal instanceof AndroidLocation) {
                AndroidLocation loc = (AndroidLocation) signal;
                switch (eventType) {
                    case AndroidLocation.EVENT_LOCATION_CHANGE:
                        //model.signals.get(signal).second.run();
                        break;
                    case AndroidLocation.EVENT_STATUS_CHANGE:

                        break;
                    case AndroidLocation.EVENT_PROVIDER_ENABLED:

                        break;
                    case AndroidLocation.EVENT_PROVIDER_DISABLED:

                        break;
                }
            } else if (signal instanceof AndroidSensor) {
                AndroidSensor sensor = (AndroidSensor) signal;
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:

                        break;
                    case AndroidSensor.EVENT_ACCURACY_CHANGE:

                        break;
                }
            } else if (signal instanceof BluetoothBeacon) {
                BluetoothBeacon bt = (BluetoothBeacon) signal;
                switch (eventType) {
                    case BluetoothBeacon.EVENT_DEVICE_DISCOVERED:

                        break;
                }
            } else if (signal instanceof Cellular) {
                Cellular cell = (Cellular) signal;
                switch (eventType) {
                    case Cellular.EVENT_CELL_STRENGTH:

                        break;
                    case Cellular.EVENT_CELL_LOCATION:

                        break;
                    case Cellular.EVENT_CELL_INFOS:

                        break;
                }
            } else if (signal instanceof WifiBeacon) {
                WifiBeacon wifi = (WifiBeacon) signal;
                switch (eventType) {
                    case WifiBeacon.EVENT_SCAN_RESULTS:

                        break;
                }
            }
        }
    };


    void initialize() {



        final AndroidSensor accel = new AndroidSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accel.registerListener(new Signal.Listener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.ranAlg = InternalSensor.LINEAR_ACCELERATION;
                state.timestamp = accel.getTimestamp();
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:
                        long last = extras.getLong(state.ranAlg);
                        extras.putLong(state.ranAlg, state.timestamp);
                        state.pos = InternalSensor.linearAcceleration(accel.getValues(), state.timestamp - last);

                        me.addPending(state);
                        break;
                    case AndroidSensor.EVENT_ACCURACY_CHANGE:

                        break;
                }
            }
        });
        model.signals.put(accel, InternalSensor.LINEAR_ACCELERATION);





        final AndroidSensor rotv = new AndroidSensor(Sensor.TYPE_ROTATION_VECTOR);
        rotv.registerListener(new Signal.Listener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.ranAlg = InternalSensor.ROTATION_VECTOR;
                state.timestamp = rotv.getTimestamp();
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:
                        // Try to get current rotation vector if it exists
                        float[] rot = extras.getFloatArray(state.ranAlg);
                        rot = InternalSensor.rotationVector(rot, rotv.getValues());
                        extras.putFloatArray(state.ranAlg, rot);

                        state.angle = new double[]{
                                rot[0] * InternalSensor.RAD_TO_DEG, rot[1] * InternalSensor.RAD_TO_DEG, rot[2] * InternalSensor.RAD_TO_DEG
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
        bt.registerListener(new Signal.Listener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.ranAlg = SignalStrength.FREE_SPACE_PATH_LOSS;
                state.timestamp = System.nanoTime();
                switch (eventType) {
                    case BluetoothBeacon.EVENT_DEVICE_DISCOVERED:
                        short rssi = bt.getScanResults().get(bt.getMostRecentDevice());
                        state.range = SignalStrength.freeSpacePathLoss((double) rssi, 2400.0);

                        // TODO npe
                        String mac = bt.getMostRecentDevice().getAddress();
                        if (model.nodes.get(mac) == null) {
                            model.nodes.put(mac, new Node(mac));
                        }
                        model.nodes.get(mac).addPending(state);
                        break;
                }
            }
        });
        model.signals.put(bt, SignalStrength.FREE_SPACE_PATH_LOSS);




        final WifiBeacon wifi = WifiBeacon.getInstance();
        wifi.registerListener(new Signal.Listener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Node.State state = new Node.State();
                state.ranAlg = SignalStrength.FREE_SPACE_PATH_LOSS;
                switch (eventType) {
                    case WifiBeacon.EVENT_SCAN_RESULTS:
                        for (ScanResult sr : wifi.getScanResults()) {
                            Node.State s = new Node.State(state);
                            s.timestamp = sr.timestamp;
                            s.range = SignalStrength.freeSpacePathLoss(sr.level, sr.frequency);
                            if (model.nodes.get(sr.BSSID) == null) {
                                model.nodes.put(sr.BSSID, new Node(sr.BSSID));
                            }
                            model.nodes.get(sr.BSSID).addPending(s);
                        }
                        break;
                }
            }
        });
        model.signals.put(wifi, SignalStrength.FREE_SPACE_PATH_LOSS);



    }

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
