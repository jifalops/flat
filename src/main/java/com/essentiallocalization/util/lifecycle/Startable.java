package com.essentiallocalization.util.lifecycle;

/**
 * Created by Jake on 1/27/14.
 */
public interface Startable extends Cancelable {
    void start();

    static interface Listener extends Cancelable.Listener {
        void onStart();
    }
}
