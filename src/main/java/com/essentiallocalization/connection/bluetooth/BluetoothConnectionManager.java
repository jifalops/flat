package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import android.util.Log;

import com.essentiallocalization.util.Filter;
import com.essentiallocalization.connection.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Jake on 9/15/13.
 */
public final class BluetoothConnectionManager implements Filter {
    private static final String TAG = BluetoothConnectionManager.class.getSimpleName();

    static final UUID[] UUIDS = {
            UUID.fromString("0aa67214-5217-4ded-b656-5cccff9a237c"),
            UUID.fromString("709bd128-8f45-45d6-aea7-cb1fb7303ea5"),
            UUID.fromString("4c91a1a9-fa9f-4338-8b5f-1d5f48d6b20e"),
            UUID.fromString("20d47c6a-eeda-45a4-98a6-7d4f371f1a34"),
            UUID.fromString("ffdc4e45-d4c7-4789-81c7-b4f6e03ca865"),
            UUID.fromString("1052c5cf-67db-4e5c-80e3-de3a89bbaf96"),
            UUID.fromString("30cb5e95-c22a-41d1-b609-dedf29e866cf")
    };
    public static final int MAX_CONNECTIONS = UUIDS.length;

    private final List<BluetoothConnection> mConnections;
    private final BluetoothConnection.Listener mListener;
    private int mMaxConnections;
    private final Looper mLooper;

    public BluetoothConnectionManager(BluetoothConnection.Listener listener, Looper looper) {
        mLooper = looper;
        mConnections = new ArrayList<BluetoothConnection>(MAX_CONNECTIONS);
        mListener = listener;
        mMaxConnections = MAX_CONNECTIONS;
    }

    public synchronized void setMaxConnections(int connections) {
        stop();
        if (connections > MAX_CONNECTIONS) connections = MAX_CONNECTIONS;
        mMaxConnections = connections;
    }

    public synchronized int getMaxConnections() {
        return mMaxConnections;
    }

    public synchronized List<BluetoothConnection> getConnections() {
        return mConnections;
    }

    public synchronized int addDevice(BluetoothDevice device) {
        mConnections.add(new BluetoothConnection(this, mListener, device, mLooper));
        return mConnections.size() - 1;
    }

//    public synchronized void start(int index) {
//        mConnections.get(index).start();
//    }
//
//    public synchronized void stop(int index) {
//        mConnections.get(index).stop();
////        mConnections.remove(index);
//    }

    public synchronized void start() {
        stop();
        for (BluetoothConnection conn : mConnections) {
            conn.start();
        }
    }

    public synchronized void stop() {
        for (BluetoothConnection conn : mConnections) {
            conn.stop();
        }
//        mConnections.clear();
    }

    public synchronized void sendMessage(int index, Message msg) throws IOException {
        sendMessage(mConnections.get(index), msg);
    }

    public synchronized void sendMessage(Message msg) throws IOException {
        for (BluetoothConnection conn : mConnections) {
            sendMessage(conn, msg);
        }
    }

    private synchronized void sendMessage(BluetoothConnection connection, Message msg) throws IOException {
        if (connection.getState() != BluetoothConnection.STATE_CONNECTED) {
            Log.e(TAG, "Tried to write to closed socket");
        } else if (connection.getConnection() != null) {
            connection.getConnection().sendMessage(msg);
        } else {
            Log.e(TAG, "State is connected but connection is null.");
        }
    }

    @Override
    public synchronized boolean accept(String address) {
        for (BluetoothConnection conn : mConnections) {
            if (conn.getAddress().equals(address)) {
                return false;
            }
        }
        return true;
    }
}
