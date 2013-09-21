package com.essentiallocalization.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.Packet;
import com.essentiallocalization.connection.bluetooth.BluetoothConnection;
import com.essentiallocalization.connection.bluetooth.BluetoothConnectionManager;
import com.essentiallocalization.util.LogFile;

import java.util.Set;

/**
 * Created by Jake on 9/2/13.
 */
public class BluetoothService extends PersistentIntentService implements BluetoothConnection.Listener {
    private  static final String TAG = BluetoothService.class.getSimpleName();
    public static final int SPEED_OF_LIGHT = 299792458; // m/s

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final String mName = mBluetoothAdapter.getName();

    /** arg1 = state, arg2 = prevState, obj = BluetoothConnection */
    public static final int MSG_STATE_CHANGE = 1;
    /** arg1 = packetIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_SENT_PACKET = 2;
    /** arg1 = msgIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_SENT_MSG = 3;
    /** arg1 = packetIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_RECEIVED_PACKET = 4;
    /** arg1 = msgIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_RECEIVED_MSG = 5;
    /** arg1 = packetIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_CONFIRMED_PACKET = 6;
    /** arg1 = msgIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_CONFIRMED_MSG = 7;

    private BluetoothConnectionManager mManager;
    private Handler mHandler = new Handler();
    private boolean mRunning;
    private LogFile mLog;

    public void setHandler(Handler handler) {
        synchronized (mHandler) {
            mHandler = handler;
        }
    }

    public void setLogFile(LogFile log) {
        mLog = log;

        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices != null) {
            synchronized (mManager) {
                int count = 0;
                for (BluetoothDevice d : devices) {
                    if (count >= mManager.getMaxConnections()) break;
                    mManager.addDevice(d);
                    ++count;
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate()");
        mManager = new BluetoothConnectionManager(this, mServiceLooper);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent()");
        // listen to system bt broadcasts
//        BluetoothAdapter.ACTION_
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind()");
        mHandler.removeCallbacksAndMessages(null);
        setHandler(new Handler());
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }

    public BluetoothConnectionManager getConnectionManager() {
        return mManager;
    }


    public synchronized void setMaxConnections(int connections) {
        stop();
        mManager.setMaxConnections(connections);
    }

    public synchronized int getMaxConnections() {
        return mManager.getMaxConnections();
    }


    public synchronized void start() {
        Log.v(TAG, "start()");
        stop();


        mManager.start();
        mRunning = true;
    }

    public synchronized void stop() {
        Log.v(TAG, "stop()");
        mManager.stop();
        mRunning = false;
    }

    public synchronized boolean isRunning() {
        return mRunning;
    }

    private float calcDistance(DataPacket p) {
        long roundTrip = p.confirmed - p.sent;
        double distance = (BluetoothService.SPEED_OF_LIGHT * (roundTrip * 1E-9)) / 2;
        return (float) Math.round(distance * 100) / 100;
    }

    @Override
    public void onStateChange(BluetoothConnection connection, int state, int previousState) {
        String name = connection.getName();
        if (name == null) {
            name = String.valueOf(mManager.getConnections().indexOf(connection));
        }
        mLog.d(TAG, name + ": state " +
            BluetoothConnection.getState(previousState) + " -> " + BluetoothConnection.getState(state));
        mHandler.obtainMessage(MSG_STATE_CHANGE, state, previousState, connection).sendToTarget();
    }



    @Override
    public void onPacketReceived(BluetoothConnection connection, int packetIndex) {
        mLog.i(TAG, makeLogEntry(connection, Packet.TYPE_MSG, packetIndex, false));
        mHandler.obtainMessage(MSG_RECEIVED_PACKET, packetIndex, -1, connection).sendToTarget();
    }

    @Override
    public void onMessageReceived(BluetoothConnection connection, int msgIndex) {

    }

    @Override
    public void onPacketSent(BluetoothConnection connection, int packetIndex) {
        mLog.i(TAG, makeLogEntry(connection, Packet.TYPE_MSG, packetIndex, true));
        mHandler.obtainMessage(MSG_SENT_PACKET, packetIndex, -1, connection).sendToTarget();
    }

    @Override
    public void onMessageSent(BluetoothConnection connection, int msgIndex) {

    }

    @Override
    public void onPacketConfirmed(BluetoothConnection connection, int packetIndex) {
        mLog.i(TAG, makeLogEntry(connection, Packet.TYPE_ACK, packetIndex, true));
        mHandler.obtainMessage(MSG_CONFIRMED_PACKET, packetIndex, -1, connection).sendToTarget();
    }

    @Override
    public void onMessageConfirmed(BluetoothConnection connection, int msgIndex) {

    }



    private String makeLogEntry(BluetoothConnection connection, int type, int packetIndex, boolean sent) {
        DataPacket packet = sent
                ? connection.getConnection().getSentPackets().get(packetIndex)
                : connection.getConnection().getReceivedPackets().get(packetIndex);
        String[] entry = null;
        switch (type) {
            case Packet.TYPE_MSG:
                entry = new String[] {
                        "Msg",
                        String.valueOf(connection.getTo()),
                        String.valueOf(packet.packetIndex),
                        String.valueOf(packet.msgIndex),
                        String.valueOf(packet.msgAttempt),
                        String.valueOf(packet.msgPart),
                        String.valueOf(packet.msgParts),
                        String.valueOf(packet.sent),
                        String.valueOf(packet.received),
                        String.valueOf(packet.confirmed),
                        Packet.trim(packet.payload)
                };
                break;
            case Packet.TYPE_TEST:
                entry = new String[] {
                        "Test",
                        String.valueOf(connection.getTo()),
                        String.valueOf(packet.packetIndex),
                        String.valueOf(packet.msgIndex),
                        String.valueOf(packet.msgAttempt),
                        String.valueOf(packet.msgPart),
                        String.valueOf(packet.msgParts),
                        String.valueOf(packet.sent),
                        String.valueOf(packet.received),
                        String.valueOf(packet.confirmed),
                        Packet.trim(packet.payload)
                };
                break;
            case Packet.TYPE_ACK:
                entry = new String[] {
                        "Ack",
                        String.valueOf(connection.getTo()),
                        String.valueOf(packet.packetIndex),
                        String.valueOf(packet.sent),
                        String.valueOf(packet.received),
                        String.valueOf(packet.confirmed),
                        String.valueOf(calcDistance(packet))
                };
                break;
        }
        return TextUtils.join(" | ", entry);
    }
}
