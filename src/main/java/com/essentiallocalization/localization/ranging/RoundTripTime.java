package com.essentiallocalization.localization.ranging;

import android.os.Bundle;

/**
 * Created by Jake on 9/19/2014.
 */
public class RoundTripTime implements Ranging {

    public static final String ARG_A_SENT = "a_sent";
    public static final String ARG_B_RECEIVED = "b_received";
    public static final String ARG_B_SENT = "b_sent";
    public static final String ARG_A_RECEIVED = "a_received";

    public static final int SPEED_OF_LIGHT_VACUUM = 299792458; // m/s

    @Override
    public int getType() {
        return Ranging.TYPE_SIGNAL_DELAY;
    }

    @Override
    public double estimateDistance(Bundle args) {
        long aSent = args.getLong(ARG_A_SENT);
        long bReceived = args.getLong(ARG_B_RECEIVED);
        long bSent = args.getLong(ARG_B_SENT);
        long aReceived = args.getLong(ARG_A_RECEIVED);
        return calcRttRange(aSent, bReceived, bSent, aReceived);
    }

    /**
     * This function assumes the timestamps have nanosecond precision
     */
    public double calcRttRange(long aSent, long bReceived, long bSent, long aReceived) {
        long roundTrip = (aReceived - aSent) - (bSent - bReceived);
        double distance = (SPEED_OF_LIGHT_VACUUM * (roundTrip * 1E-9)) / 2;
        return distance;
    }
}
