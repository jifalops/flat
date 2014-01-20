package com.essentiallocalization.util;

/**
 * Created by Jake on 1/20/14.
 */
public class Calc {
    private Calc() { throw new AssertionError("Non-instantiable"); }

    public static final int
            SPEED_OF_LIGHT = 299792458; // m/s

    public static float timeOfFlightDistance1(long aSent, long bReceived, long bSent, long aReceived) {
        long roundTrip = (aReceived - aSent) - (bSent - bReceived);
        double distance = (SPEED_OF_LIGHT * (roundTrip * 1E-9)) / 2;
        return Math.round(distance * 100) / 100;
    }
}
