package com.essentiallocalization.wifi;

/**
 * @author Jacob Phillips
 */
public final class MinMax {
    /**
     * Estimates the new position after a movement (from sensors here)
     * @param x current x
     * @param y current y
     * @param d distance moved with respect to each anchor.
     * @return the estimate of the new position.
     */
    public static float[] calcMinMax(float x, float y, float d[]) {

        // Calculate lower-left (min) and upper-right (max) bounding corners from the distance moved.
        double  minX = Float.MIN_VALUE,
                minY = Float.MIN_VALUE,
                maxX = Float.MAX_VALUE,
                maxY = Float.MAX_VALUE;
        for (float dist : d) {
            if (x - dist > minX) minX = x - dist;   // max of min
            if (y - dist > minY) minY = y - dist;
            if (x + dist < maxX) maxX = x + dist;   // min of max
            if (y + dist < maxY) maxY = y + dist;
        }

        // Use center of the new box as estimated position.
        return new float[] { (float)(minX + maxX)/2, (float)(minY + maxY)/2 };
    }
}
