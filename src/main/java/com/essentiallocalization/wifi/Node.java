package com.essentiallocalization.wifi;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips
 * For orientation of angle coordinates,
 * @see <a href='http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords'>http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords</a>
 */
public class Node {
    /** optional id field */
    public volatile long id;

    /**
     * index    Desc
     * 0:       x position
     * 1:       y position
     * 2:       z position
     */
    public final float pos[] = {0,0,0};

    /**
     * index    Desc
     * 0:       angle with respect to left-right axis on a vertically held phone.
     * 1:       angle with respect to the up-down axis on a vertically held phone.
     * 2:       angle with respect to the forward-back axis on a vertically held phone.
     */
    public final float angle[] = {0,0,0};

    public volatile long changeCount;
    public volatile long firstChangeTimeMillis;
    public volatile boolean isLocalized;

    @Override
    public String toString() {
        return String.format("%d: P{%.3f,%.3f,%.3f} θ{%.1f,%.1f,%.1f}", changeCount,
                pos[0], pos[1], pos[2], angle[0], angle[1], angle[2]);
    }

    public static interface NodeChangedListener { void onNodeChanged(Node n); }
    private final List<NodeChangedListener> listeners = new ArrayList<NodeChangedListener>(1);
    public void notifyNodeChangedListeners() { for (NodeChangedListener l: listeners) l.onNodeChanged(this); }
    public void registerNodeChangedListener(NodeChangedListener l) { listeners.add(l); }
    public void unregisterNodeChangedListener(NodeChangedListener l) { listeners.remove(l); }
    public void unregisterNodeChangedListeners() { listeners.clear(); }
}
