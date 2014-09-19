package com.essentiallocalization.localization.scheme;

import android.os.Bundle;

import com.essentiallocalization.localization.Node;
import com.essentiallocalization.localization.distance.Ranging;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips
 */
public final class MinMax implements Scheme {

    public static final String ARG_REFERENCE_NODES = "ref_nodes";
    public static final String ARG_RANGES = "ranges";

    @Override
    public Node.State calcNewState(Node node, Bundle args) {
        Node[] nodes = (Node[]) args.get(ARG_REFERENCE_NODES);
        double[] ranges = args.getDoubleArray(ARG_RANGES);


        List<double[]> coords = new ArrayList<double[]>(nodes.length);
        for (Node n : nodes) {
            coords.add(n.getState().pos);
        }

        return new Node.State(calcMinMax(coords, ranges));
    }

    /**
     * Calculate a position based on the Min/Max algorithm (up to three dimensions).
     * @param refNodes list of reference nodes' coordinates
     * @param distances linear distance to each reference node
     * @return the new position.
     */
    public double[] calcMinMax(List<double[]> refNodes, double[] distances) {
        // initialize x, y, and z coordinates.
        double[] min = { Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE};
        double[] max = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};

        // Find the maximum min and the minimum max values in each dimension.
        for (int i = 0; i < refNodes.size(); ++i) {
            for (int j = 0; j < 3; ++j) {
                double ref = refNodes.get(i)[j];
                if (ref - distances[i] > min[j]) min[j] = ref - distances[i];   // max of min
                if (ref + distances[i] < max[j]) max[j] = ref + distances[i];   // min of max
            }
        }

        // New position is center of min/max rectangle
        double[] pos = new double[3];
        for (int i = 0; i < 3; ++i) {
            pos[i] = (min[i] + max[i]) / 2;
        }
        return pos;
    }
}
