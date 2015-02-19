package com.flat.localization.algorithms;

import com.flat.localization.node.Node;
import com.flat.localization.node.NodeState;

import java.util.List;

/**
 * Location algorithms attempt to update a target node's state by using the available information
 * about that node and other nodes.
 *
 * Created by Jacob Phillips (10/2014)
 */
public interface Algorithm {
    NodeState applyTo(Node target, List<Node> references);
    String getName();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    int getUseCount();

    /*
     * Allow other objects to react to algorithm changes.
     */
    interface AlgorithmListener {
        void onApplied(Algorithm la, Node target, List<Node> references);
    }
    boolean registerListener(AlgorithmListener l);
    boolean unregisterListener(AlgorithmListener l);
//    void notifyListeners(Node target, List<Node> references);
}
