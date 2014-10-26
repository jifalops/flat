package com.flat.localization;

import com.flat.localization.ranging.SignalProcessor;
import com.flat.localization.scheme.LocationAlgorithm;
import com.flat.localization.signal.Signal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Model contains the known nodes, which signals to use, and which location algorithms to use.
 */
public class Model {
    /*
     * Simple Singleton
     */
    private Model() { initialize(); }
    private static final Model instance = new Model();
    public static Model getInstance() { return instance; }

    /**
     * Known nodes. The string is the node.id(), it is kept in a map so we dont need to loop through
     * all nodes every time we input is received from them.
     */
    final Map<String, Node> nodes = new HashMap<String, Node>();

    /** All Signals and which ranging algorithms they can use, and whether it is enabled by the user */
    final Map<Signal, Map<SignalProcessor, Boolean>> signals = new HashMap<Signal, Map<SignalProcessor, Boolean>>();

    /** All LocationAlgorithms and whether each is enabled by the user */
    final Map<LocationAlgorithm, Boolean> algorithms = new HashMap<LocationAlgorithm, Boolean>();

    /** Location algorithms that can be applied to each node */
    final Map<Node, List<LocationAlgorithm>> nodeAlgorithms = new HashMap<Node, List<LocationAlgorithm>>();

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
