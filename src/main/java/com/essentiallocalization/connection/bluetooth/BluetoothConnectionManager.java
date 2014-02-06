package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import com.essentiallocalization.connection.AckTimePacket;
import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.Message;
import com.essentiallocalization.connection.PacketConnection;
import com.essentiallocalization.connection.SnoopPacketReader;
import com.essentiallocalization.util.io.Connection;
import com.essentiallocalization.util.io.SnoopFilter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Created by Jake on 9/15/13.
 */
public final class BluetoothConnectionManager {
    private static final String TAG = BluetoothConnectionManager.class.getSimpleName();

    static final UUID[] UUIDS = {
            // limited to 7 by piconet standard
            UUID.fromString("0aa67214-5217-4ded-b656-5cccff9a237c"),
            UUID.fromString("709bd128-8f45-45d6-aea7-cb1fb7303ea5"),
            UUID.fromString("4c91a1a9-fa9f-4338-8b5f-1d5f48d6b20e"),
            UUID.fromString("20d47c6a-eeda-45a4-98a6-7d4f371f1a34"),
            UUID.fromString("ffdc4e45-d4c7-4789-81c7-b4f6e03ca865"),
            UUID.fromString("1052c5cf-67db-4e5c-80e3-de3a89bbaf96"),
            UUID.fromString("30cb5e95-c22a-41d1-b609-dedf29e866cf"),
    };
    public static final int MAX_CONNECTIONS = UUIDS.length;



    public static interface BluetoothConnectionListener {
        /**
         * Executed on a separate thread (sendAndEventLoop or SnoopFilter thread)
         * Note: use DataPacket toString() method to get msg text.
         */
        void onPacketReceived(DataPacket dp, BluetoothConnection conn);

        /** Executed on a separate thread (sendAndEventLoop or SnoopFilter thread) */
        void onTimingComplete(DataPacket dp, BluetoothConnection conn);

        /**
         * Usually executed on a separate thread:
         *  NONE:           btclient or btserver threads
         *  CONNECTING:     PendingConnection.start() (current thread or separate thread)
         *  CONNECTED:      btclient or btserver threads
         *  DISCONNECTED:   StreamConnection's instream listener thread
         */
        void onStateChanged(BluetoothDevice device, int oldState, int newState);

        /** Executed on the SnoopFilter thread */
        void onSnoopPacketReaderFinished();
    }

    private final Map<BluetoothDevice, Pair<DeviceConnection, Connection.StateChangeListener>> mConnections;
    private final Looper mLooper;
    private BluetoothConnectionListener mBluetoothListener;

    private SnoopPacketReader mSnoopReader;
    private int mConnLimit;

    public BluetoothConnectionManager(Looper sendAndEventLooper) {
        mConnections = new HashMap<BluetoothDevice, Pair<DeviceConnection, Connection.StateChangeListener>>(MAX_CONNECTIONS);

        mLooper = sendAndEventLooper;
        mConnLimit = MAX_CONNECTIONS;

    }

    public void setConnectionListener(final BluetoothConnectionListener listener) {
        synchronized (mBluetoothListener) {
            mBluetoothListener = listener;
        }
    }

    //
    // start and stop (and helper class StateListener)
    //

    public synchronized void disconnect(BluetoothDevice device) {
        Pair<DeviceConnection, Connection.StateChangeListener> pair = mConnections.get(device);
        if (pair != null) {
            pair.first.cancel();
            mConnections.remove(device);
        }
    }

