package com.essentiallocalization.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.essentiallocalization.BluetoothFragment;
import com.essentiallocalization.connection.RemoteConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Jake on 9/2/13.
 */
public class BluetoothService extends PersistentIntentService {
    public static final int BUFFER_SIZE = 1024;
    private static final String SERVICE_NAME = "EssentialLocalizationBluetoothService";

    public static final int SPEED_OF_LIGHT = 299792458; // m/s

    // Current BT piconets are at most 7 peers
    private static final String[] UUIDS = {
            "0aa67214-5217-4ded-b656-5cccff9a237c",
            "709bd128-8f45-45d6-aea7-cb1fb7303ea5",
            "4c91a1a9-fa9f-4338-8b5f-1d5f48d6b20e",
            "20d47c6a-eeda-45a4-98a6-7d4f371f1a34",
            "ffdc4e45-d4c7-4789-81c7-b4f6e03ca865",
            "1052c5cf-67db-4e5c-80e3-de3a89bbaf96",
            "30cb5e95-c22a-41d1-b609-dedf29e866cf"
    };
    public static final int MAX_CONNECTIONS = UUIDS.length;

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final String mName = mAdapter.getName();
    private final String[] mConnectedDevices = new String[MAX_CONNECTIONS];
    private final String TAG = "BluetoothService " + mName;

