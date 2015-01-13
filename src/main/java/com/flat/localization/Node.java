package com.flat.localization;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.flat.localization.util.Calc;
import com.flat.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    public static final class RangeTable extends TreeMap<String, SimpleRange> {
        /** Node ID => {Range, Timestamp} */
        public CoordinateSystem referenceFrame;
    }

    public static final class RangeTableList extends TreeMap<String, RangeTable> {}


    public static final class NodeSet extends TreeSet<String> {}
    public static final class NodeSetMap extends TreeMap<String, NodeSet> {}
    public static final class CommonNodeMap extends TreeMap<String, NodeSetMap> {}


    // TODO belongs elsewhere
    NodeSet connectedNodes = new NodeSet();

    /**
     * A coordinate system is a list of nodes with their coordinates in that system. There is also
     * a list of the relevant ranges that were used to define this coordinate system.
     */
    public static final class CoordinateSystem extends TreeMap<String, float[]> {}



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
    public CoordinateSystem findCoordinateSystem(RangeTableList rangeTables) {

        // Find coordinate system with the most nodes
        String mostNodesId;
        int nodeCount = 0;
        for (Map.Entry<String, RangeTable> entry : rangeTables.entrySet()) {
            if (entry.getValue().referenceFrame.size() > nodeCount) {
                nodeCount = entry.getValue().referenceFrame.size();
                mostNodesId = entry.getKey();
            }
        }

        // If there arent enough nodes in the coordinate system, a new coordinate system must be created.
        if (nodeCount < 3) {
            long startTime = System.nanoTime();

            // The complete list of common nodes
            CommonNodeMap allCommonNodes = new CommonNodeMap();

            // A map of common nodes from the point of view of a single node (A has common nodes with B, A has common nodes with C, etc.)
            NodeSetMap commonNodes;
            NodeSet common;

            // Iterate through each range table, except the last
            for (Map.Entry<String, RangeTable> table : rangeTables.subMap(
                        rangeTables.firstKey(), true, rangeTables.lastKey(), false).entrySet()) {
                commonNodes = new NodeSetMap();

                // Iterate through the other range tables that are after the current one.
                for (Map.Entry<String, RangeTable> nextTable : rangeTables.subMap(
                        table.getKey(), false, rangeTables.lastKey(), true).entrySet()) {

                    // Note this will not include the two nodes represented by table and nextTable because
                    // they only contain a reference to each other, not themselves. (actually, table contains
                    // a reference to nextTable, but nextTable need not contain a reference to table, but the
                    // conclusion still holds true.)
                    common = new NodeSet();
                    common.addAll(table.getValue().keySet());
                    common.retainAll(nextTable.getValue().keySet());
                    commonNodes.put(nextTable.getKey(), common);
                }

                allCommonNodes.put(table.getKey(), commonNodes);
            }
            Log.d(TAG, "Finding all common nodes took " + (System.nanoTime() - startTime) / 1E6f + "ms");

            // Now, we look for the largest number of nodes that can be localized by recursively counting
            // common nodes from other common nodes. Yep. Why? Because if C is common to A and B, then by extension,
            // any nodes common to B and C can also be localized under A. Then if D is common to any two nodes localizable
            // under A, D is also localizable. So on and so forth.

            // TODO "Directly localizable" nodes here use groups of 3 nodes, which will restrict localization to give 2 values for the y coordinate (x, +-y)
            startTime = System.nanoTime();
            NodeSetMap directlyLocalizable = new NodeSetMap();
            NodeSet directSet;
            for (Map.Entry<String, NodeSetMap> outer : allCommonNodes.entrySet()) {
                directSet = new NodeSet();
                for (Map.Entry<String, NodeSet> inner : outer.getValue().entrySet()) {
                    float innerToOuter = findRangeBetween(inner.getKey(), outer.getKey(), rangeTables);
                    boolean hasMatch = false;

                    // filter linear relationships.
                    for (String s : inner.getValue()) {
                        // range1: current node to inner
                        // range2: current node to outer
                        // range3: inner to outer
                        float r1 = findRangeBetween(s, inner.getKey(), rangeTables);
                        float r2 = findRangeBetween(s, outer.getKey(), rangeTables);
                        if (!areLinear(r1, r2, innerToOuter)) {
                            directSet.add(s);
                            hasMatch = true;
                        }
                    }

                    if (hasMatch) {
                        // If there is at least one common node for inner and outer, inner can be localized by outer.
                        directSet.add(inner.getKey());
                    }

                }
                directlyLocalizable.put(outer.getKey(), directSet);
            }
            Log.d(TAG, "Finding map of directly localizable nodes took " + (System.nanoTime() - startTime) / 1E6f + "ms");


            // Now we have a map of all the nodes directly localizable under a given node. Combining this map
            // will give us the full list of nodes localizable under any given node.

            // The list of strings is the path of directly localizable nodes taken to get to the inner node from the outer node.
            // Node -> list of localizable nodes and any in-between nodes.
            //          list 0: node that can be localized in 1 hop (neighbor)
            //          list 1: node that can be localized in 2 hops
            //          ...
            LocalizableNodeList localizable = new LocalizableNodeList();
            NodeHopList hopList;
            NodePathMap neighbors;
            NodePathMap neighborsNeighbors;

            // Get all 1 and 2 hop nodes localizable.
            for (Map.Entry<String, NodeSet> outer : directlyLocalizable.entrySet()) {
                hopList = new NodeHopList();
                neighbors = new NodePathMap();
                neighborsNeighbors = new NodePathMap();
                hopList.add(neighbors);
                hopList.add(neighborsNeighbors);
                localizable.put(outer.getKey(), hopList);

                // for every directly localizable node in "outer"
                for (String node : outer.getValue()) {

                    // all directly localizable nodes are localizable and no path is needed
                    neighbors.put(node, null);

                    // Add all the nodes that are directly localizable under this node, keeping the
                    // path of nodes followed for later use.
                    NodeSet tmp = directlyLocalizable.get(node);
                    if (tmp != null) {
                        for (String s : tmp) {
                            if (!neighbors.containsKey(s) && !neighborsNeighbors.containsKey(s) && !s.equals(outer.getKey())) {
                                neighborsNeighbors.put(s, new String[]{node});
                            }
                        }
                    }
                }
            }


            // Find the biggest
            String winnerKey = "";
            int biggest = 0;
            for (Map.Entry<String, NodeHopList> nodeFun : localizable.entrySet()) {
                if (nodeFun.getValue().size() > biggest) {
                    winnerKey = nodeFun.getKey();
                    biggest = nodeFun.getValue().size();
                }
            }

            //
            // Localization root
            //
            LocalizationRoot root = new LocalizationRoot(winnerKey);
            root.addAll(localizable.get(winnerKey));

            // So we have chosen the nodes that will be used to construct a coordinate system.
            Log.i(TAG, root.get(0).size() + " neighbors and " + root.get(1).size() + " neighbors' neighbors will be localized under " + winnerKey);

            CoordinateSystem cs = new CoordinateSystem();
            cs.put(root.id, new float[]{0, 0, 0});

            final String root2 = root.get(0).firstKey();
            float x = findRangeBetween(root.id, root2, rangeTables);
            cs.put(root2, new float[]{x, 0, 0});


            // First localize all the nodes common to the first two.
            startTime = System.nanoTime();
            for (String node : allCommonNodes.get(root.id).get(root2)) {
                if (!cs.containsKey(node)) {
                    float r1 = findRangeBetween(root.id, node, rangeTables);
                    float r2 = findRangeBetween(root2, node, rangeTables);
                    float[] coords = makeCoords(cs.get(root.id), r1, cs.get(root2), r2);
                    cs.put(node, coords);
                }

                // Localize nodes common to the root node and the current node
                for (String n1 : allCommonNodes.get(root.id).get(node)) {
                    if (!cs.containsKey(n1)) {
                        float r1 = findRangeBetween(root.id, n1, rangeTables);
                        float r2 = findRangeBetween(node, n1, rangeTables);
                        float[] coords = makeCoords(cs.get(root.id), r1, cs.get(node), r2);
                        cs.put(n1, coords);
                    }
                }

                // Localize nodes common to the second root node and the current node
                for (String n2 : allCommonNodes.get(root2).get(node)) {
                    if (!cs.containsKey(n2)) {
                        float r1 = findRangeBetween(root2, n2, rangeTables);
                        float r2 = findRangeBetween(node, n2, rangeTables);
                        float[] coords = makeCoords(cs.get(root2), r1, cs.get(node), r2);
                        cs.put(n2, coords);
                    }
                }
            }
            Log.d(TAG, "Initial localization using first two roots took " + (System.nanoTime() - startTime) / 1E6f + "ms");

            // Several nodes were localized, including all that were common to the two root nodes.
            // Now, go through all nodes that have been deemed localizable and do any necessary localization.

            startTime = System.nanoTime();
            for (NodePathMap pathMap : root) {
                for (Map.Entry<String, String[]> nodePath : pathMap.entrySet()) {
                    String node = nodePath.getKey();
                    String[] path = nodePath.getValue();

                    if (path == null) {
                        // Working with neighboring nodes. Note these will all be completed before neighbors' neighbors are handled.
                        if (!cs.containsKey(node)) {
//                            float r1 = findRangeBetween(, n2, rangeTables);
//                            float r2 = findRangeBetween(node, n2, rangeTables);
//                            float[] coords = makeCoords(cs.get(root2), r1, cs.get(node), r2);
//                            cs.put(n2, coords);
                        }

                    } else {
                        // Working with neighbors' neighbors (or beyond, if that's ever added)

                    }
                }
            }
            Log.d(TAG, "Building coordinate system from node set took " + (System.nanoTime() - startTime) / 1E6f + "ms");
        }


        return cs;
    }

    public static final class NodePathMap extends TreeMap<String, String[]> {}
    public static class NodeHopList extends ArrayList<NodePathMap> {}
    public static final class LocalizationRoot extends NodeHopList {
        public final String id;
        LocalizationRoot(String id) { this.id = id; }
    }
    public static final class LocalizableNodeList extends TreeMap<String, NodeHopList> {}


    /**
     *
     * @param p1    reference point 1
     * @param r1    range to point 1 (from node to be localized)
     * @param p2    reference point 2
     * @param r2    range to point 2 (from node to be localized)
     * @return      coordinates of newly localized node.
     */
    private float[] makeCoords(float[] p1, float r1, float[] p2, float r2) {
        float[] pos = new float[3];

        // following http://www.ms.uky.edu/~lee/ma502/pythag/cos.htm
        // a = r1, b = r2, c = r3

        float r3 = (float) Calc.linearDistance(p1, p2);

        // angle opposite of side r1. (b^2 + c^2 - a^2)/2bc
        float cosA = (r2*r2 + r3*r3 - r1*r1) / (2 * r2 * r3);
        pos[0] = r2 * cosA;

        // pythagorean to get y. y^2 = b^2 - x^2.
        pos[1] = (float) Math.sqrt(r2*r2 - pos[0]*pos[0]);

        return pos;
    }

    public float findRangeBetween(String node1, String node2, TreeMap<String, RangeTable> tables) {
        float range = 0;

        for (Map.Entry<String, RangeTable> table : tables.entrySet()) {
            if (node1.equals(table.getKey())) {
                SimpleRange sr = table.getValue().get(node2);
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
                SimpleRange sr = table.getValue().get(node1);
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
        if (range1 < 1 || range2 < 1 || range3 < 1) return true;
        float give = 0.1f;
        float min1 = range2 + range3 - (range2+range3)*give;
        float max1 = range2 + range3 + (range2+range3)*give;
        float min2 = range1 + range3 - (range1+range3)*give;
        float max2 = range1 + range3 + (range1+range3)*give;
        float min3 = range1 + range2 - (range1+range2)*give;
        float max3 = range1 + range2 + (range1+range2)*give;
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
