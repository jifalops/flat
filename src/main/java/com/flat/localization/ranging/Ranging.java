package com.flat.localization.ranging;

/**
 * Created by Jacob Phillips (10/2014)
 */
public interface Ranging {
    int TYPE_SIGNAL_STRENGTH = 1;
    int TYPE_SIGNAL_DELAY = 2;
    int TYPE_SIGNAL_PATTERN = 3;
    int TYPE_INTERNAL_SENSOR = 4;
    int getType();
    String getNameShort();
    String getNameLong();
}
