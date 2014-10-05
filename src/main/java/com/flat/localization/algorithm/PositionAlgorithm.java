package com.flat.localization.algorithm;

import com.flat.localization.Node;

/**
 * Created by Jacob Phillips (10/2014)
 */
public abstract class PositionAlgorithm implements LocationAlgorithm {

    @Override
    public final Node.State apply(Node me, Node[] nodes, double[] ranges) {
        double[] coords = findCoords(Node.toPositionArray(nodes), ranges);
        return new Node.State(coords, me.getState().angle, System.nanoTime());
    }

    public abstract double[] findCoords(double[][] positions, double[] ranges);
}
