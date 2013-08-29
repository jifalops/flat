package com.jphilli85.wifirecorder.service;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jake on 8/29/13.
 */
public final class BluetoothService extends PersistentIntentService {
    private static final String LOG_TAG = BluetoothService.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVERABLE = 2;

    private BluetoothAdapter mAdapter;
    private List<BluetoothDevice> mDiscoveredDevices;

    @Override
    public void onCreate() {
        super.onCreate();

        mDiscoveredDevices = new ArrayList<BluetoothDevice>();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Log.e(LOG_TAG, "Device does not support Bluetooth");
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
        }

//        mFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
//        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        mFilter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
//        mFilter.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        mFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);


        mFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);

            String s;
            switch (state) {
                case BluetoothAdapter.STATE_CONNECTED:      s = "connected";        break;
                case BluetoothAdapter.STATE_CONNECTING:     s = "connecting";       break;
                case BluetoothAdapter.STATE_DISCONNECTED:   s = "disconnected";     break;
                case BluetoothAdapter.STATE_DISCONNECTING:  s = "disconnecting";    break;
                case BluetoothAdapter.STATE_OFF:            s = "off";              break;
                case BluetoothAdapter.STATE_ON:             s = "on";               break;
                case BluetoothAdapter.STATE_TURNING_OFF:    s = "turning off";      break;
                case BluetoothAdapter.STATE_TURNING_ON:     s = "turning on";       break;
                default:                                    s = "unknown";          break;
            }
            Log.d(LOG_TAG, "BT state: " + s);
            Toast.makeText(this, "BT state: " + s, Toast.LENGTH_SHORT).show();

        } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothClass btClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
            mDiscoveredDevices.add(device);

        } else if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

            int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
            int prevMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);
            String s;
            switch (mode) {
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE:                s = "connectable";              break;
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:   s = "connectable discoverable"; break;
                case BluetoothAdapter.SCAN_MODE_NONE:                       s = "none";                     break;
                default:                                                    s = "unknown";                  break;
            }
            Log.d(LOG_TAG, "BT scan: " + s);
            Toast.makeText(this, "BT scan: " + s, Toast.LENGTH_SHORT).show();
        }
    }

    public void enableBluetooth(Activity activity) {
        if (!mAdapter.isEnabled()) {
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }
    }

    /** Will enable Bluetooth if it is not already enabled. */
    public void enableDiscoverability(Activity activity) {
        if (!mAdapter.isDiscovering()) {
            mDiscoveredDevices.clear(); // TODO this should be called in onActivityResult()
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), REQUEST_DISCOVERABLE);
        }
    }

    public boolean startDiscovery() {
        return mAdapter.startDiscovery();
    }
}
