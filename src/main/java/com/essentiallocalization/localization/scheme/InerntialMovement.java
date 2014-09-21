package com.essentiallocalization.localization.scheme;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.essentiallocalization.localization.signal.Signal;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips
 */
public final class InerntialMovement implements SensorEventListener {

    private final double pos[] = new double[3];

    private final float rot[] = new float[16];
    private final float rotWorld[] = new float[16];

    private boolean firstEvent = true;
    private long lastEventTime;
    private double t;


    public InerntialMovement() {
        // initialize the rotation matrix to identity
        rot[ 0] = 1;
        rot[ 4] = 1;
        rot[ 8] = 1;
        rot[12] = 1;
        rotWorld[ 0] = 1;
        rotWorld[ 4] = 1;
        rotWorld[ 8] = 1;
        rotWorld[12] = 1;
    }

    public void start(SensorManager sm) {
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 1000000/30);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),     1000000/30);
    }
    public void stop(SensorManager sm) {
        sm.unregisterListener(this);
    }


    public void calcTrajectory() {

        // Get the vector R


    }

    /**
     * Position from linear acceleration.
     * @param a acceleration for x, y, and z
     */
    private void calcTranslation(float a[]) {
        // TODO 0.707 is temp. find trajectory
        pos[0] += a[0] * t*t * 0.707;
        pos[1] += a[1] * t*t * 0.707;
        pos[2] += a[2] * t*t * 0.707;
    }

    private float[] calcRotation(float[] matrix) {
        //todo
        return new float[] {0,0,0};
    }

    /*
     * TODO use the gl graphics function to handle correct multiplication regardless of orientation.
     */
    private void matrixMultiply() {
        float[] tmp = rotWorld.clone();
        rotWorld[0] = tmp[0]* rot[0] + tmp[1]* rot[4] + tmp[2]* rot[8] + tmp[3]* rot[12];
        rotWorld[1] = tmp[0]* rot[1] + tmp[1]* rot[5] + tmp[2]* rot[9] + tmp[3]* rot[13];
        rotWorld[2] = tmp[0]* rot[2] + tmp[1]* rot[6] + tmp[2]* rot[10] + tmp[3]* rot[14];
        rotWorld[3] = tmp[0]* rot[3] + tmp[1]* rot[7] + tmp[2]* rot[11] + tmp[3]* rot[15];

        rotWorld[4] = tmp[4]* rot[0] + tmp[5]* rot[4] + tmp[6]* rot[8] + tmp[7]* rot[12];
        rotWorld[5] = tmp[4]* rot[1] + tmp[5]* rot[5] + tmp[6]* rot[9] + tmp[7]* rot[13];
        rotWorld[6] = tmp[4]* rot[2] + tmp[5]* rot[6] + tmp[6]* rot[10] + tmp[7]* rot[14];
        rotWorld[7] = tmp[4]* rot[3] + tmp[5]* rot[7] + tmp[6]* rot[11] + tmp[7]* rot[15];

        rotWorld[8] = tmp[8]* rot[0] + tmp[9]* rot[4] + tmp[10]* rot[8] + tmp[11]* rot[12];
        rotWorld[9] = tmp[8]* rot[1] + tmp[9]* rot[5] + tmp[10]* rot[9] + tmp[11]* rot[13];
        rotWorld[10] = tmp[8]* rot[2] + tmp[9]* rot[6] + tmp[10]* rot[10] + tmp[11]* rot[14];
        rotWorld[11] = tmp[8]* rot[3] + tmp[9]* rot[7] + tmp[10]* rot[11] + tmp[11]* rot[15];

        rotWorld[12] = tmp[12]* rot[0] + tmp[13]* rot[4] + tmp[14]* rot[8] + tmp[15]* rot[12];
        rotWorld[13] = tmp[12]* rot[1] + tmp[13]* rot[5] + tmp[14]* rot[9] + tmp[15]* rot[13];
        rotWorld[14] = tmp[12]* rot[2] + tmp[13]* rot[6] + tmp[14]* rot[10] + tmp[15]* rot[14];
        rotWorld[15] = tmp[12]* rot[3] + tmp[13]* rot[7] + tmp[14]* rot[11] + tmp[15]* rot[15];
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (firstEvent) {
            lastEventTime = event.timestamp;
            firstEvent = false;
            return;
        }
        t = (event.timestamp - lastEventTime) / 1E9;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                calcTranslation(event.values);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                SensorManager.getRotationMatrixFromVector(rot, event.values);
                matrixMultiply();
                break;
        }
        calcTrajectory();
        notifyMovementListeners();
        lastEventTime = event.timestamp;
    }

    @Override
    public int getSignalType() {
        return Signal.TYPE_INTERNAL;
    }

    public static interface MovementListener { void onMovement(double[] pos, float[] angle, double timeDiff); }
    private final List<MovementListener> listeners = new ArrayList<MovementListener>(1);
    public void notifyMovementListeners() { for (MovementListener l: listeners) l.onMovement(pos, rot, t); }
    public void registerMovementListener(MovementListener l) { listeners.add(l); }
    public void unregisterMovementListener(MovementListener l) { listeners.remove(l); }
    public void unregisterMovementListeners() { listeners.clear(); }

    @Override public void onAccuracyChanged(Sensor sensor, int i) {}
}
