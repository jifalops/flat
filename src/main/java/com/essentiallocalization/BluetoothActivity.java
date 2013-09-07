package com.essentiallocalization;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.essentiallocalization.service.BluetoothService2;
import com.essentiallocalization.service.PersistentIntentService;
import com.essentiallocalization.util.Jog;

/**
 * Created by Jake on 9/1/13.
 */
public class BluetoothActivity extends Activity implements ServiceConnection {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVERABLE = 2;


    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService2 mService;
    private boolean mBound;
    private Handler mHandler;
    private final int mMaxConnections = 1;
    private final TextView[] mStateViews = new TextView[mMaxConnections];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ViewGroup parent = (ViewGroup) findViewById(R.id.states);
        for (int i = 0; i < mMaxConnections; ++i) {
            mStateViews[i] = (TextView) getLayoutInflater().inflate(R.layout.state_textview, null);
            parent.addView(mStateViews[i]);
        }

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:
                        int index = msg.arg1;
                        switch (msg.arg2) {
                            case BluetoothService2.STATE_CONNECTED:
                                mStateViews[index].setText("Connection " + index + ": Connected");
                                break;
                            case BluetoothService2.STATE_CONNECTING:
                                mStateViews[index].setText("Connection " + index + ": Connecting...");
                                break;
                            case BluetoothService2.STATE_LISTEN:
                                mStateViews[index].setText("Connection " + index + ": Listening...");
                            case BluetoothService2.STATE_NONE:
                                mStateViews[index].setText("Connection " + index + ": Not connected");
                                break;
                        }
                        break;
                    case MESSAGE_WRITE:
                        byte[] writeBuf = (byte[]) msg.obj;
                        // construct a string from the buffer
                        String writeMessage = new String(writeBuf);
//                        mConversationArrayAdapter.add("Me:  " + writeMessage);
                        break;
                    case MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
//                        mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                        break;
                    case MESSAGE_DEVICE_NAME:
                        // save the connected device's name
//                        mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//                        Toast.makeText(getApplicationContext(), "Connected to "
//                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                        break;
//                    case MESSAGE_TOAST:
//                        Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
//                                Toast.LENGTH_SHORT).show();
                        break;
                }
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

    private void doBindService() {
        bindService(new Intent(this, BluetoothService2.class), this, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        if (mBound) {
            unbindService(this);
            mBound = false;
        }
    }

    private void persist() {
        startService(new Intent(BluetoothActivity.this, BluetoothService2.class));
    }

    private void unpersist() {
        Jog.v("Service unpersisting", this);
        stopService(new Intent(BluetoothActivity.this, BluetoothService2.class));
//        doBindService();
    }

    public void enableBluetooth(Activity activity) {
        if (!mBluetoothAdapter.isEnabled()) {
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }
    }

    /** Will enable Bluetooth if it is not already enabled. */
    public void enableDiscoverability(Activity activity) {
        if (!mBluetoothAdapter.isDiscovering()) {
            //mDiscoveredDevices.clear(); // TODO this should be called in onActivityResult()
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), REQUEST_DISCOVERABLE);
        }
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        PersistentIntentService.LocalBinder binder = (PersistentIntentService.LocalBinder) service;
        mService = (BluetoothService2) binder.getService();
        mService.setHandler(mHandler);
        mService.setMaxConnections(mMaxConnections);
        mService.start();
        //mService.enableDiscoverability(BluetoothActivity.this);
        //mService.startDiscovery();

        mBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        mBound = false;
        unpersist();
    }
}
