package com.flat.localization.scheme;

import com.flat.localization.Node;

/**
 * Created by Jacob Phillips (10/2014)
 */
public interface LocationAlgorithm {
    Node.State apply(Node target, Node... references);
    String getName();
}
