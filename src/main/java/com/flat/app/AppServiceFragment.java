package com.flat.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flat.R;
import com.flat.util.app.PersistentIntentService;
import com.flat.util.app.PersistentIntentServiceFragment;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class AppServiceFragment extends PersistentIntentServiceFragment {
    AppService mService;

    @Override
    public void onServiceConnected(PersistentIntentService service) {
        mService = (AppService) service;
    }

    @Override
    protected Class<? extends PersistentIntentService> getServiceClass() {
        return AppService.class;
    }

    @Override
    protected void setServiceEnabled(boolean enabled) {
        if (!isBound()) return;
        setPersistent(enabled);
        if (enabled) {
            // TODO read prefs and use enabled signals/algs
        } else {

        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPersistControl(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.real_main, container, false);

        

        return layout;
    }
}
