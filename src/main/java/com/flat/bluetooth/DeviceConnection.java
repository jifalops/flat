package com.flat.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.flat.util.io.Connection;

/**
 * Created by Jake on 2/3/14.
 */
public interface DeviceConnection extends Connection {
    BluetoothDevice getDevice();
}
