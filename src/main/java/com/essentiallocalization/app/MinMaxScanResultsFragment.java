package com.essentiallocalization.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.essentiallocalization.R;
import com.essentiallocalization.util.CsvBuffer;
import com.essentiallocalization.util.app.PersistentIntentService;
import com.essentiallocalization.util.app.PersistentIntentServiceFragment;
import com.essentiallocalization.util.wifi.ScanResultsService;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MinMaxScanResultsFragment extends PersistentIntentServiceFragment implements ScanResultsService.Callback {
    private static final String TAG = MinMaxScanResultsFragment.class.getSimpleName();
    private static final String LOG_FILE = "ScanResults.txt";

    /** Interface the containing activity must implement */
    public static interface Callback {
        //nothing yet
    }

    private ScanResultsService mService;
    private Callback mCallback;
    private TextView mTextView;

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
        if (isBound()) {
            mTextView.append(mService.getScanResultHeader());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onScanResults(List<ScanResult> results) {
        if (!isBound()) return;
        final CsvBuffer buffer = new CsvBuffer();
        int i = 0;
        for (ScanResult sr : results) {
            buffer.add(mService.formatScanResult(++i, sr));
        }
        mTextView.post(new Runnable() {
            @Override
            public void run() {
                mTextView.append(buffer.toString() + "\n");
            }
        });
        for (ScanResult sr : results) {
            // TODO make bounding box for each result
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.scan_results, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_log_scans:
                if (isBound()) {
                    if (mService.isLogging()) {
                        try {
                            mService.setLogging(null);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to stop logging scans.");
                        }
                    } else {
                        try {
                            mService.setLogging(new File(getActivity().getFilesDir(), LOG_FILE), true);
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
            case R.id.action_settings:
                //todo
                break;
        }
        return true;
    }


    @Override
    public void onServiceConnected(PersistentIntentService service) {
        mService = (ScanResultsService) service;
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
            mService.startScanning(1000);
        } else {
            mService.stopScanning();
        }
    }


}
