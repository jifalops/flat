package com.essentiallocalization.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.essentiallocalization.connection.BluetoothConnection;
import com.essentiallocalization.connection.PendingBluetoothConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Jake on 9/2/13.
 */
public class BluetoothService extends PersistentIntentService {

    public static final int SPEED_OF_LIGHT = 299792458; // m/s

    public static final int MAX_CONNECTIONS = PendingBluetoothConnection.MAX_CONNECTIONS;



    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final String mName = mBluetoothAdapter.getName();
    private final String TAG = "BluetoothService " + mName;

    private final List<BluetoothConnection> mConnections = new ArrayList<BluetoothConnection>(MAX_CONNECTIONS);

    private Handler mHandler = new Handler();
    private int mMaxConnections = MAX_CONNECTIONS;
    private boolean mRunning;

    public void setHandler(Handler handler) {
        synchronized (mHandler) {
            mHandler = handler;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate()");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent()");
        // listen to system bt broadcasts
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind()");
        setHandler(new Handler());
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }

    public List<BluetoothConnection> getConnections() {
        return mConnections;
    }


    public synchronized void setMaxConnections(int connections) {
        boolean running = mRunning;
        stop();
        if (connections > MAX_CONNECTIONS) connections = MAX_CONNECTIONS;
        mMaxConnections = connections;
        if (running) start();
    }

    public synchronized int getMaxConnections() {
        return mMaxConnections;
    }

    public synchronized void start() {
        Log.v(TAG, "start()");
        stop();

        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices != null) {
            synchronized (mConnections) {
                int count = 0;
                for (BluetoothDevice d : devices) {
                    if (count >= mMaxConnections) break;
                    BluetoothConnection conn = new BluetoothConnection(mHandler, d);
                    conn.start();
                    mConnections.add(conn);
                    ++count;
                }
            }
        }
        mRunning = true;
    }

    public synchronized void stop() {
        Log.v(TAG, "stop()");
        synchronized (mConnections) {
            for (BluetoothConnection conn : mConnections) {
                conn.stop();
                mConnections.remove(conn);
                conn = null;
            }
        }
        mRunning = false;
    }

    public synchronized boolean isRunning() {
        return mRunning;
    }
}
