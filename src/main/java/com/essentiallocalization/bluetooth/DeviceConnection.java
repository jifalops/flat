package com.essentiallocalization.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.essentiallocalization.util.io.Connection;

/**
 * Created by Jake on 2/3/14.
 */
public interface DeviceConnection extends Connection {
    BluetoothDevice getDevice();
}
