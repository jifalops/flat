package com.essentiallocalization.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;

import com.essentiallocalization.BluetoothActivity;
import com.essentiallocalization.util.Jog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Jake on 9/2/13.
 */
public class BluetoothService2 extends PersistentIntentService {
    public static final int BUFFER_SIZE = 1024;
    private static final String SERVICE_NAME = "EssentialLocalizationBluetoothService";

    // Bluetooth can support at most 7 simultaneous devices
    private static final String[] UUIDS = {
            "0aa67214-5217-4ded-b656-5cccff9a237c",
            "709bd128-8f45-45d6-aea7-cb1fb7303ea5",
            "4c91a1a9-fa9f-4338-8b5f-1d5f48d6b20e",
            "20d47c6a-eeda-45a4-98a6-7d4f371f1a34",
            "ffdc4e45-d4c7-4789-81c7-b4f6e03ca865",
            "1052c5cf-67db-4e5c-80e3-de3a89bbaf96",
            "30cb5e95-c22a-41d1-b609-dedf29e866cf"
    };

    private BluetoothAdapter mAdapter;

    private AcceptThread[] mAcceptThreads;
    private ConnectThread[] mConnectThreads;
    private ConnectedThread[] mConnectedThreads;
    private Handler mHandler;
    private int[] mState;
    private int mMaxConnections = UUIDS.length;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        int len = UUIDS.length;
        mAcceptThreads = new AcceptThread[len];
        mConnectThreads = new ConnectThread[len];
        mConnectedThreads = new ConnectedThread[len];

        mState = new int[UUIDS.length];
        for (int i = 0; i < mMaxConnections; ++i) {
            setState(i, STATE_NONE);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO listen to bt broadcasts
    }

    private synchronized void setState(int index, int state) {
        mState[index] = state;
        mHandler.obtainMessage(BluetoothActivity.MESSAGE_STATE_CHANGE, index, state).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState(int index) {
        return mState[index];
    }

    public synchronized void setMaxConnections(int connections) {
        stop();
        if (connections > UUIDS.length) connections = UUIDS.length;
        mMaxConnections = connections;
    }

    public synchronized void start() {
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
        int count = 0;
        for (BluetoothDevice d : devices) {
            if (count >= mMaxConnections) break;
            connect(count, d);
            ++count;
        }
    }

    public synchronized void stop() {
        for (int i = 0; i < mMaxConnections; ++i) {
            if (mConnectThreads[i] != null) {
                mConnectThreads[i].cancel(); mConnectThreads[i] = null;}

            if (mConnectedThreads[i] != null) {
                mConnectedThreads[i].cancel(); mConnectedThreads[i] = null;}

            if (mAcceptThreads[i] != null) {
                mAcceptThreads[i].cancel(); mAcceptThreads[i] = null;}

            setState(i, STATE_NONE);
        }
    }

    public void write(int index, byte[] data) {
        synchronized (this) {
            if (mState[index] != STATE_CONNECTED) {
                Jog.d("Tried to write to closed socket: " + index, this);
                return;
            }
        }
        mConnectedThreads[index].write(data);
    }


    public synchronized void connect(int index, BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (mState[index] == STATE_CONNECTING) {
            if (mConnectThreads[index] != null) {mConnectThreads[index].cancel(); mConnectThreads[index] = null;}
            Jog.d("ConnectThread " + index + " was connecting", this);
        }

        // Cancel any thread currently running a connection
        if (mConnectedThreads[index] != null) {
            mConnectedThreads[index].cancel();
            mConnectThreads[index] = null;
            Jog.d("ConnectedThread " + index + " is being replaced", this);
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
        mConnectedThreads[index] = new ConnectedThread(socket, index);
        mConnectedThreads[index].start();


        setState(index, STATE_CONNECTED);

        write(index, "Test data".getBytes());
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
                Jog.d("BT error getting server socket " + mmIndex, e, BluetoothService2.this);
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    // blocks thread
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Jog.d("BT error while waiting to accept connection " + mmIndex, e, BluetoothService2.this);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothService2.this) {
                        switch (mState[mmIndex]) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                manageConnectedSocket(socket, mmIndex);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                Jog.d("Either not ready or already connected: " + mmIndex, BluetoothService2.this);
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Jog.d("Could not close unwanted socket " + mmIndex, e, BluetoothService2.this);
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
                Jog.d("BT unable to cancel server socket " + mmIndex, e, BluetoothService2.this);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        //private final BluetoothDevice mmDevice;
        private final int mmIndex;

        public ConnectThread(BluetoothDevice device, int index) {
            mmIndex = index;
            BluetoothSocket tmp = null;
            //mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(UUIDS[mmIndex]));
            } catch (IOException e) {
                Jog.d("BT error getting client socket " + mmIndex, e, BluetoothService2.this);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException e) {
                Jog.d("BT client socket connection error " + mmIndex, e, BluetoothService2.this);
                cancel();
                return;
            }

            synchronized (BluetoothService2.this) {
                mConnectThreads[mmIndex] = null;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket, mmIndex);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Jog.d("BT unable to cancel client socket.", e, BluetoothService2.this);
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int mmIndex;

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
                Jog.d("BT error getting I/O streams for " + mmIndex, e, BluetoothService2.this);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Jog.i(mmIndex + " Received " + bytes + " bytes: " + new String(buffer), BluetoothService2.this);
                } catch (IOException e) {
                    Jog.d("BT error reading input stream (disconnected) " + mmIndex, e, BluetoothService2.this);
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Jog.d("BT error writing to output stream " + mmIndex, e, BluetoothService2.this);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Jog.d("BT error closing connection socket " + mmIndex, e, BluetoothService2.this);
            }
        }
    }
}
