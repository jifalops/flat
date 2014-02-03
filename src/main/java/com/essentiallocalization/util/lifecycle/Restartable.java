package com.essentiallocalization.util.lifecycle;

/**
 * Something that can be started more than once.
 */
public interface Restartable {
    void start();
    void stop();

    static interface Listener {
        void onStart();
        void onStop();
    }
}
