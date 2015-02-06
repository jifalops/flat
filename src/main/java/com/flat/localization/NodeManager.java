package com.flat.localization;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flat.sockets.MyConnectionSocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class NodeManager {

    private final SharedPreferences prefs;

    /** This is a list of all nodes NOT including the local node */
    private final List<Node> nodes = Collections.synchronizedList(new ArrayList<Node>());
    public boolean addNode(Node n) {
        if (nodes.contains(n)) return false;
        boolean added = nodes.add(n);
        n.readPrefs(prefs);
        n.registerListener(nodeListener);
        for (NodeManagerListener l : listeners) {
            l.onNodeAdded(n);
        }
        return added;
    }

    /** @return a new copy of the set of known nodes, with the local node at the end if included. */
    public List<Node> getNodes(boolean includeLocalNode) {
        List<Node> list = new ArrayList<Node>(nodes);
        if (includeLocalNode) list.add(localNode);
        return list;
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

    public Node findNodeByConnection(MyConnectionSocket mcs) {
        for (Node n : nodes) {
            if (n.getDataConnection() == mcs) {
                return n;
            }
        }
        return null;
    }

    public Node[] getConnectedNodes() {
        List<Node> connected = new ArrayList<Node>();
        for (Node n : nodes) {
            if (n.getDataConnection() != null) connected.add(n);
        }
        return connected.toArray(new Node[connected.size()]);
    }

    public int countConnectedNodes() {
        int count = 0;
        for (Node n : nodes) {
            if (n.getDataConnection() != null) ++count;
        }
        return count;
    }

    public CoordinateSystem.RangeTable getLocalRangeTable() {
        CoordinateSystem.RangeTable table = new CoordinateSystem.RangeTable(localNode.getState().referenceFrame);
        for (Node n : nodes) {
            if (n.getRange().range > 0) {
                CoordinateSystem.SimpleRange r = new CoordinateSystem.SimpleRange();
                r.range = n.getRange().range;
                r.time = n.getRange().time;
                table.put(n.getId(), r);
            }
        }
        return table;
    }

    public CoordinateSystem.RangeTableList getRangeTableList() {
        CoordinateSystem.RangeTableList list = new CoordinateSystem.RangeTableList();
        list.put(localNode.getId(), getLocalRangeTable());
        for (Node n : nodes) {
            if (n.getRangeTable() != null) {
                list.put(n.getId(), n.getRangeTable());
            }
        }
        return list;
    }

    private final Node localNode;
    public Node getLocalNode() { return localNode; }

    public NodeManager(Context ctx, Node localNode) {
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        this.localNode = localNode;
    }


    private final Node.NodeListener nodeListener = new Node.NodeListener() {
        @Override
        public void onRangePending(Node n, Node.Range r) {
            for (NodeManagerListener l : listeners) {
                l.onRangePending(n, r);
            }
        }

        @Override
        public void onStatePending(Node n, Node.State s) {
            for (NodeManagerListener l : listeners) {
                l.onStatePending(n, s);
            }
        }

        @Override
        public void onRangeChanged(Node n, Node.Range r) {
            for (NodeManagerListener l : listeners) {
                l.onRangeChanged(n, r);
            }
        }

        @Override
        public void onStateChanged(Node n, Node.State s) {
            for (NodeManagerListener l : listeners) {
                l.onStateChanged(n, s);
            }
        }
    };


    /**
     * Allow other objects to react to node events.
     */
    public static interface NodeManagerListener extends Node.NodeListener {
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
