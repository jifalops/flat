package com.flat.localization.algorithms;

import com.flat.localization.node.Node;
import com.flat.localization.node.NodeState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Jacob Phillips (10/2014)
 */
public abstract class PositionAlgorithm implements Algorithm {

    private boolean enabled;
    private int count;

    private Set<AlgorithmListener> listeners = new HashSet<AlgorithmListener>(1);
    @Override
    public boolean registerListener(AlgorithmListener l) {
        if (listeners.contains(l)) return false;
        return listeners.add(l);
    }

    @Override
    public boolean unregisterListener(AlgorithmListener l) {
        return listeners.remove(l);
    }

    @Override
    public int getUseCount() {
        return count;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public final NodeState applyTo(Node target, List<Node> references) {
        ++count;
        NodeState s = new NodeState();
        s.algorithm = getName();
        float[][] positions = Node.toPositionArray(references.toArray(new Node[references.size()]));
        float[] ranges = Node.toRangeArray(references.toArray(new Node[references.size()]));
        s.pos = findCoords(positions, ranges);
        s.angle = target.getState().angle;
        s.time = System.currentTimeMillis(); //System.nanoTime();

        for (AlgorithmListener l : listeners) {
            l.onApplied(this, target, references);
        }

        return s;
    }

    public abstract float[] findCoords(float[][] positions, float[] ranges);
}
