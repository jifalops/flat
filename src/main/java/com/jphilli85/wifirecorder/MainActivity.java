package com.jphilli85.wifirecorder;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName(); // Android's log

    private static final int REQUEST_IMAGE = 0;

    private String mLabel;
    private TextView mScanCountView;
    private int mScanCount;
    private Switch mPersistentSwitch;
    private Switch mScannerSwitch;
    private boolean mBound;
    private MyService mService;
    private ServiceConnection mConnection;
    private Bitmap mPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_HOME |
//                ActionBar.DISPLAY_HOME_AS_UP |
                ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.power_switch);
        mPersistentSwitch = (Switch) getActionBar().getCustomView();

        mScanCountView = (TextView) findViewById(R.id.scanCount);
        mScannerSwitch = (Switch) findViewById(R.id.scannerSwitch);

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                MyService.LocalBinder binder = (MyService.LocalBinder) service;
                mService = binder.getService();
                mService.setListener(new MyService.Listener() {
                    @Override
                    public void onScanRecorded(int count) {
                        mScanCount = count;
                        mScanCountView.post(new Runnable() {
                            @Override
                            public void run() {
                                mScanCountView.setText(String.valueOf(mScanCount));
                            }
                        });
                    }
                });

                mScannerSwitch.setOnCheckedChangeListener(null);
                mScannerSwitch.setChecked(mService.isRecording());
                mScannerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (mBound) {
                            mService.setRecording(b);
                        }
                    }
                });

                mPersistentSwitch.setOnCheckedChangeListener(null);
                mPersistentSwitch.setChecked(mService.isPersistent());
                mPersistentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b && !mService.isPersistent()) {
                            persist();
                        }
                        else if (!b && mService.isPersistent()) {
                            unpersist();
                        }
                    }
                });

                if (mPhoto != null) {
                    Intent i = new Intent(MyService.ACTION_SAVE_PHOTO);
                    i.putExtra(MyService.EXTRA_PHOTO, mPhoto);
                    sendBroadcast(i);
                    mPhoto = null;
                }

                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        doBindService();
    }


    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clearLog:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage("Clear Log");
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mBound) {
                            mService.clearLog();
                        }
                    }
                });
                alert.setNegativeButton("Cancel", null);
                alert.show();
                break;
        }

        return true;
    }

    public void onButtonClick(View v) {
        switch (v.getId()) {
            case R.id.markLocation:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                mLabel = "Mark " + (mScanCount + 1);
                final EditText input = (EditText) getLayoutInflater().inflate(R.layout.edittext, null);
                input.setText(mLabel);
                input.setSelection(0, mLabel.length());
                alert.setView(input);
                alert.setPositiveButton("Text only", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mLabel = input.getText().toString();
                        if (mBound) {
                            mService.setLabel(mLabel);
                        }
                    }
                });
                alert.setNegativeButton("Add Pic", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mLabel = input.getText().toString();
                        if (mBound) {
                            mService.setLabel(mLabel);
                        }
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, REQUEST_IMAGE);
                    }
                });
                alert.show();
                break;
            case R.id.viewLog:
                if (mBound) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(Uri.fromFile(mService.getLogFile()), "text/csv");
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
                break;
            case R.id.viewPics:
//                if (mBound) {
//                    mService.viewPhotos();
//                }
                if (mBound) {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.setDataAndType(Uri.fromFile(mService.getBaseDir()), "file/*");
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            mPhoto = (Bitmap) data.getExtras().get("data");
        }
    }

    private void persist() {
        startService(new Intent(MainActivity.this, MyService.class));
    }

    private void unpersist() {
        stopService(new Intent(MainActivity.this, MyService.class));
        doBindService();
        mScannerSwitch.setChecked(false);
    }

    private void doBindService() {
        bindService(new Intent(this, MyService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}
