package com.flat.localization.util;

/**
 * Created by jacob on 10/25/14.
 */
public final class Calc {
    private Calc() {}

    /** Both points must have the same number of elements */
    public static double linearDistance(float[] p1, float[] p2) {
        double d = 0, tmp;
        for (int i = 0; i < p1.length; ++i) {
            tmp = p1[i] - p2[i];
            d += tmp * tmp;
        }
        return Math.sqrt(d);
    }

    /** Sums each element of the arrays, up to v1.length elements. */
    public static float[] vectorSum(float[] v1, float[] v2) {
        float[] sum = new float[v1.length];
        for (int i = 0; i < v1.length; ++i) {
            sum[i] = v1[i] + v2[i];
        }
        return sum;
    }

    /** Subtracts each element of the arrays, up to v1.length elements. */
    public static float[] vectorDiff(float[] v1, float[] v2) {
        float[] diff = new float[v1.length];
        for (int i = 0; i < v1.length; ++i) {
            diff[i] = v1[i] - v2[i];
        }
        return diff;
    }

    /** @return true if v1 <= v2 in every element. */
    public static boolean isLessThanOrEqual(float[] v1, float[] v2) {
        for (int i = 0; i < v1.length; ++i) {
            if (v1[i] > v2[i]) return false;
        }
        return true;
    }
}
