package com.flat.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.flat.util.lifecycle.Connectable;
import com.flat.util.lifecycle.Finishable;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jake on 9/20/13.
 */
public final class BluetoothServer extends Thread implements Finishable, Connectable {
    private static final String TAG = BluetoothServer.class.getSimpleName();
    private static final String SERVICE_NAME = "EssLocBT";

//    /** obj = BluetoothServer */
//    public static final int MSG_CONNECTED = 1;
//    /** obj = BluetoothServer */
//    public static final int MSG_FINISHED = 2;

    public static interface ServerListener {
        /**
         * Called on separate thread (this). Implementations should
         * return whether the connection was accepted or not.
         */
        boolean onConnected(String macAddress, BluetoothServer btServer);
        /** called on separate thread (this). */
        void onFinished(BluetoothServer btServer);
    }

    private final ServerListener mListener;
    private final BluetoothAdapter mAdapter;

    private BluetoothServerSocket mServerSocket;
    private boolean mCanceled;
    private boolean mConnected;
    private boolean mFinished;
    private BluetoothSocket mSocket;
    private UUID mUuid;

    public BluetoothServer(ServerListener listener) {
        mListener = listener;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized BluetoothSocket getSocket() {
        return mSocket;
    }

    public synchronized UUID getUuid() {
        return mUuid;
    }

    @Override
    public void run() {
        for (int i = 0; i < BluetoothConnectionManager.MAX_CONNECTIONS; i++) {
            if (mConnected || isCanceled()) break;

            mUuid = BluetoothConnectionManager.UUIDS[i];
            try {
                mServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, mUuid);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't create server socket " + i);
                continue;
            }

            if (mServerSocket != null) {
                try {
                    mSocket = mServerSocket.accept(); // Blocks
                    close();
                } catch (IOException e) {
                    Log.e(TAG, "Server " + i + " interrupted.");
                    break;
                }

                if (mSocket != null) {
                    BluetoothDevice device = mSocket.getRemoteDevice();
                    if (device != null && mListener.onConnected(device.getAddress(), this)) {
                        setConnected(true);
//                        mHandler.obtainMessage(MSG_CONNECTED, this).sendToTarget();
                    }
                }
            }
        }
        if (!isCanceled()) {
            finish();
        }
    }

    private synchronized void setConnected(boolean connected) {
        mConnected = connected;
    }

    private synchronized void finish() {
        mFinished = true;
        close();
        mListener.onFinished(this);
    }

    public synchronized boolean isConnected() {
        return mConnected;
    }

    public synchronized boolean isFinished() {
        return mFinished;
    }

    public synchronized boolean isCanceled() {
        return mCanceled;
    }

    public synchronized void cancel() {
        mCanceled = true;
        close();
    }

    private synchronized void close() {
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close server socket!");
            }
        }
    }
}
