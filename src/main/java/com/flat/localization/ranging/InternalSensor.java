package com.flat.localization.ranging;

import android.hardware.SensorManager;

/**
 * Created by Jacob Phillips (10/2014)
 */
public class InternalSensor {
    public static final double RAD_TO_DEG = 57.2957795;

    /**
     * Position from linear acceleration.
     * @param a acceleration for x, y, and z
     * @param t time duration of the acceleration, in nanoseconds.
     * @return estimated distance moved in meters for x, y, and z.
     */
    public static double[] linearAcceleration(float a[], long t) {
        double[] d = new double[3];
        // TODO 0.707 is temp. find trajectory
        d[0] = a[0] * t*t * 0.707;
        d[1] = a[1] * t*t * 0.707;
        d[2] = a[2] * t*t * 0.707;
        return d;
    }
    public static String LINEAR_ACCELERATION = "Lin. Accel.";


    /**
     * Position from a rotation vector.
     * @param rot the current rotation matrix, or null to start anew.
     * @param values the RotationVectorSensor event's values
     * @return rot, the rotation matrix.
     */
    public static float[] rotationVector(float[] rot, float values[]) {
        if (rot == null) {
            rot = new float[16];
            // initialize the rotation matrix to identity
            rot[ 0] = 1;
            rot[ 4] = 1;
            rot[ 8] = 1;
            rot[12] = 1;
        }
        SensorManager.getRotationMatrixFromVector(rot, values);
        return rot;
    }
    public static String ROTATION_VECTOR = "Rot. Vector";
}
