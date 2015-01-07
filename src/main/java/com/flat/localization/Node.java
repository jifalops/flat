package com.flat.localization;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.flat.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Nodes are the main objects to be manipulated in the localization system and they are
 * designed here to facilitate being localized.
 */
public final class Node {
    private static final String TAG = Node.class.getSimpleName();
    /**
     * Ranging provides the primary input to the localization system. A range is created by
     * interpreting an external signal such as a wifi beacon.
     *
     * {@link com.flat.localization.signal.Signal}
     * {@link com.flat.localization.signal.interpreters.SignalInterpreter}
     */
    public static final class Range {
        public float range = 0;
        public float rangeOverride = 0; // when given
        public String signal = "none";
        public String interpreter = "none";
        public long time = System.currentTimeMillis();
        @Override public String toString() { return String.format("%.2fm (overridden to %.2fm)", range, rangeOverride); }
    }

    public static final class SimpleRange {
        public float range;
        public long time;
    }

    /**
     * Each node maintains its own range table, which is a mapping of node IDs to their range and the time ranging occurred.
     * Before creating a coordinate system, each node must share its range table with the other nodes it has ranges to
     * (and a data connection to, since the two are independent).
     */
    public static final class RangeTable {
        /** Node ID => {Range, Timestamp} */
        public final TreeMap<String, SimpleRange> ranges = new TreeMap<String, SimpleRange>();
        public CoordinateSystem ownerReferenceFrame;
    }

    // TODO belongs elsewhere
    TreeSet<String> connectedNodes = new TreeSet<String>();

    /**
     * A coordinate system is a list of nodes with their coordinates in that system. There is also
     * a list of the relevant ranges that were used to define this coordinate system.
     */
    public static final class CoordinateSystem {
        public final TreeMap<String, RangeTable> definingRanges = new TreeMap<String, RangeTable>();
        public final TreeMap<String, float[]> coords = new TreeMap<String, float[]>();
    }

    /**
     * A state is the position [x,y,z] and angle (see below) of a node at a specific time. It also
     * contains the reference frame through which this state was defined ({@link Node.CoordinateSystem}).
     * <pre>
     * Angle:
     * 0:       angle with respect to left-right axis on a vertically held phone.
     * 1:       angle with respect to the up-down axis on a vertically held phone.
     * 2:       angle with respect to the forward-back axis on a vertically held phone.
     * </pre>
     * For orientation of angle coordinates, see
     * <a href='http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords'>http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords</a>
     */
    public static final class State {
        public CoordinateSystem referenceFrame;
        public float pos[] = {0,0,0};           // TODO put the position of the local node in the coordinate system?
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

