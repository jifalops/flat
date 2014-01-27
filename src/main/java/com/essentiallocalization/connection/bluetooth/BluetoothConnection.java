package com.essentiallocalization.connection.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.essentiallocalization.connection.ConnectionFilter;
import com.essentiallocalization.connection.PacketManager;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jake on 9/20/13.
 */
public final class BluetoothConnection implements ConnectionFilter {
    private static final String TAG = BluetoothConnection.class.getSimpleName();

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static String getState(int state) {
        switch (state) {
            case STATE_NONE:        return "None";
            case STATE_CONNECTING:  return "Connecting";
            case STATE_CONNECTED:   return "Connected";
        }
        return "Unknown";
    }

    public static interface Listener {
        void onStateChange(BluetoothConnection connection, int state, int previousState);
        void onPacketReceived(BluetoothConnection connection, int packetIndex);
        void onMessageReceived(BluetoothConnection connection, int msgIndex);
        void onPacketSent(BluetoothConnection connection, int packetIndex);
        void onMessageSent(BluetoothConnection connection, int msgIndex);
        void onPacketConfirmed(BluetoothConnection connection, int packetIndex);
        void onMessageConfirmed(BluetoothConnection connection, int msgIndex);
    }

    private BluetoothServer mServer;
    private BluetoothClient mClient;
    private PacketManager mConnection;
    private BluetoothSocket mSocket;
    private int mState;
    private String mAddress;
    private String mName;
    private final byte mSrc;
    private byte mDest;
    private BluetoothDevice mTargetDevice;
    private volatile boolean mIsServer;
    private UUID mUuid;
    private final Listener mListener;
    private volatile boolean mIsConnected;
    private final ConnectionFilter mFilter;
    private final ServerHandler mServerHandler;
    private final ClientHandler mClientHandler;
    private final ConnectionHandler mConnectionHandler;
    private volatile boolean mCancelled;

    BluetoothConnection(ConnectionFilter filter, Listener listener, BluetoothDevice target, Looper looper) {
        mConnectionHandler = new ConnectionHandler(looper);
        mServerHandler = new ServerHandler(looper);
        mClientHandler = new ClientHandler(looper);

        mFilter = filter;
        mListener = listener;
        mTargetDevice = target;
        mState = -1;
        mAddress = "";

        String me = BluetoothAdapter.getDefaultAdapter().getName();
        mSrc = Byte.valueOf(me.substring(me.length() - 1));

        setState(STATE_NONE);
    }

    public synchronized PacketManager getConnection() {
        return mConnection;
    }

    public synchronized boolean isServer() {
        return mIsServer;
    }

    public synchronized UUID getUuid() {
        return mUuid;
    }

    public synchronized String getAddress() {
        return mAddress;
    }

    public synchronized String getName() {
        return mName;
    }

    public synchronized byte getTo() {
        return mDest;
    }

    private synchronized void setState(int state) {
        if (state == mState) return;
        int prevState = mState;
        mState = state;
        mListener.onStateChange(this, mState, prevState);
    }

    public synchronized int getState() {
        return mState;
    }


    public synchronized void start() {
        stop();
        mCancelled = false;
        mServer = new BluetoothServer(this, mServerHandler);
        mClient = new BluetoothClient(this, mClientHandler, mTargetDevice);
        mServer.start();
        mClient.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void stop() {
        if (mServer != null) {
            mServer.cancel();
            mServerHandler.removeCallbacksAndMessages(null);
//            mServer = null;
        }
        if (mClient != null) {
            mClient.cancel();
            mClientHandler.removeCallbacksAndMessages(null);
//            mClient = null;
        }
        if (mConnection != null) {
            mConnection.close();
            mConnectionHandler.removeCallbacksAndMessages(null);
//            mConnection = null;
        }
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close info socket!");
            }
//            mSocket = null;
        }
        mAddress = "";
        mUuid = null;
        mIsConnected = false;
        mCancelled = true;
        setState(STATE_NONE);
    }

    private void manageNewConnection(BluetoothDevice device) {
        mName = device.getName();
        mDest = Byte.valueOf(mName.substring(mName.length() - 1));
        try {
            mConnection = new PacketManager(mSrc, mDest, mSocket.getInputStream(), mSocket.getOutputStream(), mConnectionHandler);
            mIsConnected = true;
            setState(STATE_CONNECTED);
        } catch (IOException e) {
            Log.e(TAG, "Unable to get I/O stream", e);
            start();
        }
    }

    @Override
    public synchronized boolean isAllowed(String address) {
        if (mAddress.length() > 0) {
            return  address.equals(mAddress);
        } else if (mFilter.isAllowed(address)) {
            mAddress = address;
            return true;
        }
        return false;
    }


    private class ServerHandler extends Handler {
        ServerHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothServer.MSG_CONNECTED:
                    mClient.cancel();
                    if (mServer != (BluetoothServer) msg.obj) {
                        Log.e(TAG, "Unexpected BT server.");
                    }
                    mIsServer = true;
                    mSocket = mServer.getSocket();
                    mUuid = mServer.getUuid();
                    manageNewConnection(mSocket.getRemoteDevice());
                    break;
                case BluetoothServer.MSG_FINISHED:
                    if (!mIsConnected && mClient.isFinished()
                            && !mCancelled) {
                        start();
                    }
                    break;
            }
        }
    };

    private class ClientHandler extends Handler {
        ClientHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothClient.MSG_CONNECTED:
                    mServer.cancel();
                    if (mClient != (BluetoothClient) msg.obj) {
                        Log.e(TAG, "Unexpected BT client.");
                    }
                    mIsServer = false;
                    mSocket = mClient.getSocket();
                    mUuid = mClient.getUuid();
                    manageNewConnection(mSocket.getRemoteDevice());
                    break;
                case BluetoothClient.MSG_FINISHED:
                    if (mServer.isFinished() && !mIsConnected
                            && !mCancelled) {
                        start();
                    }
                    break;
            }
        }
    };

    private class ConnectionHandler extends Handler {
        ConnectionHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PacketManager.MSG_SENT_PACKET:
                    mListener.onPacketSent(BluetoothConnection.this, msg.arg2);
                    break;
                case PacketManager.MSG_SENT_MSG:
                    mListener.onMessageSent(BluetoothConnection.this, msg.arg2);
                    break;
                case PacketManager.MSG_RECEIVED_PACKET:
                    mListener.onPacketReceived(BluetoothConnection.this, msg.arg2);
                    break;
                case PacketManager.MSG_RECEIVED_MSG:
                    mListener.onMessageReceived(BluetoothConnection.this, msg.arg2);
                    break;
                case PacketManager.MSG_CONFIRMED_PACKET:
                    mListener.onPacketConfirmed(BluetoothConnection.this, msg.arg2);
                    break;
                case PacketManager.MSG_CONFIRMED_MSG:
                    mListener.onMessageConfirmed(BluetoothConnection.this, msg.arg2);
                    break;
                case PacketManager.MSG_DISCONNECTED:
                    mIsConnected = false;
                    setState(STATE_NONE);
                    if (!mCancelled) {
                        start();
                    }
                    break;
            }
        }
    };
}
