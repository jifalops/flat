package com.flat.localization;

import android.content.Context;
import android.hardware.Sensor;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Pair;

import com.flat.localization.ranging.LinearAcceleration;
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
                switch (eventType) {
                    case AndroidSensor.EVENT_SENSOR_CHANGE:

                        break;
                    case AndroidSensor.EVENT_ACCURACY_CHANGE:

                        break;
                }
            }
        });
        model.signals.put(new AndroidSensor(Sensor.TYPE_LINEAR_ACCELERATION), new Pair<String, Runnable>())
    }

    static String getWifiMac(Context ctx) {
        WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        return info.getMacAddress();
    }
}
