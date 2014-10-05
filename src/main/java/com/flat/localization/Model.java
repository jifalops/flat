package com.flat.localization;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Pair;

import com.flat.localization.algorithm.LocationAlgorithm;
import com.flat.localization.ranging.Ranging;
import com.flat.localization.signal.Signal;
import com.flat.util.CsvBuffer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jacob Phillips (10/2014)
 */
public class Model {
    /*
     * Simple Singleton
     */
    private Model() {}
    private static final Model instance = new Model();
    public static Model getInstance() { return instance; }

    /** Known nodes. */
    final Map<String, Node> nodes = new HashMap<String, Node>();

    /** Signal chooser (string to describe runnable) */
    final Map<Signal, Pair<String, Runnable>> signals = new HashMap<Signal, Pair<String, Runnable>>();

    /** Algorithm chooser */
    final Map<LocationAlgorithm, Boolean> algorithms = new HashMap<LocationAlgorithm, Boolean>();

    /** node_id	remote_node_id	algorithm	estimate	actual	node_time */
    final CsvBuffer rangeLog = new CsvBuffer();

    /** node_id	algorithm	node_time	x	y	z	a	b	c */
    final CsvBuffer algLog = new CsvBuffer();
}
