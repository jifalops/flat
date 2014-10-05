package com.flat.localization;

import java.util.ArrayList;
import java.util.List;


public final class Node {
    public static class State {
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
        public final double pos[], angle[];
        public final long timestamp;

        public State(double[] pos, double[] angle, long timestamp) {
            this.pos = pos;
            this.angle = angle;
            this.timestamp = timestamp;
        }

        public State clone() {
            return new State(pos, angle, timestamp);
        }

        /**
         * @return pos[], angle[], timestamp
         */
        public double[] flatten() {
            return new double[] { pos[0], pos[1], pos[2], angle[0], angle[1], angle[2], timestamp};
        }

        @Override
        public String toString() {
            return String.format("P{%.3f,%.3f,%.3f} θ{%.1f,%.1f,%.1f} T{%d}",
                    pos[0], pos[1], pos[2], angle[0], angle[1], angle[2], timestamp);
        }
    }


    private final List<State> history = new ArrayList<State>();
    private final String id;


    public Node(String id) {
        this.id = id;
    }

    public String getId() { return id; }


    /** Set current state */
    public synchronized void setState(State s) {
        history.add(s);
        notifyNodeChangedListeners();
    }

    /** Get previous (or current) state */
    public synchronized State getState(int index) {
        return history.get(index).clone();
    }
    /** Get current state */
    public synchronized State getState() {
        return getState(history.size() - 1);
    }

    public synchronized int getStateCount() {
        return history.size();
    }


    /**
     * Flatten this node's history to a double[][].
     */
    public synchronized double[][] flatten() {
        double[][] n = new double[history.size()][7];
        for (int i=0; i<history.size(); ++i) {
            n[i] = history.get(i).flatten();
        }
        return n;
    }

    /**
     * Flatten several nodes' current state to a double[][].
     */
    public static double[][] flattenStates(Node... nodes) {
        double[][] n = new double[nodes.length][3];
        for (int i=0; i<nodes.length; ++i) {
            n[i] = nodes[i].getState().flatten();
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
