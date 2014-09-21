package com.essentiallocalization.localization.signal;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Created by Jacob Phillips (09/2014)
 */
public final class AndroidLocation extends AbstractSignal {
    public static final int EVENT_LOCATION_CHANGE = 1;
    public static final int EVENT_STATUS_CHANGE = 2;
    public static final int EVENT_PROVIDER_ENABLED = 3;
    public static final int EVENT_PROVIDER_DISABLED = 4;

    private final String provider;

    /**
     * @param provider One of the {@code LocationManager._PROVIDER}.
     *                  (LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
     */
    public AndroidLocation(String provider) {
        this.provider = provider;
    }


    @Override
    public int getSignalType() {
        return Signal.TYPE_ELECTROMAGNETIC;
    }

    /**
     * @param args args[0] is a Context used to get the LocationManager.
     *             args[1] is a Long representing the minimum time between updates, in milliseconds.
     *             args[2] is a Float representing the minimum distance between updates, in meters.
     */
    @Override
    public void enable(Object... args) {
        Context ctx = (Context) args[0];
        long minTime = (Long) args[1];
        float minDist = (Float) args[2];
        LocationManager manager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        manager.requestLocationUpdates(provider, minTime, minDist, locationListener);
        enabled = true;
    }

    /**
     * @param args args[0] is a Context used to get the LocationManager.
     */
    @Override
    public void disable(Object... args) {
        Context ctx = (Context) args[0];
        LocationManager manager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        manager.removeUpdates(locationListener);
        enabled = false;
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            AndroidLocation.this.location = location;
            notifyListeners(EVENT_LOCATION_CHANGE);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            AndroidLocation.this.status = status;
            AndroidLocation.this.statusExtras = extras;
            notifyListeners(EVENT_STATUS_CHANGE);
        }

        @Override
        public void onProviderEnabled(String provider) {
            notifyListeners(EVENT_PROVIDER_ENABLED);
        }

        @Override
        public void onProviderDisabled(String provider) {
            notifyListeners(EVENT_PROVIDER_DISABLED);
        }
    };

    private Location location;
    private int status;
    private Bundle statusExtras;

    public Location getLocation() { return location; }
    public int getStatus() { return status; }
    public Bundle getStatusExtras() { return statusExtras; }
}
