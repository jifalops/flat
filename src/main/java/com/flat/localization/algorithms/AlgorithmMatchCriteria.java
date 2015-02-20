package com.flat.localization.algorithms;

import android.util.Log;

import com.flat.localization.node.Node;
import com.flat.localization.node.NodeMatchCriteria;
import com.flat.localization.node.RemoteNode;

import java.util.ArrayList;
import java.util.List;

/**
* @author Jacob Phillips (02/2015, jphilli85 at gmail)
*/
public final class AlgorithmMatchCriteria {
    private String TAG = AlgorithmMatchCriteria.class.getSimpleName();
    // All must be matched by all nodes
    public List<NodeMatchCriteria> nodeRequirements = new ArrayList<NodeMatchCriteria>();
    // All must be matched by at least one node
    public List<NodeMatchCriteria> nodeListRequirements = new ArrayList<NodeMatchCriteria>();

    /**
     * Note this moves on to the next node when a nodeListRequirement is met, so there will be
     * at least as many nodes as there are requirements.
     */
    public List<Node> filter(List<RemoteNode> nodes) {
        nodes = new ArrayList<RemoteNode>(nodes);
        List<Node> matches = new ArrayList<Node>();

        // Remove nodes that do not meet all criteria for individual nodes.
        int size = nodeRequirements.size();
        if (size > 0) {
            for (RemoteNode n : nodes) {
                for (NodeMatchCriteria nmc : nodeRequirements) {
                    if (!nmc.matches(n)) {
                        Log.v(TAG, n.getId() + " did not meet requirements.");
                        nodes.remove(n);
                        break;
                    }
                }
            }
        }

        // Add all nodes that meet at least one list criterion.
        size = nodeListRequirements.size();
        if (size > 0) {
            boolean[] met = new boolean[size];
            for (RemoteNode n : nodes) {
                for (int i = 0; i < size; ++i) {
                    if (nodeListRequirements.get(i).matches(n)) {
                        Log.v(TAG, String.format("list requirement %d matches %s.", i, n.getId()));
                        matches.add(n);
                        met[i] = true;
                        break;
                    }
                }
            }
            for (boolean b : met) {
                if (!b) {
                    Log.d(TAG, "Failed to meet all list criteria.");
                    matches.clear();
                }
            }
        } else {
            matches.addAll(nodes);
        }
        return matches;
    }

}
