package com.flat.localization.signal.interpreters;

import android.hardware.SensorManager;

/**
 * Created by jacob on 10/25/14.
 */
public final class RotationVector implements SignalInterpreter {

    private final float[] matrix = {1,0,0,0,
                                  1,0,0,0,
                                  1,0,0,0,
                                  1,0,0,0};

    @Override
    public String getName() {
        return "Rot. Vector";
    }

    public float[] getMatrix() {
        return matrix;
    }


    /**
     * Position from a rotation vector.
     * @param values the RotationVectorSensor event's values
     * @return rot, the rotation matrix.
     */
    public void toWorldOrientation(float values[]) {
        SensorManager.getRotationMatrixFromVector(matrix, values);
    }
}
