package com.essentiallocalization.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Jake on 9/15/13.
 */
public class BluetoothConnection {
    private static final String TAG = BluetoothConnection.class.getSimpleName();

    public static final int MSG_STATE_CHANGE = 1;
    public static final int MSG_RECEIVED = 2;
    public static final int MSG_SENT = 3;
    public static final int MSG_CONFIRMED = 4;
    public static final int MSG_CONNECTED = 5;
    public static final int MSG_CONNECTION_FAILED = 6;
    public static final int MSG_DISCONNECTED = 7;

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private PendingBluetoothConnection mPendingConnection;
    private RemoteConnection mConnection;
    private Info mInfo;
    private final BluetoothDevice mDevice;
    private final Handler mHandler;
    private int mState;

    public BluetoothConnection(Handler handler, BluetoothDevice device) {
        mDevice = device;
        mHandler = handler;
        setState(STATE_NONE);
    }

    private synchronized void setState(int state) {
        mState = state;
        mHandler.obtainMessage(MSG_STATE_CHANGE, state).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        stop();
        mPendingConnection = new PendingBluetoothConnection(mPendingConnectionHandler, mDevice);
        mPendingConnection.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void stop() {
        if (mPendingConnection != null) {
            mPendingConnection.cancel();
            mPendingConnection = null;
        }
        if (mConnection != null) {
            mConnection.close();
            mConnection = null;
        }
        if (mInfo != null) {
            try {
                mInfo.socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close info socket!");
            }
            mInfo = null;
        }
        setState(STATE_NONE);
    }

    public synchronized void sendMessage(Message msg) throws IOException {
        if (getState() != STATE_CONNECTED) {
            Log.e(TAG, "Tried to write to closed socket");
        }
        else if (mConnection != null) {
            mConnection.sendMessage(msg);
        } else {
            Log.e(TAG, "State is connected but connection is null.");
        }
    }

    public BluetoothDevice getConnectedDevice() {
        if (mInfo != null) {
            return mInfo.device;
        }
        return null;
    }

    public BluetoothDevice getIntendedDevice() {
        return mDevice;
    }

    public boolean isIntendedDevice(BluetoothDevice device) {
        return mDevice.getAddress().equals(device.getAddress());
    }

    private final Handler mPendingConnectionHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case PendingBluetoothConnection.MSG_CONNECTED:
                    mInfo = (Info) msg.obj;
                    Log.d(TAG, "Connection established. Is intended device? " + isIntendedDevice(mInfo.device));

                    if (mConnection == null) {
                        String name = mInfo.device.getName();
                        byte to = Byte.valueOf(name.substring(name.length() - 1));
                        mConnection = new RemoteConnection(to, mInfo.inputStream, mInfo.outputStream, mRemoteConnectionHandler);
                        setState(STATE_CONNECTED);
                        mHandler.obtainMessage(MSG_CONNECTED, name).sendToTarget();
                    } else {
                        Log.e(TAG, "Connection already exists!");
                    }
                    break;

                case PendingBluetoothConnection.MSG_COMPLETED:
                    if (!mPendingConnection.isConnected()) {
                        mPendingConnection.cancel();
                        setState(STATE_NONE);
                        mHandler.obtainMessage(MSG_CONNECTION_FAILED, mDevice.getName()).sendToTarget();
                    }
                    mPendingConnection = null;
                    break;
            }
        }
    };

    private final Handler mRemoteConnectionHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            int to, sentMsgIndex, receivedMsgIndex;
            String message;
            switch (msg.what) {
                case RemoteConnection.MSG_SENT:
                    to = msg.arg1;
                    sentMsgIndex = msg.arg2;
                    message = (String) msg.obj;
                    mHandler.obtainMessage(MSG_SENT, to, sentMsgIndex, message).sendToTarget();
                    break;

                case RemoteConnection.MSG_RECEIVED:
                    to = msg.arg1;
                    receivedMsgIndex = msg.arg2;
                    message = (String) msg.obj;
                    mHandler.obtainMessage(MSG_RECEIVED, to, receivedMsgIndex, message).sendToTarget();
                    break;

                case RemoteConnection.MSG_CONFIRMED:
                    to = msg.arg1;
                    sentMsgIndex = msg.arg2;
                    DataPacket dp = (DataPacket) msg.obj;
                    mHandler.obtainMessage(MSG_CONFIRMED, to, sentMsgIndex, dp).sendToTarget();
                    break;

                case RemoteConnection.MSG_DISCONNECTED:
                    to = msg.arg1;
                    setState(STATE_NONE);
                    mHandler.obtainMessage(MSG_DISCONNECTED, to).sendToTarget();
                    break;
            }
        }
    };

    public static class Info {
        public final BluetoothSocket socket;
        public final InputStream inputStream;
        public final OutputStream outputStream;
        public final BluetoothDevice device;
        public final UUID uuid;
        public final boolean isServer;
        public Info(BluetoothSocket socket, UUID uuid, boolean isServer) {
            this.socket = socket;
            this.uuid = uuid;
            this.isServer = isServer;
            device = socket.getRemoteDevice();

            InputStream in = null;
            OutputStream out = null;
            try {
                in = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error getting input stream.");
            }
            try {
                out = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error getting output stream.");
            }
            inputStream = in;
            outputStream = out;
        }
    }
}
