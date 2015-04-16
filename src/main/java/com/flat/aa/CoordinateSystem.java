package com.flat.aa;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Jacob Phillips.
 */
public class CoordinateSystem {
    final TreeMap<String, State> nodes = new TreeMap<String, State>();
    public State getState(String node) { return nodes.get(node); }
    public Collection<State> getStates() { return nodes.values(); }
    public Set<String> getNodeIds() { return nodes.keySet(); }
    public State setState(State state) { return nodes.put(state.id, state); }

    public CoordinateSystem() {}
    public CoordinateSystem(String jsonArray) {
        try {
            JSONArray json = new JSONArray(jsonArray);
            for (int i=0; i < json.length(); ++i) {
                setState(State.from(json.getString(i)));
            }
        } catch (JSONException ignored) {}
    }

    @Override
    public String toString() {
        return new JSONArray(nodes.values()).toString();
    }
}
