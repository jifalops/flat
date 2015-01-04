package com.flat.localization.algorithm;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class LocationAlgorithmManager {
    /**
     * Location algorithms that can be applied to each node.
     */
    private final Map<LocationAlgorithm, Criteria.AlgorithmMatchCriteria> algorithms =
            Collections.synchronizedMap(new LinkedHashMap<LocationAlgorithm, Criteria.AlgorithmMatchCriteria>());

    private final SharedPreferences prefs;

    public LocationAlgorithmManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setEnabledPreference(LocationAlgorithm la, boolean enabled) {
        prefs.edit().putBoolean(la.getName(), enabled).apply();
    }

    public boolean getEnabledPreference(LocationAlgorithm la) {
        return prefs.getBoolean(la.getName(), false);
    }

    public void enable() {
        for (LocationAlgorithm la : algorithms.keySet()) {
            if (getEnabledPreference(la)) {
                la.setEnabled(true);
            }
        }
    }

    public void disable() {
        for (LocationAlgorithm la : algorithms.keySet()) {
            la.setEnabled(false);
        }
    }


    public int getAlgorithmCount() {
        return algorithms.size();
    }

    public Set<LocationAlgorithm> getAlgorithms() {
        return algorithms.keySet();
    }

    public Criteria.AlgorithmMatchCriteria getCriteria(LocationAlgorithm la) {
        return algorithms.get(la);
    }

    public void addAlgorithm(LocationAlgorithm la, Criteria.AlgorithmMatchCriteria amc) {
        algorithms.put(la, amc);
    }
}
