package com.flat.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

/**
 * @author Jacob Phillips (11/2014, jphilli85 at gmail)
 */
public final class MyWifiManager {
    private static final String TAG = MyWifiManager.class.getSimpleName();

    private WifiManager wifiManager;
    private ConnectivityManager connManager;
    private SoftAccessPointManager apManager = null;

    // Singleton
    private static MyWifiManager instance;
    public static MyWifiManager getInstance(Context ctx) {
        if (instance == null) instance = new MyWifiManager(ctx);
        return instance;
    }
    private MyWifiManager(Context ctx) {
        wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            apManager = SoftAccessPointManager.getInstance(ctx);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Soft AP not available.", e);
        }
    }

    public WifiManager getWifiManager() {
        return wifiManager;
    }
    public SoftAccessPointManager getSoftApManager() {
        return apManager;
    }
    public ConnectivityManager getConnectionManager() {
        return connManager;
    }





    /*
     * ConnectionManager related
     */
    public NetworkInfo getNetworkInfo() {
        return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }
    public boolean isConnected() {
        NetworkInfo info = getNetworkInfo();
        return info != null && info.isConnected();
    }
    public boolean isConnectedOrConnecting() {
        NetworkInfo info = getNetworkInfo();
        return info != null && info.isConnectedOrConnecting();
    }
    public boolean isAvailable() {
        NetworkInfo info = getNetworkInfo();
        return info != null && info.isAvailable();
    }

    /*
     * WifiManager basic related
     */
    public boolean setWifiEnabled(boolean enabled) {
        return wifiManager.setWifiEnabled(enabled);
    }

    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }
    public String getMacAddress() {
        WifiInfo info = wifiManager.getConnectionInfo();
        return info == null ? "" : info.getMacAddress();
    }
    public String getIpAddress() {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null) return null;

        int ipAddress = info.getIpAddress();
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }
        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e(TAG, "Unable to get host address.");
            ipAddressString = null;
        }
        return ipAddressString;
    }

    /*
     * SoftAccessPointManager basic state
     */
    public boolean setSoftApEnabled(boolean enabled) {
        return apManager.setEnabled(enabled);
    }

    public boolean isSoftApEnabled() {
        return apManager.isEnabled();
    }
}
