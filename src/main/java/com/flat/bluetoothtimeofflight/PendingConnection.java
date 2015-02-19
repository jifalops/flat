package com.flat.bluetoothtimeofflight;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Looper;
import android.util.Log;

import com.flat.bluetoothtimeofflight.io.BasicConnection;
import com.flat.bluetoothtimeofflight.io.Connection;
import com.flat.bluetoothtimeofflight.lifecycle.Finishable;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jake on 2/2/14.
 */
public final class PendingConnection extends BasicConnection implements DeviceConnection, Finishable,
        BluetoothClient.ClientListener, BluetoothServer.ServerListener {
    private static final String TAG = PendingConnection.class.getSimpleName();

    public static interface PendingListener {
        /**
         * Called on separate thread (client or server). Implementations should
         * return whether the connection was accepted or not.
         */
        boolean onConnected(BluetoothConnection conn);
        /** called on separate thread (client or server). */
        void onFinished(PendingConnection conn);
    }

    private  BluetoothServer mServer;
    private  BluetoothClient mClient;
    private final PendingListener mListener;
    private final Looper mLooper;

    private BluetoothSocket mSocket;

    private boolean mCanceled;
    private boolean mFinished;

    public PendingConnection(BluetoothDevice target, PendingListener listener, Looper connectionSendAndEventLooper) {
        mListener = listener;
        mLooper = connectionSendAndEventLooper;
        mServer = new BluetoothServer(this);
        mClient = new BluetoothClient(target, this);
    }

    @Override
    public BluetoothDevice getDevice() {
        return mClient.getTarget();
    }

    @Override
    public synchronized void start() {
        setState(Connection.STATE_CONNECTING);
        mServer.start();
        mClient.start();
    }

    @Override
    public synchronized void cancel() {
        mCanceled = true;
        close();
    }

    private synchronized void finish() {
        mFinished = true;
        close();
        mListener.onFinished(this);
    }

    private void close() {
        if (mClient != null) {
            mClient.cancel();
            mClient = null;
        }

        if (mServer != null) {
            mServer.cancel();
            mServer = null;
        }

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close bt socket!");
            }
            // Causes error for some reason?
//            mSocket = null;
        }
    }

    @Override
    public synchronized boolean isFinished() {
        return mFinished;
    }

    @Override
    public synchronized boolean isCanceled() {
        return mCanceled;
    }

    @Override
    public synchronized void onFinished(BluetoothServer btServer) {
        if (!isConnected() && (mClient.isFinished() || mClient.isCanceled())) {
            finish();
        }
    }

    @Override
    public synchronized void onFinished(BluetoothClient btClient) {
        if (!isConnected() && (mServer.isFinished() || mServer.isCanceled())) {
            finish();
        }
    }

    @Override
    public synchronized boolean onConnected(String macAddress, BluetoothClient btClient) {
        if (mListener.onConnected(makeConnection(false))) {
            setState(STATE_CONNECTED);
            mServer.cancel();
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean onConnected(String macAddress, BluetoothServer btServer) {
        if (mListener.onConnected(makeConnection(true))) {
            setState(STATE_CONNECTED);
            mClient.cancel();
            return true;
        }
        return false;
    }

    private BluetoothConnection makeConnection(boolean selfIsServer) {
        UUID uuid = null;
        if (selfIsServer) {
            mSocket = mServer.getSocket();
            uuid = mServer.getUuid();
        } else {
            mSocket = mClient.getSocket();
            uuid = mClient.getUuid();
        }

        BluetoothConnection conn = null;
        try {
            conn = new BluetoothConnection(mSocket, uuid, selfIsServer, mLooper);
        } catch (IOException e) {
            Log.e(TAG, "Unable to get I/O stream", e);
        }
        return conn;
    }


    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Throwable t) {
            Log.e(TAG, "Exception in finalize().");
        }
        finally {
            super.finalize();
        }
    }
}
