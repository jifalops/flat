package com.flat.localization;

/**
 * Created by jacob on 10/25/14.
 */
public final class Calc {
    private Calc() {}

    /** Both points must have the same number of elements */
    public static double distance(double[] p1, double[] p2) {
        double d = 0, tmp;
        for (int i = 0; i < p1.length; ++i) {
            tmp = p1[i] - p2[i];
            d += tmp * tmp;
        }
        return Math.sqrt(d);
    }

    /** Both points must have the same number of elements */
    public static double[] sum(double[] p1, double[] p2) {
        double[] sum = new double[p1.length];
        for (int i = 0; i < p1.length; ++i) {
            sum[i] = p1[i] + p2[i];
        }
        return sum;
    }
}
