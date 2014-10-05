package com.flat.localization.signal;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Can register for updates from one of the three built-in location providers:
 * {@code LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, or LocationManager.PASSIVE_PROVIDER}
 *
 * Note that the network provider uses both Cellular and WiFi.
 *
 * Created by Jacob Phillips (09/2014)
 */
public final class AndroidLocation extends AbstractSignal {
    public static final int EVENT_LOCATION_CHANGE = 1;
    public static final int EVENT_STATUS_CHANGE = 2;
    public static final int EVENT_PROVIDER_ENABLED = 3;
    public static final int EVENT_PROVIDER_DISABLED = 4;

    private boolean enabled;

    private final String provider;

    /**
     * @param provider One of the {@code LocationManager._PROVIDER}.
     *                  (LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
     */
    public AndroidLocation(String provider) {
        this.provider = provider;
    }

    /**
     * @param ctx used to get the LocationManager.
     * @param minTime the minimum time between updates, in milliseconds.
     * @param minDist the minimum distance between updates, in meters.
     */
    public void enable(Context ctx, long minTime, float minDist) {
        LocationManager manager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        manager.requestLocationUpdates(provider, minTime, minDist, locationListener);
        enabled = true;
    }

    /** Get updates as fast as possible (battery drain) */
    @Override
    public void enable(Context ctx) {
        enable(ctx, 0, 0);
    }

    @Override
    public void disable(Context ctx) {
        LocationManager manager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        manager.removeUpdates(locationListener);
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
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
