package com.flat.localization.ranging;

/**
 * Ranging algorithms based on signal strength/decay.
 */
public final class FreeSpacePathLoss implements Ranging {

    public double findDistance(double levelInDb, double freqInMHz)    {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }

    @Override
    public int getType() {
        return TYPE_SIGNAL_STRENGTH;
    }

    @Override
    public String getNameShort() {
        return "FSPL";
    }

    @Override
    public String getNameLong() {
        return "Free Space Path Loss";
    }
}
