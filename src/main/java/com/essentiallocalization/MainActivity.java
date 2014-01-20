package com.essentiallocalization;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity implements BluetoothFragment.Listener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        if (!RootTools.isAccessGiven()) {
//            Toast.makeText(this, "Root is not available or was denied! App may not function correctly.", Toast.LENGTH_LONG).show();
//        } else if (!RootTools.isBusyboxAvailable()) {
//            RootTools.offerBusyBox(this);
//        }
    }

    @Override
    public void onRequestBluetoothEnabled() {

    }

    @Override
    public void onRequestDiscoverable() {

    }

    @Override
    public void onBluetoothSupported(boolean supported) {

    }
}