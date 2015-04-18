package com.flat.wifi;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jacob Phillips.
 */
public class AggregateScanResult {
    public String bssid;
    public String ssid;
    public int freq;
    public List<Integer> rssi = new ArrayList<Integer>();
    public List<Long> time = new ArrayList<Long>();

    boolean add(ScanResult result) {
        if (bssid == null) {
            bssid = result.BSSID;
            ssid = result.SSID;
            freq = result.frequency;
        }
        if (bssid.equals(result.BSSID)) {
            rssi.add(result.level);
            time.add(result.timestamp);
            return true;
        }
        return false;
    }

    public int size() {
        return rssi.size();
    }


    /** Uses the max rssi from up to the 10 most recent results,
     * but they must be less than 1 minute old. */
    public int effectiveRssi() {
        long cutoff = (System.nanoTime() / 1000) - 1000000*60;
        int index;
        for (index=0; index<time.size(); ++index) {
            if (time.get(index) > cutoff) {
                break;
            }
        }

        if (index < rssi.size() - 10) {
            index = rssi.size() - 10;
        }

        List<Integer> relevant = rssi.subList(index, rssi.size());

        Collections.sort(relevant);
        return relevant.get(relevant.size() - 1);
    }

    public long elapsedTime() {
        List<Long> tmp = new ArrayList<Long>(time);
        Collections.sort(tmp);
        return tmp.get(tmp.size()-1) - tmp.get(0);
    }

    public float resultsPerMinute() {
        return 60f * ((float)rssi.size() / ((int)elapsedTime() / 1000f));
    }

//    public int medianRssi() {
//        int[] tmp = ArrayUtils.toPrimitive(rssi.toArray(new Integer[rssi.size()]));
//        return Statistics.getMedian(tmp);
//    }
//    public int meanRssi() {
//        int[] tmp = ArrayUtils.toPrimitive(rssi.toArray(new Integer[rssi.size()]));
//        return Statistics.getMean(tmp);
//    }
//    public int stdDevRssi() {
//        int[] tmp = ArrayUtils.toPrimitive(rssi.toArray(new Integer[rssi.size()]));
//        return Statistics.getStdDev(tmp);
//    }
}
