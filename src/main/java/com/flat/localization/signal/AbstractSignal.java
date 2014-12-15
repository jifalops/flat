package com.flat.localization.signal;


import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSignal implements Signal {
    private final Set<SignalListener> listeners = new HashSet<SignalListener>(1);
    private final String name;
    private int count = 0;

    protected AbstractSignal(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final void registerListener(SignalListener l) {
        listeners.add(l);
    }

    @Override
    public final void unregisterListener(SignalListener l) {
        if (l == null) {
            listeners.clear();
        } else {
            listeners.remove(l);
        }
    }

    @Override
    public final int getChangeCount() {
        return count;
    }

    @Override
    public final void notifyListeners(int eventType) {
        ++count;
        for (SignalListener l : listeners) {
            l.onChange(this, eventType);
        }
    }
}
