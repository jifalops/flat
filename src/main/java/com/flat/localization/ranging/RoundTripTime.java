package com.flat.localization.ranging;

import com.flat.localization.Const;

/**
 * Created by Jacob Phillips (10/2014)
 */
public class RoundTripTime implements SignalProcessor {
    @Override
    public String getName() {
        return "RTT";
    }

    /**
     * This assumes the timestamps have nanosecond precision
     */
    public double apply(long aSent, long bReceived, long bSent, long aReceived) {
        long roundTrip = (aReceived - aSent) - (bSent - bReceived);
        double d = (Const.SPEED_OF_LIGHT_VACUUM * (roundTrip * 1E-9)) / 2;
        return d;
    }
}
