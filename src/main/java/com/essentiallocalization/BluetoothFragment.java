package com.essentiallocalization;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.essentiallocalization.connection.bluetooth.BluetoothConnection;
import com.essentiallocalization.connection.bluetooth.BluetoothConnectionManager;
import com.essentiallocalization.service.BluetoothService;
import com.essentiallocalization.service.PersistentIntentService;
import com.essentiallocalization.util.LogFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jake on 9/1/13.
 */
public final class BluetoothFragment extends Fragment implements ServiceConnection {
    /** Tag for Android's Logcat */
    private static final String TAG = BluetoothFragment.class.getSimpleName();

    public static final String LOG_FILE = "log.txt";

    /** Interface the containing activity must implement */
    static interface Listener {
        void onRequestBluetoothEnabled();
        void onRequestDiscoverable();
        void onBluetoothSupported(boolean supported);
    }

    // Message types sent from the BluetoothService Handler


    // Intent request codes
//    private static final int REQUEST_ENABLE_BT = 1;
//    private static final int REQUEST_DISCOVERABLE = 2;

    /** This device's BT adapter */
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothService mService;

    /** Whether the service is bound */
    private boolean mBound;

    /** Contains callback methods for this class. */
    private Listener mListener;

    private TextView[] mStateViews;
    private int mMaxConnections = 4;
    private boolean mBluetoothSupported;
    private Switch mServiceSwitch;
    private CheckBox mServicePersist;
    private ListView mListView;
    private ArrayAdapter<String> mListAdapter;

    /** This device's Bluetooth name. */
    private String mName;

    private LogFile mLogFile;

//    private FileObserver mObserver;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.v(TAG, "onAttach()");
        try {
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(TAG + ": Activity must implement the fragment's listener.");
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            mBluetoothSupported = false;
            Toast.makeText(getActivity(), getString(R.string.bt_not_supported), Toast.LENGTH_LONG).show();
        } else {
            mBluetoothSupported = true;
            mName = mBluetoothAdapter.getName();
        }
        mListener.onBluetoothSupported(mBluetoothSupported);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        setHasOptionsMenu(true);

        // Setup Actionbar
        final ActionBar ab = getActivity().getActionBar();
        if (mName != null) ab.setTitle(mName);
        ab.setDisplayOptions(ab.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        ab.setCustomView(R.layout.service_controls);
        View controls = ab.getCustomView();
        mServiceSwitch = (Switch) controls.findViewById(R.id.service_power);
        mServicePersist = (CheckBox) controls.findViewById(R.id.service_persist);

        try {
            mLogFile = new LogFile(new File(getActivity().getExternalFilesDir(null), LOG_FILE));
        } catch (IOException e) {
            Log.e(TAG, "Couldn't create log file.", e);
            Toast.makeText(getActivity(), "Logging disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupServiceControls() {
        if (mBound) {
            mServiceSwitch.setOnCheckedChangeListener(null);
            mServiceSwitch.setChecked(mService.isRunning());

            mServicePersist.setOnCheckedChangeListener(null);
            mServicePersist.setChecked(mService.isPersistent());
        }
        mServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setServiceEnabled(isChecked);
            }
        });
        mServicePersist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setServicePersist(isChecked);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.fragment_bt, container, false);

        // Add all connection state views to the layout,
        // hiding views which are beyond the current max connections.
        ViewGroup states = (ViewGroup) root.findViewById(R.id.states);
        mStateViews = new TextView[BluetoothConnectionManager.MAX_CONNECTIONS];
        for (int i = 0; i < BluetoothConnectionManager.MAX_CONNECTIONS; ++i) {
            mStateViews[i] = (TextView) inflater.inflate(R.layout.state_textview, null);
            if (i >= mMaxConnections) {
                mStateViews[i].setVisibility(View.GONE);
            }
            states.addView(mStateViews[i]);
        }

        mListView = (ListView) root.findViewById(R.id.list);
        mListAdapter = new ArrayAdapter<String>(getActivity(), R.layout.listitem_textview, readLog());
        mListView.setAdapter(mListAdapter);

