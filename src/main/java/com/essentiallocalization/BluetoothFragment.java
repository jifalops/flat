package com.essentiallocalization;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
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

import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.bluetooth.BluetoothConnection;
import com.essentiallocalization.connection.bluetooth.BluetoothConnectionManager;
import com.essentiallocalization.util.app.PersistentIntentService;
import com.essentiallocalization.util.app.PersistentIntentServiceController;
import com.essentiallocalization.util.io.Connection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jake on 9/1/13.
 */
public final class BluetoothFragment extends PersistentIntentServiceController {
    private static final String TAG = BluetoothFragment.class.getSimpleName();

    private static final String LOG_NAME = "write.csv";
    private static final File LOG_FILE = new File(Environment.getExternalStorageDirectory(), LOG_NAME);

    private static final String TEST_MSG_8   = "Testing.";
    private static final String TEST_MSG_256 = "TW5C0IpW2fWeDBfrqoaPp028JCRcuFdaCoN65e4LX8YhlQ6QQfRLMotNUJCTQ91pH8fk1Y56RlGaAHMsd25DZOmESoQJ9ezB67T8Zu4fzhUKfm78xvOrBcrjBTpAlCr3eUjW2m9CMEZfoyU9Kl3bzHDSswlFT8kM0o12SRkkPNvzMT2bTUMr1epmOoieEDHtZdBYAZPKsL6I6l8EWQhrlxyUwFlqtgP2GJXWEMvQ04bUlWFeqd67Lp8xKuTKH9rH";
    private static final String TEST_MSG     = TEST_MSG_8;
    private static final int TEST_MULTIPLIER   = 5;

    /** Interface the containing activity must implement */
    static interface BluetoothFragmentListener {
        void onRequestBluetoothEnabled();
        void onRequestDiscoverable();
        void onBluetoothSupported(boolean supported);
    }



    // Intent request codes
//    private static final int REQUEST_ENABLE_BT = 1;
//    private static final int REQUEST_DISCOVERABLE = 2;

    private BluetoothConnectionService mService;

    private int mMaxConnections = 4;

    /** Contains callback methods for this class. */
    private BluetoothFragmentListener mListener;

    private ViewGroup mStateViewsContainer;
    private SparseArray<TextView> mStateViews;
    private Switch mServiceSwitch;
    private CheckBox mServicePersist;
    private ListView mListView;
    private ArrayAdapter<String> mListAdapter;

    private TimingLog mTimeLog;

    private final Handler mUiHandler = new Handler();


//    private FileObserver mObserver;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.v(TAG, "onAttach()");
        try {
            mListener = (BluetoothFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(TAG + ": Activity must implement the fragment's listener.");
        }
        mListener.onBluetoothSupported(BluetoothConnection.SELF != null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        setHasOptionsMenu(true);

        // Setup Actionbar
        final ActionBar ab = getActivity().getActionBar();
        if (BluetoothConnection.SELF_NAME != null) ab.setTitle(BluetoothConnection.SELF_NAME);
        ab.setDisplayOptions(ab.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        ab.setCustomView(R.layout.service_controls);
        View controls = ab.getCustomView();
        mServiceSwitch = (Switch) controls.findViewById(R.id.service_power);
        mServicePersist = (CheckBox) controls.findViewById(R.id.service_persist);
        mStateViews = new SparseArray<TextView>(BluetoothConnectionManager.MAX_CONNECTIONS);
        initTimeLog();
    }

    private void initTimeLog() {
        try {
            if (mTimeLog != null) {
                mTimeLog.close();
            }
            mTimeLog = new TimingLog(LOG_FILE, true, new Runnable() {
                @Override
                public void run() {
                    // initialized
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Couldn't open write file.", e);
            //Toast.makeText(this, "Logging disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupServiceControls() {
        if (isBound()) {
            mServiceSwitch.setOnCheckedChangeListener(null);
            mServiceSwitch.setChecked(mService.getConnectionManager().hasDevice());

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
                setPersistent(isChecked);
            }
        });
    }

    private void setServiceEnabled(boolean enabled) {
        if (!isBound()) return;
        if (enabled) {
            mService.setConnectionListener(mConnectionListener);
            mService.setTimingLog(mTimeLog);
            mService.getConnectionManager().setReconnect(true);

            try {
                mService.getConnectionManager().startSnoopReader();
            } catch (IOException e) {
                Log.e(TAG, "Failed to start snoop packet reader");
            }

            // Attempt connections to all paired devices!
            for (BluetoothDevice device : BluetoothConnection.SELF.getBondedDevices()) {
                makeStateView(device);
                mService.getConnectionManager().connect(device);
            }

        } else {

            mService.getConnectionManager().setReconnect(false);
            mService.getConnectionManager().stopSnoopReader();
            mService.getConnectionManager().disconnect();
            try {
                mService.setTimingLog(null);
                mTimeLog.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close Time write");
            }
//            mService.setTimingLog(null, null);
//            mService.setConnectionListener(null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.fragment_bt, container, false);

        // Add all connection state views to the layout,
        // hiding views which are beyond the current max connections.
        mStateViewsContainer = (ViewGroup) root.findViewById(R.id.states);

//        for (int i = 0; i < BluetoothConnectionManager.MAX_CONNECTIONS; ++i) {
//            mStateViews[i] = (TextView) inflater.inflate(R.layout.state_textview, null);
//            mStateViews[i].setVisibility(View.GONE);
//            states.addView(mStateViews[i]);
//        }

        mListView = (ListView) root.findViewById(R.id.list);
        mListAdapter = new ArrayAdapter<String>(getActivity(), R.layout.listitem_textview, R.id.text, readLog());
        mListView.setAdapter(mListAdapter);

//        mObserver = new FileObserver(mTimeLog.toString(), FileObserver.MODIFY) {
//            @Override
//            public void onEvent(int event, String path) {
//
//            }
//        };
//        mObserver.startWatching();

        return root;
    }

    private void makeStateView(BluetoothDevice device) {
        final TextView tv = (TextView) getActivity().getLayoutInflater().inflate(R.layout.state_textview, null);
        byte dest = BluetoothConnection.idFromName(device.getName());
        mStateViewsContainer.removeView(mStateViews.get(dest));
        mStateViews.put(dest, tv);
        mStateViewsContainer.addView(tv);
    }

    private List<String> readLog() {
        List<String> lines = new ArrayList<String>();
        List<String[]> lineParts = null;
//        try {
            lineParts = mTimeLog.getAll();
            for (String[] s : lineParts) {
                lines.add(TextUtils.join(",", s));
            }
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to read write");
//        }
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
                        if (isBound()) {
                            mService.getConnectionManager().setMaxConnections(Integer.valueOf(input.getText().toString()));
                        }
                    }
                });
                alert.setNegativeButton(R.string.dialog_cancel, null);
                alert.show();
                break;
            case R.id.bt_test:
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        for (int i = 0; i < TEST_MULTIPLIER; ++i) {
                            if (isBound()) {
                                try {
                                    mService.getConnectionManager().sendToAll(TEST_MSG);
                                } catch (IOException e) {
                                    Log.e(TAG, "Failed sending test message to all devices");
                                } catch (com.essentiallocalization.connection.Message.MessageTooLongException ignored) {}
                            }
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "Thread interrupted when sending multiple tests.");
                            }
                        }
                        return null;
                    }
                }.execute();

                break;

            case R.id.bt_reset_hci:
                //mService.resetSnoopFile();
                break;

            case R.id.bt_refresh_log:
                updateListView();
                break;

            case R.id.bt_clear_log:
