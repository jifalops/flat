package com.essentiallocalization.util.lifecycle;

/**
 * Created by Jake on 1/27/14.
 */
public interface Restartable {
    void start();
    void stop();

    static interface Listener {
        void onStart();
        void onStop();
    }
}
