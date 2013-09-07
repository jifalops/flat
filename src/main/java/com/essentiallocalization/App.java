package com.essentiallocalization;

import android.app.Application;

import com.essentiallocalization.util.Jog;

/**
 * Created by Jake on 8/29/13.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Jog.setTag(getPackageName());
        Jog.setLogLevel(Jog.VERBOSE);
        Jog.setToastLevel(Jog.VERBOSE);
    }
}
