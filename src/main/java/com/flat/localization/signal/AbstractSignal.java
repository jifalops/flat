package com.flat.localization.signal;


import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSignal implements Signal {
    private Set<SignalListener> listeners = new HashSet<SignalListener>(1);
    private final String name;

    protected AbstractSignal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void registerListener(SignalListener l) {
        listeners.add(l);
    }

    @Override
    public void unregisterListener(SignalListener l) {
        if (l == null) {
            listeners.clear();
        } else {
            listeners.remove(l);
        }
    }

    @Override
    public void notifyListeners(int eventType) {
        for (SignalListener l : listeners) {
            l.onChange(this, eventType);
        }
    }
}
