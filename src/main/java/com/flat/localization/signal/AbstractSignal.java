package com.flat.localization.signal;


import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSignal implements Signal {
    private Set<Listener> listeners = new HashSet<Listener>(1);
    protected boolean enabled;

    @Override
    public void registerListener(Listener l) {
        listeners.add(l);
    }

    @Override
    public void unregisterListener(Listener l) {
        if (l == null) {
            listeners.clear();
        } else {
            listeners.remove(l);
        }
    }

    @Override
    public void notifyListeners(int eventType) {
        for (Listener l : listeners) {
            l.onChange(this, eventType);
        }
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    /** Remember to set {@code enabled = true} */
    public abstract void enable(Object... args);

    /** Remember to set {@code enabled = false} */
    public abstract void disable(Object... args);
}
