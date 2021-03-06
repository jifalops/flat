package com.flat.util;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.flat.R;

/**
 * Created by Jake on 2/6/14.
 */
public abstract class PersistentIntentServiceFragment extends Fragment {
    private static final String TAG = PersistentIntentServiceFragment.class.getSimpleName();

    private PersistentIntentService mService;

    /** Whether a service is bound to the Fragment */
    private volatile boolean mBound;

    abstract public void onServiceConnected(PersistentIntentService service);
    protected abstract Class<? extends PersistentIntentService> getServiceClass();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupServiceControls();
    }

    @Override
    public void onStart() {
        super.onStart();
        doBindService();
    }


    @Override
    public void onStop() {
        super.onStop();
        doUnbindService();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isBound()) {
            bindServiceControls(mService);

        }
    }

    private void doBindService() {
        Activity a = getActivity();
        if (a != null) {
            a.bindService(new Intent(a, getServiceClass()), mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void doUnbindService() {
        Activity a = getActivity();
        if (mBound && a != null) {
            a.unbindService(mServiceConnection);
            mBound = false;
        }
    }

    protected final void setPersistent(boolean persist) {
        Activity a = getActivity();
        if (a != null) {
            Intent i = new Intent(a, getServiceClass());
            if (persist) {
                a.startService(i);
            } else {
                a.stopService(i);
            }
        }
    }

    public boolean isBound() {
        return mBound;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public final void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            mService = ((PersistentIntentService.LocalBinder) service).getService();
            PersistentIntentServiceFragment.this.onServiceConnected(mService);
            bindServiceControls(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    // ==========================================================================================
    // Service Controls
    //

    private Switch mServiceSwitch;
    private CheckBox mServicePersist;

    private void setupServiceControls() {
        ActionBar ab = getActivity().getActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        ab.setCustomView(R.layout.persistent_intent_service_controls);
        View controls = ab.getCustomView();
        mServiceSwitch = (Switch) controls.findViewById(R.id.service_power);
        mServicePersist = (CheckBox) controls.findViewById(R.id.service_persist);
    }

    private void bindServiceControls(final PersistentIntentService service) {
        if (isBound()) {
            mServiceSwitch.setOnCheckedChangeListener(null);
            mServiceSwitch.setChecked(service.isEnabled());

            mServicePersist.setOnCheckedChangeListener(null);
            mServicePersist.setChecked(service.isPersistent());
        }
        mServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                service.setEnabled(isChecked);
                onServiceEnabled(isChecked);
            }
        });
        mServicePersist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setPersistent(isChecked);
            }
        });
    }

    protected abstract void onServiceEnabled(boolean enabled);

    public void showPersistControl(boolean show) {
        int v = show ? View.VISIBLE : View.GONE;
        mServicePersist.setVisibility(v);
    }

    public void showSwitchControl(boolean show) {
        int v = show ? View.VISIBLE : View.GONE;
        mServiceSwitch.setVisibility(v);
    }
}
