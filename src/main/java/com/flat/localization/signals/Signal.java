package com.flat.localization.signals;

import android.content.Context;

/**
 * A signal is any measurable input from an internal or external source.
 */
public interface Signal {
    /**
     * Each signal must have it's own unique name.
     * TODO physical, class, or instance unique?
     */
    String getName();
    int getChangeCount();

    /**
     * Enable updates from a Signal. Most will need to use a Context to access the signal source.
     * Additional arguments can be handled with method overloading.
     */
    void enable(Context ctx);
    void disable(Context ctx);
    boolean isEnabled();

    boolean registerListener(SignalListener signalListener);
    boolean unregisterListener(SignalListener signalListener);

    /*
     * Allow other objects to react to signal changes.
     */
    interface SignalListener {
        void onChange(Signal signal, int eventType);
    }
}