    private AcceptThread[] mAcceptThreads;
    private ConnectThread[] mConnectThreads;
    private ConnectedThread[] mConnectedThreads;
    private Handler mHandler = new Handler();
    private int[] mState;
    private int mMaxConnections = MAX_CONNECTIONS;
    private boolean mRunning;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public void setHandler(Handler handler) {
        synchronized (mHandler) {
            mHandler = handler;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate()");

        mAcceptThreads = new AcceptThread[MAX_CONNECTIONS];
        mConnectThreads = new ConnectThread[MAX_CONNECTIONS];
        mConnectedThreads = new ConnectedThread[MAX_CONNECTIONS];

        mState = new int[MAX_CONNECTIONS];
        for (int i = 0; i < MAX_CONNECTIONS; ++i) {
            setState(i, STATE_NONE);
        }
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

    private synchronized void setState(int index, int state) {
        mState[index] = state;
        mHandler.obtainMessage(BluetoothFragment.MESSAGE_STATE_CHANGE, index, state).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState(int index) {
        return mState[index];
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
        for (int i = 0; i < mMaxConnections; ++i) {
            setState(i, STATE_LISTEN);

            // Start the threads to listen on a BluetoothServerSocket
            if (mAcceptThreads[i] == null) {
                mAcceptThreads[i] = new AcceptThread(i);
                mAcceptThreads[i].start();
            }
        }

        Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
        if (devices != null) {
            int count = 0;
            for (BluetoothDevice d : devices) {
                if (count >= mMaxConnections) break;
                connect(count, d);
                ++count;
            }
        }
        mRunning = true;
    }

    public synchronized void stop() {
        Log.v(TAG, "stop()");
        for (int i = 0; i < mMaxConnections; ++i) {
            if (mConnectThreads[i] != null) {
                mConnectThreads[i].cancel(); mConnectThreads[i] = null;}

            if (mConnectedThreads[i] != null) {
                mConnectedThreads[i].cancel(); mConnectedThreads[i] = null;}

            if (mAcceptThreads[i] != null) {
                mAcceptThreads[i].cancel(); mAcceptThreads[i] = null;}

            setState(i, STATE_NONE);
        }
        mRunning = false;
    }

    public synchronized boolean isRunning() {
        return mRunning;
    }

    public void write(int index, byte[] buffer) {
        synchronized (this) {
            if (mState[index] != STATE_CONNECTED) {
                Log.d(TAG, "Tried to write to closed socket: " + index);
                return;
            }
        }
        mConnectedThreads[index].write(buffer);
    }


    private synchronized void connect(int index, BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (mState[index] == STATE_CONNECTING) {
            if (mConnectThreads[index] != null) {mConnectThreads[index].cancel(); mConnectThreads[index] = null;}
            Log.d(TAG, "ConnectThread " + index + " was connecting");
        }

        // Cancel any thread currently running a connection
        if (mConnectedThreads[index] != null) {
            mConnectedThreads[index].cancel();
            mConnectThreads[index] = null;
            Log.d(TAG, "ConnectedThread " + index + " is being replaced");
        }
        // Start the thread to connect with the given device
        mConnectThreads[index] = new ConnectThread(device, index);
        mConnectThreads[index].start();
        setState(index, STATE_CONNECTING);
    }

    private void manageConnectedSocket(BluetoothSocket socket, int index) {
        // Cancel the thread that completed the connection
        if (mConnectThreads[index] != null) {
            mConnectThreads[index].cancel(); mConnectThreads[index] = null;}

        // Start the thread to manage the connection and perform transmissions
        if (mConnectedThreads[index] != null) {
            mConnectedThreads[index].cancel();
            mConnectedThreads[index] = null;
        }
        mConnectedThreads[index] = new ConnectedThread(socket, index);
        mConnectedThreads[index].start();

        mConnectedDevices[index] = socket.getRemoteDevice().getName();

//        mHandler.obtainMessage(BluetoothFragment.MESSAGE_DEVICE_NAME,
//                index, -1, mConnectedDevices[index]).sendToTarget();


        setState(index, STATE_CONNECTED);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private final int mmIndex;

        public AcceptThread(int index) {
            mmIndex = index;
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(
                        SERVICE_NAME, UUID.fromString(UUIDS[mmIndex]));
            } catch (IOException e) {
                Log.e(TAG, "BT error getting server socket " + mmIndex, e);
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    // blocks thread
                    socket = mmServerSocket.accept();
                    cancel();
                } catch (IOException e) {
                    Log.e(TAG, "BT error while waiting to accept connection " + mmIndex, e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState[mmIndex]) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                manageConnectedSocket(socket, mmIndex);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                Log.d(TAG, "Either not ready or already connected: " + mmIndex);
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket " + mmIndex, e);
                                }
                                break;
                        }
                    }
                }


            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "BT unable to cancel server socket " + mmIndex, e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket[] mmSockets;
        private final BluetoothDevice mmDevice;
        private final int mmIndex;
        private int mmConnectedSocket;
//        private BluetoothSocket mmSocket0;
//        private final BluetoothSocket mmSocket1;
//        private final BluetoothSocket mmSocket2;
//        private final BluetoothSocket mmSocket3;
//        private final BluetoothSocket mmSocket4;
//        private final BluetoothSocket mmSocket5;
//        private final BluetoothSocket mmSocket6;

        public ConnectThread(BluetoothDevice device, int index) {
            mmDevice = device;
            mmIndex = index;

//            BluetoothSocket[] tmp = new BluetoothSocket[MAX_CONNECTIONS];
            mmSockets = new BluetoothSocket[mMaxConnections];
            for (int i = 0; i < mMaxConnections; ++i) {
                try {
                    mmSockets[i] = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(UUIDS[i]));
                } catch (IOException e) {
                    Log.e(TAG, "Error creating client socket " + i + ", thread " + mmIndex);
                }
            }

//            BluetoothSocket tmp = null;
//            try {
//                tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(UUIDS[mmIndex]));
//            } catch (IOException e) {
//                Log.e(TAG, "Error creating client socket " + mmIndex);
//            }
//            mmSocket0 = tmp;

//            mmSocket0 = tmp[0];
//            mmSocket1 = tmp[1];
//            mmSocket2 = tmp[2];
//            mmSocket3 = tmp[3];
//            mmSocket4 = tmp[4];
//            mmSocket5 = tmp[5];
//            mmSocket6 = tmp[6];
//
//            mmSockets = new BluetoothSocket[] { mmSocket0, mmSocket1, mmSocket2, mmSocket3, mmSocket4, mmSocket5, mmSocket6 };
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            //mAdapter.cancelDiscovery();

            for (int i = 0; i < mMaxConnections; ++i) {
                if (mmSockets[i] != null) {
                    try {
                        mmSockets[i].connect();
                    } catch (IOException e) {
                        Log.w(TAG, "BT client socket connection error " + mmIndex, e);
                        cancel(i);
                        continue;
                    }

                    if (mmSockets[i] == null) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Thread interrupted during sleep", e);
                        }
                    } else {

                        synchronized (BluetoothService.this) {
                            mConnectThreads[mmIndex] = null;
                        }

                        manageConnectedSocket(mmSockets[i], mmIndex);
                        return;
                    }
                }
            }

