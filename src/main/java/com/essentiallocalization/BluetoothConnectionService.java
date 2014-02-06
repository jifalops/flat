package com.essentiallocalization;

import android.content.Intent;

import com.essentiallocalization.util.app.PersistentIntentService;

/**
 *
 */
public class BluetoothConnectionService extends PersistentIntentService {
    private  static final String TAG = BluetoothConnectionService.class.getSimpleName();

    @Override
    protected void onHandleIntent(Intent intent) {
        // listen to system bt broadcasts
//        BluetoothAdapter.ACTION_
    }
}
