package com.essentiallocalization.app;

import android.app.Fragment;
import android.os.Bundle;

import com.essentiallocalization.R;
import com.essentiallocalization.util.wifi.ScanResultsFragment;

/**
 * Created by Jake on 5/19/2014.
 */
public class MainItems {

    //This correspond to the items in the arrays resource.
    public static final int BLUETOOTH = 0;
    public static final int WIFI = 1;
    public static final int ROTATION_VECTOR_DEMO = 2;

    public static final int DEFAULT = WIFI;

    public static final String KEY_ITEM = MainItems.class.getName() + ".ITEM";

    public static int getItem(Bundle bundle) {
        return bundle.getInt(KEY_ITEM, DEFAULT);
    }


    public static int getFragmentId(int item) {
        switch (item) {
            case BLUETOOTH:
                return R.id.bluetoothFragment;
            case WIFI:
                return R.id.scanResultsFragment;
        }
        return 0;
    }

    public static String getFragmentName(int item) {
        switch (item) {
            case BLUETOOTH:
                return BluetoothFragment.class.getName();
            case WIFI:
                return ScanResultsFragment.class.getName();
        }
        return null;
    }

    public static Fragment getFragment(int item) {
        switch (item) {
            case BLUETOOTH:
                return new BluetoothFragment();
            case WIFI:
                return new ScanResultsFragment();
        }
        return null;
    }
}
