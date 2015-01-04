package com.flat.localization.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class NodeManager {

    /** This is a list of all nodes including the local node */
    private final Set<Node> nodes = Collections.synchronizedSet(new HashSet<Node>());
    public boolean addNode(Node n) {
        if (nodes.add(n)) {
//            n.registerListener(nodeListener);
            return true;
        }
        return false;
    }
    public Set<Node> getNodesCopy() {
        return new HashSet<Node>(nodes);
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



    private Node localNode;
    public Node getLocalNode() { return localNode; }
    public boolean setLocalNode(Node n) {
        localNode = n;
        return addNode(n);
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
