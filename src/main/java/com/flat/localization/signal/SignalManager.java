package com.flat.localization.signal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flat.localization.signal.interpreters.SignalInterpreter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class SignalManager {

    /** All Signals and which ranging algorithms they can use */
    private final Map<Signal, List<SignalInterpreter>> signals = Collections.synchronizedMap(new LinkedHashMap<Signal, List<SignalInterpreter>>());
    private final SharedPreferences prefs;

    public SignalManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setEnabledPreference(Signal s, boolean enabled) {
        prefs.edit().putBoolean(s.getName(), enabled).apply();
    }

    public boolean getEnabledPreference(Signal s) {
        return prefs.getBoolean(s.getName(), false);
    }

    public void enable(Context ctx) {
        for (Signal s : signals.keySet()) {
            if (getEnabledPreference(s)) {
                s.enable(ctx);
            }
        }
    }

    public void disable(Context ctx) {
        for (Signal s : signals.keySet()) {
            s.disable(ctx);
        }
    }

    public int getSignalCount() {
        return signals.size();
    }
    public Set<Signal> getSignals() {
        return signals.keySet();
    }
    public List<SignalInterpreter> getInterpreters(Signal signal) {
        return signals.get(signal);
    }
    public void addSignal(Signal signal, List<SignalInterpreter> processors) {
        signals.put(signal, processors);
    }
}
