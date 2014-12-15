package com.flat.localization.coordinatesystem;

import com.flat.localization.Node;

import java.util.List;

/**
 * Location algorithms attempt to update a target node's state by using the available information
 * about that node and other nodes.
 *
 * Created by Jacob Phillips (10/2014)
 */
public interface LocationAlgorithm {
    Node.State applyTo(Node target, List<Node> references);
    String getName();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    int getUseCount();

    /*
     * Allow other objects to react to algorithm changes.
     */
    interface AlgorithmListener {
        void onApplied(LocationAlgorithm la, Node target, List<Node> references);
    }
    void registerListener(AlgorithmListener l);
    void unregisterListener(AlgorithmListener l);
//    void notifyListeners(Node target, List<Node> references);
}
