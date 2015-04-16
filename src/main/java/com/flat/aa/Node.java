package com.flat.aa;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.flat.AppController;
import com.flat.sockets.MyConnectionSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacob Phillips.
 */
public final class Node {
    static final String TAG = Node.class.getSimpleName();
    final String bssid;
    RangeTable rangeTable;
    boolean isPassive;
    final boolean isNodeThisDevice;
    MyConnectionSocket connection;
    float rangeOverride;
    String name;
    CoordinateSystem coords;

    Node(String bssid, boolean isNodeThisDevice) {
        this.bssid = bssid;
        this.name = bssid;
        this.isNodeThisDevice = isNodeThisDevice;
        isPassive = !isNodeThisDevice;
        rangeTable = new RangeTable();
        coords = new CoordinateSystem();
        coords.setState(new State(bssid));
        readPrefs();
    }
    public Node(String bssid) {
        this(bssid, false);
    }

    public String getId() { return bssid; }
    public String getName() { return name; }
    public void setName(String name) {
        if (!this.name.equals(name)) {
            this.name = name;
            savePrefs();
        }
    }

    public void setRangeOverride(float range) {
        rangeOverride = range;
    }
    public float getRangeOverride() {
        return rangeOverride;
    }

    public void setRange(RangeTable.Entry entry) {
        rangeTable.putEntry(entry);
        for (NodeListener l : listeners) l.onRangeChange(this, entry);
    }

    public RangeTable getRangeTable() { return rangeTable; }
    public void setRangeTable(RangeTable rt) {
        rangeTable = rt;
        for (NodeListener l : listeners) l.onNewRangeTable(this, rt);
    }

    public void setConnection(MyConnectionSocket conn) {
        if (conn != null) {
            isPassive = false;
        }
        if (connection != null && connection != conn) {
            Log.e(TAG, "Overriding connection to " + name);
        }
        connection = conn;
        for (NodeListener l : listeners) l.onConnectionChange(this, conn);
    }
    public MyConnectionSocket getConnection() { return connection; }

    public boolean isPassive() { return isPassive; }
    public boolean isNodeThisDevice() { return isNodeThisDevice; }

    private void savePrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                AppController.getInstance());
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
        } catch (JSONException ignored) {}
        prefs.edit().putString(getId(), json.toString()).apply();
    }

    private void readPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                AppController.getInstance());
        String info = prefs.getString(getId(), "");
        try {
            JSONObject json = new JSONObject(info);
            if (!TextUtils.isEmpty(json.getString("name"))) {
                name = (json.getString("name"));
            }
        } catch (JSONException ignored) {}
    }

    public void setCoords(CoordinateSystem coords) {
        this.coords = coords;
        for (NodeListener l : listeners) l.onCoordsChange(this, coords);
    }

    public CoordinateSystem getCoords() { return coords; }

//    public void setPosition(float[] pos, String algorithm) {
//        State s = getState();
//        s.pos[0] = pos[0];
//        s.pos[1] = pos[1];
//        s.pos[2] = pos[2];
//        s.algorithm = algorithm;
//        s.time = System.currentTimeMillis();
//        for (NodeListener l : listeners) l.onCoordsChange(this, coords);
//    }
    public State getState() {
        return coords.getState(bssid);
    }

    public void setOrientation(float[] orientation) {
        State s = getState();
        s.angle[0] = orientation[0];
        s.angle[1] = orientation[1];
        s.angle[2] = orientation[2];
    }

    @Override
    public String toString() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", bssid);
            json.put("name", name);
            json.put("isPassive", isPassive);
            json.put("rangeTable", rangeTable);
            json.put("coordinateSystem", coords);
            return json.toString();
        } catch (JSONException ignored) {}
        return super.toString();
    }

    static Node from(String jsonObject) {
        try {
            JSONObject json = new JSONObject(jsonObject);
            String id = json.getString("id");
            Node n = NodeManager.getInstance().getNode(id);
            if (n == null) {
                n = new Node(id);
                NodeManager.getInstance().addNode(n);
            }
            n.setName(json.getString("name"));
            n.isPassive = json.getBoolean("isPassive");
            n.setRangeTable(new RangeTable(json.getString("rangeTable")));
            n.setCoords(new CoordinateSystem(json.getString("coordinateSystem")));

            return n;
        } catch (JSONException ignored) {}
        return null;
    }

    /**
     * Allow other objects to react to node events.
     */
    interface NodeListener {
        void onCoordsChange(Node node, CoordinateSystem coords);
        void onConnectionChange(Node node, MyConnectionSocket conn);
        void onNewRangeTable(Node node, RangeTable rangeTable);
        void onRangeChange(Node node, RangeTable.Entry entry);
    }
    private final List<NodeListener> listeners = new ArrayList<NodeListener>(1);
    public boolean registerListener(NodeListener l) {
        return !listeners.contains(l) && listeners.add(l);
    }
    public boolean unregisterListener(NodeListener l) {
        return listeners.remove(l);
    }
}