//        mObserver = new FileObserver(mLogFile.toString(), FileObserver.MODIFY) {
//            @Override
//            public void onEvent(int event, String path) {
//
//            }
//        };
//        mObserver.startWatching();

        return root;
    }

    private List<String> readLog() {
        List<String[]> lineParts = mLogFile.read();
        List<String> lines = new ArrayList<String>(lineParts.size());
        for (String[] s : lineParts) {
            lines.add(TextUtils.join(",", s));
        }
        return lines;
    }

    private void updateListView() {
        mListAdapter.clear();
        mListAdapter.addAll(readLog());
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.bt, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bt_set_max:
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                final EditText input = (EditText) getActivity().getLayoutInflater().inflate(R.layout.edittext, null);
                input.setText(String.valueOf(mMaxConnections));
                input.setSelection(0, 1);
                alert.setView(input);
                alert.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setMaxConnections(Integer.valueOf(input.getText().toString()));
                    }
                });
                alert.setNegativeButton(R.string.dialog_cancel, null);
                alert.show();
                break;
            case R.id.bt_test:
                if (mBound) {
                    try {
                        mService.getConnectionManager().sendMessage(new com.essentiallocalization.connection.Message("Testing"));
                    } catch (IOException e) {

                    } catch (com.essentiallocalization.connection.Message.MessageTooLongException e) {

                    }
                }
                break;

            case R.id.bt_refresh_log:
                updateListView();
                break;
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.v(TAG, "onStart()");
        doBindService();
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.v(TAG, "onStop()");
        doUnbindService();
    }

    private void setServiceEnabled(boolean enabled) {
        Log.v(TAG, "setServiceEnabled(" + enabled + ")");
        if (mBound) {
            if (enabled) {
                mService.start();
            } else {
                mService.stop();
            }
        }
    }

    private void setServicePersist(boolean persist) {
        Log.v(TAG, "setServicePersist(" + persist + ")");
        Activity a = getActivity();
        if (persist) {
            a.startService(new Intent(a, BluetoothService.class));
        } else {
            a.stopService(new Intent(a, BluetoothService.class));
            // doBindService();
        }
    }

    private void doBindService() {
        getActivity().bindService(new Intent(getActivity(), BluetoothService.class), this, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        if (mBound) {
            getActivity().unbindService(this);
            mBound = false;
        }
    }

    private void setMaxConnections(int connections) {
        if (connections > BluetoothConnectionManager.MAX_CONNECTIONS) connections = BluetoothConnectionManager.MAX_CONNECTIONS;
        mMaxConnections = connections;
        for (int i = 0, value; i < mStateViews.length; ++i) {
            value = i < mMaxConnections ? View.VISIBLE : View.GONE;
            mStateViews[i].setVisibility(value);
        }
        if (mBound) {
            mService.setMaxConnections(mMaxConnections);
        }
    }

//    public void enableBluetooth(Activity activity) {
//        if (!mBluetoothAdapter.isEnabled()) {
//            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
//        }
//    }
//
//    /** Will enable Bluetooth if it is not already enabled. */
//    public void enableDiscoverability(Activity activity) {
//        if (!mBluetoothAdapter.isDiscovering()) {
//            //mDiscoveredDevices.clear(); // TODO this should be called in onActivityResult()
//            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), REQUEST_DISCOVERABLE);
//        }
//    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.v(TAG, "onServiceConnected()");
        PersistentIntentService.LocalBinder binder = (PersistentIntentService.LocalBinder) service;
        mService = (BluetoothService) binder.getService();
        if (mMaxConnections != mService.getMaxConnections()) {
            mService.setMaxConnections(mMaxConnections);
        }
        mService.setHandler(mHandler);
        mService.setLogFile(mLogFile);
        mBound = true;
        setupServiceControls();
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        Log.v(TAG, "onServiceDisconnected()");
        mBound = false;
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String name = null;
            int index = 0;
            if (msg.obj != null) {
                BluetoothConnection connection = (BluetoothConnection) msg.obj;
                index = mService.getConnectionManager().getConnections().indexOf(connection);
                name = connection.getName();
            }
            if (name == null || name.length() == 0) {
                name = String.valueOf(index);
            }
            switch (msg.what) {
                case BluetoothService.MSG_STATE_CHANGE:
//                    mStateViews[index].setVisibility(View.VISIBLE);
                    mStateViews[index].setText(name + ": " + BluetoothConnection.getState(msg.arg1));
                    break;
                case BluetoothService.MSG_SENT_PACKET:

                    break;

                case BluetoothService.MSG_SENT_MSG:

                    break;

                case BluetoothService.MSG_RECEIVED_PACKET:

                    break;

                case BluetoothService.MSG_RECEIVED_MSG:

                    break;

                case BluetoothService.MSG_CONFIRMED_PACKET:

                    break;

                case BluetoothService.MSG_CONFIRMED_MSG:

                    break;
            }
        }
    };
}
