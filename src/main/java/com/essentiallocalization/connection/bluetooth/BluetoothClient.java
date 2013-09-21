package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothDevice;
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
public final class BluetoothClient extends Thread implements PendingConnection {
    private static final String TAG = BluetoothClient.class.getSimpleName();

    /** obj = BluetoothClient */
    public static final int MSG_CONNECTED = 1;
    /** obj = BluetoothClient */
    public static final int MSG_FINISHED = 2;

    private static final int MAX_ATTEMPTS = 3;

    private final ConnectionFilter mManager;
    private final Handler mHandler;
    private final BluetoothDevice mTargetDevice;
    private BluetoothSocket mSocket;
    private UUID mUuid;
    private boolean mCanceled;
    private boolean mConnected;
    private boolean mFinished;


    public BluetoothClient(ConnectionFilter connMgr, Handler handler, BluetoothDevice device) {
        mTargetDevice = device;
        mManager = connMgr;
        mHandler = handler;
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
            tryConnection(i);
        }
        setFinished(true);
    }

    private void tryConnection(int i) {
        mUuid = BluetoothConnectionManager.UUIDS[i];
        for (int j = 0; j < MAX_ATTEMPTS; j++) {
            if (isConnected() || isCanceled()) break;

            try {
                mSocket = mTargetDevice.createRfcommSocketToServiceRecord(mUuid);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't create client socket " + i + ", attempt " + j);
            }

            if (mSocket != null) {
                try {
                    mSocket.connect(); // Blocks
                } catch (IOException e) {
                    Log.w(TAG, "Client " + i + " attempt " + j + " interrupted");
                    close();
                    continue;
                }

                BluetoothDevice device = mSocket.getRemoteDevice();
                if (device != null && mManager.isAllowed(device.getAddress())) {
                    setConnected(true);
                    mHandler.obtainMessage(MSG_CONNECTED, this).sendToTarget();
                    break;
                }
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Client " + i + " attempt " + j + " unable to sleep");
                }
            }
        }
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

    public void close() {
        try {
            mSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to close client socket!");
        }
    }
}
