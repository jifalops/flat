package com.flat.localization;

import android.util.Log;

import com.flat.localization.util.Calc;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A coordinate system is a list of nodes with their coordinates in that system. There is also
 * a list of the relevant ranges that were used to define this coordinate system.
 */
public class CoordinateSystem extends TreeMap<String, float[]> {
    private static final String TAG = CoordinateSystem.class.getSimpleName();

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
        public final TreeMap<String, float[]> coords;
        public RangeTable(TreeMap<String, float[]> coords) { this.coords = coords; }
    }
    public static final class RangeTableList extends TreeMap<String, RangeTable> {}

    public static final class NodeSet extends TreeSet<String> {}
    public static final class NodeSetMap extends TreeMap<String, NodeSet> {}

    public static final class CommonNodeMap extends TreeMap<String, NodeSetMap> {}

    public static final class NodePathMap extends TreeMap<String, String[]> {}
    public static final class NodeHopList extends ArrayList<NodePathMap> {}
    public static final class LocalizableNodeList extends TreeMap<String, NodeHopList> {}
    public static final class LocalizationRoot extends ArrayList<NodePathMap> {
        public final String id;
        LocalizationRoot(String id) { this.id = id; }
        public NodeSetMap commonNodeMap;
    }


    public CoordinateSystem(RangeTableList rangeTables) {
        this.rangeTables = rangeTables;
        updateCoordinates();
    }

    private LocalizationRoot root;
    public synchronized LocalizationRoot getRoot() { return root; }



    private final RangeTableList rangeTables;
    public synchronized RangeTableList getRangeTables() { return rangeTables; }




    private synchronized void updateCoordinates() {
        // Find coordinate system with the most nodes
        String winnerNode = null;
        int nodeCount = 0;
        for (String node : rangeTables.keySet()) {
            try {
                if (rangeTables.get(node).coords.size() > nodeCount) {
                    nodeCount = rangeTables.get(node).coords.size();
                    winnerNode = node;
                }
            } catch (NullPointerException ignored) {}
        }

        // If there arent enough nodes in the coordinate system, a new coordinate system must be created.
        if (nodeCount < 3) {

            // The complete list of common nodes
            CommonNodeMap allCommonNodes = new CommonNodeMap();

            long startTime = System.nanoTime();

            // Iterate through each range table, except the last
            for (Map.Entry<String, RangeTable> table : rangeTables.subMap(
                    rangeTables.firstKey(), true, rangeTables.lastKey(), false).entrySet()) {

                // A map of common nodes from the point of view of a single node (A has common nodes with B, A has common nodes with C, etc.)
                // "commonNodesFromThePointOfViewOfAParticularNode"
                NodeSetMap nodeCommonNodes = new NodeSetMap();

                // Iterate through the other range tables that are after the current one.
                for (Map.Entry<String, RangeTable> nextTable : rangeTables.subMap(
                        table.getKey(), false, rangeTables.lastKey(), true).entrySet()) {

                    // Note this will not include the two nodes represented by "table" and "nextTable" because
                    // they only contain a reference to each other, not themselves (both would have to be true).
                    // (actually, table contains a reference to nextTable, but nextTable need not contain a reference to table.)
                    NodeSet nodes = new NodeSet();
                    nodes.addAll(table.getValue().keySet());            // start with the nodes in the range table in the outer loop
                    nodes.retainAll(nextTable.getValue().keySet());     // only keep nodes that are common to the range table in the inner loop
                    nodeCommonNodes.put(nextTable.getKey(), nodes);     // the owner of "nextTable" has all those nodes in common with the owner of "table".
                }

                allCommonNodes.put(table.getKey(), nodeCommonNodes);    // the owner of "table" has a complete mapping of common nodes between itself
                                                                        // and all the nodes in front of it in the list of range tables.
            }
            Log.d(TAG, "Finding all common nodes took " + (System.nanoTime() - startTime) / 1E6f + "ms");




            // Now, we look for the largest number of nodes that can be localized by recursively counting
            // common nodes from other common nodes. Yep. Why? Because if C is common to A and B, then by extension,
            // any nodes common to B and C can also be localized under A. Then if D is common to any two nodes localizable
            // under A, D is also localizable. So on and so forth.

            // TODO "Directly localizable" nodes here use groups of 3 nodes, which will restrict localization to give 2 values for the y coordinate (x, +-y)

            // Keep track of which nodes are directly localizable from the point of view of each node.
            NodeSetMap directlyLocalizable = new NodeSetMap();

            startTime = System.nanoTime();
            for (Map.Entry<String, NodeSetMap> outer : allCommonNodes.entrySet()) {

                NodeSet directSet = new NodeSet();

                for (Map.Entry<String, NodeSet> inner : outer.getValue().entrySet()) {
                    float innerToOuter = findRangeBetween(inner.getKey(), outer.getKey());
                    boolean hasMatch = false;

                    // filter linear nodes.
                    for (String s : inner.getValue()) {
                        // range1: current node to inner
                        // range2: current node to outer
                        // range3: inner to outer
                        float r1 = findRangeBetween(s, inner.getKey());
                        float r2 = findRangeBetween(s, outer.getKey());
                        if (!areLinear(r1, r2, innerToOuter)) {
                            directSet.add(s);
                            hasMatch = true;
                        }
                    }

                    // If there is at least one common node for inner and outer, inner can be localized by outer.
                    if (hasMatch) {
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
            LocalizableNodeList localizableNodes = new LocalizableNodeList();


            // Get all 1 and 2 hop nodes localizable.
            for (Map.Entry<String, NodeSet> outer : directlyLocalizable.entrySet()) {

                // TODO, the term "neighbors" is used here meaning the directly localizable nodes, not necessarily meaning those nodes which have a range.

                // create the associations to be populated later in the loop.
                NodeHopList hopList = new NodeHopList();
                NodePathMap neighbors = new NodePathMap();
                NodePathMap neighborsNeighbors = new NodePathMap();
                hopList.add(neighbors);
                hopList.add(neighborsNeighbors);
                localizableNodes.put(outer.getKey(), hopList);

                // for every directly localizable node in "outer"
                for (String outerNode : outer.getValue()) {

                    // all directly localizable nodes are neighbors and no path is needed
                    neighbors.put(outerNode, null);

                    // Add all the nodes that are directly localizable under this node, keeping the
                    // path of nodes followed for later use.
                    NodeSet directSet = directlyLocalizable.get(outerNode);
                    if (directSet != null) {
                        for (String innerNode : directSet) {
                            // make sure this node isn't already in the list somewhere
                            if (!neighbors.containsKey(innerNode) && !neighborsNeighbors.containsKey(innerNode) && !innerNode.equals(outer.getKey())) { // TODO outer or outernode or both?
                                neighborsNeighbors.put(innerNode, new String[]{outerNode});
                            }
                        }
                    }
                }
            }


            // Find the biggest coordinate system that can be built from the LocalizableNodeList.
            int biggest = 0;
            for (String node : localizableNodes.keySet()) {
                NodeHopList hopList = localizableNodes.get(node);
                if (hopList.size() > biggest) {
                    winnerNode = node;
                    biggest = hopList.size();
                }
            }


            //
            // Localization root
            //
            root = new LocalizationRoot(winnerNode);
            root.addAll(localizableNodes.get(winnerNode));
            root.commonNodeMap = allCommonNodes.get(winnerNode);



            // So we have chosen the nodes that will be used to construct a coordinate system.
            Log.i(TAG, root.get(0).size() + " neighbors and " + root.get(1).size() + " neighbors' neighbors will be localized under " + root.id);




            //
            // Clearing known coordinates
            //
            clear();

            // Root node coords
            put(root.id, new float[] {0, 0, 0});

            // Second node coords
            final String root2 = root.get(0).firstKey();
            float x = findRangeBetween(root.id, root2);
            put(root2, new float[]{x, 0, 0});


            // First localize all the nodes common to the first two.
//            startTime = System.nanoTime();
//            for (String node : allCommonNodes.get(root.id).get(root2)) {
//                putCoords(node, root.id, root2);
//
//                // Localize nodes common to the root node and the current node
//                for (String n1 : allCommonNodes.get(root.id).get(node)) {
//                    putCoords(n1, root.id, node);
//                }
//
//                // Localize nodes common to the second root node and the current node
//                for (String n2 : allCommonNodes.get(root2).get(node)) {
//                    putCoords(n2, root2, node);
//                }
//            }
//            Log.d(TAG, "Initial localization using first two roots took " + (System.nanoTime() - startTime) / 1E6f + "ms");



            // Localize all directly localizable nodes from the root.



            // Several nodes were localized, including all that were common to the two root nodes.
            // Now, go through all nodes that have been deemed localizable and do any remaining localization.

            startTime = System.nanoTime();
            for (NodePathMap pathMap : root) {
                // pathMap is either neighbors, or neighbors' neighbors.

                for (String node : pathMap.keySet()) {
                    String[] path = pathMap.get(node);

                    if (path == null) {
                        // Working with the root's directly localizable nodes. Note these will all be completed first.
                        for (String targetNode : root.commonNodeMap.get(node)) {
                            if (!targetNode.equals(node) && pathMap.containsKey(targetNode)) {
                                putCoords(targetNode, root.id, node);
                            }
                        }
                    } else {
                        // Working with neighbors' neighbors (or beyond, if that's ever added)
                        for (String middleNode : path) {

                            // This was probably taken care of when path==null, but just in case.
                            putCoords(middleNode, root.id, node);


                            // Figure out which way the target node set is stored
                            NodeSetMap setMap = allCommonNodes.get(node);
                            NodeSet nodeSet = null;
                            if (setMap != null) {
                                nodeSet = setMap.get(middleNode);
                            }
                            if (nodeSet == null) {
                                setMap = allCommonNodes.get(middleNode);
                                if (setMap != null) {
                                    nodeSet = setMap.get(node);
                                }
                            }


                            if (nodeSet != null) {
                                for (String targetNode : nodeSet) {
                                    putCoords(targetNode, node, middleNode);
                                }
                            }


                        }
                    }
                }
            }
            Log.d(TAG, "Building coordinate system from node set took " + (System.nanoTime() - startTime) / 1E6f + "ms");
        }
    }





    private float[] putCoords(String targetNode, String referenceNode1, String referenceNode2) {
        if (containsKey(targetNode)) {
            Log.e(TAG, "Target node already in coordinate system: " + targetNode);
            return null;
        }

        if (targetNode.equals(referenceNode1) || targetNode.equals(referenceNode2) || referenceNode1.equals(referenceNode2)) {
            Log.e(TAG, "Aborting attempt to make coordinates where the target or references are the same node");
            return null;
        }

        float r1 = findRangeBetween(targetNode, referenceNode1);
        float r2 = findRangeBetween(targetNode, referenceNode2);
        float[] p1 = get(referenceNode1);
        float[] p2 = get(referenceNode2);

        if (r1 == 0) {
            Log.e(TAG, "No range between target " + targetNode + " and reference node 1: " + referenceNode1);
        }
        if (r2 == 0) {
            Log.e(TAG, "No range between target " + targetNode + " and reference node 2:" + referenceNode2);
        }
        if (p1 == null) {
            Log.e(TAG, "No coordinates known for reference node 1: " + referenceNode1);
        }
        if (p2 == null) {
            Log.e(TAG, "No coordinates known for reference node 2: " + referenceNode2);
        }


        if (r1 == 0 || r2 == 0 || p1 == null || p2 == null) {
            Log.i(TAG, "Aborting coordinate system creation.");
            return null;
        }

        float[] coords = makeCoords(p1, r1, p2, r2);
        put(targetNode, coords);
        return coords;
    }

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

    public float findRangeBetween(String node1, String node2) {
        float range = 0;

        for (Map.Entry<String, RangeTable> table : rangeTables.entrySet()) {
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


    public boolean areLinear(float range1, float range2, float range3) {
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
}
