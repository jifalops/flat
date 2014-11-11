package com.flat.localization.ranging;

import android.hardware.SensorManager;

/**
 * Created by jacob on 10/25/14.
 */
public final class RotationVector implements SignalProcessor {
    @Override
    public String getName() {
        return "Rot. Vector";
    }

    /**
     * Position from a rotation vector.
     * @param rot the current rotation matrix, or null to start anew.
     * @param values the RotationVectorSensor event's values
     * @return rot, the rotation matrix.
     */
    public float[] rotateBy(float[] rot, float values[]) {
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
}
