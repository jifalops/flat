package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import com.essentiallocalization.connection.AckPacket;
import com.essentiallocalization.connection.AckTimePacket;
import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.Message;
import com.essentiallocalization.connection.PacketConnection;
import com.essentiallocalization.connection.SnoopPacketReader;
import com.essentiallocalization.util.io.Connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Jake on 9/15/13.
 */
public final class BluetoothConnectionManager implements PendingConnection.Listener,
            PacketConnection.Listener, SnoopPacketReader.Listener {
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

    public static interface Listener {
        void onPacketCompleted(DataPacket dp);
        void onStateChanged(BluetoothDevice device, int oldState, int newState);
    }

    private final Map<BluetoothDevice, Pair<Connection, Connection.StateChangeListener>> mConnections;
    private final Looper mLooper;
    private final Listener mListener;

    private int mConnLimit;

    public BluetoothConnectionManager(Looper sendAndEventLooper, Listener listener) {
        mConnections = new HashMap<BluetoothDevice, Pair<Connection, Connection.StateChangeListener>>(MAX_CONNECTIONS);

        mLooper = sendAndEventLooper;
        mListener = listener;
        mConnLimit = MAX_CONNECTIONS;
    }

    //
    // Shortcut methods
    //

    public synchronized Connection getConnection(BluetoothDevice device) {
        return mConnections.get(device).first;
    }

    public synchronized Connection.StateChangeListener getListener(BluetoothDevice device) {
        return mConnections.get(device).second;
    }

    public synchronized BluetoothDevice getDevice(Connection conn) {
        for (Map.Entry<BluetoothDevice, Pair<Connection, Connection.StateChangeListener>> e : mConnections.entrySet()) {
            if (e.getValue().first == conn) return e.getKey();
        }
        return null;
    }

    public synchronized BluetoothDevice getDevice(Connection.StateChangeListener listener) {
        for (Map.Entry<BluetoothDevice, Pair<Connection, Connection.StateChangeListener>> e : mConnections.entrySet()) {
            if (e.getValue().second == listener) return e.getKey();
        }
        return null;
    }


    //
    // start and stop (and helper class)
    //

    public synchronized void stop(BluetoothDevice device) {
        Pair<Connection, Connection.StateChangeListener> pair = mConnections.get(device);
        if (pair != null) {
            pair.first.cancel();
            mConnections.remove(device);
        }
    }

    public synchronized void stopAll() {
        for (Map.Entry<BluetoothDevice, Pair<Connection, Connection.StateChangeListener>> e : mConnections.entrySet()) {
            stop(e.getKey());
        }
    }

    public synchronized int send(BluetoothDevice device, String msg) throws IOException, Message.MessageTooLongException {
        Connection conn = getConnection(device);
        if (conn.isConnected()) {
            return ((BluetoothConnection) conn).send(msg);
        } else {
            Log.e(TAG, "Cannot send: connection not connected.");
        }
        return 0;
    }

    public synchronized void start(BluetoothDevice device) {
        stop(device);

        PendingConnection pc = new PendingConnection(device, this, mLooper);
        StateListener listener = new StateListener();
        pc.setStateChangeListener(listener);
        mConnections.put(device, new Pair<Connection, Connection.StateChangeListener>(pc, listener));
        pc.start();
    }

    private class StateListener implements Connection.StateChangeListener {
        @Override
        public void onStateChange(int oldState, int newState) {
            mListener.onStateChanged(getDevice(this), oldState, newState);
        }
    }

    //
    // connection limits
    //
    public synchronized void setMaxConnections(int limit) {
        stopAll();
        if (limit > MAX_CONNECTIONS) limit = MAX_CONNECTIONS;
        mConnLimit = limit;
    }

    public synchronized int getMaxConnections() {
        return mConnLimit;
    }


    //
    // PendingConnection.Listener
    //

    @Override
    public synchronized boolean onConnected(BluetoothConnection btconn) {
        BluetoothDevice device = btconn.getRemoteDevice();
        Connection conn = getConnection(device);
        if (conn != null) {
            if (conn.isConnected()) {
                Log.e(TAG, "Connection already established: " + btconn.getName());
                return false;
            } else {
                mConnections.remove(device);
                StateListener listener = new StateListener();
                btconn.setStateChangeListener(listener);
                mConnections.put(device, new Pair<Connection, Connection.StateChangeListener>(btconn, listener));
                btconn.setState(Connection.STATE_CONNECTED);
                btconn.start();
                return true;
            }
        } else {
            Log.e(TAG, "Received unexpected connection: " + btconn.getName());
            return false;
        }
    }

    @Override
    public void onFinished(PendingConnection conn) {
        conn.setState(Connection.STATE_NONE);
    }


    //
    // PacketConnection.Listener
    //

    @Override
    public void onSendAck(PacketConnection pc, AckPacket ap) {

    }

    @Override
    public void onPacketCompleted(PacketConnection pc, DataPacket dp) {

    }


    //
    // SnoopPacketReader.Listener
    //

    @Override
    public void onSendAck(AckPacket ap) {

    }

    @Override
    public void onSendAckTime(AckTimePacket atp) {

    }

    @Override
    public void onPacketCompleted(DataPacket dp) {

    }
}
