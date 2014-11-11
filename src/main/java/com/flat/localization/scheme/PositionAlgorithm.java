package com.flat.localization.scheme;

import com.flat.localization.Node;

/**
 * Created by Jacob Phillips (10/2014)
 */
public abstract class PositionAlgorithm implements LocationAlgorithm {

    @Override
    public final Node.State applyTo(Node target, Node... references) {
        Node.State s = new Node.State();
        s.algorithm = getName();
        s.pos = findCoords(Node.toPositionArray(references), Node.toRangeArray(references));
        s.angle = target.getState().angle;
        s.time = System.nanoTime();
        return s;
    }

    public abstract double[] findCoords(double[][] positions, double[] ranges);
}
