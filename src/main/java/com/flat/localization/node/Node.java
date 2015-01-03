package com.flat.localization.node;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Pair;

import com.flat.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Nodes are the main objects to be manipulated in the localization system and they are
 * designed here to facilitate being localized.
 */
public final class Node {

    /**
     * Ranging provides the primary input to the localization system. A range is created by
     * interpreting an external signal such as a wifi beacon.
     *
     * {@link com.flat.localization.signal.Signal}
     * {@link com.flat.localization.signal.rangingandprocessing.SignalInterpreter}
     */
    public static final class Range {
        public float range = 0;
        public float rangeOverride = 0; // when given
        public String signal = "none";
        public String interpreter = "none";
        public long time = System.currentTimeMillis();
        @Override public String toString() { return String.format("%.2fm (overridden to %.2fm)", range, rangeOverride); }
    }

    /**
     *
     * <pre>
     * Position:
     * 0:       x position
     * 1:       y position
     * 2:       z position
     *
     * Angle:
     * 0:       angle with respect to left-right axis on a vertically held phone.
     * 1:       angle with respect to the up-down axis on a vertically held phone.
     * 2:       angle with respect to the forward-back axis on a vertically held phone.
     * </pre>
     * For orientation of angle coordinates, see
     * <a href='http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords'>http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords</a>
     */
    public static final class State {
        public RangeTable[] referenceFrame;
        public float pos[] = {0,0,0};
        public float angle[] = {0,0,0};
        public String algorithm = "none";
        public long time = System.currentTimeMillis();

        @Override
        public String toString() {
            String x,y,z;
            x = Util.Format.SCIENTIFIC_3SIG.format(pos[0]);
            y = Util.Format.SCIENTIFIC_3SIG.format(pos[1]);
            z = Util.Format.SCIENTIFIC_3SIG.format(pos[2]);
            return String.format("(%s, %s, %s), (%.0f, %.0f, %.0f)",
                    x,y,z, angle[0], angle[1], angle[2]);
        }
    }

    public static final class RangeTable {
        // Node ID, Range, Timestamp
        public final Map<String, Pair<Float, Long>> ranges = Collections.synchronizedMap(new HashMap<String, Pair<Float, Long>>());
        // Node ID of owner
        public final String id;
        public RangeTable(String id) { this.id = id; }
    }

    public List<State> findStates(RangeTable... tables) {
        List<State> states = new ArrayList<State>();

        return states;
    }


    private Map<String, Pair<Float, Long>> rangeTable;
    public void setRangeTable(Map<String, Pair<Float, Long>> table) {
        rangeTable = table;
    }
    public Map<String, Pair<Float, Long>> getRangeTable() {
        return rangeTable;
    }


    private final List<Range> rangePending = new ArrayList<Range>();
    private final List<Range> rangeHistory = new ArrayList<Range>();
    private final List<State> statePending = new ArrayList<State>();
    private final List<State> stateHistory = new ArrayList<State>();

    public static final boolean idIsWifiMac = true;
    private final String id;
    private String name;
    private boolean fixed;
    private boolean ignore;
    private float actualRangeOverride;

    public synchronized void setActualRangeOverride(float range) {
        actualRangeOverride = range;
    }
    public synchronized float getActualRangeOverride() {
        return actualRangeOverride;
    }

    public Node(String id) {
        this.id = id;
        this.name = "Unknown Node";
        fixed = true;
        rangeHistory.add(new Range());
        stateHistory.add(new State());
    }


    public String getId() {
        return id;
    }
    public String getName() { return name; }
    public void setName(String name) {
        if (!TextUtils.isEmpty(name)) {
            this.name = name;
        }
    }

    public synchronized boolean isFixed() { return fixed; }
    public synchronized void setFixed(boolean fixed) { this.fixed = fixed; }

    public synchronized boolean shouldIgnore() { return ignore; }
    public synchronized void setIgnore(boolean ignore) { this.ignore = ignore; }


