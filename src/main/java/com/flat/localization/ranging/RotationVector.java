package com.flat.localization.ranging;

import android.hardware.SensorManager;

/**
 * Created by Jacob Phillips (10/2014)
 */
public class RotationVector implements Ranging {
    @Override
    public int getType() {
        return TYPE_INTERNAL_SENSOR;
    }

    @Override
    public String getNameShort() {
        return "Rot. Vector";
    }

    @Override
    public String getNameLong() {
        return "Rotation Vector";
    }

    /**
     * Position from a rotation vector.
     * @param rot the current rotation matrix, or null to start anew.
     * @param values the RotationVectorSensor event's values
     * @return rot, the rotation matrix.
     */
    public float[] findDistance(float[] rot, float values[]) {
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
