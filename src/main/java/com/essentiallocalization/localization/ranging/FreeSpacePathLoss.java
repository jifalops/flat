package com.essentiallocalization.localization.ranging;


import android.os.Bundle;

public final class FreeSpacePathLoss implements Ranging {

    public static final String ARG_LEVEL_IN_DB = "level";
    public static final String ARG_FREQ_IN_MHZ = "frequency";

    @Override
    public int getRangingType() {
        return Ranging.TYPE_SIGNAL_STRENGTH;
    }

    public static double calcFsplRange(double levelInDb, double freqInMHz)    {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }
}
