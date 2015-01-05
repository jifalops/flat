package com.flat.localization.algorithm;

import android.util.Log;

import com.flat.localization.Node;
import com.flat.localization.util.Calc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class Criteria {



    public static final class AlgorithmMatchCriteria {
        private String TAG = AlgorithmMatchCriteria.class.getSimpleName();
        // All must be matched by all nodes
        public List<NodeMatchCriteria> nodeRequirements = new ArrayList<NodeMatchCriteria>();
        // All must be matched by at least one node
        public List<NodeMatchCriteria> nodeListRequirements = new ArrayList<NodeMatchCriteria>();

        /**
         * Note this moves on to the next node when a nodeListRequirement is met, so there will be
         * at least as many nodes as there are requirements.
         */
        public List<Node> filter(List<Node> nodes) {
            nodes = new ArrayList<Node>(nodes);
            List<Node> matches = new ArrayList<Node>();

            // Remove nodes that do not meet all criteria for individual nodes.
            int size = nodeRequirements.size();
            if (size > 0) {
                for (Node n : nodes) {
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
                for (Node n : nodes) {
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






    /**
     * A single criterion to check if a node should be considered in a given localization algorithm.
     */
    public static final class NodeMatchCriteria {
        public boolean matchAll;
        public Pattern idMatches;

        public float[] posMin = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        public float[] posMax = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
        public float[] angleMin = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        public float[] angleMax = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
        public Pattern stateAlgMatches;
        public long stateAgeMin = Long.MAX_VALUE;
        public long stateAgeMax = Long.MIN_VALUE;
        public int statePendingCountMin = Integer.MAX_VALUE;
        public int statePendingCountMax = Integer.MIN_VALUE;

        public float rangeMin = Float.MAX_VALUE;
        public float rangeMax = Float.MIN_VALUE;
        public Pattern rangeSigMatches;
        public Pattern rangeAlgMatches;
        public long rangeAgeMin = Long.MAX_VALUE;
        public long rangeAgeMax = Long.MIN_VALUE;
        public int rangePendingCountMin = Integer.MAX_VALUE;
        public int rangePendingCountMax = Integer.MIN_VALUE;

        /**
         * True if any of the above match. For min/max values, a match is when the value is in [min, max].
         */
        public boolean matches(Node n) {
            int pr = n.getRangePendingSize();
            int ps = n.getStatePendingSize();
            return matchAll ||
                    (pr >= rangePendingCountMin && pr <= rangePendingCountMax) ||
                    (ps >= statePendingCountMin && ps <= statePendingCountMax) ||
                    (n.getRange().range >= rangeMin && n.getRange().range <= rangeMax) ||
                    (n.getRange().rangeOverride >= rangeMin && n.getRange().rangeOverride <= rangeMax) ||
                    (idMatches != null && idMatches.matcher(n.getId()).matches()) ||
                    (stateAlgMatches != null && stateAlgMatches.matcher(n.getState().algorithm).matches()) ||
                    (rangeSigMatches != null && rangeSigMatches.matcher(n.getRange().signal).matches()) ||
                    (rangeAlgMatches != null && rangeAlgMatches.matcher(n.getRange().interpreter).matches()) ||
                    (Calc.isLessThanOrEqual(n.getState().pos, posMax) && Calc.isLessThanOrEqual(posMin, n.getState().pos)) ||
                    (Calc.isLessThanOrEqual(n.getState().angle, angleMax) && Calc.isLessThanOrEqual(angleMin, n.getState().angle)) ||
                    (System.currentTimeMillis() - n.getState().time >= stateAgeMin && System.currentTimeMillis() - n.getState().time <= stateAgeMax) ||
                    (System.currentTimeMillis() - n.getRange().time >= rangeAgeMin && System.currentTimeMillis() - n.getRange().time <= rangeAgeMax);
        }
    }
}