    /**
     * Find the best coordinate system based on a list of range tables.
     */
    public CoordinateSystem findCoordinateSystem(TreeMap<String, RangeTable> knownRangeTablesIncludingMyOwn) {

        // Find coordinate system with the most nodes
        String mostNodesId;
        int nodeCount = 0;
        for (Map.Entry<String, RangeTable> entry : knownRangeTablesIncludingMyOwn.entrySet()) {
            if (entry.getValue().ownerReferenceFrame.coords.size() > nodeCount) {
                nodeCount = entry.getValue().ownerReferenceFrame.coords.size();
                mostNodesId = entry.getKey();
            }
        }

        // If there are < 4 nodes in the coordinate system, a new coordinate system must be created.
        if (nodeCount < 4) {
            long startTime = System.nanoTime();

            // The complete list of common nodes
            TreeMap<String, TreeMap<String, Set<String>>> allCommonNodes = new TreeMap<String, TreeMap<String, Set<String>>>();

            // A map of common nodes from the point of view of a single node (A has common nodes with B, A has common nodes with C, etc.)
            TreeMap<String, Set<String>> commonNodes;
            Set<String> common;

            // Iterate through each range table, except the last
            for (Map.Entry<String, RangeTable> table : knownRangeTablesIncludingMyOwn.subMap(
                        knownRangeTablesIncludingMyOwn.firstKey(), true, knownRangeTablesIncludingMyOwn.lastKey(), false).entrySet()) {
                commonNodes = new TreeMap<String, Set<String>>();

                // Iterate through the other range tables that are after the current one.
                for (Map.Entry<String, RangeTable> nextTable : knownRangeTablesIncludingMyOwn.subMap(
                        table.getKey(), false, knownRangeTablesIncludingMyOwn.lastKey(), true).entrySet()) {

                    // Note this will not include the two nodes represented by table and nextTable because
                    // they only contain a reference to each other, not themselves. (actually, table contains
                    // a reference to nextTable, but nextTable need not contain a reference to table, but the
                    // conclusion still holds true.)
                    common = new HashSet<String>(table.getValue().ranges.keySet());
                    common.retainAll(nextTable.getValue().ranges.keySet());
                    commonNodes.put(nextTable.getKey(), common);
                }

                allCommonNodes.put(table.getKey(), commonNodes);
            }
            Log.d(TAG, "Finding all common nodes took " + (System.nanoTime() - startTime) / 1E6f + "ms");

            // Now, we look for the largest number of nodes that can be localized by recursively counting
            // common nodes from other common nodes. Yep. Why? Because if C is common to A and B, then by extension,
            // any nodes common to B and C can also be localized under A. Then if D is common to any two nodes localizable
            // under A, D is also localizable. So on and so forth. Fuck my life.
            startTime = System.nanoTime();
            TreeMap<String, TreeSet<String>> directlyLocalizable = new TreeMap<String, TreeSet<String>>();
            TreeSet<String> directSet;
            for (Map.Entry<String, TreeMap<String, Set<String>>> outer : allCommonNodes.entrySet()) {
                directSet = new TreeSet<String>();
                for (Map.Entry<String, Set<String>> inner : outer.getValue().entrySet()) {
                    if (inner.getValue().size() > 0) {
                        // If there is a common node for inner and outer, inner can be localized by outer.
                        directSet.add(inner.getKey());
                    }
                    directSet.addAll(inner.getValue());
                }
                directlyLocalizable.put(outer.getKey(), directSet);
            }
            Log.d(TAG, "Finding map of directly localizable nodes took " + (System.nanoTime() - startTime) / 1E6f + "ms");


            // Now we have a map of all the nodes directly localizable under a given node. Combining this map
            // will give us the full list of nodes localizable under any given node.
            TreeMap<String, TreeSet<String>> localizable = new TreeMap<String, TreeSet<String>>();
            TreeSet<String> localNodes;
            for (Map.Entry<String, TreeSet<String>> outer : directlyLocalizable.entrySet()) {
                localNodes = new TreeSet<String>(outer.getValue());
                for (String node : outer.getValue()) {
                    TreeSet<String> tmp = directlyLocalizable.get(node);
                    if (tmp != null) {
                        localNodes.addAll(tmp);
                    }
                }
                localizable.put(outer.getKey(), localNodes);
            }

            // Lets say D was directly localizable under A, but B and C were not. However, C was directly
            // localizable under D, and B was directly localizable under C, meaning all are localizable under A.
            // The above loop fails to see this connection, and running it again would only fix this example problem
            // and not its more general description.
            // UPDATE: I think it does solve this problem but I am leaving it here because I'm not sure that it does and my brain hurts.


            // What's this? A complete list of nodes localizable under any given node? I'll believe it when I see it.

            // Find the biggest
            String winnerKey = "";
            int biggest = 0;
            for (Map.Entry<String, TreeSet<String>> nodeFun : localizable.entrySet()) {
                if (nodeFun.getValue().size() > biggest) {
                    winnerKey = nodeFun.getKey();
                    biggest = nodeFun.getValue().size();
                }
            }
            TreeSet<String> nodes = localizable.get(winnerKey);
            Log.i(TAG, nodes.size() + " nodes will be localized under " + winnerKey);

            // So we have chosen the nodes that will be used to construct a coordinate system.
            // Why wait to complete this fun process? Because I'm hungry and getting cranky and I need to go watch something stupid on TV. That's why.

            CoordinateSystem cs = new CoordinateSystem();
            cs.coords.put(winnerKey, new float[] {0, 0, 0});

            // Step 1. Get the necessary ranges between nodes.
            startTime = System.nanoTime();
            boolean first = true;
            for (String s : nodes) {
                if (first) {
                    first = false;
                    float x = findRangeBetween(winnerKey, s, knownRangeTablesIncludingMyOwn);
                    cs.coords.put(s, new float[] {x, 0, 0});
                }

            }
            Log.d(TAG, "Building coordinate system from node set took " + (System.nanoTime() - startTime) / 1E6f + "ms");
        }


        return frames;
    }

