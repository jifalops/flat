package com.flat.app;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flat.R;
import com.flat.sensors.InertialMovement;
import com.flat.sensors.MovementSensorService;
import com.flat.util.app.PersistentIntentService;
import com.flat.util.app.PersistentIntentServiceFragment;

public class MovementSensorFragment extends PersistentIntentServiceFragment {
    private static final String TAG = MovementSensorFragment.class.getSimpleName();
    private static final String LOG_FILE = "MovementResults.txt";

    /** Interface the containing activity must implement */
    public static interface Callback {
        //nothing yet
    }

    private MovementSensorService mService;
    private Callback mCallback;
    private TextView mTextView;
    private double mTime;

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
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setHasOptionsMenu(true);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.wifi_beacon_main, container, false);
        mTextView = (TextView) root.findViewById(R.id.text);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isBound()) {
            //
        }
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.wifi_beacon, menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_log_scans:
//                if (isBound()) {
//                    if (mService.isLogging()) {
//                        try {
//                            mService.setLogging(null);
//                        } catch (IOException e) {
//                            Log.e(TAG, "Failed to stop logging scans.");
//                        }
//                    } else {
//                        try {
//                            mService.setLogging(new File(getActivity().getFilesDir(), LOG_FILE), true);
//                        } catch (IOException e) {
//                            Log.e(TAG, "Failed to start logging scans.");
//                        }
//                    }
//                }
//                break;
//            case R.id.action_scan_interval:
//                int t = 0;
//                if (isBound()) t = mService.getScanTimerPeriod();
//                final int period = t;
//                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//                EditText edit = new EditText(getActivity());
//                edit.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//                edit.setText(String.valueOf(period));
//                edit.setSelectAllOnFocus(true);
//                builder.setView(edit);
//                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        if (isBound()) mService.startScanning(period);
//                    }
//                });
//                builder.show();
//                edit.requestFocus();
//            case R.id.action_settings:
//                //todo
//                break;
//        }
//        return true;
//    }

    InertialMovement.MovementListener mListener = new InertialMovement.MovementListener() {
        @Override
        public void onMovement(double[] pos, float angle[], double time) {
            mTime += time;
            mTextView.setText(String.format("%.0f: P{%.3f, %.3f, %.3f} θ{%.1f, %.1f, %.1f}\n", mTime,
                    pos[0], pos[1], pos[2], angle[0] * 57.2957795, angle[1] * 57.2957795, angle[2] * 57.2957795));
        }
    };


    @Override
    public void onServiceConnected(PersistentIntentService service) {
        mService = (MovementSensorService) service;
        mService.getMovementSensor().registerMovementListener(mListener);
    }

    @Override
    protected Class<? extends PersistentIntentService> getServiceClass() {
        return MovementSensorService.class;
    }

    @Override
    protected void setServiceEnabled(boolean enabled) {
        if (!isBound()) return;
        if (enabled) {
            mService.startSensing();
        } else {
            mService.stopSensing();
        }
    }


}
