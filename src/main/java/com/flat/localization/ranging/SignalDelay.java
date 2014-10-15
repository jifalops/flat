package com.flat.localization.ranging;

/**
 * Created by Jacob Phillips (10/2014)
 */
public class SignalDelay {
    private static final int SPEED_OF_LIGHT_VACUUM = 299792458; // m/s

    /**
     * This function assumes the timestamps have nanosecond precision
     */
    public double roundTripTime(long aSent, long bReceived, long bSent, long aReceived) {
        long roundTrip = (aReceived - aSent) - (bSent - bReceived);
        double distance = (SPEED_OF_LIGHT_VACUUM * (roundTrip * 1E-9)) / 2;
        return distance;
    }
    public static String ROUND_TRIP_TIME = "RTT";
}
