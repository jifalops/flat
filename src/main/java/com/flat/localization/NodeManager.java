package com.flat.localization;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class NodeManager {

    private final SharedPreferences prefs;

    /** This is a list of all nodes NOT including the local node */
    private final Set<Node> nodes = Collections.synchronizedSet(new HashSet<Node>());
    public boolean addNode(Node n) {
        if (nodes.add(n)) {
//            n.registerListener(nodeListener);
            n.readPrefs(prefs);
            return true;
        }
        return false;
    }

    /** @return a new copy of the set of known nodes */
    public Set<Node> getNodes(boolean includeLocalNode) {
        Set<Node> nodeSet = new HashSet<Node>(nodes);
        if (includeLocalNode) nodeSet.add(localNode);
        return nodeSet;
    }
    public int getNodeCount() {
        return nodes.size();
    }
    public Node getNode(String id) {
        for (Node n : nodes) {
            if (n.getId().equals(id)) return n;
        }
        return null;
    }



    private final Node localNode;
    public Node getLocalNode() { return localNode; }

    public NodeManager(Context ctx, Node localNode) {
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        this.localNode = localNode;
    }


    /**
     * Allow other objects to react to node events.
     */
    public static interface NodeManagerListener {
        void onNodeAdded(Node n);
    }
    private final List<NodeManagerListener> listeners = new ArrayList<NodeManagerListener>(1);
    public boolean registerListener(NodeManagerListener l) {
        if (listeners.contains(l)) return false;
        return listeners.add(l);
    }
    public boolean unregisterListener(NodeManagerListener l) {
        return listeners.remove(l);
    }
}
