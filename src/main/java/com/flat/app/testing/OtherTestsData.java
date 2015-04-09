package com.flat.app.testing;

import android.app.Fragment;
import android.os.Bundle;

import com.flat.R;
import com.flat.bluetoothtimeofflight.BluetoothFragment;

/**
 * Created by Jake on 5/19/2014.
 */
public class OtherTestsData {

    // #############################################################
    // ### These correspond to the items in the arrays resource. ###
    // ### When changing one, you must change the other.         ###
    // #############################################################
    public static final int ROTATION_VECTOR_DEMO = 0;
    public static final int BLUETOOTH = 1;
    public static final int WIFI = 2;
    public static final int MOVEMENT_SENSOR = 3;
    public static final int NETWORK_SERVICE_DISCOVERY = 4;
    public static final int BLUETOOTH_LE = 5;

    public static final int DEFAULT = ROTATION_VECTOR_DEMO;

    public static final String KEY_ITEM = OtherTestsData.class.getName() + ".ITEM";

    public static int getItem(Bundle bundle) {
        return bundle.getInt(KEY_ITEM, DEFAULT);
    }


    public static int getFragmentId(int item) {
        switch (item) {
            case BLUETOOTH:
                return R.id.bluetoothFragment;
            case WIFI:
                return R.id.scanResultsFragment;
            case MOVEMENT_SENSOR:
                return R.id.movementSensorFragment;
        }
        return 0;
    }

    public static String getFragmentName(int item) {
        switch (item) {
            case BLUETOOTH:
                return BluetoothFragment.class.getName();
            case WIFI:
                return ScanResultsFragment.class.getName();
            case MOVEMENT_SENSOR:
                return MovementSensorFragment.class.getName();
        }
        return null;
    }

    public static Fragment getFragment(int item) {
        switch (item) {
            case BLUETOOTH:
                return new BluetoothFragment();
            case WIFI:
                return new ScanResultsFragment();
            case MOVEMENT_SENSOR:
                return new MovementSensorFragment();
        }
        return null;
    }
}
