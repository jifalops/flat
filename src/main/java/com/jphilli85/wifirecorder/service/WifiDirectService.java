package com.jphilli85.wifirecorder.service;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jake on 8/28/13.
 */
public final class WifiDirectService extends PersistentIntentService {
    private static final String LOG_TAG = WifiDirectService.class.getSimpleName();
    public static final int SERVER_PORT = 8888;
    public static final int BUFFER_SIZE = 1024;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager.PeerListListener mPeerListListener;
    private WifiP2pManager.ActionListener mDiscoveryListener;
    private WifiP2pManager.ActionListener mConnectListener;

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, mServiceLooper, null);

        mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        createListeners();
    }

    public void discoverPeers() {
        mManager.discoverPeers(mChannel, mDiscoveryListener);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct is enabled
            } else {
                Toast.makeText(this, "Wi-Fi Direct is not enabled", Toast.LENGTH_SHORT).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            mManager.requestPeers(mChannel, mPeerListListener);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }

    private void createListeners() {

        mDiscoveryListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "discovery process succeeded");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e(LOG_TAG, "discovery process failed: " + reasonCode);
            }
        };


        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                    Log.d(LOG_TAG,
                            "addr: " + device.deviceAddress +
                            "\nname: " + device.deviceName);
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = device.deviceAddress;
                    mManager.connect(mChannel, config, mConnectListener);
                }
            }
        };


        mConnectListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WifiDirectService.this, "Connection successful", Toast.LENGTH_SHORT).show();
                // TODO now what?
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "connection failed: " + reason);
            }
        };
    }

    // TODO EXPERIMENTAL -------------------------------------------------------

    public static class ServerTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            ServerSocket serverSocket = null;
            Socket client = null;
            try {
                /**
                 * Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */
                serverSocket = new ServerSocket(SERVER_PORT);
                client = serverSocket.accept();

                byte[] buffer = new byte[BUFFER_SIZE];
                InputStream inputstream = client.getInputStream();
                inputstream.read(buffer);

                Log.d(LOG_TAG, "Buffer contents:\n" + buffer.toString());

            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed during communication", e);
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Failed closing server socket", e);
                    }
                }
            }
            return null;
        }
    }

    public static class ClientTask extends AsyncTask<Void, Void, Void> {
        Socket socket = new Socket();
        byte[] buffer = new byte[BUFFER_SIZE];

        @Override
        protected Void doInBackground(Void... params) {
            ServerSocket serverSocket = null;
            Socket client = null;
            try {
                /**
                 * Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */
                serverSocket = new ServerSocket(SERVER_PORT);
                client = serverSocket.accept();

                byte[] buffer = new byte[BUFFER_SIZE];
                InputStream inputstream = client.getInputStream();
                inputstream.read(buffer);
// TODO now what?
                Log.d(LOG_TAG, "Buffer contents:\n" + buffer.toString());

            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed during communication", e);
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Failed closing server socket", e);
                    }
                }
            }
            return null;
        }
    }
}
