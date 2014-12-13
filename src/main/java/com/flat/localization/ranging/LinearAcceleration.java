package com.flat.localization.ranging;

/**
* Created by jacob on 10/25/14.
*/
public final class LinearAcceleration implements RangingProcessor {
    @Override
    public String getName() {
        return "Lin. Accel.";
    }

    /**
     * Position from linear acceleration.
     * @param a acceleration for x, y, and z
     * @param t time duration of the acceleration, in nanoseconds.
     * @return estimated distance moved in meters for x, y, and z.
     */
    public double[] integrate(float a[], long t) {
        double[] d = new double[3];
        // TODO 0.707 is temp. find trajectory by incorporating rotation to make arc
        d[0] = a[0] * t*t * 0.707;
        d[1] = a[1] * t*t * 0.707;
        d[2] = a[2] * t*t * 0.707;
        return d;
    }
}
