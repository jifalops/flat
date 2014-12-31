/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flat.nsd;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.flat.localization.util.Util;
import com.flat.nsd.sockets.MyConnectionSocket;
import com.flat.nsd.sockets.MyServerSocket;
import com.flat.nsd.sockets.MySocketManager;
import com.flat.nsd.sockets.Sockets;

import java.net.ServerSocket;
import java.net.Socket;

public class NsdHelper {
    private static final String TAG = NsdHelper.class.getSimpleName();
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String SERVICE_PREFIX = "flatloco_";

    public static final int HOST_CONNECTION_LIMIT = 1;

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;

    private String mServiceName;
    private String mIp;

    private MySocketManager socketManager;

    private final MySocketManager.SocketListener socketListener = new MySocketManager.SocketListener() {
        @Override
        public void onServerConnected(MyServerSocket mss, Socket socket) {
            Log.i(TAG, "Server connected to " + Sockets.toString(socket));
        }

        @Override
        public void onServerFinished(MyServerSocket mss) {
            Log.v(TAG, "onServerFinished " + Sockets.toString(mss.getServerSocket()));
        }

        @Override
        public void onNewServerSocket(MyServerSocket mss, ServerSocket ss) {
            registerService(ss.getLocalPort());
            Log.v(TAG, "onNewServerSocket " + Sockets.toString(ss));
        }

        @Override
        public void onMessageSent(MyConnectionSocket mcs, String msg) {
            Log.v(TAG, "onMessageSent " + Sockets.toString(mcs.getSocket()));
        }

        @Override
        public void onMessageReceived(MyConnectionSocket mcs, String msg) {
            Log.v(TAG, "onMessageReceived " + Sockets.toString(mcs.getSocket()));
        }

        @Override
        public void onClientFinished(MyConnectionSocket mcs) {
            Log.v(TAG, "onClientFinished " + Sockets.toString(mcs.getSocket()));
        }

        @Override
        public void onClientConnected(MyConnectionSocket mcs, Socket socket) {
            Log.i(TAG, "Client connected to " + Sockets.toString(socket));
        }
    };


    public NsdHelper(Context context) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mIp = Util.getWifiIp(context);
        if (mIp == null) mIp = "0.0.0.0";
        mServiceName = SERVICE_PREFIX + mIp;

        socketManager = MySocketManager.getInstance();
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();

        //mNsdManager.init(mContext.getMainLooper(), this);
        //createServerConnection();
    }

    public void start() {
        socketManager.registerListener(socketListener);
        socketManager.start();
    }
    public void stop() {
        socketManager.stop();
        socketManager.unregisterListener(socketListener);
        mNsdManager.unregisterService(mRegistrationListener); // TODO might not be registered yet
    }



    public void initializeDiscoveryListener() {
        if (mDiscoveryListener == null) {
            Log.v(TAG, "initializing discovery listener (null)");
        } else {
            Log.v(TAG, "initializing discovery listener (not null)");
        }
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started, I am " + mServiceName);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().startsWith(SERVICE_PREFIX)
                            && socketManager.countConnectionsTo(service.getServiceName()) < HOST_CONNECTION_LIMIT) {
                    resolveService(service);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost " + service);
                // TODO remove connection in socketmanager
            }
            
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
                // TODO restart discovery if necessary
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private void resolveService(NsdServiceInfo service) {
        Log.i(TAG, "Resolving service " + getServiceString(service));
        try {
            mNsdManager.resolveService(service, mResolveListener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to resolve service, " + e.getMessage() + ". Retrying...");
            initializeResolveListener();
            try {
                mNsdManager.resolveService(service, mResolveListener);
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Failed to resolve service, " + e2.getMessage());
                initializeResolveListener();
            }
        }
    }

    public void initializeResolveListener() {
        if (mResolveListener == null) {
            Log.v(TAG, "initializing resolve listener (null)");
        } else {
            Log.v(TAG, "initializing resolve listener (not null)");
        }
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "Resolve Succeeded for " + getServiceString(serviceInfo));

                if (serviceInfo.getServiceName().contains(mIp)) {
                    Log.e(TAG, "Same service name. Connection aborted.");
                    return;
                }

                socketManager.connectTo(serviceInfo.getHost(), serviceInfo.getPort());
            }
        };
    }


//    public void retryConnections() {
//        for (Connection conn : mConnections) {
//            Log.d(TAG, "Retrying connection to " + getServiceString(conn.clientInfo));
//            conn.client.connectToServer(conn.clientInfo.getHost(), conn.clientInfo.getPort());
//        }
//    }
//
//    private void connect(NsdServiceInfo service) {
//        if (service == null) {
//            Log.w(TAG, "Resolved null serviceInfo.");
//            return;
//        }
//
//        for (Connection conn : mConnections) {
//            if (conn.clientMatches(service)) {
//                Log.d(TAG, "Connection already in list, " + getServiceString(service));
//                return;
//            }
//        }
//
//        Connection conn = new Connection();
//        conn.clientInfo = service;
//        conn.client = new ChatConnection(this, mUpdateHandler);
//
//        Log.i(TAG, "Connecting to " + getServiceString(service));
//        conn.client.connectToServer(service.getHost(), service.getPort());
//        mConnections.add(conn);
//
//    }

    public void initializeRegistrationListener() {
        if (mRegistrationListener == null) {
            Log.v(TAG, "initializing registration listener (null)");
        } else {
            Log.v(TAG, "initializing registration listener (not null)");
        }
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                mServiceName = nsdServiceInfo.getServiceName();
                Log.e(TAG, "Registered service " + nsdServiceInfo.getServiceName());
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e(TAG, "Registration failed for " + getServiceString(nsdServiceInfo) + ". Error " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                Log.i(TAG, "Service unregistered for " + getServiceString(nsdServiceInfo));
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e(TAG, "Unregistration failed for " + getServiceString(nsdServiceInfo) + ". Error " + errorCode);
            }

        };
    }


    public void registerService(int port) {
        Log.i(TAG, "Registering service at " + mIp + ":" + port); // Log.e is red
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        try {
            mNsdManager.registerService(
                    serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to register service, " + e.getMessage() + ". Retrying...");
            try {
                initializeRegistrationListener();
                mNsdManager.registerService(
                        serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Failed to register service, " + e2.getMessage());
            }
        }
    }

    public void discoverServices() { //TODO this doesn't normally fail and probably doesnt need to retry
        // This is a work-around for the "listener already in use" error.
        // It seems discoverServices() needs a new DiscoveryListener each call.
        try {
            //initializeDiscoveryListener();
            mNsdManager.discoverServices(
                    SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to discover services, " + e.getMessage() + ". Retrying...");
            initializeDiscoveryListener();
            try {
                mNsdManager.discoverServices(
                        SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Failed to discover services, " + e2.getMessage());
            }
        }

    }
    
    public void stopDiscovery() {
        try {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Exception while stopping discovery, " + e.getMessage());
        }
    }


    public static String getServiceString(NsdServiceInfo service) {
        try {
            return service.getHost().getHostAddress() + ":" + service.getPort();
        } catch (NullPointerException ignored) {}
        return null;
    }
}
