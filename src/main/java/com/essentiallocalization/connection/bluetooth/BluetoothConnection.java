package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Looper;

import com.essentiallocalization.connection.PacketConnection;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jake on 9/20/13.
 */
public final class BluetoothConnection extends PacketConnection implements DeviceConnection {
    private static final String TAG = BluetoothConnection.class.getSimpleName();

    // TODO belongs elsewhere
    public static String getState(int state) {
        switch (state) {
            case STATE_NONE:         return "None";
            case STATE_CONNECTING:   return "Connecting";
            case STATE_CONNECTED:    return "Connected";
            case STATE_DISCONNECTED: return "Disconnected";
        }
        return "Unknown";
    }

    public static byte idFromName(String name) {
        return Byte.valueOf(name.substring(name.length() - 1));
    }

    private final UUID mUuid;
    private final boolean mIsServer;
    private final BluetoothDevice mRemoteDevice;

    BluetoothConnection(BluetoothSocket socket, UUID uuid, boolean selfIsServer, Looper sendAndEventLooper) throws IOException {
        super(idFromName(BluetoothAdapter.getDefaultAdapter().getName()), idFromName(socket.getRemoteDevice().getName()),
                socket.getInputStream(), socket.getOutputStream(), sendAndEventLooper);
        mUuid = uuid;
        mIsServer = selfIsServer;
        mRemoteDevice = socket.getRemoteDevice();
    }

    public boolean isServer() {
        return mIsServer;
    }

    public UUID getUuid() {
        return mUuid;
    }

    @Override
    public BluetoothDevice getDevice() {
        return mRemoteDevice;
    }
}
