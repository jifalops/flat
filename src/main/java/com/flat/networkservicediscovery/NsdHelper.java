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

package com.flat.networkservicediscovery;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.flat.wifi.MyWifiManager;

import java.util.ArrayList;
import java.util.List;

public class NsdHelper {
    private static final String TAG = NsdHelper.class.getSimpleName();
    public static final String SERVICE_TYPE = "_http._tcp.";

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;

    private String mServiceName;
    private boolean registered;

    public static interface NsdServiceFilter {
        boolean isAcceptableService(NsdServiceInfo info);
    }
    private final NsdServiceFilter mFilter;

    public NsdHelper(Context context, String serviceName, NsdServiceFilter filter) {
        mContext = context;
        mFilter = filter;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mServiceName = serviceName;
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();
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
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (mFilter.isAcceptableService(service)) {
                    resolveService(service);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost " + service);
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

                if (serviceInfo.getHost().getHostAddress().equals(MyWifiManager.getIpAddress(mContext))) {
                    Log.e(TAG, "Same host. Connection aborted.");
                    return;
                }

                for (NsdHelperListener l : listeners) {
                    l.onAcceptableServiceResolved(serviceInfo);
                }
            }
        };
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
                Log.e(TAG, "Registered service " + nsdServiceInfo.getServiceName());
                for (NsdHelperListener l : listeners) {
                    l.onServiceRegistered(nsdServiceInfo);
                }
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


    private synchronized void doRegisterService(NsdServiceInfo info) throws IllegalArgumentException {
        mNsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        registered = true;
    }

    public synchronized boolean registerService(int port) {
        Log.e(TAG, "Registering service at " + MyWifiManager.getIpAddress(mContext) + ":" + port); // Log.e is red
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        try {
            doRegisterService(serviceInfo);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to register service, " + e.getMessage() + ". Retrying...");
            try {
                initializeRegistrationListener();
                doRegisterService(serviceInfo);
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Failed to register service, " + e2.getMessage());
            }
        }
        return registered;
    }

    public synchronized boolean unregisterService() {
        if (!registered) return false;
        mNsdManager.unregisterService(mRegistrationListener);
        return true;
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



    /**
     * Allow other objects to react to events. Called on main thread.
     */
    public static interface NsdHelperListener {
        void onServiceRegistered(NsdServiceInfo info);
        void onAcceptableServiceResolved(NsdServiceInfo info);
    }
    private final List<NsdHelperListener> listeners = new ArrayList<NsdHelperListener>(1);
    public boolean registerListener(NsdHelperListener l) {
        if (listeners.contains(l)) return false;
        return listeners.add(l);
    }
    public boolean unregisterListener(NsdHelperListener l) {
        return listeners.remove(l);
    }
}
