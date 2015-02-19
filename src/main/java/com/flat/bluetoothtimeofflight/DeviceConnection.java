package com.flat.bluetoothtimeofflight;

import android.bluetooth.BluetoothDevice;

import com.flat.bluetoothtimeofflight.io.Connection;

/**
 * Created by Jake on 2/3/14.
 */
public interface DeviceConnection extends Connection {
    BluetoothDevice getDevice();
}