//            mHandler.obtainMessage(BluetoothFragment.MESSAGE_CONNECTION_FAILED, mmIndex).sendToTarget();
            //retry(mmIndex);
        }

        public void cancel() {
//            try {
//                mmSocket0.close();
//            } catch (IOException e) {
//                Log.e(TAG, "BT unable to cancel client " + mmIndex, e);
//            }


            for (int i = 0; i < mMaxConnections; ++i) {
                cancel(i);
            }
        }

        private void cancel(int index) {
            try {
                mmSockets[index].close();
            } catch (IOException e) {
                Log.e(TAG, "BT unable to cancel client socket " + index + ", thread " + mmIndex, e);
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int mmIndex;

        private final RemoteConnection mRemoteConnection;

        public ConnectedThread(BluetoothSocket socket, int index) {
            mmIndex = index;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "BT error getting I/O streams for " + mmIndex, e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            byte from = Byte.valueOf(mName.substring(mName.length() - 1));
            String name = mmSocket.getRemoteDevice().getName();
            byte to = Byte.valueOf(name.substring(name.length() - 1));
            mRemoteConnection = new RemoteConnection(from, to, mmInStream, mmOutStream, mHandler);
        }

        @Override
        public void run() {
//            byte[] buffer = new byte[BUFFER_SIZE];  // buffer store for the stream
//            int bytes; // bytes returned from read()
//
//            // Keep listening to the InputStream until an exception occurs
//            while (true) {
//                try {
//                    // Read from the InputStream
//                    bytes = mmInStream.read(buffer);
//                    mHandler.obtainMessage(BluetoothFragment.MESSAGE_READ, mmIndex, bytes, buffer).sendToTarget();
//                } catch (IOException e) {
//                    Log.w(TAG, "BT error reading input stream (disconnected) " + mmIndex, e);
//                    setState(mmIndex, STATE_NONE);
//                    mHandler.obtainMessage(BluetoothFragment.MESSAGE_CONNECTION_LOST, mmIndex).sendToTarget();
//                    break;
//                }
//            }
        }

        public void write(byte[] buffer) {
            try {
                mRemoteConnection.sendTestMessage();
//                mmOutStream.write(buffer);
//                mHandler.obtainMessage(BluetoothFragment.MESSAGE_WRITE, mmIndex, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "BT error writing to output stream " + mmIndex, e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "BT error closing connection socket " + mmIndex, e);
            }
        }
    }

    private synchronized void retry(int index) {
        if (mConnectedThreads[index] != null) {
            mConnectedThreads[index].cancel();
            mConnectedThreads[index] = null;
        }

        setState(index, STATE_NONE);
        if (mAcceptThreads[index] != null) {
            mAcceptThreads[index].cancel();
            mAcceptThreads[index] = new AcceptThread(index);
        }
        if (mConnectThreads[index] != null) {
            BluetoothDevice device = mConnectThreads[index].mmDevice;
            mConnectThreads[index].cancel();
            mConnectThreads = null;
            connect(index, device);
        }
    }
}
