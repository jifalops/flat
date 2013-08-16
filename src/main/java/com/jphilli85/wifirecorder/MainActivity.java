package com.jphilli85.wifirecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
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

    private String mMarkLabel;
    private TextView mScanCountView;
    private int mScanCount;
    private BroadcastReceiver mReceiver;
    private Switch mPowerSwitch;
    private Switch mScannerSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        mScanCountView = (TextView) findViewById(R.id.scanCount);
        mScannerSwitch = (Switch) findViewById(R.id.scannerSwitch);
        mScannerSwitch.setChecked(MyService.isScanning());
        mScannerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                String action = b
                        ? MyService.ACTION_START_RECORDING
                        : MyService.ACTION_STOP_RECORDING;
                sendBroadcast(new Intent(action));
            }
        });

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MyService.ACTION_RECORD_WRITTEN)) {
                    mScanCount = intent.getIntExtra(MyService.EXTRA_SCAN_COUNT, 0);
                    mScanCountView.setText(String.valueOf(mScanCount));
                }
            }
        };

        if (!MyService.isRunning()) startService(new Intent(this, MyService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mReceiver, new IntentFilter(MyService.ACTION_RECORD_WRITTEN));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO why does this cause error?
//        mPowerSwitch.setChecked(MyService.isRunning());
    }


    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mPowerSwitch = (Switch) menu.findItem(R.id.powerSwitch).getActionView();
        mPowerSwitch.setChecked(MyService.isRunning());
        mScannerSwitch.setEnabled(MyService.isRunning());
        mPowerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b && !MyService.isRunning()) {
                    startService(new Intent(MainActivity.this, MyService.class));
                    mScannerSwitch.setEnabled(true);
                } else if (!b && MyService.isRunning()) {
                    stopService(new Intent(MainActivity.this, MyService.class));
                    mScannerSwitch.setEnabled(false);
                    mScannerSwitch.setChecked(false);
                }
            }
        });
        return true;
    }

    public void onMarkLocation(View v) {
        AlertDialog.Builder editalert = new AlertDialog.Builder(this);

        editalert.setTitle("Mark Location");


        final EditText input = (EditText) getLayoutInflater().inflate(R.layout.edittext, null);
        mMarkLabel = "Mark " + (mScanCount + 1);
        input.setText(mMarkLabel);
        input.setSelection(0, mMarkLabel.length());
        editalert.setView(input);

        editalert.setPositiveButton("Text only", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mMarkLabel = input.getText().toString();
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MyService.ACTION_LABEL);
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

    public void onViewLog(View v) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MyService.ACTION_VIEW_LOG);
        sendBroadcast(broadcastIntent);
    }

    public void onClearLog(MenuItem item) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MyService.ACTION_CLEAR_LOG);
        sendBroadcast(broadcastIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MyService.ACTION_PHOTO);
            broadcastIntent.putExtra(MyService.EXTRA_PHOTO, photo);
            broadcastIntent.putExtra(MyService.EXTRA_LABEL, mMarkLabel);
            sendBroadcast(broadcastIntent);
        }
    }
}
