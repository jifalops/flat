package com.flat.localization;

import android.location.Location;

import com.flat.localization.algorithm.LocationAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class Node {
    /**
     * <pre>
     * Position:
     * 0:       x position
     * 1:       y position
     * 2:       z position
     *
     * Angle:
     * 0:       angle with respect to left-right axis on a vertically held phone.
     * 1:       angle with respect to the up-down axis on a vertically held phone.
     * 2:       angle with respect to the forward-back axis on a vertically held phone.
     * </pre>
     * For orientation of angle coordinates, see
     * <a href='http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords'>http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords</a>
     */
    public static class State {
        public double pos[];
        public double angle[];
        public long timestamp;
        public String locAlg;
        public double range;
        public String ranAlg;

        public State(){}
        public State(State s) {
            pos = s.pos;
            angle = s.angle;
            timestamp = s.timestamp;
            locAlg = s.locAlg;
            range = s.range;
            ranAlg = s.ranAlg;
        }
    }

    private final List<State> pending = new ArrayList<State>();
    private final List<State> history = new ArrayList<State>();
    private final String id;


    public Node(String id) {
        this.id = id;
    }
    public String getId() { return id; }


    public synchronized void addPending(State s) {
        synchronized (pending) {
            pending.add(s);
        }
    }



    /** Set current state */
    public synchronized void setState(State s) {
        history.add(s);
        notifyNodeChangedListeners();
    }

    /** Get previous (or current) state */
    public synchronized State getState(int index) {
        return new State(history.get(index));
    }
    /** Get current state */
    public synchronized State getState() {
        return getState(history.size() - 1);
    }

    public synchronized int getStateCount() {
        return history.size();
    }



    /**
     * Flatten several nodes' current state to a double[][].
     */
    public static double[][] toPositionArray(Node... nodes) {
        double[][] n = new double[nodes.length][3];
        for (int i=0; i<nodes.length; ++i) {
            for (int j=0; j<3; ++j) {
                n[i][j] = nodes[i].getState().pos[j];
            }
        }
        return n;
    }

    /**
     * Allow other objects to react to node changes.
     */
    public static interface NodeChangedListener {
        void onNodeChanged(Node n);
    }

    private final List<NodeChangedListener> listeners = new ArrayList<NodeChangedListener>(1);

    public void notifyNodeChangedListeners() {
        for (NodeChangedListener l: listeners) {
            l.onNodeChanged(this);
        }
    }
    public void registerNodeChangedListener(NodeChangedListener l) {
        listeners.add(l);
    }
    public void unregisterNodeChangedListener(NodeChangedListener l) {
        listeners.remove(l);
    }
    public void unregisterNodeChangedListeners() {
        listeners.clear();
    }
}
