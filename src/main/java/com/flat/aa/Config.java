package com.flat.aa;

/**
 * @author Jacob Phillips (05/2015, jphilli85 at gmail)
 */
public class Config {
    public static final int SCAN_PERIOD_MS = 2000; // millis
    public static final int SCAN_MIN_SCANS = 30;
    public static final int SCAN_MAX_SCANS = 45;
    public static final long SCAN_CUTOFF_AGE_US = 2 * 60 * 1000000; // micro sec

    public static final int BEACON_PERIOD_MIN_MS = 10000;
    public static final int BEACON_PERIOD_MAX_MS = 15000;
}
