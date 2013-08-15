package com.jphilli85.wifirecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_IMAGE = 0;

    //boolean mIsEnabled;
    private String mMarkLabel;
    private TextView mScanCountView;
    private int mScanCount;
    private MyService.WifiReceiver mReceiver;
    private Switch mPowerSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScanCountView = (TextView) findViewById(R.id.scanCount);

        mReceiver = new MyService.WifiReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MyService.WifiReceiver.ACTION_WIFI)) {
                    mScanCount = intent.getIntExtra(MyService.EXTRA_SCAN_COUNT, 0);
                    mScanCountView.setText(String.valueOf(mScanCount));
                }
//                else if (intent.getAction().equals(MyService.ACTION_ON_STOPPED)) {
//                    if (mPowerSwitch != null) mPowerSwitch.setChecked(MyService.isRunning());
//                }

            }
        };


    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyService.WifiReceiver.ACTION_WIFI);
//        filter.addAction(MyService.ACTION_ON_STOPPED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mPowerSwitch.setChecked(MyService.isRunning());
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mIsEnabled = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        mPowerSwitch = (Switch) menu.findItem(R.id.powerSwitch).getActionView();
        mPowerSwitch.setChecked(MyService.isRunning());
        mPowerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setEnabled(b);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        }
        return true;
    }

    public void onMarkLocation(View v) {
        AlertDialog.Builder editalert = new AlertDialog.Builder(this);

        //editalert.setTitle("messagetitle");
        editalert.setMessage("Mark Location");


        final EditText input = (EditText) getLayoutInflater().inflate(R.layout.edittext, null);
        mMarkLabel = "Mark " + mScanCount;
        input.setText(mMarkLabel);
        input.setSelection(0, mMarkLabel.length());
        editalert.setView(input);

        editalert.setPositiveButton("Set Label", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mMarkLabel = input.getText().toString();
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MyService.WifiReceiver.ACTION_LABEL);
                broadcastIntent.putExtra(MyService.EXTRA_LABEL, mMarkLabel);
                sendBroadcast(broadcastIntent);
            }
        });

        editalert.setNegativeButton("Add Pic", new DialogInterface.OnClickListener() {
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
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MyService.WifiReceiver.ACTION_PHOTO);
            broadcastIntent.putExtra(MyService.EXTRA_PHOTO, photo);
            broadcastIntent.putExtra(MyService.EXTRA_LABEL, mMarkLabel);
            sendBroadcast(broadcastIntent);
        }
    }

    private void setEnabled(boolean enabled) {
        if (enabled && !MyService.isRunning()) {
            startService(new Intent(this, MyService.class));
        } else if (!enabled && MyService.isRunning()) {
            stopService(new Intent(this, MyService.class));
//            Intent broadcastIntent = new Intent();
//            broadcastIntent.setAction(MyService.ACTION_STOP);
//            sendBroadcast(broadcastIntent);
        }

    }
}
