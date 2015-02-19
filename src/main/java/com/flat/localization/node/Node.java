package com.flat.localization.node;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.flat.localization.CoordinateSystem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodes are the main objects to be manipulated in the localization system and they are
 * designed here to facilitate being localized.
 */
public class Node {
    private static final String TAG = Node.class.getSimpleName();

    private CoordinateSystem.RangeTable rangeTable;
    public CoordinateSystem.RangeTable getRangeTable() { return rangeTable; }
    public void setRangeTable(CoordinateSystem.RangeTable table) {
        rangeTable = table;
    }

    private final List<NodeState> statePending = new ArrayList<NodeState>();
    private final List<NodeState> stateHistory = new ArrayList<NodeState>();

    public static final boolean idIsWifiMac = true;
    private final String id;
    private String name;
    private boolean fixed;

    public Node(String id) {
        this.id = id;
        this.name = id;
        fixed = true;
        stateHistory.add(new NodeState());
    }


    public String getId() {
        return id;
    }
    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
    }


    public synchronized boolean isFixed() { return fixed; }
    public synchronized void setFixed(boolean fixed) { this.fixed = fixed; }




    public synchronized void addPending(NodeState s) {
        statePending.add(s);
        for (NodeListener l: listeners) {
            l.onStatePending(this, s);
        }
    }


    public synchronized void update(NodeState s) {
        stateHistory.add(s);
        for (NodeListener l: listeners) {
            l.onStateChanged(this, s);
        }
    }

    /** Get previous (or current) state */
    public synchronized NodeState getState(int index) {
        return stateHistory.get(index);
    }



    /** Get current state */
    public synchronized NodeState getState() {
        return getState(stateHistory.size() - 1);
    }



    /** Get previous (or current) pending state */
    public synchronized NodeState getPendingState(int index) {
        return statePending.get(index);
    }



    /** Get most recent pending state */
    public synchronized NodeState getPendingState() { return getPendingState(statePending.size() - 1); }


    public synchronized int getStateHistorySize() {
        return stateHistory.size();
    }


    public synchronized int getStatePendingSize() {
        return stateHistory.size();
    }

    public synchronized void savePrefs(SharedPreferences prefs) {
        JSONObject json = new JSONObject();
        try {
            json.put("name", getName());
        } catch (JSONException ignored) {}
        prefs.edit().putString(getId(), json.toString()).apply();
    }

    public synchronized void readPrefs(SharedPreferences prefs) {
        String info = prefs.getString(getId(), "");
        try {
            JSONObject json = new JSONObject(info);
            if (!TextUtils.isEmpty(json.getString("name"))) {
                setName(json.getString("name"));
            }
        } catch (JSONException ignored) {}
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Flatten several nodes' current state to a float[][].
     */
    public static float[][] toPositionArray(Node... nodes) {
        float[][] n = new float[nodes.length][3];
        for (int i=0; i<nodes.length; ++i) {
            for (int j=0; j<3; ++j) {
                n[i][j] = nodes[i].getState().pos[j];
            }
        }
        return n;
    }



    /**
     * Allow other objects to react to node events.
     */
    public static interface NodeListener {
        void onStatePending(Node n, NodeState s);
        void onStateChanged(Node n, NodeState s);
    }
    protected final List<NodeListener> listeners = new ArrayList<NodeListener>(1);
    public boolean registerListener(NodeListener l) {
        if (listeners.contains(l)) return false;
        return listeners.add(l);
    }
    public boolean unregisterListener(NodeListener l) {
        return listeners.remove(l);
    }
}
