package com.flat.localization.util;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class Conv {
    public static void rad2deg(float[] rads) {
        for (int i=0; i<rads.length; ++i) {
            rads[i] = rads[i] * Const.RAD_TO_DEG;
        }
    }
}
