package com.flat.aa;

import java.util.List;

/**
 * Created by Jacob Phillips.
 */
public class ArrayUtils {
    public static int[] toPrimitive(Integer[] array) {
        int[] tmp = new int[array.length];
        for (int i = 0; i < tmp.length; ++i) {
            tmp[i] = array[i];
        }
        return tmp;
    }

    public static long[] toPrimitive(Long[] array) {
        long[] tmp = new long[array.length];
        for (int i = 0; i < tmp.length; ++i) {
            tmp[i] = array[i];
        }
        return tmp;
    }

    public static double[] toPrimitive(Double[] array) {
        double[] tmp = new double[array.length];
        for (int i = 0; i < tmp.length; ++i) {
            tmp[i] = array[i];
        }
        return tmp;
    }

    public static float[] toPrimitive(Float[] array) {
        float[] tmp = new float[array.length];
        for (int i = 0; i < tmp.length; ++i) {
            tmp[i] = array[i];
        }
        return tmp;
    }
}