//                try {
                    initTimeLog();
//                } catch (IOException e) {
//                    Log.e(TAG, "Failed to clear write");
//                }
                updateListView();
                break;

            case R.id.bt_view_log:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.fromFile(LOG_FILE), "text/*");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                break;
        }
        return true;
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
    public void onServiceConnected(PersistentIntentService service) {
        mService = (BluetoothConnectionService) service;
        if (mMaxConnections != mService.getConnectionManager().getMaxConnections()) {
            mService.getConnectionManager().setMaxConnections(mMaxConnections);
        }
        setupServiceControls();
    }

    @Override
    protected Class<? extends PersistentIntentService> getServiceClass() {
        return BluetoothConnectionService.class;
    }

    private final BluetoothConnectionManager.BluetoothConnectionListener mConnectionListener =
            new BluetoothConnectionManager.BluetoothConnectionListener() {
                @Override
                public synchronized void onPacketReceived(DataPacket dp, BluetoothConnection conn) {
//                    try {
//                        Toast.makeText(getActivity(), dp.src + " (" + dp.pktIndex + "): " +
//                                new String(dp.payload, "UTF-8"), Toast.LENGTH_SHORT).show();
//                    } catch (UnsupportedEncodingException e) {
//                        Log.e(TAG, "Failed to encode packet payload");
//                    }
                }

                @Override
                public synchronized void onTimingComplete(final DataPacket dp, BluetoothConnection conn) {
//                    final double javaDist = Util.Calc.timeOfFlightDistanceNano(dp.javaSrcSent, dp.javaDestReceived, dp.javaDestSent, dp.javaSrcReceived);
//                    final double hciDist = Util.Calc.timeOfFlightDistanceMicro(dp.hciSrcSent, dp.hciDestReceived, dp.hciDestSent, dp.hciSrcReceived);
//                    mUiHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getActivity(), "packet " + dp.pktIndex + " to " + dp.dest + ":\n"
//                                    + "java: " + Util.Format.SEPARATOR_2DEC.format(javaDist) + "\n"
//                                    + "hci: " + Util.Format.SEPARATOR_2DEC.format(hciDist), Toast.LENGTH_LONG).show();
//                        }
//                    });
                }

                @Override
                public synchronized void onStateChanged(final BluetoothDevice device, final int oldState, final int newState) {
                    if (device == null) return;

                    final byte dest = BluetoothConnection.idFromName(device.getName());

                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mStateViews.get(dest).setText(device.getName() + ": " + getState(newState));
                        }
                    });
                }

                @Override
                public synchronized void onSnoopPacketReaderFinished() {
                    Log.d(TAG, "onSnoopPacketReaderFinished");
                }
    };

    private String getState(int state) {
        switch (state) {
            case Connection.STATE_NONE:         return "None";
            case Connection.STATE_CONNECTING:   return "Connecting";
            case Connection.STATE_CONNECTED:    return "Connected";
            case Connection.STATE_DISCONNECTED: return "Disconnected";
        }
        return "Unknown";
    }
}
