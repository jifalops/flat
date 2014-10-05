package com.flat.localization.signal;

import android.content.Context;

import com.flat.localization.ranging.Ranging;

public interface Signal {
    /**
     * Enable updates from a Signal. Most will need to use a Context to access the signal source.
     * Additional arguments can be handled with method overloading.
     */
    void enable(Context ctx);
    void disable(Context ctx);
    boolean isEnabled();

    /*
     * Allow other objects to react to signal changes.
     */
    interface Listener {
        void onChange(Signal signal, int eventType);
    }
    void registerListener(Listener l);
    void unregisterListener(Listener l);
    void notifyListeners(int eventType);
}
