package com.flat.util.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;

import com.flat.localization.ranging.FreeSpacePathLoss;
import com.flat.util.Util;

import java.io.File;
import java.util.Date;

/**
 * @author Jacob Phillips
 */
public final class ScanResultsConfig {
    // Singleton pattern
    private static ScanResultsConfig instance = null;
    private ScanResultsConfig(){}
    public static ScanResultsConfig getInstance() {
        if (instance == null) {
            instance = new ScanResultsConfig();
        }
        return instance;
    }
    ////

    public static final int DEFAULT_PERIOD = 1000;
    public static final int DEFAULT_RANDOM_MIN = 1000;
    public static final int DEFAULT_RANDOM_MAX = 10000;

    private long mFirstScan;
    private File mLogFile;
    private Date mFileStartTime;

    public File getLogFile(Context ctx) {
        if (mLogFile == null) {
            mLogFile = new File(ctx.getExternalFilesDir(null), "ScanResults " + Util.Format.LOG_FILENAME.format(getStartTime()) + ".txt");
        }
        return mLogFile;
    }

    public String[] getScanResultHeader() {
        return new String[] {"#  ", "Delay", "Elapsed    ", "BSSID   ", "SSID", "RSSI", "Freq", "Dist"};
    }

    public String[] formatScanResult(int scanCount, int delay, ScanResult sr) {
        int end = sr.SSID.length();
        if (end > 4) end = 4;
        return new String[]{
                String.format("%3d", scanCount),
                String.format("%5.2f", delay / 1000f),
                String.format("%11s", Util.Format.newBasic6dec().format(calcTimeDiff(sr))),
                sr.BSSID.substring(9),
                String.format("%-4s", sr.SSID.substring(0, end)),
                String.format("%4d", sr.level),
                String.format("%4d", sr.frequency),
                String.format("%6.2f", FreeSpacePathLoss.fspl(sr.level, sr.frequency))
        };
    }

    private float calcTimeDiff(ScanResult sr) {
        if (mFirstScan == 0) {
            mFirstScan = sr.timestamp;
        }
        return (sr.timestamp - mFirstScan) / 1E6f;
    }

    public Date getStartTime() {
        if (mFileStartTime == null) {
            mFileStartTime = new Date();
        }
        return mFileStartTime;
    }
}
