package com.flat.localization;

import android.util.Log;

import com.flat.localization.ranging.SignalProcessor;
import com.flat.localization.scheme.LocationAlgorithm;
import com.flat.localization.signal.Signal;
import com.flat.localization.util.Calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The Model contains the known nodes, which signals to use, and which location algorithms to use.
 */
public final class Model {
    /*
     * Simple Singleton
     */
    private Model() { initialize(); }
    private static final Model instance = new Model();
    public static Model getInstance() { return instance; }

    /**
     * Known nodes. The string is the node.id(), it is kept in a map so we don't need to loop through
     * all nodes every time input is received.
     */
     final Map<String, Node> nodes = new HashMap<String, Node>();

    /** All Signals and which ranging algorithms they can use */
    private final Map<Signal, List<SignalProcessor>> signals = new HashMap<Signal, List<SignalProcessor>>();

    /** All LocationAlgorithms */
    //private final List<LocationAlgorithm> algorithms = new ArrayList<LocationAlgorithm>();

    /**
     * Location algorithms that can be applied to each node.
     */
     final Map<LocationAlgorithm, AlgorithmMatchCriteria> algorithms = new HashMap<LocationAlgorithm, AlgorithmMatchCriteria>();


    public synchronized void add(Node n) {
        nodes.put(n.getId(), n);
        for (ModelListener l : listeners) {
            l.onNodeAdded(n);
        }
    }

    public synchronized Node get(String id) {
        return nodes.get(id);
    }

    public synchronized List<SignalProcessor> get(Signal s) {
        return signals.get(s);
    }

    public synchronized AlgorithmMatchCriteria get(LocationAlgorithm la) {
        return algorithms.get(la);
    }

    public synchronized void put(Signal s, List<SignalProcessor> sp) {
        signals.put(s, sp);
    }

    public synchronized void put(LocationAlgorithm la, AlgorithmMatchCriteria amc) {
        algorithms.put(la, amc);
    }

    public synchronized void remove(Node n) {
        nodes.remove(n.getId());
        for (ModelListener l : listeners) {
            l.onNodeRemoved(n);
        }
    }

    public synchronized void remove(Signal s) {
        signals.remove(s);
    }

    public synchronized void remove(LocationAlgorithm la) {
        algorithms.remove(la);
    }




    public static interface ModelListener {
        void onNodeAdded(Node n);
        void onNodeRemoved(Node n);
    }
    private final List<ModelListener> listeners = new ArrayList<ModelListener>(1);
    public void registerListener(ModelListener l) {
        listeners.add(l);
    }
    public void unregisterListener(ModelListener l) {
        if (l == null) {
            listeners.clear();
        } else {
            listeners.remove(l);
        }
    }

    public static final class AlgorithmMatchCriteria {
        String TAG = AlgorithmMatchCriteria.class.getSimpleName();
        // All must be matched by all nodes
        List<NodeMatchCriteria> nodeRequirements = new ArrayList<NodeMatchCriteria>();
        // All must be matched by at least one node
        List<NodeMatchCriteria> nodeListRequirements = new ArrayList<NodeMatchCriteria>();

        /**
         * Note this moves on to the next node when a nodeListRequirement is met, so there will be
         * at least as many nodes as there are requirements.
         */
        public List<Node> filter(List<Node> nodes) {
            nodes = new ArrayList<Node>(nodes);
            List<Node> matches = new ArrayList<Node>();

            // Remove nodes that do not meet all criteria.
            int size = nodeRequirements.size();
            if (size > 0) {
                for (Node n : nodes) {
                    for (NodeMatchCriteria nmc : nodeRequirements) {
                        if (!nmc.matches(n)) {
                            Log.v(TAG, n.getId() + " did not meet requirements.");
                            nodes.remove(n);
                            break;
                        }
                    }
                }
            }

            // Add all nodes that meet at least one list criterion.
            size = nodeListRequirements.size();
            if (size > 0) {
                boolean[] met = new boolean[size];
                for (Node n : nodes) {
                    for (int i = 0; i < size; ++i) {
                        if (nodeListRequirements.get(i).matches(n)) {
                            Log.v(TAG, String.format("list requirement %d matches %s.", i, n.getId()));
                            matches.add(n);
                            met[i] = true;
                            break;
                        }
                    }
                }
                for (boolean b : met) {
                    if (!b) {
                        Log.d(TAG, "Failed to meet all list criteria.");
                        matches.clear();
                    }
                }
            }
            return matches;
        }
    }

    /**
     * A single criterion to check if a node should be considered in a given localization algorithm.
     */
    public static final class NodeMatchCriteria {
        boolean matchAll;
        Pattern idMatches;

        double[] posMin;
        double[] posMax;
        float[] angleMin;
        float[] angleMax;
        Pattern stateAlgMatches;
        long stateAgeMin = Long.MAX_VALUE;
        long stateAgeMax = Long.MIN_VALUE;
        int statePendingCountMin = Integer.MAX_VALUE;
        int statePendingCountMax = Integer.MIN_VALUE;

        double rangeMin = Double.MAX_VALUE;
        double rangeMax = Double.MIN_VALUE;
        Pattern rangeSigMatches;
        Pattern rangeAlgMatches;
        long rangeAgeMin = Long.MAX_VALUE;
        long rangeAgeMax = Long.MIN_VALUE;
        int rangePendingCountMin = Integer.MAX_VALUE;
        int rangePendingCountMax = Integer.MIN_VALUE;

        /**
         * True if any of the above match. For min/max values, a match is when the value is in [min, max].
         */
        boolean matches(Node n) {
            int pr = n.getRangePendingSize();
            int ps = n.getStatePendingSize();
            return matchAll ||
                (pr >= rangePendingCountMin && pr <= rangePendingCountMax) ||
                (ps >= statePendingCountMin && ps <= statePendingCountMax) ||
                (n.getRange().dist >= rangeMin && n.getRange().dist <= rangeMax) ||
                (n.getRange().actual >= rangeMin && n.getRange().actual <= rangeMax) ||
                (idMatches != null && idMatches.matcher(n.getId()).matches()) ||
                (stateAlgMatches != null && stateAlgMatches.matcher(n.getState().algorithm).matches()) ||
                (rangeSigMatches != null && rangeSigMatches.matcher(n.getRange().signal).matches()) ||
                (rangeAlgMatches != null && rangeAlgMatches.matcher(n.getRange().algorithm).matches()) ||
                    // TODO NPE
                (Calc.isLessThanOrEqual(n.getState().pos, posMax) && Calc.isLessThanOrEqual(posMin, n.getState().pos)) ||
                (Calc.isLessThanOrEqual(n.getState().angle, angleMax) && Calc.isLessThanOrEqual(angleMin, n.getState().angle)) ||
                (System.nanoTime() - n.getState().time >= stateAgeMin && System.nanoTime() - n.getState().time <= stateAgeMax) ||
                (System.nanoTime() - n.getRange().time >= rangeAgeMin && System.nanoTime() - n.getState().time <= rangeAgeMax);
        }
    }



    private void initialize() {

    }



    /*
     * These correspond to the logging database.
     */

//    /** node_id	remote_node_id	algorithm	estimate	actual	node_time node_datetime */
//    final CsvBuffer rangeLog = new CsvBuffer();
//
//    /** node_id	algorithm	node_time node_datetime	x	y	z	a	b	c */
//    final CsvBuffer algLog = new CsvBuffer();
}
