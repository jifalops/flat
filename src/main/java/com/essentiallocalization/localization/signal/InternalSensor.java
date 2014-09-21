package com.essentiallocalization.localization.signal;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public final class InternalSensor extends AbstractSignal {
    public static final int EVENT_SENSOR_CHANGE = 1;
    public static final int EVENT_ACCURACY_CHANGE = 2;

    private final int sensorType;

    // Event paramaters
    private Sensor sensor;
    private int accuracy;
    private long timestamp;
    private float[] values;

    /**
     * @param sensorType one of the {@code Sensor.TYPE_} constants.
     */
    public InternalSensor(int sensorType) {
        this.sensorType = sensorType;
    }

    public int getSensorType() { return sensorType; }

    /** @returns one of the {@code SensorManager.SENSOR_STATUS_} constants. */
    public int getAccuracy() { return accuracy; }
    public Sensor getSensor() { return sensor; }
    public long getTimestamp() { return timestamp; }
    public float[] getValues() { return values; }


    @Override
    public int getSignalType() {
        return Signal.TYPE_INTERNAL;
    }

    /**
     * @param args args[0] is a Context used to get the SensorManager.
     *             args[1] is the update rate in micro seconds, defaulting to 30 updates per second.
     */
    @Override
    public void enable(Object... args) {
        Context ctx = (Context) args[0];
        int rateUs = 1000000 / 30;
        if (args.length == 2) {
            rateUs = (Integer) args[1];
        }
        SensorManager manager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        manager.registerListener(sensorListener, manager.getDefaultSensor(sensorType), rateUs);
        enabled = true;
    }


    /**
     * @param args args[0] is a Context used to get the SensorManager.
     */
    @Override
    public void disable(Object... args) {
        Context ctx = (Context) args[0];
        SensorManager manager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        manager.unregisterListener(sensorListener);
        enabled = false;
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            // "unroll" the SensorEvent because it is part of a system managed object pool
            sensor = sensorEvent.sensor;
            accuracy = sensorEvent.accuracy;
            timestamp = sensorEvent.timestamp;
            values = sensorEvent.values.clone();
            notifyListeners(EVENT_SENSOR_CHANGE);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            InternalSensor.this.sensor = sensor;
            InternalSensor.this.accuracy = accuracy;
            notifyListeners(EVENT_ACCURACY_CHANGE);
        }
    };
}
