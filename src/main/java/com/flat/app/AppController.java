package com.flat.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.nsd.NsdServiceInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.flat.localization.Controller;
import com.flat.localization.Model;
import com.flat.localization.node.Node;
import com.flat.localization.algorithm.LocationAlgorithm;
import com.flat.localization.signal.Signal;
import com.flat.nsd.NsdHelper;
import com.flat.nsd.sockets.MyConnectionSocket;
import com.flat.nsd.sockets.MyServerSocket;
import com.flat.nsd.sockets.MySocketManager;
import com.flat.nsd.sockets.Sockets;

import java.net.ServerSocket;
import java.net.Socket;

public class AppController extends Application {

	public static final String TAG = AppController.class.getSimpleName();



	private static AppController sInstance;
    public static synchronized AppController getInstance() {
        return sInstance;
    }

    // Main power switch in AppServiceFragment
    private boolean enabled;

    private SharedPreferences prefs;

    private Controller controller;
    private Model model;




	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        controller = Controller.getInstance(this); // will populate the model.
        model = Model.getInstance();
        model.registerListener(modelListener); // TODO need to unregister?


    }





    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (enabled) {
            enableNsd();
            for (Signal s : model.getSignals()) {
                if (prefs.getBoolean(s.getName(), false)) {
                    s.enable(this);
                }
            }
            for (LocationAlgorithm la : model.getAlgorithms()) {
                if (prefs.getBoolean(la.getName(), false)) {
                    la.setEnabled(true);
                }
            }
        } else {
            disableNsd();
            for (Signal s : model.getSignals()) {
                s.disable(this);
            }
            for (LocationAlgorithm la : model.getAlgorithms()) {
                la.setEnabled(false);
            }
        }
    }

    private final Model.ModelListener modelListener = new Model.ModelListener() {
        @Override
        public void onNodeAdded(Node n) {
            n.readPrefs(prefs);
        }
    };



}
