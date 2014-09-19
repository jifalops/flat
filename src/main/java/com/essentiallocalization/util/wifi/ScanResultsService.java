package com.essentiallocalization.util.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.essentiallocalization.util.CsvBuffer;
import com.essentiallocalization.util.app.PersistentIntentService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jake on 5/28/2014.
 */
public final class ScanResultsService extends PersistentIntentService {
    private static final String TAG = ScanResultsService.class.getSimpleName();
    private Timer mTimer;

    private WifiManager mManager;
    private CsvBuffer mBuffer;
    private int mScanCount;

    private int mTimerPeriod;
    private volatile AtomicInteger mTimerDelay;

    private ScanResultsConfig mConfig;



    public static interface Callback {
        /** Called on service's thread. */
        void onScanResults(List<ScanResult> results);
    }
    private Callback mCallback;

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "scan results received");
        final List<ScanResult> results = mManager.getScanResults();

        if (mBuffer.isWriteThrough()) {
            for (ScanResult sr : results) {
                mBuffer.add(mConfig.formatScanResult(++mScanCount, mTimerDelay.get(), sr));
            }
            try {
                mBuffer.flush();
            } catch (IOException e) {
                Log.e(TAG, "Failed to flush output to file.");
            }
        }

        if (mCallback != null) {
            mCallback.onScanResults(results);
        }
    }



    @Override
    public void onCreate() {
        super.onCreate();
        mManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mConfig = ScanResultsConfig.getInstance();
        mBuffer = new CsvBuffer();
        try {
            setLogging(mConfig.getLogFile(this), true);
        } catch (IOException e) {
            Log.e(TAG, "Failed to open log.");
        }
        mTimerDelay = new AtomicInteger(0);
//        mScanResults = new ArrayList<List<ScanResult>>();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "running onDestroy()");
        cancelTimer();
        try {
            mBuffer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close buffer");
        }
        super.onDestroy();
    }

    @Override
    public void unregisterReceiver() {
        Log.i(TAG, "unregistering receiver");
        cancelTimer();
        super.unregisterReceiver();
    }

    /** Does not stop service instance */
    public void stopScanning() {
        unregisterReceiver();
    }

    /**
     * Scan at random intervals with the given bounds.
     * @param minDelay the minimum time in ms before next scan (inclusive).
     * @param maxDelay the maximum time in ms before next scan (exclusive).
     */
    public void startScanning(final int minDelay, final int maxDelay) {
        cancelTimer();
        mTimer = new Timer();
        mTimerDelay.set(minDelay + new Random().nextInt(maxDelay - minDelay));
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mManager.startScan();
                startScanning(minDelay, maxDelay);
            }
        }, mTimerDelay.get());

    }

    /** @param period the frequency in ms to scan. Use 0 for a single scan. */
    public void startScanning(final int period) {
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

    public int getScanCount() {
        return mScanCount;
    }

    public int getTimerDelay() {
        return mTimerDelay.get();
    }

    public int getScanTimerPeriod() {
        return mTimerPeriod;
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }



    private void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    public void setLogging(File file, boolean append) throws IOException {
        mBuffer.setWriteThroughFile(file, append);
        mBuffer.add(mConfig.getScanResultHeader());
        mBuffer.flush();
    }
    public void setLogging(File file) throws IOException {
        setLogging(file, true);
    }


    public boolean isLogging() {
        return mBuffer.isWriteThrough();
    }
}
