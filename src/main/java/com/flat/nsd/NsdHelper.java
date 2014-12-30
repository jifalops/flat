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
import android.os.Handler;
import android.util.Log;

import com.flat.localization.util.Util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NsdHelper {
    private static final String TAG = NsdHelper.class.getSimpleName();
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String SERVICE_PREFIX = "flatloco_";

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;

    private String mServiceName;
    private Handler mUpdateHandler;

    Map<NsdServiceInfo, ChatConnection> mConnections = Collections.synchronizedMap(new LinkedHashMap<NsdServiceInfo, ChatConnection>());
    ChatConnection mAdvertisingConnection;



    public NsdHelper(Context context, Handler handler) {
        mContext = context;
        mUpdateHandler = handler;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        String ip = Util.getWifiIp(context);
        if (ip == null) ip = "0.0.0.0";
        mServiceName = SERVICE_PREFIX + ip;

    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();

        //mNsdManager.init(mContext.getMainLooper(), this);
        initializeAdvertisingConnection();
    }

    private void initializeAdvertisingConnection() {
        if (mAdvertisingConnection == null) {
            Log.d(TAG, "initializing null advertising service");
            mAdvertisingConnection = new ChatConnection(mUpdateHandler);
        }
        if(mAdvertisingConnection.getLocalPort() > -1) {
            registerService(mAdvertisingConnection.getLocalPort());
        } else {
            Log.d(TAG, "no port to connect to (ServerSocket isn't bound). Retrying...");
            try {
                Thread.sleep(17); // 1000/60 frames
            } catch (InterruptedException e) {

            }
            initializeAdvertisingConnection();
        }
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
                Log.e(TAG, "Service discovery started, I am " + mServiceName); // Log.e is red
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().startsWith(SERVICE_PREFIX)){
                    resolveService(service);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost " + service);
                ChatConnection conn = mConnections.remove(service);
                if (conn != null) {
                    conn.tearDown();
                    if (conn == mAdvertisingConnection) {
                        initializeAdvertisingConnection();
                    }
                }
            }
            
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);        
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
        Log.d(TAG, "Resolving service...");
        try {
            mNsdManager.resolveService(service, mResolveListener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to resolve service " + service.getServiceName() + ", " + e.getMessage() + ". Retrying...");
            initializeResolveListener();
            try {
                mNsdManager.resolveService(service, mResolveListener);
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Failed to resolve service " + service.getServiceName() + ", " + e2.getMessage());
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
                Log.i(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.wtf(TAG, "Same service name.");
                    return;
                }

                connect(serviceInfo);
            }
        };
    }

    public void retryConnections() {
        for (NsdServiceInfo service : mConnections.keySet()) {
            Log.d(TAG, "Retrying connection to " + service.getHost().getHostAddress() + ":" + service.getPort());
            mConnections.get(service).connectToServer(service.getHost(), service.getPort());
        }
    }

    private void connect(NsdServiceInfo service) {
        if (service == null) {
            Log.w(TAG, "Resolved null serviceInfo.");
            return;
        }

        ChatConnection conn;
        if (service.getPort() == mAdvertisingConnection.getLocalPort()) {
            conn = mAdvertisingConnection;
        } else {
            conn = new ChatConnection(mUpdateHandler);
        }

        if (mConnections.containsValue(conn)) {
            Log.d(TAG, "Connection already in list, port " + conn.getLocalPort());
        } else {
            Log.i(TAG, "Connecting to " + service.getServiceName() + " port " + service.getPort());
            // TODO causing failed connections?
            conn.connectToServer(service.getHost(), service.getPort());
            mConnections.put(service, conn);
        }
    }

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
            }
            
            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e(TAG, "Registration failed: " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                Log.i(TAG, "Service unregistered");
            }
            
            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e(TAG, "Unregistration failed: " + errorCode);
            }
            
        };
    }

    public void registerService(int port) {
        Log.i(TAG, "Registering service on port " + port);
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        try {
            mNsdManager.registerService(
                    serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to register service, " + e.getMessage());
            Log.i(TAG, "Reinitializing registration listener...");
            initializeRegistrationListener();
        }
    }

    public void discoverServices() {
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
    
    public void tearDown() {
        for (ChatConnection conn : mConnections.values()) {
            conn.tearDown();
        }
        mAdvertisingConnection.tearDown();
        mNsdManager.unregisterService(mRegistrationListener);
    }
}
