package com.flat.localization.algorithm;

import com.flat.localization.Node;

/**
 * Created by Jacob Phillips (10/2014)
 */
public abstract class PositionAlgorithm implements LocationAlgorithm {

    @Override
    public final Node.State apply(Node me, Node[] nodes, double[] ranges) {
        Node.State s = new Node.State();
        s.pos = findCoords(Node.toPositionArray(nodes), ranges);
        s.angle = me.getState().angle;
        s.timestamp = System.nanoTime();
        return s;
    }

    public abstract double[] findCoords(double[][] positions, double[] ranges);
}
