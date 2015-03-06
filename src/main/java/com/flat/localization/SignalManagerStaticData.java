package com.flat.localization;

import android.bluetooth.BluetoothDevice;
import android.hardware.Sensor;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;

import com.flat.localization.node.NodeRange;
import com.flat.localization.node.NodeState;
import com.flat.localization.node.RemoteNode;
import com.flat.localization.signals.AndroidSensor;
import com.flat.localization.signals.BluetoothBeacon;
import com.flat.localization.signals.BluetoothLeBeacon;
import com.flat.localization.signals.Signal;
import com.flat.localization.signals.WifiBeacon;
import com.flat.localization.signals.interpreters.FreeSpacePathLoss;
import com.flat.localization.signals.interpreters.LinearAcceleration;
import com.flat.localization.signals.interpreters.RotationVector;
import com.flat.localization.signals.interpreters.SignalInterpreter;
import com.flat.localization.util.Calc;
import com.flat.localization.util.Conv;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class SignalManagerStaticData {
    private static final String TAG = SignalManagerStaticData.class.getSimpleName();

    private static final Bundle extras = new Bundle();
    public static void initialize(SignalManager signalManager, final NodeManager nodeManager) {
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
        signalManager.addSignal(accelSignal, signalProcessors);

        // signal change listener
        accelSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                NodeState state = new NodeState();
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
                        state.pos = Calc.vectorSum(nodeManager.getLocalNode().getState().pos, state.pos);
                        nodeManager.getLocalNode().addPending(state);
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
        signalManager.addSignal(rotSignal, signalProcessors);

        // signal change listener
        rotSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                NodeState state = new NodeState();
                state.algorithm = rv.getName();
                state.time = System.currentTimeMillis(); //rotSignal.getTimestamp();
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:
                        float[] angle = rotSignal.getValues();
                        rv.toWorldOrientation(angle);
                        Conv.rad2deg(angle);
                        state.angle = angle;

                        nodeManager.getLocalNode().addPending(state);
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
        signalManager.addSignal(btSignal, signalProcessors);

        // signal change listener
        btSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                NodeRange range = new NodeRange();
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
                        if (nodeManager.getNode(mac) == null) {
                            nodeManager.addNode(new RemoteNode(mac));
                        }
                        nodeManager.getNode(mac).addPending(range);
                        break;
                }
            }
        });


        /*
         * Bluetooth LE beacon
         */
        final BluetoothLeBeacon leBeacon = BluetoothLeBeacon.getInstance();

        // boilerplate
        signalProcessors = new ArrayList<SignalInterpreter>(1);
        signalProcessors.add(fspl);
        signalManager.addSignal(leBeacon, signalProcessors);

        // signal change listener
        leBeacon.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                NodeRange range = new NodeRange();
                range.signal = leBeacon.getName();
                range.interpreter = fspl.getName();
                range.time = System.currentTimeMillis();
                switch (eventType) {
                    case BluetoothLeBeacon.EVENT_SCAN_RESULT:


                        android.bluetooth.le.ScanResult result = leBeacon.getScanResult();

                        // TODO use formula that includes tx power.
//                        ScanRecord record = result.getScanRecord();
//                        if (record != null) {
//                            int txPower = record.getTxPowerLevel();
//                            range.range = fspl.fromPathLoss(txPower - result.getRssi());
//                        } else {
                            range.range = fspl.fromDbMhz(result.getRssi(), 2400.0f);
//                        }


                        // TODO using BT mac instead of wifi
                        String mac = result.getDevice().getAddress();
                        Log.v(TAG, "got scan result for " + mac);
                        if (nodeManager.getNode(mac) == null) {
                            nodeManager.addNode(new RemoteNode(mac));
                        }
                        nodeManager.getNode(mac).addPending(range);
                        break;
                    case BluetoothLeBeacon.EVENT_BATCH_SCAN_RESULTS:
                        Log.v(TAG, "got batch scan results");
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
        signalManager.addSignal(wifiSignal, signalProcessors);

        // signal change listener
        wifiSignal.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                switch (eventType) {
                    case WifiBeacon.EVENT_SCAN_RESULTS:
                        for (ScanResult sr : wifiSignal.getScanResults()) {
                            NodeRange range = new NodeRange();
                            range.signal = wifiSignal.getName();
                            range.interpreter = fspl2.getName();
                            range.time = System.currentTimeMillis(); //sr.timestamp;
                            range.range = fspl2.fromDbMhz(sr.level, sr.frequency);
                            if (nodeManager.getNode(sr.BSSID) == null) {
                                nodeManager.addNode(new RemoteNode(sr.BSSID));
                            }
                            nodeManager.getNode(sr.BSSID).addPending(range);
                        }
                        break;
                }
            }
        });
    }

    private static String getKey(NodeRange r) {
        return r.signal + r.interpreter;
    }
    private static String getKey(Signal sig, NodeState st) {
        return sig.getName() + st.algorithm;
    }
}
