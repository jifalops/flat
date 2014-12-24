package com.flat.localization;

import com.flat.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class Node {
    public static final class Range {
        public float dist = 0;
        public float actual = 0; // when given
        public String signal = "none";
        public String algorithm = "none";
        public long time = System.nanoTime();

        @Override
        public String toString() {
            return String.format("%.2fm (%.2fm)", dist, actual);
        }
    }

    /**
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
    public static final class State {  // TODO use states in the context of a coordinate system or reference frame
        public float pos[] = {0,0,0};
        public float angle[] = {0,0,0};
        public String algorithm = "none";
        public long time = System.nanoTime();

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

    public static final class RangeTableEntry {
        private String nodeId;
        private float range;
        private long time;
    }

    private final List<RangeTableEntry> rangeTable = new ArrayList<RangeTableEntry>();

    // TODO nodes -> rangetable for sending, accept other range tables.

    private final List<Range> rangePending = new ArrayList<Range>();
    private final List<Range> rangeHistory = new ArrayList<Range>();
    private final List<State> statePending = new ArrayList<State>();
    private final List<State> stateHistory = new ArrayList<State>();

    private final String id;
    private String name;
    private boolean fixed;


    public Node(String id) {

        this.id = id;
        this.name = id;
        fixed = true;
        rangeHistory.add(new Range());
        stateHistory.add(new State());
    }


    public String getId() {
        return id;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public synchronized boolean isFixed() { return fixed; }
    public synchronized void setFixed(boolean fixed) { this.fixed = fixed; }


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
            r[i] = nodes[i].getRange().dist;
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
    private final List<NodeListener> listeners = new ArrayList<NodeListener>(1);
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
