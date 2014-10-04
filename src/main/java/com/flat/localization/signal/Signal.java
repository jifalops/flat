package com.flat.localization.signal;

public interface Signal {
    int TYPE_INTERNAL = 1;
    int TYPE_ELECTROMAGNETIC = 2;
    int TYPE_MECHANINCAL = 3;
    int getSignalType();

    /** Enable updates from signal */
    void enable(Object... args);
    void disable(Object... args);
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
