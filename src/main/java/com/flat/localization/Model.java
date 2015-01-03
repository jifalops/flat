package com.flat.localization;

import android.util.Pair;

import com.flat.localization.node.Node;
import com.flat.localization.signal.rangingandprocessing.SignalInterpreter;
import com.flat.localization.coordinatesystem.LocationAlgorithm;
import com.flat.localization.signal.Signal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Model contains the known nodes, which signals to use, and which location algorithms to use.
 */
public final class Model {


    /*
     * Singleton (simple)
     */
    private Model() {}
    private static final Model instance = new Model();
    public static Model getInstance() { return instance; }



    /*
     * Events
     */
    public static interface ModelListener {
        void onNodeAdded(Node n);
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



    /**
     * Known nodes. The string is the node.id(), it is kept in a map so we don't need to loop through
     * all nodes every time input is received.
     */
    private final List<Node> nodes = Collections.synchronizedList(new ArrayList<Node>());

    public List<Node> getNodesCopy() {
        return new ArrayList<Node>(nodes);
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public Node getNode(int index) {
        return nodes.get(index);
    }

    public Node getNode(String id) {
        for (Node n : nodes) {
            if (n.getId().equals(id)) return n;
        }
        return null;
    }

    public void addNode(Node n) {
        nodes.add(n);
        for (ModelListener l : listeners) {
            l.onNodeAdded(n);
        }
    }




    /** All Signals and which ranging algorithms they can use */
    private final Map<Signal, List<SignalInterpreter>> signals = Collections.synchronizedMap(new LinkedHashMap<Signal, List<SignalInterpreter>>());

    public int getSignalCount() {
        return signals.size();
    }

    public Signal[] getSignals() {
        return signals.keySet().toArray(new Signal[signals.size()]);
    }

    public List<SignalInterpreter> getRangingProcessors(Signal signal) {
        return signals.get(signal);
    }

    public void addSignal(Signal signal, List<SignalInterpreter> processors) {
        signals.put(signal, processors);
    }





    /**
     * Location algorithms that can be applied to each node.
     */
    public final Map<LocationAlgorithm, Criteria.AlgorithmMatchCriteria> algorithms =
            Collections.synchronizedMap(new LinkedHashMap<LocationAlgorithm, Criteria.AlgorithmMatchCriteria>());

    public int getAlgorithmCount() {
        return algorithms.size();
    }

    public LocationAlgorithm[] getAlgorithms() {
        return algorithms.keySet().toArray(new LocationAlgorithm[algorithms.size()]);
    }

    public Criteria.AlgorithmMatchCriteria getCriteria(LocationAlgorithm la) {
        return algorithms.get(la);
    }

    public void addAlgorithm(LocationAlgorithm la, Criteria.AlgorithmMatchCriteria amc) {
        algorithms.put(la, amc);
    }




    // TODO nodes -> rangetable for sending, accept other range tables.
    public Map<String, Pair<Float, Long>> getRangeTable() {
        Map<String, Pair<Float, Long>> rangeTable = new HashMap<String, Pair<Float, Long>>();
        for (Node n : nodes) {
            rangeTable.put(n.getId(), new Pair<Float, Long>(n.getRange().rangeOverride, n.getRange().time));
        }
        return rangeTable;
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
