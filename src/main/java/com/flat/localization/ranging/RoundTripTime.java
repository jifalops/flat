package com.flat.localization.ranging;

import android.os.Bundle;

/**
 * Ranging algorithms based on propagation delay of the signal.
 */
public final class RoundTripTime implements Ranging {
    private static final int SPEED_OF_LIGHT_VACUUM = 299792458; // m/s

    /**
     * This function assumes the timestamps have nanosecond precision
     */
    public double findDistance(long aSent, long bReceived, long bSent, long aReceived) {
        long roundTrip = (aReceived - aSent) - (bSent - bReceived);
        double distance = (SPEED_OF_LIGHT_VACUUM * (roundTrip * 1E-9)) / 2;
        return distance;
    }

    @Override
    public int getType() {
        return TYPE_SIGNAL_DELAY;
    }

    @Override
    public String getNameShort() {
        return "RTT";
    }

    @Override
    public String getNameLong() {
        return "Round Trip Time";
    }
}
