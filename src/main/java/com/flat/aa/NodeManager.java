package com.flat.aa;

import com.flat.AppController;
import com.flat.sockets.MyConnectionSocket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class NodeManager {
    static final String TAG = NodeManager.class.getSimpleName();

    final String wifiMac;
    final Map<String, Node> nodes;

    private static NodeManager instance;
    public static NodeManager getInstance() {
        if (instance == null) {
            instance = new NodeManager();
        }
        return instance;
    }
    private NodeManager() {
        wifiMac = AppController.getInstance().getWifiMac();
        nodes = new HashMap<String, Node>();
        addNode(new Node(wifiMac, true));
    }


    public boolean addNode(Node n) {
        if (nodes.containsKey(n.getId())) return false;
        nodes.put(n.getId(), n);

//        n.registerListener(nodeListener);
        for (NodeManagerListener l : listeners) {
            l.onNodeAdded(n);
        }
        return true;
    }

    public Node getLocalNode() {
        return nodes.get(wifiMac);
    }

    public Node getNode(String id) {
        return nodes.get(id);
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public Node getNodeByConnection(MyConnectionSocket conn) {
        for (Node n : nodes.values()) {
            if (n.getConnection() == conn) {
                return n;
            }
        }
        return null;
    }

    public String[] getConnectedNodes() {
        List<String> connected = new ArrayList<String>();
        for (Node n : nodes.values()) {
            if (n.getConnection() != null) connected.add(n.getId());
        }
        return connected.toArray(new String[connected.size()]);
    }





//    final Node.NodeListener nodeListener = new Node.NodeListener() {
//        @Override
//        public void onCoordsChange(Node node, CoordinateSystem coords) {
//            for (NodeManagerListener l : listeners) {
//                l.onCoordsChange(node, coords);
//            }
//        }
//
//        @Override
//        public void onConnectionChange(Node node, MyConnectionSocket conn) {
//            for (NodeManagerListener l : listeners) {
//                l.onConnectionChange(node, conn);
//            }
//        }
//
//        @Override
//        public void onNewRangeTable(Node node, RangeTable rangeTable) {
//            for (NodeManagerListener l : listeners) {
//                l.onNewRangeTable(node, rangeTable);
//            }
//        }
//
//        @Override
//        public void onRangeChange(Node node, RangeTable.Entry entry) {
//            for (NodeManagerListener l : listeners) {
//                l.onRangeChange(node, entry);
//            }
//        }
//    };


    /**
     * Allow other objects to react to node events.
     */
    public interface NodeManagerListener {
        void onNodeAdded(Node n);
    }
    private final List<NodeManagerListener> listeners = new ArrayList<NodeManagerListener>(1);
    public boolean registerListener(NodeManagerListener l) {
        return !listeners.contains(l) && listeners.add(l);
    }
    public boolean unregisterListener(NodeManagerListener l) {
        return listeners.remove(l);
    }
}
