package com.flat.localization.util;

/**
 * Created by jacob on 10/25/14.
 */
public final class Calc {
    private Calc() {}

    /** Both points must have the same number of elements */
    public static double linearDistance(double[] p1, double[] p2) {
        double d = 0, tmp;
        for (int i = 0; i < p1.length; ++i) {
            tmp = p1[i] - p2[i];
            d += tmp * tmp;
        }
        return Math.sqrt(d);
    }

    /** Sums each element of the arrays, up to v1.length elements. */
    public static double[] vectorSum(double[] v1, double[] v2) {
        double[] sum = new double[v1.length];
        for (int i = 0; i < v1.length; ++i) {
            sum[i] = v1[i] + v2[i];
        }
        return sum;
    }

    /** @return true if v1 <= v2 in every element. */
    public static boolean isLessThanOrEqual(double[] v1, double[] v2) {
        for (int i = 0; i < v1.length; ++i) {
            if (v1[i] > v2[i]) return false;
        }
        return true;
    }
    /** @return true if v1 <= v2 in every element. */
    public static boolean isLessThanOrEqual(float[] v1, float[] v2) {
        for (int i = 0; i < v1.length; ++i) {
            if (v1[i] > v2[i]) return false;
        }
        return true;
    }
}
