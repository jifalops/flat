package com.flat.localization.signal;

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
public final class BluetoothBeacon extends AbstractSignal {
    public static final int EVENT_DEVICE_DISCOVERED = 1;

    private Timer timer;
    private Map<BluetoothDevice, Short> scanResults = new HashMap<BluetoothDevice, Short>();
    public Map<BluetoothDevice, Short> getScanResults() {
        return scanResults;
    }
    private BluetoothDevice lastDevice;
    public BluetoothDevice getMostRecentDevice() {
        return lastDevice;
    }
    private boolean enabled;

    /*
     * Simple Singleton
     */
    private BluetoothBeacon() { super("BT-beacon"); }
    private static final BluetoothBeacon instance = new BluetoothBeacon();
    public static BluetoothBeacon getInstance() { return instance; }

    /**
     * @param ctx used to get the BluetoothManager.
     * @param interval an interval in seconds at which scans will repeat, defaulting to 30 (it takes a while, Bluetooth Low Energy is an alternative).
     *                      If a zero is passed, only a single update will be requested.
     */
    public void enable(Context ctx, int interval) {
        BluetoothManager manager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter me = manager.getAdapter();

        ctx.registerReceiver(scanReceiver, scanFilter);

        cancelTimer();
        if (interval == 0) {
            me.startDiscovery();
        } else {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    me.startDiscovery();
                }
            }, 0, interval);
        }
        enabled = true;
    }

    @Override
    public void enable(Context ctx) {
        enable(ctx, 30);
    }

    @Override
    public void disable(Context ctx) {
        if (!isEnabled()) return;
        BluetoothManager manager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter me = manager.getAdapter();
        me.cancelDiscovery();
        ctx.unregisterReceiver(scanReceiver);
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            // Disable if not recurring
            if (timer == null) {
                disable(context);
            }
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
            scanResults.put(device, rssi);
            lastDevice = device;
            notifyListeners(EVENT_DEVICE_DISCOVERED);
        }
    };
    private final IntentFilter scanFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
}
