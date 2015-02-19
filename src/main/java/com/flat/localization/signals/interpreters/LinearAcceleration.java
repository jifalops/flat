package com.flat.localization.signals.interpreters;

/**
* Created by jacob on 10/25/14.
*/
public final class LinearAcceleration implements SignalInterpreter {
    @Override
    public String getName() {
        return "Lin. Accel.";
    }

    /**
     * Position from linear acceleration.
     * @param a acceleration for x, y, and z, in m/s.
     * @param t time duration of the acceleration, in seconds.
     * @return estimated distance moved in meters for x, y, and z.
     */
    public float[] integrate(float a[], double t) {
        float[] d = new float[3];
        // TODO 0.707 is temp. find trajectory by incorporating rotation to make arc
        double factor = t*t * 0.707;
        d[0] = (float) (a[0] * factor);
        d[1] = (float) (a[1] * factor);
        d[2] = (float) (a[2] * factor);
        return d;
    }
}
