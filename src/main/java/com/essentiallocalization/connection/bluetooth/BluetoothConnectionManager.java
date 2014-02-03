package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import android.util.Log;

import com.essentiallocalization.connection.AckPacket;
import com.essentiallocalization.connection.AckTimePacket;
import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.Message;
import com.essentiallocalization.connection.PacketConnection;
import com.essentiallocalization.connection.SnoopPacketReader;
import com.essentiallocalization.util.io.Connection;
import com.essentiallocalization.util.lifecycle.Restartable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Jake on 9/15/13.
 */
public final class BluetoothConnectionManager implements Restartable, PendingConnection.Listener,
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
        void onStateChanged(Connection conn, int oldState, int newState);
    }

    private final Map<BluetoothDevice, Connection> mConnections;
    private final Map<BluetoothDevice, Connection.Listener> mStates;
//    private final List<PendingConnection> mPending;
//    private final List<BluetoothConnection> mConnections;
//    private final Map<Connection.Listener, Connection> mStates;
    private final Looper mLooper;
    private final Listener mListener;

    private int mConnLimit;

    public BluetoothConnectionManager(Looper sendAndEventLooper, Listener listener) {
//        mPending = new ArrayList<PendingConnection>(MAX_CONNECTIONS);
//        mConnections = new ArrayList<BluetoothConnection>(MAX_CONNECTIONS);
//        mStates = new HashMap<Connection.Listener, Connection>(MAX_CONNECTIONS);
        mConnections = new HashMap<BluetoothDevice, Connection>(MAX_CONNECTIONS);
        mStates = new HashMap<BluetoothDevice, Connection.Listener>(MAX_CONNECTIONS);

        mLooper = sendAndEventLooper;
        mListener = listener;
        mConnLimit = MAX_CONNECTIONS;
    }


    public synchronized void setMaxConnections(int limit) {
        stop();
        if (limit > MAX_CONNECTIONS) limit = MAX_CONNECTIONS;
        mConnLimit = limit;
    }

    public synchronized int getMaxConnections() {
        return mConnLimit;
    }


//    public synchronized int addDevice(BluetoothDevice device) {
//        mConnections.add(new BluetoothConnection(this, mListener, device, mLooper));
//        return mConnections.size() - 1;
//    }


    public synchronized Connection getConnection(BluetoothDevice device) {
        return mConnections.get(device);
    }

    public synchronized Connection.Listener getListener(BluetoothDevice device) {
        return mStates.get(device);
    }

    public synchronized BluetoothConnection findConnectionByAddress(String address) {
        for (Connection conn : mConnections) {
            if (conn.getRemoteDevice().getAddress().equals(address)) {
                return conn;
            }
        }
        return null;
    }

    public synchronized BluetoothConnection getConnectionByName(String name) {
        for (BluetoothConnection conn : mConnections) {
            if (conn.getRemoteDevice().getName().equals(name)) {
                return conn;
            }
        }
        return null;
    }

    public synchronized PendingConnection getPending(BluetoothDevice device) {
        return getPendingByAddress(device.getAddress());
    }

    public synchronized PendingConnection getPending(byte dest) {
        for (PendingConnection conn : mPending) {
            if (BluetoothConnection.idFromName(conn.getTarget().getName()) == dest) {
                return conn;
            }
        }
        return null;
    }

    public synchronized PendingConnection getPendingByAddress(String address) {
        for (PendingConnection conn : mPending) {
            if (conn.getTarget().getAddress().equals(address)) {
                return conn;
            }
        }
        return null;
    }

    public synchronized PendingConnection getPendingByName(String name) {
        for (PendingConnection conn : mPending) {
            if (conn.getTarget().getName().equals(name)) {
                return conn;
            }
        }
        return null;
    }

    public synchronized Connection.Listener getListener(Connection conn) {
        for (Map.Entry<Connection.Listener, Connection> e : mStates.entrySet()) {
            if (e.getValue() == conn) {
                return e.getKey();
            }
        }
        return null;
    }

    public synchronized void start(BluetoothDevice device) {
        stop(device);
        PendingConnection pc = getPending(device);
        if (pc != null) {

        }

        mConnections.get(index).start();
    }

    public synchronized void stop(BluetoothDevice device) {
        PendingConnection pc = getPending(device);
        if (pc != null) {
            pc.cancel();
            mPending.remove(pc);
            pc = null;
        }

        BluetoothConnection bc = getConnection(device);
        if () {

        }
        mConnections.get(index).stop();
        mConnections.remove(index);
    }

    @Override
    public synchronized void start() {
        stop();
        for (BluetoothConnection conn : mConnections) {
            conn.start();
        }
    }

    @Override
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


    //
    // PendingConnection.Listener
    //

    @Override
    public synchronized boolean onConnected(BluetoothConnection conn) {
        if (getConnection(conn.getRemoteDevice()) != null) {
            Log.e(TAG, "Connection already established: " + conn.getName());
            return false;
        }

        PendingConnection pc = getPending(conn.getRemoteDevice());
        if (pc != null) {
            // expected outcome.
            mPending.remove(pc);
            mConnections.add(conn);
            return true;
        } else {
            Log.e(TAG, "Received unexpected connection: " + conn.getName());
            return false;
        }
    }

    @Override
    public void onFinished(PendingConnection conn) {

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
