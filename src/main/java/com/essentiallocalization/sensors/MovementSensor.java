package com.essentiallocalization.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * @author Jacob Phillips
 */
public final class MovementSensor implements SensorEventListener {
    public float accel[];
    public float rot[];

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:

                break;
            case Sensor.TYPE_ROTATION_VECTOR:

                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
//        switch (sensor.getType()) {
//            case Sensor.TYPE_LINEAR_ACCELERATION:
//
//                break;
//            case Sensor.TYPE_ROTATION_VECTOR:
//
//                break;
//        }
    }
}
