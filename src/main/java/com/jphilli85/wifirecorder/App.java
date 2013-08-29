package com.jphilli85.wifirecorder;

import android.app.Application;

import com.jphilli85.wifirecorder.util.Jog;

/**
 * Created by Jake on 8/29/13.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Jog.setLogLevel(Jog.VERBOSE);
        Jog.setToastLevel(Jog.VERBOSE);
    }
}
