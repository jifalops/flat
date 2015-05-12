package com.flat.localization;

import android.content.Context;
import android.content.SharedPreferences;

import com.flat.localization.node.Node;
import com.flat.localization.node.NodeRange;
import com.flat.localization.node.NodeState;
import com.flat.localization.node.RemoteNode;
import com.flat.sockets.MyConnectionSocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class NodeManager {
    private static final String TAG = NodeManager.class.getSimpleName();
    private static final String PREFS_FILE = "nodeman";

    private final Node localNode;
    public Node getLocalNode() { return localNode; }

    private final List<RemoteNode> nodes = Collections.synchronizedList(new ArrayList<RemoteNode>());
    public List<RemoteNode> getNodes() { return nodes; }

    private final SharedPreferences prefs;

    public NodeManager(Context ctx, Node localNode) {
        prefs = ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        this.localNode = localNode;
    }


    public boolean addNode(RemoteNode n) {
        if (nodes.contains(n)) return false;
        boolean added = nodes.add(n);
        n.readPrefs(prefs);
        n.registerListener(nodeListener);
        for (NodeManagerListener l : listeners) {
            l.onNodeAdded(n);
        }
        return added;
    }


    public int getNodeCount() {
        return nodes.size();
    }
    public RemoteNode getNode(int index) {
        return nodes.get(index);
    }
    public RemoteNode getNode(String id) {
        for (RemoteNode n : nodes) {
            if (n.getId().equals(id)) return n;
        }
        return null;
    }

    public RemoteNode findNodeByConnection(MyConnectionSocket mcs) {
        for (RemoteNode n : nodes) {
            if (n.getDataConnection() == mcs) {
                return n;
            }
        }
        return null;
    }

    public RemoteNode[] getConnectedNodes() {
        List<RemoteNode> connected = new ArrayList<RemoteNode>();
        for (RemoteNode n : nodes) {
            if (n.getDataConnection() != null) connected.add(n);
        }
        return connected.toArray(new RemoteNode[connected.size()]);
    }

    public int countConnectedNodes() {
        int count = 0;
        for (RemoteNode n : nodes) {
            if (n.getDataConnection() != null) ++count;
        }
        return count;
    }

    public CoordinateSystem.RangeTable getLocalRangeTable() {
        CoordinateSystem.RangeTable table = new CoordinateSystem.RangeTable(localNode.getState().referenceFrame);
        for (RemoteNode n : nodes) {
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






    private final RemoteNode.RemoteNodeListener nodeListener = new RemoteNode.RemoteNodeListener() {
        @Override
        public void onRangePending(RemoteNode n, NodeRange r) {
            for (NodeManagerListener l : listeners) {
                l.onRangePending(n, r);
            }
        }

        @Override
        public void onStatePending(Node n, NodeState s) {
            for (NodeManagerListener l : listeners) {
                l.onStatePending(n, s);
            }
        }

        @Override
        public void onRangeChanged(RemoteNode n, NodeRange r) {
            for (NodeManagerListener l : listeners) {
                l.onRangeChanged(n, r);
            }
        }

        @Override
        public void onStateChanged(Node n, NodeState s) {
            for (NodeManagerListener l : listeners) {
                l.onStateChanged(n, s);
            }
        }
    };


    /**
     * Allow other objects to react to node events.
     */
    public interface NodeManagerListener extends RemoteNode.RemoteNodeListener {
        void onNodeAdded(RemoteNode n);
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
