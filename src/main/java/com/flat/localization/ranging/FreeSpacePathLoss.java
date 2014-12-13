package com.flat.localization.ranging;

/**
 * Created by Jacob Phillips (10/2014)
 */
public final class FreeSpacePathLoss implements RangingProcessor {
    @Override
    public String getName() {
        return "FSPL";
    }

    /** @return The distance in meters. */
    public double fromDbMhz(double levelInDb, double freqInMHz)    {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }
}
