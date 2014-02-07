package com.essentiallocalization;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.Message;
import com.essentiallocalization.connection.bluetooth.BluetoothConnection;
import com.essentiallocalization.connection.bluetooth.BluetoothConnectionManager;
import com.essentiallocalization.util.app.PersistentIntentService;
import com.essentiallocalization.util.io.Connection;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Jake on 2/6/14.
 */
public class BluetoothConnectionService extends PersistentIntentService {
    private  static final String TAG = BluetoothConnectionService.class.getSimpleName();

    private BluetoothConnectionManager mManager;
    private Map<BluetoothDevice, Boolean> mDevices;
    private BluetoothConnectionManager.BluetoothConnectionListener mUserListener;
    private TimingLog mTimeLog;

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = new BluetoothConnectionManager(getLooper());
        mManager.setConnectionListener(mConnectionListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mManager.disconnect();
        mManager.stopSnoopReader();
    }

//    public BluetoothConnectionManager getConnectionManager() {
//        return mManager;
//    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // listen to system bt broadcasts
//        BluetoothAdapter.ACTION_
    }

    //
    // Methods from BluetoothConnectionManager.
    //

    public synchronized int send(BluetoothDevice device, String msg) throws IOException, Message.MessageTooLongException {
        return mManager.send(device, msg);
    }

    public synchronized void sendToAll(String msg) throws IOException, Message.MessageTooLongException {
        mManager.sendToAll(msg);
    }

    public synchronized void disconnect(BluetoothDevice device) {
        mManager.disconnect(device);
    }

    public synchronized void disconnect() {
        mManager.disconnect();
    }

    public synchronized void connect(BluetoothDevice device) {
        mManager.connect(device);
    }

    public synchronized void connect(Set<BluetoothDevice> devices) {
        for (BluetoothDevice device : devices) {
            connect(device);
        }
    }

    public void startSnoopReader(File snoopFile) throws IOException {
        mManager.startSnoopReader(snoopFile);
    }

    public void startSnoopReader() throws IOException {
        mManager.stopSnoopReader();
    }

    public void stopSnoopReader() {
        mManager.stopSnoopReader();
    }


    //
    //
    //

    public synchronized void setConnectionListener(BluetoothConnectionManager.BluetoothConnectionListener listener) {
        mUserListener = listener;
    }

    public synchronized void setTimingLog(File log) {
        try {
            mTimeLog = new TimingLog(log, true, new TimingLog.TimeLogListener() {
                @Override
                public void onInitialized() {
                    //todo
                }

                @Override
                public void onReadAll(List<String[]> lines) {
                    //todo
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Couldn't open log file.", e);
            //Toast.makeText(this, "Logging disabled", Toast.LENGTH_SHORT).show();
        }
    }

    public synchronized TimingLog getTimeLog() {
        return mTimeLog;
    }

    public synchronized void setReconnect(BluetoothDevice device, boolean reconnect) {
        mDevices.put(device, reconnect);
    }

    public synchronized boolean shouldReconnect(BluetoothDevice device) {
        return mDevices.get(device);
    }


    private final BluetoothConnectionManager.BluetoothConnectionListener mConnectionListener =
            new BluetoothConnectionManager.BluetoothConnectionListener() {
        @Override
        public synchronized void onPacketReceived(DataPacket dp, BluetoothConnection conn) {
            if (mUserListener != null) mUserListener.onPacketReceived(dp, conn);
        }

        @Override
        public synchronized void onTimingComplete(DataPacket dp, BluetoothConnection conn) {
            if (mUserListener != null) mUserListener.onTimingComplete(dp, conn);
        }

        @Override
        public synchronized void onStateChanged(BluetoothDevice device, int oldState, int newState) {
            if (newState == Connection.STATE_DISCONNECTED && shouldReconnect(device)) {
                mManager.connect(device);
            }
            if (mUserListener != null) mUserListener.onStateChanged(device, oldState, newState);
        }

        @Override
        public synchronized void onSnoopPacketReaderFinished() {
            if (mUserListener != null) mUserListener.onSnoopPacketReaderFinished();
        }
    };
}
