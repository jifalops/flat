package com.flat.localization.signal;

import android.bluetooth.BluetoothDevice;
import android.hardware.Sensor;
import android.net.wifi.ScanResult;
import android.os.Bundle;

import com.flat.localization.Node;
import com.flat.localization.NodeManager;
import com.flat.localization.signal.interpreters.FreeSpacePathLoss;
import com.flat.localization.signal.interpreters.LinearAcceleration;
import com.flat.localization.signal.interpreters.RotationVector;
import com.flat.localization.signal.interpreters.SignalInterpreter;
import com.flat.localization.util.Calc;
import com.flat.localization.util.Conv;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class SignalManagerStaticData {
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
                Node.State state = new Node.State();
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
                        if (nodeManager.getNode(mac) == null) {
                            nodeManager.addNode(new Node(mac));
                        }
                        nodeManager.getNode(mac).addPending(range);
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
                            Node.Range range = new Node.Range();
                            range.signal = wifiSignal.getName();
                            range.interpreter = fspl2.getName();
                            range.time = System.currentTimeMillis(); //sr.timestamp;
                            range.range = fspl2.fromDbMhz(sr.level, sr.frequency);
                            if (nodeManager.getNode(sr.BSSID) == null) {
                                nodeManager.addNode(new Node(sr.BSSID));
                            }
                            nodeManager.getNode(sr.BSSID).addPending(range);
                        }
                        break;
                }
            }
        });
    }

    private static String getKey(Node.Range r) {
        return r.signal + r.interpreter;
    }
    private static String getKey(Signal sig, Node.State st) {
        return sig.getName() + st.algorithm;
    }
}
