package com.flat.localization.coordinatesystem;

import com.flat.localization.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Jacob Phillips (10/2014)
 */
public abstract class PositionAlgorithm implements LocationAlgorithm {

    private boolean enabled;
    private int count;

    private Set<AlgorithmListener> listeners = new HashSet<AlgorithmListener>(1);
    @Override
    public void registerListener(AlgorithmListener l) {
        listeners.add(l);
    }

    @Override
    public void unregisterListener(AlgorithmListener l) {
        if (l == null) {
            listeners.clear();
        } else {
            listeners.remove(l);
        }
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
    public final Node.State applyTo(Node target, List<Node> references) {
        ++count;
        Node.State s = new Node.State();
        s.algorithm = getName();
        float[][] positions = Node.toPositionArray(references.toArray(new Node[references.size()]));
        float[] ranges = Node.toRangeArray(references.toArray(new Node[references.size()]));
        s.pos = findCoords(positions, ranges);
        s.angle = target.getState().angle;
        s.time = System.nanoTime();

        for (AlgorithmListener l : listeners) {
            l.onApplied(this, target, references);
        }

        return s;
    }

    public abstract float[] findCoords(float[][] positions, float[] ranges);
}
