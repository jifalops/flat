package com.flat.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Looper;

import com.flat.bluetooth.connection.PacketConnection;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jake on 9/20/13.
 */
public final class BluetoothConnection extends PacketConnection implements DeviceConnection {
    private static final String TAG = BluetoothConnection.class.getSimpleName();

    public static byte idFromName(String name) {
        if (name == null) return -1;
        return Byte.valueOf(name.substring(name.length() - 1));
    }

    public static final BluetoothAdapter SELF = BluetoothAdapter.getDefaultAdapter();
    public static final String SELF_NAME;
    public static final byte SELF_ID;

    static {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            SELF_NAME = adapter.getName();
        } else {
            SELF_NAME = null;
        }
        SELF_ID = idFromName(SELF_NAME);
    }


    private final UUID mUuid;
    private final boolean mIsServer;
    private final BluetoothDevice mRemoteDevice;

    BluetoothConnection(BluetoothSocket socket, UUID uuid, boolean selfIsServer, Looper sendAndEventLooper) throws IOException {
        super(SELF_ID, idFromName(socket.getRemoteDevice().getName()),
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
