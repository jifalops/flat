package com.essentiallocalization.localization.signal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jacob Phillips (09/2014)
 */
public class BluetoothBeacon extends AbstractSignal {
    public static final int EVENT_DEVICE_DISCOVERED = 1;

    private Timer timer;
    private Map<BluetoothDevice, Short> scanResults = new HashMap<BluetoothDevice, Short>();
    public Map<BluetoothDevice, Short> getScanResults() {
        return scanResults;
    }

    /*
     * Singleton
     */
    private BluetoothBeacon() {}
    private static BluetoothBeacon instance;
    public static BluetoothBeacon getInstance() {
        if(instance == null) {
            instance = new BluetoothBeacon();
        }
        return instance;
    }

    @Override
    public int getSignalType() {
        return Signal.TYPE_ELECTROMAGNETIC;
    }

    /**
     * @param args args[0] is a Context used to get the BluetoothManager.
     *             args[1] is an interval in seconds at which scans will repeat, defaulting to 30 (it takes a while).
     *                      If a zero is passed, only a single update will be requested.
     */
    @Override
    public void enable(Object... args) {
        Context ctx = (Context) args[0];
        BluetoothManager manager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter me = manager.getAdapter();

        ctx.registerReceiver(scanReceiver, scanFilter);

        int scanInterval = 30;
        if (args.length == 2) scanInterval = (Integer) args[1];
        scanInterval *= 1000;

        cancelTimer();
        if (scanInterval == 0) {
            me.startDiscovery();
        } else {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    me.startDiscovery();
                }
            }, 0, scanInterval);
        }
        enabled = true;
    }

    @Override
    public void disable(Object... args) {
        Context ctx = (Context) args[0];
        BluetoothManager manager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter me = manager.getAdapter();
        me.cancelDiscovery();
        ctx.unregisterReceiver(scanReceiver);
        enabled = false;
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
            scanResults.put(device, rssi);
            notifyListeners(EVENT_DEVICE_DISCOVERED);
        }
    };
    private final IntentFilter scanFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
}
