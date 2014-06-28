package com.essentiallocalization.sensors;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.net.wifi.ScanResult;
import android.util.Log;

import com.essentiallocalization.util.CsvBuffer;
import com.essentiallocalization.util.Util;
import com.essentiallocalization.util.app.PersistentIntentService;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jake on 5/28/2014.
 */
public final class MovementSensorService extends PersistentIntentService {
    private static final String TAG = MovementSensorService.class.getSimpleName();

    private CsvBuffer mBuffer;
    private SensorManager mSensorManager;

    @Override
    protected void onHandleIntent(Intent intent) {
        //blank
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mBuffer = new CsvBuffer();
    }

    @Override
    public void onDestroy() {
        try {
            mBuffer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close buffer");
        }
        super.onDestroy();
    }

//    @Override
//    public void unregisterReceiver() {
//        cancelTimer();
//    }
//
//    /** Does not stop service instance */
//    public void stopScanning() {
//        unregisterReceiver();
//    }
//
//    /** @param period the frequency in ms to scan. Use 0 for a single scan. */
//    public void startScanning(final int period) {
//        mTimerPeriod = period;
//        if (period == 0) {
//            mManager.startScan();
//        } else {
//            cancelTimer();
//            mTimer = new Timer();
//            mTimer.scheduleAtFixedRate(new TimerTask() {
//                @Override
//                public void run() {
//                    mManager.startScan();
//                }
//            }, 0, period);
//        }
//    }
//
//    public int getScanTimerPeriod() {
//        return mTimerPeriod;
//    }
//
//    public void setCallback(Callback cb) {
//        mCallback = cb;
//    }
//
//
//
//    private void cancelTimer() {
//        if (mTimer != null) {
//            mTimer.cancel();
//        }
//    }
//
//    public void setLogging(File file, boolean append) throws IOException {
//        mBuffer.setWriteThroughFile(file, append);
//    }
//    public void setLogging(File file) throws IOException {
//        setLogging(file, true);
//    }
//
//
//    public boolean isLogging() {
//        return mBuffer.isWriteThrough();
//    }
//
//    public String getScanResultHeader() {
//        return " #, Timestamp   , BSSID/MAC        , SSID/name   , RSSI,  Freq\n";
//    }
//
//    public String[] formatScanResult(int id, ScanResult sr) {
//        return new String[]{
//                String.format("%2d", id),
//                String.format("%12s", Util.Format.newBasic6dec().format(calcTimeDiff(sr))),
//                sr.BSSID,
//                String.format("%-12s", sr.SSID),
//                String.format("%4d", sr.level),
//                String.format("%5d", sr.frequency),
//        };
//    }
//
//    private float calcTimeDiff(ScanResult sr) {
//        if (mTimeReference == 0) mTimeReference = sr.timestamp;
//        return (sr.timestamp - mTimeReference) / 1E6f;
//    }


}
