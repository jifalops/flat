package com.flat.bluetoothtimeofflight.lifecycle;

/**
 * Something that can be started more than once.
 */
public interface Restartable {
    void start();
    void stop();

    static interface StartStopListener {
        void onStart();
        void onStop();
    }
}
