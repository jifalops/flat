package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.essentiallocalization.connection.ConnectionFilter;
import com.essentiallocalization.connection.PendingConnection;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jake on 9/20/13.
 */
public final class BluetoothServer extends Thread implements PendingConnection {
    private static final String TAG = BluetoothServer.class.getSimpleName();
    private static final String SERVICE_NAME = "EssentialLocalizationBluetoothService";

    /** obj = BluetoothServer */
    public static final int MSG_CONNECTED = 1;
    /** obj = BluetoothServer */
    public static final int MSG_FINISHED = 2;

    private final BluetoothAdapter mAdapter;
    private final ConnectionFilter mManager;
    private final Handler mHandler;
    private BluetoothServerSocket mServerSocket;
    private boolean mCanceled;
    private boolean mConnected;
    private boolean mFinished;
    private BluetoothSocket mSocket;
    private UUID mUuid;

    public BluetoothServer(ConnectionFilter connMgr, Handler handler) {
        mManager = connMgr;
        mHandler = handler;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized BluetoothSocket getSocket() {
        return mSocket;
    }

    public synchronized UUID getUuid() {
        return mUuid;
    }

    @Override
    public synchronized void start() {
        super.start();
    }


    @Override
    public void run() {
        for (int i = 0; i < BluetoothConnectionManager.MAX_CONNECTIONS; i++) {
            if (isConnected() || isCanceled()) break;

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
                    Log.w(TAG, "Server " + i + " interrupted.");
                    break;
                }

                if (mSocket != null) {
                    BluetoothDevice device = mSocket.getRemoteDevice();
                    if (device != null && mManager.isAllowed(device.getAddress())) {
                        setConnected(true);
                        mHandler.obtainMessage(MSG_CONNECTED, this).sendToTarget();
                    }
                }
            }
        }
        setFinished(true);
    }

    private synchronized void setConnected(boolean connected) {
        mConnected = connected;
    }

    private synchronized void setFinished(boolean finished) {
        mFinished = finished;
        if (mFinished) {
            mHandler.obtainMessage(MSG_FINISHED, this).sendToTarget();
        }
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
        try {
            mServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close server socket!");
        }
    }
}
