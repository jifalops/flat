package com.essentiallocalization.util.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Created by Jake on 2/6/14.
 */
public abstract class PersistentIntentServiceController extends Fragment implements ServiceConnection {
    private static final String TAG = PersistentIntentServiceController.class.getSimpleName();

    /** Whether the service is bound to a Fragment/Activity */
    private boolean mBound;

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

    protected void doBindService() {
        getActivity().bindService(new Intent(getActivity(), getServiceClass()), this, Context.BIND_AUTO_CREATE);
    }

    protected void doUnbindService() {
        if (mBound) {
            getActivity().unbindService(this);
            mBound = false;
        }
    }

    protected void setPersistent(boolean persist) {
        Activity a = getActivity();
        if (persist) {
            a.startService(new Intent(a, getServiceClass()));
        } else {
            a.stopService(new Intent(a, getServiceClass()));
            // doBindService();
        }
    }

    @Override
    public final void onServiceConnected(ComponentName name, IBinder service) {
        mBound = true;
        onServiceConnected(((PersistentIntentService.LocalBinder) service).getService());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBound = false;
    }

    abstract public void onServiceConnected(PersistentIntentService service);
    protected abstract Class<? extends PersistentIntentService> getServiceClass();

    public boolean isBound() {
        return mBound;
    }
}
