package com.flat.aa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jacob Phillips.
 */
public class Statistics {
    public static double getSum(double[] data) {
        double sum = 0;
        for (double d : data) {
            sum += d;
        }
        return sum;
    }

    public static double getMean(double[] data) {
        return getSum(data) / data.length;
    }

    public static double getVariance(double[] data) {
        double mean = getMean(data);
        double tmp = 0;
        for (double d : data) {
            tmp += (mean - d) * (mean - d);
        }
        return tmp / data.length;
    }

    public static double getStdDev(double[] data) {
        return Math.sqrt(getVariance(data));
    }

    public static double getMedian(double[] data) {
        if (data.length == 0) return 0;

        double[] d = new double[data.length];
        System.arraycopy(data, 0, d, 0, d.length);
        Arrays.sort(d);
        if (d.length % 2 == 0) {
            return (d[d.length/2] + d[(d.length/2)-1]) / 2.0;
        } else {
            return d[d.length/2];
        }
    }





    public static float getSum(float[] data) {
        float sum = 0;
        for (float d : data) {
            sum += d;
        }
        return sum;
    }

    public static float getMean(float[] data) {
        return getSum(data) / data.length;
    }

    public static float getVariance(float[] data) {
        float mean = getMean(data);
        float tmp = 0;
        for (float d : data) {
            tmp += (mean - d) * (mean - d);
        }
        return tmp / data.length;
    }

    public static float getStdDev(float[] data) {
        return (float) Math.sqrt(getVariance(data));
    }

    public static float getMedian(float[] data) {
        if (data.length == 0) return 0;

        float[] d = new float[data.length];
        System.arraycopy(data, 0, d, 0, d.length);
        Arrays.sort(d);
        if (d.length % 2 == 0) {
            return (d[d.length/2] + d[(d.length/2)-1]) / 2.0f;
        } else {
            return d[d.length/2];
        }
    }
    
    
    
    
    
    
    public static long getSum(long[] data) {
        long sum = 0;
        for (long d : data) {
            sum += d;
        }
        return sum;
    }

    public static long getMean(long[] data) {
        return getSum(data) / data.length;
    }

    public static long getVariance(long[] data) {
        long mean = getMean(data);
        long tmp = 0;
        for (long d : data) {
            tmp += (mean - d) * (mean - d);
        }
        return tmp / data.length;
    }

    public static long getStdDev(long[] data) {
        return (long) Math.sqrt(getVariance(data));
    }

    public static long getMedian(long[] data) {
        if (data.length == 0) return 0;

        long[] d = new long[data.length];
        System.arraycopy(data, 0, d, 0, d.length);
        Arrays.sort(d);
        if (d.length % 2 == 0) {
            return (d[d.length/2] + d[(d.length/2)-1]) / 2l;
        } else {
            return d[d.length/2];
        }
    }





    public static int getSum(int[] data) {
        int sum = 0;
        for (int d : data) {
            sum += d;
        }
        return sum;
    }

    public static int getMean(int[] data) {
        return getSum(data) / data.length;
    }

    public static int getVariance(int[] data) {
        int mean = getMean(data);
        int tmp = 0;
        for (int d : data) {
            tmp += (mean - d) * (mean - d);
        }
        return tmp / data.length;
    }

    public static int getStdDev(int[] data) {
        return (int) Math.sqrt(getVariance(data));
    }

    public static int getMedian(int[] data) {
        if (data.length == 0) return 0;

        int[] d = new int[data.length];
        System.arraycopy(data, 0, d, 0, d.length);
        Arrays.sort(d);
        if (d.length % 2 == 0) {
            return (d[d.length/2] + d[(d.length/2)-1]) / 2;
        } else {
            return d[d.length/2];
        }
    }
}
