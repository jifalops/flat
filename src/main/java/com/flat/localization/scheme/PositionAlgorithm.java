package com.flat.localization.scheme;

import com.flat.localization.Node;

import java.util.List;

/**
 * Created by Jacob Phillips (10/2014)
 */
public abstract class PositionAlgorithm implements LocationAlgorithm {

    @Override
    public final Node.State applyTo(Node target, List<Node> references) {
        Node.State s = new Node.State();
        s.algorithm = getName();
        s.pos = findCoords(Node.toPositionArray(references.toArray(new Node[references.size()])),
                Node.toRangeArray(references.toArray(new Node[references.size()])));
        s.angle = target.getState().angle;
        s.time = System.nanoTime();
        return s;
    }

    public abstract double[] findCoords(double[][] positions, double[] ranges);
}
