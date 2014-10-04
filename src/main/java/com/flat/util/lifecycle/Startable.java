package com.flat.util.lifecycle;

/**
 * Something that can be started only once, i.e. a Thread.
 */
public interface Startable extends Cancelable {
    void start();

    static interface StartListener extends CancelListener{
        void onStart();
    }
}