    public synchronized void addPending(Range r) {
        rangePending.add(r);
        for (NodeListener l: listeners) {
            l.onRangePending(this, r);
        }
    }

    public synchronized void addPending(State s) {
        statePending.add(s);
        for (NodeListener l: listeners) {
            l.onStatePending(this, s);
        }
    }

    public synchronized void update(Range r) {
        r.rangeOverride = getActualRangeOverride();
        rangeHistory.add(r);
        for (NodeListener l: listeners) {
            l.onRangeChanged(this, r);
        }
    }

    public synchronized void update(State s) {
        stateHistory.add(s);
        for (NodeListener l: listeners) {
            l.onStateChanged(this, s);
        }
    }

    /** Get previous (or current) range */
    public synchronized Range getRange(int index) {
        return rangeHistory.get(index);
    }

    /** Get previous (or current) state */
    public synchronized State getState(int index) {
        return stateHistory.get(index);
    }

    /** Get current range */
    public synchronized Range getRange() {
        return getRange(rangeHistory.size() - 1);
    }

    /** Get current state */
    public synchronized State getState() {
        return getState(stateHistory.size() - 1);
    }

    /** Get previous (or current) pending range */
    public synchronized Range getPendingRange(int index) {
        return rangePending.get(index);
    }

    /** Get previous (or current) pending state */
    public synchronized State getPendingState(int index) {
        return statePending.get(index);
    }

    /** Get most recent pending range */
    public synchronized Range getPendingRange() { return getPendingRange(rangePending.size() - 1); }

    /** Get most recent pending state */
    public synchronized State getPendingState() { return getPendingState(statePending.size() - 1); }

    public synchronized int getRangeHistorySize() {
        return rangeHistory.size();
    }
    public synchronized int getStateHistorySize() {
        return stateHistory.size();
    }

    public synchronized int getRangePendingSize() {
        return rangeHistory.size();
    }
    public synchronized int getStatePendingSize() {
        return stateHistory.size();
    }

    public synchronized void savePrefs(SharedPreferences prefs) {
        JSONObject json = new JSONObject();
        try {
            json.put("name", getName());
            json.put("ignore", shouldIgnore());
        } catch (JSONException ignored) {}
        prefs.edit().putString(json.toString(), "").commit();
    }

    public synchronized void readPrefs(SharedPreferences prefs) {
        String info = prefs.getString(getId(), "");
        try {
            JSONObject json = new JSONObject(info);
            if (!TextUtils.isEmpty(json.getString("name"))) {
                setName(json.getString("name"));
            }
            setIgnore(json.getBoolean("ignore"));
        } catch (JSONException ignored) {}
    }


    /**
     * Flatten several nodes' current state to a float[][].
     */
    public static float[][] toPositionArray(Node... nodes) {
        float[][] n = new float[nodes.length][3];
        for (int i=0; i<nodes.length; ++i) {
            for (int j=0; j<3; ++j) {
                n[i][j] = nodes[i].getState().pos[j];
            }
        }
        return n;
    }

    /**
     * Flatten several nodes' current ranges to a float[].
     */
    public static float[] toRangeArray(Node... nodes) {
        float[] r = new float[nodes.length];
        for (int i=0; i<nodes.length; ++i) {
            r[i] = nodes[i].getRange().range;
        }
        return r;
    }

    /**
     * Allow other objects to react to node events.
     */
    public static interface NodeListener {
        void onRangePending(Node n, Range r);
        void onStatePending(Node n, State s);
        void onRangeChanged(Node n, Range r);
        void onStateChanged(Node n, State s);
    }
    private final Set<NodeListener> listeners = new LinkedHashSet<NodeListener>(1);
    public void registerListener(NodeListener l) {
        listeners.add(l);
    }
    public void unregisterListener(NodeListener l) {
        if (l == null) {
            listeners.clear();
        } else {
            listeners.remove(l);
        }
    }
}
