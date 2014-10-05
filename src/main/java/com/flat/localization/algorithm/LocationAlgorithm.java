package com.flat.localization.algorithm;

import com.flat.localization.Node;

/**
 * Created by Jacob Phillips (10/2014)
 */
public interface LocationAlgorithm {
    Node.State apply(Node me, Node[] nodes, double[] ranges);
}
