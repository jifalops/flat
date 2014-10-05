package com.flat.localization.ranging;

/**
 * Created by Jacob Phillips (10/2014)
 */
public class LinearAcceleration implements Ranging {
    @Override
    public int getType() {
        return TYPE_INTERNAL_SENSOR;
    }

    @Override
    public String getNameShort() {
        return "Lin. Accel.";
    }

    @Override
    public String getNameLong() {
        return "Linear Acceleration";
    }

    /**
     * Position from linear acceleration.
     * @param a acceleration for x, y, and z
     * @param t time duration of the acceleration, in nanoseconds.
     * @return estimated distance moved in meters for x, y, and z.
     */
    public static double[] findDistance(float a[], long t) {
        double[] d = new double[3];
        // TODO 0.707 is temp. find trajectory
        d[0] = a[0] * t*t * 0.707;
        d[1] = a[1] * t*t * 0.707;
        d[2] = a[2] * t*t * 0.707;
        return d;
    }
}
