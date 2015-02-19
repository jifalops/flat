package com.flat.wifi;

import android.content.Context;
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
public final class WifiHelper {
    private static final String TAG = WifiHelper.class.getSimpleName();

    private Context context;
    private WifiManager manager;


    public WifiHelper(Context ctx) {
        //context = ctx;
        manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    }

    public String getMacAddress() {
        WifiInfo info = manager.getConnectionInfo();
        return info == null ? "" : info.getMacAddress();
    }

    public String getIpAddress() {
        int ipAddress = manager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
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
}