    public synchronized void disconnect() {
        for (Map.Entry<BluetoothDevice, Pair<DeviceConnection, Connection.StateChangeListener>> e : mConnections.entrySet()) {
            disconnect(e.getKey());
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

    public synchronized void connect(BluetoothDevice device) {
        disconnect(device);

        PendingConnection pc = new PendingConnection(device, mPendingListener, mLooper);
        final StateListener listener = new StateListener();
        pc.setStateChangeListener(listener);
        mConnections.put(device, new Pair<DeviceConnection, Connection.StateChangeListener>(pc, listener));
        pc.start();
    }

    public void startSnoopReader(File snoopFile) throws IOException {
        stopSnoopReader();
        mSnoopReader = new SnoopPacketReader(snoopFile, BluetoothConnection.SELF_ID, mSnoopPacketListener);
        mSnoopReader.start();
    }

    public void startSnoopReader() throws IOException {
        startSnoopReader(SnoopFilter.DEFAULT_SNOOP_FILE);
    }

    public void stopSnoopReader() {
        if (mSnoopReader != null) {
            mSnoopReader.cancel();
            mSnoopReader = null;
        }
    }

    private class StateListener implements Connection.StateChangeListener {
        @Override
        public void onStateChange(int oldState, int newState) {
            if (mBluetoothListener != null) mBluetoothListener.onStateChanged(getDevice(this), oldState, newState);
        }
    }

    //
    // connection limits
    //
    public synchronized void setMaxConnections(int limit) {
        disconnect();
        if (limit > MAX_CONNECTIONS) limit = MAX_CONNECTIONS;
        mConnLimit = limit;
    }

    public synchronized int getMaxConnections() {
        return mConnLimit;
    }



    private final PendingConnection.PendingListener mPendingListener =  new PendingConnection.PendingListener() {
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
                    btconn.setPacketConnectionListener(mPacketListener);
                    final StateListener listener = new StateListener();
                    btconn.setStateChangeListener(listener);
                    mConnections.remove(device);
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
            // todo: disconnected and connecting are handled in StreamConnection and PendingConnection.start(),
            // respectively (they should probably be handled in this class).
            if (conn.getState() != Connection.STATE_CONNECTED) {
                conn.setState(Connection.STATE_NONE);
            } else {
                Log.w(TAG, "Expected no connection on finish: " + conn.getDevice().getName());
            }
        }
    };



    private final PacketConnection.PacketListener mPacketListener =  new PacketConnection.PacketListener() {

        @Override
        public synchronized void onDataPacketReceived(PacketConnection pc, DataPacket dp) {
            sendAck(pc, dp);
            if (mBluetoothListener != null) mBluetoothListener.onPacketReceived(dp, getBtConn(getConnection(pc.getDest())));
        }

        @Override
        public synchronized void onJavaTimingComplete(PacketConnection pc, DataPacket dp) {
            if (dp.isTimingComplete()) {
                if (mBluetoothListener != null) mBluetoothListener.onTimingComplete(dp, getBtConn(getConnection(dp.dest)));
            }
        }
    };


    private final SnoopPacketReader.SnoopPacketListener mSnoopPacketListener =  new SnoopPacketReader.SnoopPacketListener() {
        @Override
        public synchronized void onDataReceived(DataPacket dp) {
            // lookup the source of this packet
            sendAck(getBtConn(getConnection(dp.src)), dp);
        }

        @Override
        public synchronized void onSendAckTime(AckTimePacket atp) {
            BluetoothConnection bc = getBtConn(getConnection(atp.src));
            if (bc != null) {
                try {
                    bc.send(atp);
                } catch (IOException e) {
                    Log.e(TAG, "Error sending ACK to " + bc.getDest() + " for pktIndex " + atp.pktIndex);
                }
            }
        }

        @Override
        public synchronized void onHciTimingComplete(DataPacket dp) {
            if (dp.isTimingComplete()) {
                if (mBluetoothListener != null) mBluetoothListener.onTimingComplete(dp, getBtConn(getConnection(dp.dest)));
            }
        }

        @Override
        public void onFinished() {
            if (mBluetoothListener != null) mBluetoothListener.onSnoopPacketReaderFinished();
        }
    };

    //
    // Other packet management
    //

    private boolean sendAck(PacketConnection pc, DataPacket dp) {
        if (!pc.isConnected()) {
            Log.w(TAG, "Received packet without being connected");
            return false;
        }

        // check required timestamps and that the packet is from the current connection.
        if (dp.isAckReady() && pc.findPacket(dp.src, dp.dest, dp.pktIndex, dp.javaSrcSent) != null) {
            try {
                pc.send(dp.toAckPacket());
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error sending ACK to " + pc.getDest() + " for pktIndex " + dp.pktIndex);
            }
        }
        return false;
    }

    private BluetoothConnection getBtConn(DeviceConnection dc) {
        if (dc == null) {
            Log.w(TAG, "Cannot fetch null connection.");
            return null;
        }

        try {
            return (BluetoothConnection) dc;
        } catch (ClassCastException e) {
            Log.w(TAG, "Could not cast to BluetoothConnection.");
        }
        return null;
    }

    private PendingConnection getPendingConn(DeviceConnection dc) {
        if (dc == null) {
            Log.w(TAG, "Cannot fetch null connection.");
            return null;
        }
        try {
            return (PendingConnection) dc;
        } catch (ClassCastException e) {
            Log.w(TAG, "Could not cast to PendingConnection.");
        }
        return null;
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
}
