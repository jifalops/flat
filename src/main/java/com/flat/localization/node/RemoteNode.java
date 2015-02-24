package com.flat.localization.node;

import com.flat.sockets.MyConnectionSocket;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodes are the main objects to be manipulated in the localization system and they are
 * designed here to facilitate being localized.
 */
public final class RemoteNode extends Node {
    private static final String TAG = RemoteNode.class.getSimpleName();

    private final List<NodeRange> rangePending = new ArrayList<NodeRange>();
    private final List<NodeRange> rangeHistory = new ArrayList<NodeRange>();

    private MyConnectionSocket connection;
    public synchronized void setDataConnection(MyConnectionSocket mcs) {
        connection = mcs;
    }
    public synchronized MyConnectionSocket getDataConnection() {
        return connection;
    }

    private String[] connectedNodes;
    public void setConnectedNodes(String[] nodes) { connectedNodes = nodes; }
    public String[] getConnectedNodes() { return connectedNodes; }

    private float actualRangeOverride;
    public synchronized void setActualRangeOverride(float range) {
        actualRangeOverride = range;
    }
    public synchronized float getActualRangeOverride() {
        return actualRangeOverride;
    }

    public RemoteNode(String id) {
        super(id);
    }

    public synchronized void addPending(NodeRange r) {
        rangePending.add(r);
        for (RemoteNodeListener l: listeners) {
            l.onRangePending(this, r);
        }
    }

    public synchronized void update(NodeRange r) {
        r.rangeOverride = getActualRangeOverride();
        rangeHistory.add(r);
        for (RemoteNodeListener l: listeners) {
            l.onRangeChanged(this, r);
        }
    }

    /** Get previous (or current) range */
    public synchronized NodeRange getRange(int index) {
        return rangeHistory.get(index);
    }

    /** Get current range */
    public synchronized NodeRange getRange() {
        return getRange(rangeHistory.size() - 1);
    }

    /** Get previous (or current) pending range */
    public synchronized NodeRange getPendingRange(int index) {
        return rangePending.get(index);
    }

    /** Get most recent pending range */
    public synchronized NodeRange getPendingRange() { return getPendingRange(rangePending.size() - 1); }

    public synchronized int getRangeHistorySize() {
        return rangeHistory.size();
    }
    public synchronized int getRangePendingSize() {
        return rangeHistory.size();
    }

    /**
     * Flatten several nodes' current ranges to a float[].
     */
    public static float[] toRangeArray(RemoteNode... nodes) {
        float[] r = new float[nodes.length];
        for (int i=0; i<nodes.length; ++i) {
            r[i] = nodes[i].getRange().range;
        }
        return r;
    }


    /**
     * Allow other objects to react to node events.
     */
    public static interface RemoteNodeListener extends NodeListener {
        void onRangePending(RemoteNode n, NodeRange r);
        void onRangeChanged(RemoteNode n, NodeRange r);
    }
    protected final List<RemoteNodeListener> listeners = new ArrayList<RemoteNodeListener>(1);
    public boolean registerListener(RemoteNodeListener l) {
        if (listeners.contains(l)) return false;
        return listeners.add(l);
    }
    public boolean unregisterListener(RemoteNodeListener l) {
        return listeners.remove(l);
    }
}
