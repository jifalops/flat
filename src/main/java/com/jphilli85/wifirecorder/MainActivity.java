package com.jphilli85.wifirecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Switch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    static final String BASE_DIR = "Wifi Records";
    static final String LOG_FILE = "wifirecords.txt";
    static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    static final int REQUEST_IMAGE = 0;
    WifiManager mWifiManager;
    File mBaseDir;
    FileWriter mLogWriter;
    int mScanCount;
    boolean mIsEnabled;
    String mMarkLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        mIsEnabled = true;

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mBaseDir = new File(getExternalFilesDir(null) + File.separator + BASE_DIR);
        mLogWriter = null;

        try {
            mLogWriter = new FileWriter(new File(mBaseDir, LOG_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(new BroadcastReceiver(){
            public void onReceive(Context c, Intent i) {
                ++mScanCount;
                String timestamp = SDF.format(new Date());
                List<ScanResult> results = mWifiManager.getScanResults ();
                StringBuilder sb = new StringBuilder();
                for (ScanResult sr : results) {
                    sb.append(mScanCount + "," + timestamp + "," + sr.BSSID + ","
                            + sr.SSID + "," + sr.level + "," + mMarkLabel + "\n");
                }

                try {
                    mLogWriter.append(sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (mIsEnabled) mWifiManager.startScan();
            }
        }, filter);


        if (!mWifiManager.startScan()) {

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsEnabled = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.powerSwitchItem:
                //toggle();
                //((Switch) item.getActionView()).setChecked(mIsEnabled);
                mIsEnabled = ((Switch) item.getActionView()).isChecked();
                break;
        }
        return true;
    }

    void onMarkLocation() {
        AlertDialog.Builder editalert = new AlertDialog.Builder(this);

        //editalert.setTitle("messagetitle");
        editalert.setMessage("Mark Location");


        final EditText input = (EditText) getLayoutInflater().inflate(R.layout.edittext, null);
        mMarkLabel = "Mark " + mScanCount;
        input.setText(mMarkLabel);
        input.setSelection(0, mMarkLabel.length() - 1);
        editalert.setView(input);

        editalert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mMarkLabel = input.getText().toString();
            }
        });

        editalert.setNegativeButton("Take Pic", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mMarkLabel = input.getText().toString();
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, REQUEST_IMAGE);
            }
        });


        editalert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            try {
                FileOutputStream out = new FileOutputStream(new File(mBaseDir, mMarkLabel));
                photo.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void toggle() {
        mIsEnabled = !mIsEnabled;
    }
}
