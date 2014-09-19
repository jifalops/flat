package com.essentiallocalization.util.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.essentiallocalization.R;
import com.essentiallocalization.util.CsvBuffer;
import com.essentiallocalization.util.Util;
import com.essentiallocalization.util.app.PersistentIntentService;
import com.essentiallocalization.util.app.PersistentIntentServiceFragment;

import java.io.IOException;
import java.util.List;

public class ScanResultsFragment extends PersistentIntentServiceFragment implements ScanResultsService.Callback {
    private static final String TAG = ScanResultsFragment.class.getSimpleName();


    /** Interface the containing activity must implement */
    public static interface Callback {
        //nothing yet
    }

    private ScanResultsService mService;
    private Callback mCallback;

    private ScanResultsConfig mConfig;
    private int mScanCount;
    private TextView mTextView;
    private MenuItem mLogToggle;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.v(TAG, "onAttach()");
        try {
            mCallback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(TAG + ": Activity must implement the fragment's callbacks.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        mConfig = ScanResultsConfig.getInstance();
        setHasOptionsMenu(true);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.scan_results_view, container, false);
        mTextView = (TextView) root.findViewById(R.id.text);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (isBound()) {
            mTextView.append("Session start: " + Util.Format.LOG.format(mConfig.getStartTime()) + "\n");
            mTextView.append(TextUtils.join(", ", mConfig.getScanResultHeader()) + "\n");
//        }
    }

    /** {@inheritDoc} */
    @Override
    public void onScanResults(List<ScanResult> results) {
        if (!isBound()) return;
        final CsvBuffer buffer = new CsvBuffer();
        for (ScanResult sr : results) {
            buffer.add(mConfig.formatScanResult(++mScanCount, mService.getTimerDelay(), sr));
        }
        mTextView.post(new Runnable() {
            @Override
            public void run() {
                mTextView.append(buffer.toString() + "\n");
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.scan_results, menu);
        mLogToggle = menu.findItem(R.id.action_log_scans); // TODO not used
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_log_scans:
                if (isBound()) {
                    if (mService.isLogging()) {
                        Toast.makeText(getActivity(), "logging is enabled, disabling...", Toast.LENGTH_SHORT).show();
                        try {
                            mService.setLogging(null);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to stop logging scans.");
                        }
                    } else {
                        Toast.makeText(getActivity(), "logging is disabled, enabling...", Toast.LENGTH_SHORT).show();
                        try {
                            mService.setLogging(mConfig.getLogFile(getActivity()), true);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to start logging scans.");
                        }
                    }
                }
                break;
            case R.id.action_scan_interval:
                int t = 0;
                if (isBound()) t = mService.getScanTimerPeriod();
                final int period = t;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                EditText edit = new EditText(getActivity());
                edit.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                edit.setText(String.valueOf(period));
                edit.setSelectAllOnFocus(true);
                builder.setView(edit);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (isBound()) mService.startScanning(period);
                    }
                });
                builder.show();
                edit.requestFocus();
                break;

            case R.id.action_view_log:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.fromFile(mConfig.getLogFile(getActivity())), "text/*");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                break;

            case R.id.action_clear_log:
                if (isBound()) {
                    try {
                        boolean isLogging = mService.isLogging();
                        mService.setLogging(mConfig.getLogFile(getActivity()), false); //truncate
                        if (!isLogging) {
                            mService.setLogging(null);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to clear log.");
                    }
                }
                break;

            case R.id.action_settings:
                //todo
                Toast.makeText(getActivity(), "settings not available", Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }


    @Override
    public void onServiceConnected(PersistentIntentService service) {
        mService = (ScanResultsService) service;
        mScanCount = mService.getScanCount();
        mService.setCallback(this);
    }

    @Override
    protected Class<? extends PersistentIntentService> getServiceClass() {
        return ScanResultsService.class;
    }

    @Override
    protected void setServiceEnabled(boolean enabled) {
        if (!isBound()) return;
        if (enabled) {
            mService.registerReceiver(new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mService.startScanning(ScanResultsConfig.DEFAULT_RANDOM_MIN, ScanResultsConfig.DEFAULT_RANDOM_MAX);
        } else {
            mService.stopScanning();
        }
    }


}
