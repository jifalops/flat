package com.flat.localization.algorithm;

/**
 * @author Jacob Phillips
 */
public final class MinMax extends PositionAlgorithm {
    @Override
    public String getName() {
        return "MinMax";
    }

    /**
     * Calculate a position based on the Min/Max algorithm (up to three dimensions).
     * @param positions list of reference nodes' coordinates
     * @param ranges linear distance to each reference node
     * @return the new position.
     */
    @Override
    public float[] findCoords(float[][] positions, float[] ranges) {
        // initialize x, y, and z coordinates.
        float[] min = { Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
        float[] max = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};

        // Find the maximum min and the minimum max values in each dimension.
        for (int i = 0; i < positions.length; ++i) {
            for (int j = 0; j < 3; ++j) {
                float ref = positions[i][j];
                if (ref-ranges[i] > min[j]) min[j] = ref-ranges[i];   // max of min
                if (ref+ranges[i] < max[j]) max[j] = ref+ranges[i];   // min of max
            }
        }

        // New position is center of min/max rectangle
        return new float[] {(min[0]+max[0])/2,
                             (min[1]+max[1])/2,
                             (min[2]+max[2])/2};
    }
}
