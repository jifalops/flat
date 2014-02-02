package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.essentiallocalization.util.Filter;
import com.essentiallocalization.util.lifecycle.Cancelable;
import com.essentiallocalization.util.lifecycle.Connectable;
import com.essentiallocalization.util.lifecycle.Finishable;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jake on 9/20/13.
 */
public final class BluetoothClient extends Thread implements Cancelable, Finishable, Connectable {
    private static final String TAG = BluetoothClient.class.getSimpleName();

//    /** obj = BluetoothClient */
//    public static final int MSG_CONNECTED = 1;
//    /** obj = BluetoothClient */
//    public static final int MSG_FINISHED = 2;

    private static final int MAX_ATTEMPTS = 1;

    public static interface Listener {
        void onConnected(BluetoothClient btc);
        void onFinished(BluetoothClient btc);
    }

    private final Filter mFilter;
    private final Listener mListener;
    private final BluetoothDevice mTargetDevice;
    private BluetoothSocket mSocket;
    private UUID mUuid;

    private boolean mCanceled;
    private boolean mConnected;
    private boolean mFinished;

    public BluetoothClient(BluetoothDevice device, Filter filter, Listener listener) {
        mTargetDevice = device;
        mFilter = filter;
        mListener = listener;
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
            if (isConnected() || isCanceled()) break;
            tryConnection(i);
        }
        setFinished(true);
    }

    private void tryConnection(int i) {
        mUuid = BluetoothConnectionManager.UUIDS[i];
        for (int j = 0; j < MAX_ATTEMPTS; j++) {
            if (isConnected() || isCanceled()) {
                break;
            }

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
                if (device != null && mFilter.allow(device.getAddress())) {
                    setConnected(true);
//                    mHandler.obtainMessage(MSG_CONNECTED, this).sendToTarget();
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
        if (mConnected) {
            mListener.onConnected(this);
        }
    }

    private synchronized void setFinished(boolean finished) {
        mFinished = finished;
        if (mFinished) {
            mListener.onFinished(this);
        }
    }


    @Override
    public synchronized boolean isConnected() {
        return mConnected;
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
    public synchronized void cancel() {
        mCanceled = true;
        close();
    }

    private void close() {
        try {
            mSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to close client socket!");
        }
    }
}
