package com.flat.localization;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flat.localization.algorithms.Algorithm;
import com.flat.localization.algorithms.AlgorithmMatchCriteria;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public final class AlgorithmManager {
    /**
     * Location algorithms that can be applied to each node.
     */
    private final Map<Algorithm, AlgorithmMatchCriteria> algorithms =
            Collections.synchronizedMap(new LinkedHashMap<Algorithm, AlgorithmMatchCriteria>());

    private final SharedPreferences prefs;

    public AlgorithmManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setEnabledPreference(Algorithm la, boolean enabled) {
        prefs.edit().putBoolean(la.getName(), enabled).apply();
    }

    public boolean getEnabledPreference(Algorithm la) {
        return prefs.getBoolean(la.getName(), false);
    }

    public void enable() {
        for (Algorithm la : algorithms.keySet()) {
            if (getEnabledPreference(la)) {
                la.setEnabled(true);
            }
        }
    }

    public void disable() {
        for (Algorithm la : algorithms.keySet()) {
            la.setEnabled(false);
        }
    }


    public int getAlgorithmCount() {
        return algorithms.size();
    }

    public Algorithm[] getAlgorithms() {
        return algorithms.keySet().toArray(new Algorithm[algorithms.size()]);
    }

    public AlgorithmMatchCriteria getCriteria(Algorithm la) {
        return algorithms.get(la);
    }

    public void addAlgorithm(Algorithm la, AlgorithmMatchCriteria amc) {
        algorithms.put(la, amc);
    }



    public void registerListener(Algorithm.AlgorithmListener l) {
        for (Algorithm la : algorithms.keySet()) {
            la.registerListener(l);
        }
    }
    public void unregisterListener(Algorithm.AlgorithmListener l) {
        for (Algorithm la : algorithms.keySet()) {
            la.unregisterListener(l);
        }
    }
}
