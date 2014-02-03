package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import com.essentiallocalization.connection.AckPacket;
import com.essentiallocalization.connection.AckTimePacket;
import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.Message;
import com.essentiallocalization.connection.Packet;
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

    private final Map<BluetoothDevice, Pair<DeviceConnection, Connection.StateChangeListener>> mConnections;
    private final Looper mLooper;
    private final Listener mListener;

    private int mConnLimit;

    public BluetoothConnectionManager(Looper sendAndEventLooper, Listener listener) {
        mConnections = new HashMap<BluetoothDevice, Pair<DeviceConnection, Connection.StateChangeListener>>(MAX_CONNECTIONS);

        mLooper = sendAndEventLooper;
        mListener = listener;
        mConnLimit = MAX_CONNECTIONS;
    }

    //
    // Shortcut methods
    //

    public synchronized DeviceConnection getConnection(byte dest) {
        for (Map.Entry<BluetoothDevice, Pair<DeviceConnection, Connection.StateChangeListener>> e : mConnections.entrySet()) {
            DeviceConnection conn = e.getValue().first;
            byte id = BluetoothConnection.idFromName(conn.getDevice().getName());
            if (id == dest) return conn;
        }
        return null;
    }

    public synchronized DeviceConnection getConnection(BluetoothDevice device) {
        return mConnections.get(device).first;
    }

    public synchronized Connection.StateChangeListener getListener(BluetoothDevice device) {
        return mConnections.get(device).second;
    }

    public synchronized BluetoothDevice getDevice(DeviceConnection conn) {
        for (Map.Entry<BluetoothDevice, Pair<DeviceConnection, Connection.StateChangeListener>> e : mConnections.entrySet()) {
            if (e.getValue().first == conn) return e.getKey();
        }
        return null;
    }

    public synchronized BluetoothDevice getDevice(Connection.StateChangeListener listener) {
        for (Map.Entry<BluetoothDevice, Pair<DeviceConnection, Connection.StateChangeListener>> e : mConnections.entrySet()) {
            if (e.getValue().second == listener) return e.getKey();
        }
        return null;
    }


    //
    // start and stop (and helper class)
    //

    public synchronized void stop(BluetoothDevice device) {
        Pair<DeviceConnection, Connection.StateChangeListener> pair = mConnections.get(device);
        if (pair != null) {
            pair.first.cancel();
            mConnections.remove(device);
        }
    }

    public synchronized void stopAll() {
        for (Map.Entry<BluetoothDevice, Pair<DeviceConnection, Connection.StateChangeListener>> e : mConnections.entrySet()) {
            stop(e.getKey());
        }
    }

    public synchronized int send(BluetoothDevice device, String msg) throws IOException, Message.MessageTooLongException {
        DeviceConnection conn = getConnection(device);
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
        mConnections.put(device, new Pair<DeviceConnection, Connection.StateChangeListener>(pc, listener));
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

    /**
     * This function determines if a connection is accepted or not.
     */
    @Override
    public synchronized boolean onConnected(BluetoothConnection btconn) {
        BluetoothDevice device = btconn.getDevice();
        DeviceConnection conn = getConnection(device);
        if (conn != null) {
            if (conn.isConnected()) {
                Log.e(TAG, "Connection already established: " + btconn.getName());
                return false;
            } else {
                mConnections.remove(device);
                StateListener listener = new StateListener();
                btconn.setStateChangeListener(listener);
                mConnections.put(device, new Pair<DeviceConnection, Connection.StateChangeListener>(btconn, listener));
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
        if (conn.getState() != Connection.STATE_CONNECTED) {
            conn.setState(Connection.STATE_NONE);
        } else {
            Log.w(TAG, "Expected no connection on finish: " + conn.getDevice().getName());
        }
    }


    //
    // PacketConnection.Listener
    //

    @Override
    public void onSendAck(PacketConnection pc, AckPacket ap) {
        if (ap.)
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

    //
    // Other packet management
    //

    private static class PacketWatcher {
        static void process(PacketConnection pc, AckPacket ap) {
            if (
                    && pc.getPackets()) {
                pc.send(ap);
            }
        }

        /**
         * Used to determine if packets found are from this session
         * (they may be stale in the hci log file).
         */
        static boolean isValid(PacketConnection pc, AckPacket ap) {
            for (DataPacket dp : pc.getPackets()) {
                if ((dp.hciDestReceived == ap.hciDestReceived && ap.hciDestReceived > 0)
                        || (dp.javaDestReceived == ap.javaDestReceived && ap.javaDestReceived > 0)
                        || (dp.javaDestSent == ap.javaDestSent && ap.javaDestSent > 0)) {
                    return true;
                }
            }
            // todo: make sure this will be called AFTER the packet is added to the list.
            return ;
        }
    }



}