    public float findRangeBetween(String node1, String node2, TreeMap<String, RangeTable> tables) {
        float range = 0;

        for (Map.Entry<String, RangeTable> table : tables.entrySet()) {
            if (node1.equals(table.getKey())) {
                SimpleRange sr = table.getValue().ranges.get(node2);
                if (sr != null) {
                    if (range > 0) {
                        if (range != sr.range) {
                            // return the lesser of the two.
                            return range < sr.range ? range : sr.range;
                        }
                    } else {
                        range = sr.range;
                    }
                }
            } else if (node2.equals(table.getKey())) {
                SimpleRange sr = table.getValue().ranges.get(node1);
                if (sr != null) {
                    if (range > 0) {
                        if (range != sr.range) {
                            // return the lesser of the two.
                            return range < sr.range ? range : sr.range;
                        }
                    } else {
                        range = sr.range;
                    }
                }
            }
        }

        return range;
    }


    private boolean areLinear(float range1, float range2, float range3) {
        float give = 0.05f;
        float min1 = range2 + range3 - (range2*range3*give);
        float max1 = range2 + range3 + (range2*range3*give);
        float min2 = range1 + range3 - (range1*range3*give);
        float max2 = range1 + range3 + (range1*range3*give);
        float min3 = range1 + range2 - (range1*range2*give);
        float max3 = range1 + range2 + (range1*range2*give);
        return (range1 >= min1 && range1 <= max1) || (range2 >= min2 && range2 <= max2) || (range3 >= min3 && range3 <= max3);
    }






    private final List<Range> rangePending = new ArrayList<Range>();
    private final List<Range> rangeHistory = new ArrayList<Range>();
    private final List<State> statePending = new ArrayList<State>();
    private final List<State> stateHistory = new ArrayList<State>();

    public static final boolean idIsWifiMac = true;
    private final String id;
    private String name;
    private boolean fixed;
    private float actualRangeOverride;

    public synchronized void setActualRangeOverride(float range) {
        actualRangeOverride = range;
    }
    public synchronized float getActualRangeOverride() {
        return actualRangeOverride;
    }

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
    public void setName(String name) {
        if (!TextUtils.isEmpty(name)) {
            this.name = name;
        }
    }

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
        } catch (JSONException ignored) {}
        prefs.edit().putString(getId(), json.toString()).apply();
    }

    public synchronized void readPrefs(SharedPreferences prefs) {
        String info = prefs.getString(getId(), "");
        try {
            JSONObject json = new JSONObject(info);
            if (!TextUtils.isEmpty(json.getString("name"))) {
                setName(json.getString("name"));
            }
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
    private final List<NodeListener> listeners = new ArrayList<NodeListener>(1);
    public boolean registerListener(NodeListener l) {
        if (listeners.contains(l)) return false;
        return listeners.add(l);
    }
    public boolean unregisterListener(NodeListener l) {
        return listeners.remove(l);
    }
}
