package com.essentiallocalization.localization;

import java.util.ArrayList;
import java.util.List;


public class Node {
    /**

     */
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

        @Override
        public String toString() {
            return String.format("P{%.3f,%.3f,%.3f} θ{%.1f,%.1f,%.1f} T{%d}",
                    pos[0], pos[1], pos[2], angle[0], angle[1], angle[2], timestamp);
        }
    }


    private final List<State> history = new ArrayList<State>();
    private final String name;


    public Node(String name) {
        this.name = name;
    }
    public Node() {
        this.name = "";
    }



    /** Set current state */
    public void setState(State s) {
        history.add(s);
    }

    /** Get current state */
    public State getState() {
        return getState(history.size() - 1);
    }

    /** Get previous (or current) state */
    public State getState(int index) {
        return history.get(index);
    }

    public int getStateCount() {
        return history.size();
    }


    public static double[][] toPositionList(Node[] nodes) {
        double[][] positions = new double[nodes.length][3];
        for (int i=0; i<nodes.length; ++i) {
            for (int j=0; j<3; ++j) {
                positions[i][j] = nodes[i].getState().pos[j];
            }
        }
    }

    /*
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
