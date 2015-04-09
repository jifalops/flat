package com.flat.app.testing;

import android.app.Activity;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.flat.R;
import com.flat.localization.signals.BluetoothLeBeacon;
import com.flat.localization.signals.Signal;

/**
 * @author Jacob Phillips (03/2015, jphilli85 at gmail)
 */
public class BluetoothLeBeaconTestActivity extends Activity {
    TextView text;
    BluetoothLeBeacon btBeacon;
    int count;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        Log.e("BTLE test", "Creating...");
        setTitle("BT-LE Testing");

        setContentView(R.layout.wifi_beacon_main);
        text = (TextView) findViewById(R.id.text);


        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btBeacon.isEnabled()) {
                    btBeacon.enable(BluetoothLeBeaconTestActivity.this);
                } else {
                    btBeacon.disable(BluetoothLeBeaconTestActivity.this);
                }
            }
        });

        btBeacon = BluetoothLeBeacon.getInstance();
        btBeacon.registerListener(new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                switch (eventType) {
                    case BluetoothLeBeacon.EVENT_SCAN_RESULT:
                        text.append(TextUtils.join("", formatScanResult(btBeacon.getScanResult())));
                        break;
                    case BluetoothLeBeacon.EVENT_BATCH_SCAN_RESULTS:

                        break;
                }
            }
        });

        text.setText(TextUtils.join("", getScanResultHeader()));

        btBeacon.enable(this);
        //BluetoothLeAdvertiser advertiser =
    }

    public String[] getScanResultHeader() {
        return new String[] {"#  ", "    Name", "RSSI", "TxPwr", "   Name2"};

    }

    public String[] formatScanResult(ScanResult result) {
        ScanRecord record = result.getScanRecord();
//        result.getTimestampNanos();
        String[] s = new String[5];
        s[0] = String.format("%3d", count);
        s[1] = String.format("%8s", result.getDevice().getName());
        s[2] = String.format("%4d", result.getRssi());

        if (record != null) {
            s[3] = String.format("%5.1d", record.getTxPowerLevel());
            s[4] = String.format("%8s", record.getDeviceName());
        }
        return s;
    }
}
