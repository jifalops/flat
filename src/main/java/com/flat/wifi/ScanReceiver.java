package com.flat.wifi;

import android.net.wifi.ScanResult;

import java.util.List;

/**
* Created by Jacob Phillips.
*/
public interface ScanReceiver {
    void onScanResults(List<ScanResult> scanResults);
}
