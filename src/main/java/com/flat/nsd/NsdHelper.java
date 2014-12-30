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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NsdHelper {
    private static final String TAG = NsdHelper.class.getSimpleName();
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String SERVICE_PREFIX = "flatloco_";

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;

    private final List<NsdManager.RegistrationListener> mRegistrationListeners = Collections.synchronizedList(new ArrayList<NsdManager.RegistrationListener>());

    private String mServiceName;
    private Handler mUpdateHandler;
    private String mIp;

    Map<NsdServiceInfo, ChatConnection> mConnections = Collections.synchronizedMap(new LinkedHashMap<NsdServiceInfo, ChatConnection>());
    ChatConnection mAdvertisingConnection;



    public NsdHelper(Context context, Handler handler) {
        mContext = context;
        mUpdateHandler = handler;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mIp = Util.getWifiIp(context);
        if (mIp == null) mIp = "0.0.0.0";
        mServiceName = SERVICE_PREFIX + mIp;

    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        //createRegistrationListener();

        //mNsdManager.init(mContext.getMainLooper(), this);
        initializeAdvertisingConnection();
    }

    void initializeAdvertisingConnection() {
        if (mAdvertisingConnection == null) {
            mAdvertisingConnection = new ChatConnection(this, mUpdateHandler);
            Log.i(TAG, "Created advertising connection " + mAdvertisingConnection.toString());
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
                Log.d(TAG, "Service discovery started, I am " + mServiceName);
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

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.e(TAG, "Same service name. Connection aborted.");
                    return;
                }

                connect(serviceInfo);
            }
        };
    }

    public void retryConnections() {
        for (NsdServiceInfo service : mConnections.keySet()) {
            Log.d(TAG, "Retrying connection to " + getServiceString(service));
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
            conn = new ChatConnection(this, mUpdateHandler);
        }

        if (mConnections.containsValue(conn)) {
            Log.d(TAG, "Connection already in list, " + getServiceString(service));
        } else {
            Log.i(TAG, "Connecting to " + getServiceString(service));
            // TODO causing failed connections?
            conn.connectToServer(service.getHost(), service.getPort());
            mConnections.put(service, conn);
        }
    }

    public NsdManager.RegistrationListener createRegistrationListener() {

        NsdManager.RegistrationListener listener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                mServiceName = nsdServiceInfo.getServiceName();
                Log.d(TAG, getServiceString(nsdServiceInfo) + " onServiceRegistered()");
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
        mRegistrationListeners.add(listener);
        return listener;
    }

    public void registerService(int port) {
        Log.e(TAG, "Registering service at " + mIp + ":" + port); // Log.e is red
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        NsdManager.RegistrationListener listener = createRegistrationListener();
        try {
            mNsdManager.registerService(
                    serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to register service, " + e.getMessage() + ". Retrying...");
            mRegistrationListeners.remove(listener);
            try {
                listener = createRegistrationListener();
                mNsdManager.registerService(
                        serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener);
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Failed to register service, " + e2.getMessage());
            }
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
        for (NsdManager.RegistrationListener rl : mRegistrationListeners) {
            mNsdManager.unregisterService(rl);
        }
    }

    public static String getServiceString(NsdServiceInfo service) {
        try {
            return service.getHost().getHostAddress() + ":" + service.getPort();
        } catch (NullPointerException ignored) {}
        return null;
    }
}
