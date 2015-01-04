package com.flat.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flat.data.Controller;
import com.flat.data.Model;
import com.flat.localization.node.Node;
import com.flat.localization.algorithm.LocationAlgorithm;
import com.flat.localization.signal.Signal;

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
