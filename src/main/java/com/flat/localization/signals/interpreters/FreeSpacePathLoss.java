package com.flat.localization.signals.interpreters;

/**
 * Created by Jacob Phillips (10/2014)
 */
public final class FreeSpacePathLoss implements SignalInterpreter {
    @Override
    public String getName() {
        return "FSPL";
    }

    /** @return The distance in meters. */
    public float fromDbMhz(float levelInDb, float freqInMHz)    {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
        return (float) Math.pow(10.0, exp);
    }

}
