package com.flat.localization.signals;


import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSignal implements Signal {
    private final String name;
    private int count = 0;

    protected AbstractSignal(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    public final int getChangeCount() {
        return count;
    }
    public final void notifyListeners(int eventType) {
        ++count;
        for (SignalListener l : listeners) {
            l.onChange(this, eventType);
        }
    }


    /**
     * Allow other objects to react to events.
     */
    private final List<SignalListener> listeners = new ArrayList<SignalListener>(1);
    public boolean registerListener(SignalListener l) {
        if (listeners.contains(l)) return false;
        return listeners.add(l);
    }
    public boolean unregisterListener(SignalListener l) {
        return listeners.remove(l);
    }
}
