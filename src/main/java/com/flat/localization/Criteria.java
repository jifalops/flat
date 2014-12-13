package com.flat.localization;

import android.util.Log;

import com.flat.localization.util.Calc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class Criteria {



    public static final class AlgorithmMatchCriteria {
        String TAG = AlgorithmMatchCriteria.class.getSimpleName();
        // All must be matched by all nodes
        List<NodeMatchCriteria> nodeRequirements = new ArrayList<NodeMatchCriteria>();
        // All must be matched by at least one node
        List<NodeMatchCriteria> nodeListRequirements = new ArrayList<NodeMatchCriteria>();

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
        boolean matchAll;
        Pattern idMatches;

        double[] posMin = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] posMax = {Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE};
        float[] angleMin = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] angleMax = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
        Pattern stateAlgMatches;
        long stateAgeMin = Long.MAX_VALUE;
        long stateAgeMax = Long.MIN_VALUE;
        int statePendingCountMin = Integer.MAX_VALUE;
        int statePendingCountMax = Integer.MIN_VALUE;

        double rangeMin = Double.MAX_VALUE;
        double rangeMax = Double.MIN_VALUE;
        Pattern rangeSigMatches;
        Pattern rangeAlgMatches;
        long rangeAgeMin = Long.MAX_VALUE;
        long rangeAgeMax = Long.MIN_VALUE;
        int rangePendingCountMin = Integer.MAX_VALUE;
        int rangePendingCountMax = Integer.MIN_VALUE;

        /**
         * True if any of the above match. For min/max values, a match is when the value is in [min, max].
         */
        boolean matches(Node n) {
            int pr = n.getRangePendingSize();
            int ps = n.getStatePendingSize();
            return matchAll ||
                    (pr >= rangePendingCountMin && pr <= rangePendingCountMax) ||
                    (ps >= statePendingCountMin && ps <= statePendingCountMax) ||
                    (n.getRange().dist >= rangeMin && n.getRange().dist <= rangeMax) ||
                    (n.getRange().actual >= rangeMin && n.getRange().actual <= rangeMax) ||
                    (idMatches != null && idMatches.matcher(n.getId()).matches()) ||
                    (stateAlgMatches != null && stateAlgMatches.matcher(n.getState().algorithm).matches()) ||
                    (rangeSigMatches != null && rangeSigMatches.matcher(n.getRange().signal).matches()) ||
                    (rangeAlgMatches != null && rangeAlgMatches.matcher(n.getRange().algorithm).matches()) ||
                    (Calc.isLessThanOrEqual(n.getState().pos, posMax) && Calc.isLessThanOrEqual(posMin, n.getState().pos)) ||
                    (Calc.isLessThanOrEqual(n.getState().angle, angleMax) && Calc.isLessThanOrEqual(angleMin, n.getState().angle)) ||
                    (System.nanoTime() - n.getState().time >= stateAgeMin && System.nanoTime() - n.getState().time <= stateAgeMax) ||
                    (System.nanoTime() - n.getRange().time >= rangeAgeMin && System.nanoTime() - n.getState().time <= rangeAgeMax);
        }
    }
}
