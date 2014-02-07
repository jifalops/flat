package com.essentiallocalization;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.bluetooth.BluetoothConnection;
import com.essentiallocalization.connection.bluetooth.BluetoothConnectionManager;
import com.essentiallocalization.util.Util;
import com.essentiallocalization.util.app.PersistentIntentService;
import com.essentiallocalization.util.io.Connection;

import java.io.IOException;

/**
 * Created by Jake on 2/6/14.
 */
public class BluetoothConnectionService extends PersistentIntentService {
    private  static final String TAG = BluetoothConnectionService.class.getSimpleName();

    private BluetoothConnectionManager mManager;
    private BluetoothConnectionManager.BluetoothConnectionListener mUserListener;
    private TimingLog mTimeLog;


    @Override
    public void onCreate() {
        super.onCreate();
        mManager = new BluetoothConnectionManager(getLooper(), mConnectionListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mManager != null) {
            mManager.disconnect();
            mManager.stopSnoopReader();
        }

        if (mTimeLog != null) {
            try {
                mTimeLog.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close time log");
            }
        }
    }

    public BluetoothConnectionManager getConnectionManager() {
        return mManager;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // listen to system bt broadcasts
//        BluetoothAdapter.ACTION_
    }

    //
    // Methods from BluetoothConnectionManager.
    //

//    public synchronized int send(BluetoothDevice device, String msg) throws IOException, Message.MessageTooLongException {
//        return mManager.send(device, msg);
//    }
//
//    public synchronized void sendToAll(String msg) throws IOException, Message.MessageTooLongException {
//        mManager.sendToAll(msg);
//    }
//
//    public synchronized void disconnect(BluetoothDevice device) {
//        mManager.disconnect(device);
//    }
//
//    public synchronized void disconnect() {
//        mManager.disconnect();
//    }
//
//    public synchronized void connect(BluetoothDevice device) {
//        mManager.connect(device);
//    }
//
//    public synchronized void connect(Set<BluetoothDevice> devices) {
//        for (BluetoothDevice device : devices) {
//            connect(device);
//        }
//    }
//
//    public void startSnoopReader(File snoopFile) throws IOException {
//        mManager.startSnoopReader(snoopFile);
//    }
//
//    public void startSnoopReader() throws IOException {
//        mManager.stopSnoopReader();
//    }
//
//    public void stopSnoopReader() {
//        mManager.stopSnoopReader();
//    }
//
//    public void setReconnect(boolean reconnect) {
//        mManager.setReconnect(reconnect);
//    }


    //
    //
    //

    public synchronized void setConnectionListener(BluetoothConnectionManager.BluetoothConnectionListener listener) {
        mUserListener = listener;
    }


//    public synchronized void readTimeLog() throws IOException {
//        mTimeLog.readAll();
//    }


    public synchronized void setTimingLog(TimingLog log) {
        mTimeLog = log;
    }

    private final BluetoothConnectionManager.BluetoothConnectionListener mConnectionListener =
            new BluetoothConnectionManager.BluetoothConnectionListener() {
        @Override
        public synchronized void onPacketReceived(DataPacket dp, BluetoothConnection conn) {
            if (mUserListener != null) mUserListener.onPacketReceived(dp, conn);
        }

        @Override
        public synchronized void onTimingComplete(DataPacket dp, BluetoothConnection conn) {
            if (mTimeLog != null) {
                float javaDist = Util.Calc.timeOfFlightDistance1(dp.javaSrcSent, dp.javaDestReceived, dp.javaDestSent, dp.javaSrcReceived);
                float hciDist = Util.Calc.timeOfFlightDistance1(dp.hciSrcSent, dp.hciDestReceived, dp.hciDestSent, dp.hciSrcReceived);
                mTimeLog.log(dp, javaDist, hciDist);
            }
            if (mUserListener != null) mUserListener.onTimingComplete(dp, conn);
        }

        @Override
        public synchronized void onStateChanged(BluetoothDevice device, int oldState, int newState) {
            if (newState == Connection.STATE_CONNECTED) {
                mTimeLog.incConnectionCount(BluetoothConnection.idFromName(device.getName()));
            }
            if (mUserListener != null) mUserListener.onStateChanged(device, oldState, newState);
        }

        @Override
        public synchronized void onSnoopPacketReaderFinished() {
            if (mUserListener != null) mUserListener.onSnoopPacketReaderFinished();
        }
    };
}
