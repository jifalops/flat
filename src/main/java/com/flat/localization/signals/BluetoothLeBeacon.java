package com.flat.localization.signals;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.flat.R;

import java.util.List;

/**
 * Created by Jacob Phillips (09/2014)
 */
public final class BluetoothLeBeacon extends AbstractSignal {
    private static final String TAG = BluetoothLeBeacon.class.getSimpleName();

    public static final int EVENT_SCAN_RESULT = 1;
    public static final int EVENT_BATCH_SCAN_RESULTS = 2;

    private boolean enabled;
    private BluetoothLeScanner scanner;
    private BluetoothLeAdvertiser advertiser;

    private ScanResult scanResult;
    public ScanResult getScanResult() { return scanResult; }

    private List<ScanResult> scanResultList;
    public List<ScanResult> getScanResults() { return scanResultList; }

    private AdvertiseSettings settings = new AdvertiseSettings.Builder().build();
    private AdvertiseData adData = new AdvertiseData.Builder().build();
    private AdvertiseData scanResponse = new AdvertiseData.Builder().build();
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "Advertiser started successfully.");
        }
    };


    /*
     * Singleton
     */
    private static final BluetoothLeBeacon instance = new BluetoothLeBeacon();
    public static BluetoothLeBeacon getInstance() { return instance; }
    private BluetoothLeBeacon() {
        super("BT-LE-Beacon");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            scanResult = result;
            notifyListeners(EVENT_SCAN_RESULT);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            scanResultList = results;
            notifyListeners(EVENT_BATCH_SCAN_RESULTS);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan failed, " + errorCode);
        }
    };

    public boolean hasBluetoothLe(Context ctx) {
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public boolean isBluetoothEnabled(Context ctx) {
        BluetoothAdapter btAdapter = ((BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        return btAdapter != null && btAdapter.isEnabled();
    }

    public void startSystemBluetoothActivity(Context ctx) {
        ctx.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
    }

    /** Calls {@link android.app.Activity}.startActivityForResult(Intent, int). */
    public void startSystemBluetoothActivity(Activity resultHandler, int requestCode) {
        resultHandler.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), requestCode);
    }

    @Override
    public void enable(Context ctx) {

        // check hardware support and if the radio is enabled (should be done by the caller instead of here)
        if (!hasBluetoothLe(ctx)) {
            Toast.makeText(ctx, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        } else if (!isBluetoothEnabled(ctx)) {
            startSystemBluetoothActivity(ctx);
            //Toast.makeText(ctx, R.string.enable_bt, Toast.LENGTH_SHORT).show();
            return;
        }

        // get scanner instance
        BluetoothManager manager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        scanner = adapter.getBluetoothLeScanner();

        advertiser = adapter.getBluetoothLeAdvertiser();




//        ScanSettings.Builder builder = new ScanSettings.Builder();
//        builder.setReportDelay(0);
//        builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
//        scanSettings = builder.build();
        enabled = true;
        scanner.startScan(scanCallback);
        if (advertiser != null) advertiser.startAdvertising(settings, adData, advertiseCallback);
    }



    @Override
    public void disable(Context ctx) {
        enabled = false;
        if (scanner != null) scanner.stopScan(scanCallback);
        if (advertiser != null) advertiser.stopAdvertising(advertiseCallback);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
