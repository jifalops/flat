package com.essentiallocalization.service.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.Packet;
import com.essentiallocalization.connection.bluetooth.BluetoothConnection;
import com.essentiallocalization.connection.bluetooth.BluetoothConnectionManager;
import com.essentiallocalization.util.SnoopFilter;
import com.essentiallocalization.service.PersistentIntentService;
import com.essentiallocalization.util.Calc;
import com.essentiallocalization.util.LogFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created by Jake on 9/2/13.
 */
public class BluetoothService extends PersistentIntentService implements BluetoothConnection.Listener {
    private  static final String TAG = BluetoothService.class.getSimpleName();


    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final String mName = mBluetoothAdapter.getName();

    /** arg1 = state, arg2 = prevState, obj = BluetoothConnection */
    public static final int MSG_STATE_CHANGE = 1;
    /** arg1 = pktIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_SENT_PACKET = 2;
    /** arg1 = msgIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_SENT_MSG = 3;
    /** arg1 = pktIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_RECEIVED_PACKET = 4;
    /** arg1 = msgIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_RECEIVED_MSG = 5;
    /** arg1 = pktIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_CONFIRMED_PACKET = 6;
    /** arg1 = msgIndex, arg2 = -1, obj = BluetoothConnection */
    public static final int MSG_CONFIRMED_MSG = 7;

    private BluetoothConnectionManager mManager;
    private Handler mHandler = new Handler();
    private boolean mRunning;
    private LogFile mLog;

    private SnoopFilter mMsgFilter;

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

        startSnoopFilter();

        mRunning = true;
    }

    public synchronized void stop() {
        Log.v(TAG, "stop()");
        mManager.stop();

        stopSnoopFilter();


        mRunning = false;
    }

    public synchronized void startSnoopFilter() {
        try {
            mMsgFilter = new SnoopFilter(BtSnoop.FILE, BtSnoop.OUT_FILE, Packet.PREFIX, null);
            mMsgFilter.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to create message filter");
        }
    }

    public synchronized void stopSnoopFilter() {
        if (mMsgFilter != null) {
            mMsgFilter.cancel();
            mMsgFilter = null;
        }
    }

    public synchronized void send(String msg) {
        try {
            mManager.sendMessage(new com.essentiallocalization.connection.Message(msg));
        } catch (IOException e) {
            Log.e(TAG, "Failed sending message");
        } catch (com.essentiallocalization.connection.Message.MessageTooLongException e) {
            Log.e(TAG, "Message too long!");
        }
    }

    public synchronized boolean isRunning() {
        return mRunning;
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

        DataPacket packet;
        String dir;
        if (sent) {
            packet = connection.getConnection().getSentPackets().get(packetIndex);
            dir = "Sent";
        } else {
            packet = connection.getConnection().getReceivedPackets().get(packetIndex);
            dir = "Received";
        }
        String[] entry = null;
        switch (type) {
            case Packet.TYPE_MSG:
                entry = new String[] {
                        "Msg " + dir,
                        String.valueOf(connection.getTo()),
                        String.valueOf(packet.pktIndex),
                        String.valueOf(packet.msgIndex),
                        String.valueOf(packet.attempt),
                        String.valueOf(packet.msgPart),
                        String.valueOf(packet.msgParts),
                        String.valueOf(packet.sent),
                        String.valueOf(packet.received),
                        String.valueOf(packet.resent),
                        String.valueOf(packet.confirmed),
                        Packet.trim(packet.payload)
                };
                break;
            case Packet.TYPE_TEST:
                entry = new String[] {
                        "Test " + dir,
                        String.valueOf(connection.getTo()),
                        String.valueOf(packet.pktIndex),
                        String.valueOf(packet.msgIndex),
                        String.valueOf(packet.attempt),
                        String.valueOf(packet.msgPart),
                        String.valueOf(packet.msgParts),
                        String.valueOf(packet.sent),
                        String.valueOf(packet.received),
                        String.valueOf(packet.resent),
                        String.valueOf(packet.confirmed),
                        Packet.trim(packet.payload)
                };
                break;
            case Packet.TYPE_ACK:
                entry = new String[] {
                        "Ack",
                        String.valueOf(connection.getTo()),
                        String.valueOf(packet.pktIndex),
                        String.valueOf(packet.sent),
                        String.valueOf(packet.received),
                        String.valueOf(packet.resent),
                        String.valueOf(packet.confirmed),
                        String.valueOf(Calc.timeOfFlightDistance1(packet.sent, packet.received, packet.resent, packet.confirmed))
                };
                break;
        }
        return TextUtils.join(" | ", entry);
    }

    private static class BtSnoop {
        public static final File FILE = new File(Environment.getExternalStorageDirectory(), "btsnoop_hci.log"),
                                OUT_FILE = new File(Environment.getExternalStorageDirectory(), "btsnoop_packets.txt"),
                                CSV_FILE = new File(Environment.getExternalStorageDirectory(), "btsnoop_packet_data.csv");
    }
}
