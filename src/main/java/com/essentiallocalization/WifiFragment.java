package com.essentiallocalization;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.essentiallocalization.util.CsvBuffer;
import com.essentiallocalization.util.Util;
import com.essentiallocalization.wifi.DataPoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WifiFragment extends Fragment {
    private static final String TAG = WifiFragment.class.getSimpleName();

    /** Interface the containing activity must implement */
    static interface Callbacks {

    }

    private Callbacks mCallbacks;
    private TextView mTextView;
    private WifiManager mManager;
    private Timer mTimer;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.v(TAG, "onAttach()");
        try {
            mCallbacks = (Callbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(TAG + ": Activity must implement the fragment's callbacks.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        setHasOptionsMenu(true);
        mManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.fragment_wifi, container, false);
        mTextView = (TextView) root.findViewById(R.id.text);
        mTextView.setTypeface(Typeface.MONOSPACE);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        return root;
    }

    @Override
    public void onPause() {
        mTimer.cancel();
        getActivity().unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mTextView.append(" #, Timestamp   , BSSID/MAC        , SSID/name   , RSSI,  Freq\n");
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mManager.startScan();
            }
        }, 0, 1000);
        super.onResume();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            CsvBuffer buffer = new CsvBuffer();
            List<ScanResult> results = mManager.getScanResults();

            int i = 0;
            for (ScanResult result : results) {
                buffer.add(new String[]{
                    String.format("%2d", ++i),
                    String.valueOf(result.timestamp),
                    result.BSSID,
                    String.format("%-12s", result.SSID),
                    String.format("%4d", result.level),
                    String.format("%5d", result.frequency),
                });
            }

            mTextView.append(buffer.toString() + "\n");
        }
    };
}
