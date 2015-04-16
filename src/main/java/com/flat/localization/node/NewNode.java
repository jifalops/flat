package com.flat.localization.node;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.flat.AppController;
import com.flat.localization.CoordinateSystem;
import com.flat.sockets.MyConnectionSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacob Phillips.
 */
public final class NewNode {
    private static final String TAG = NewNode.class.getSimpleName();
    private final String bssid;
    private RangeTable rangeTable = new RangeTable();
    private boolean isPassive;
    private final boolean isNodeThisDevice;
    private MyConnectionSocket connection;
    private CoordinateSystem coords; //TODO
    private float rangeOverride;
    private String name;

    public NewNode(String bssid, boolean isNodeThisDevice) {
        this.bssid = bssid;
        this.name = bssid;
        this.isNodeThisDevice = isNodeThisDevice;
        isPassive = !isNodeThisDevice;
        readPrefs();
    }
    public NewNode(String bssid) {
        this(bssid, false);
    }

    public String getId() { return bssid; }
    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        savePrefs();
    }

    public void setRangeOverride(float range) {
        rangeOverride = range;
    }
    public float getRangeOverride() {
        return rangeOverride;
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

    public void setCoords(CoordinateSystem coords) {
        this.coords = coords;
        for (NodeListener l : listeners) l.onCoordsChange(this, coords);
    }
    public CoordinateSystem getCoords() { return coords; }

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

    @Override
    public String toString() {
        return bssid;
    }



    /**
     * Allow other objects to react to node events.
     */
    public static interface NodeListener {
        void onCoordsChange(NewNode node, CoordinateSystem coords);
        void onConnectionChange(NewNode node, MyConnectionSocket conn);
        void onNewRangeTable(NewNode node, RangeTable rangeTable);
        void onRangeChange(NewNode node, RangeTable rangeTable);
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
