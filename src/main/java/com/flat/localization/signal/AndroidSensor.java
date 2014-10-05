package com.flat.localization.signal;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * This can represent any of the Android sensors.
 */
public final class AndroidSensor extends AbstractSignal {
    public static final int EVENT_SENSOR_CHANGE = 1;
    public static final int EVENT_ACCURACY_CHANGE = 2;

    private boolean enabled;
    private final int sensorType;

    // Event paramaters
    private Sensor sensor;
    private int accuracy;
    private long timestamp;
    private float[] values;

    /**
     * @param sensorType one of the {@code Sensor.TYPE_} constants.
     */
    public AndroidSensor(int sensorType) {
        this.sensorType = sensorType;
    }

    public int getSensorType() { return sensorType; }

    /** @returns one of the {@code SensorManager.SENSOR_STATUS_} constants. */
    public int getAccuracy() { return accuracy; }
    public Sensor getSensor() { return sensor; }
    public long getTimestamp() { return timestamp; }
    public float[] getValues() { return values; }

    /**
     * @param ctx Used to get the SensorManager.
     * @param rate The update rate in micro seconds, defaulting to 30 updates per second.
     */
    public void enable(Context ctx, int rate) {
        SensorManager manager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        manager.registerListener(sensorListener, manager.getDefaultSensor(sensorType), rate);
        enabled = true;
    }
    @Override
    public void enable(Context ctx) {
        enable(ctx, 1000000/30);
    }

    @Override
    public void disable(Context ctx) {
        SensorManager manager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        manager.unregisterListener(sensorListener);
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
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
            AndroidSensor.this.sensor = sensor;
            AndroidSensor.this.accuracy = accuracy;
            notifyListeners(EVENT_ACCURACY_CHANGE);
        }
    };
}
