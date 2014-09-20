package com.essentiallocalization.localization.signal;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;

import com.essentiallocalization.util.CsvBuffer;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WifiBeacon extends AbstractSignal {

    private Timer timer;


    /*
     * Singleton
     */
    private WifiBeacon() {}
    private static WifiBeacon instance;
    public static WifiBeacon getInstance() {
        if(instance == null) {
            instance = new WifiBeacon();
        }
        return instance;
    }

    private boolean enabled;

    @Override
    public int getType() {
        return Signal.TYPE_ELECTROMAGNETIC;
    }

    /**
     * @param args args[0] is a Context used to get the WifiManager and start a scan.
     */
    @Override
    public void requestSingleUpdate(Object... args) {
        Context ctx = (Context) args[0];
        WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        manager.startScan();
    }

    /**
     * @param args args[0] is a Context used to get the WifiManager and start a scan.
     *             args[1] is an interval in seconds at which scans will repeat, defaulting to 1.
     *                      If a zero is passed, only a single update will be requested.
     */
    @Override
    public void enable(Object... args) {
        Context ctx = (Context) args[0];
        WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        int scanInterval = 1;
        if (args.length == 2) scanInterval = (Integer) args[1];

        if (period == 0) {
            mManager.startScan();
        } else {
            cancelTimer();
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    mManager.startScan();
                }
            }, 0, period);
        }


        manager.startScan();




        mTimerPeriod = period;
        if (period == 0) {
            mManager.startScan();
        } else {
            cancelTimer();
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    mManager.startScan();
                }
            }, 0, period);
        }
    }

    @Override
    public void disable(Object... args) {

    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }





    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            notifyListeners();
        }
    };

    private final IntentFilter scanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
}
