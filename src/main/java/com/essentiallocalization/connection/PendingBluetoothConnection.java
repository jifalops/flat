package com.essentiallocalization.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jake on 9/15/13.
 */
public class PendingBluetoothConnection {
    private static final String TAG = PendingBluetoothConnection.class.getSimpleName();

    public static final int MSG_CONNECTED = 1;
    public static final int MSG_COMPLETED = 2;

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

    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;
    private boolean mConnected;
    private boolean mServerCompleted;
    private boolean mClientCompleted;
    private final ServerThread mServerThread;
    private final ClientThread mClientThread;

    /** If BluetoothDevice is null, this will only accept connections as a server. */
    public PendingBluetoothConnection(Handler handler, BluetoothDevice device) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        mServerThread = new ServerThread();
        mClientThread = new ClientThread(device);
    }

    public synchronized void start() {
        mServerThread.start();
        mClientThread.start();
    }

    public synchronized void cancel() {
        mServerThread.cancel();
        mClientThread.cancel();
    }

    public synchronized boolean isConnected() {
        return mConnected;
    }

    private synchronized void setConnected(boolean connected) {
        mConnected = connected;
    }

    public synchronized boolean isCompleted() {
        return mServerCompleted && mClientCompleted;
    }

    private synchronized void setServerCompleted(boolean completed) {
        mServerCompleted = completed;
        if (isCompleted()) {
            mHandler.obtainMessage(MSG_COMPLETED).sendToTarget();
        }
    }

    private synchronized void setClientCompleted(boolean completed) {
        mClientCompleted = completed;
        if (isCompleted()) {
            mHandler.obtainMessage(MSG_COMPLETED).sendToTarget();
        }
    }


    private class ServerThread extends  Thread {
        private static final String SERVICE_NAME = "EssentialLocalizationBluetoothService";

        private BluetoothServerSocket mmServerSocket;

        public ServerThread() {
            mmServerSocket = null;
        }

        @Override
        public void run() {
            BluetoothSocket socket;

            UUID uuid;
            for (int i = 0; i < MAX_CONNECTIONS; i++) {
                if (isConnected()) break;

                uuid = UUID.fromString(UUIDS[i]);

                try {
                    mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
                            SERVICE_NAME, uuid);
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't create server socket " + i);
                    continue;
                }

                if (mmServerSocket != null) {
                    try {
                        socket = mmServerSocket.accept(); // Blocks
                        cancel();
                    } catch (IOException e) {
                        Log.w(TAG, "Server " + i + " interrupted.");
                        break;
                    }

                    if (socket != null) {
                        setConnected(true);
                        mClientThread.cancel();
                        mHandler.obtainMessage(MSG_CONNECTED, new BluetoothConnection.Info(socket, uuid, true)).sendToTarget();
                    }
                }
            }
            setServerCompleted(true);
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close server socket!");
            }
        }
    }



    private class ClientThread extends Thread {
        private static final int MAX_ATTEMPTS = 3;
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ClientThread(BluetoothDevice device) {
            mmDevice = device;
        }

        public void run() {
            if (mmDevice != null) {
                for (int i = 0; i < MAX_CONNECTIONS; i++) {
                    if (isConnected()) break;
                    tryConnection(i);
                }
            }
            setClientCompleted(true);
        }

        private void tryConnection(int i) {
            UUID uuid = UUID.fromString(UUIDS[i]);
            for (int j = 0; j < MAX_ATTEMPTS; j++) {
                if (isConnected()) break;

                try {
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't create client socket " + i + ", attempt " + j);
                }

                if (mmSocket != null) {
                    try {
                        mmSocket.connect(); // Blocks
                    } catch (IOException e) {
                        Log.w(TAG, "Client " + i + " attempt " + j + " interrupted");
                        cancel();
                        continue;
                    }

                    setConnected(true);
                    mServerThread.cancel();
                    mHandler.obtainMessage(MSG_CONNECTED, new BluetoothConnection.Info(mmSocket, uuid, false)).sendToTarget();
                    break;
                } else {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Client " + i + " attempt " + j + " unable to sleep");
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close client socket!");
            }
        }
    }
}
