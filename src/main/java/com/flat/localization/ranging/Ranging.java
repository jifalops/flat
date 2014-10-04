package com.flat.localization.ranging;

public interface Ranging {
    int TYPE_SIGNAL_DELAY = 1;
    int TYPE_SIGNAL_STRENGTH = 2;
    int TYPE_SIGNAL_PATTERN = 3;
    int getRangingType();
}
