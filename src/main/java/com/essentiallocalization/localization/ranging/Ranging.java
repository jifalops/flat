package com.essentiallocalization.localization.ranging;

import com.essentiallocalization.localization.signal.Signal;

public interface Ranging {
    int TYPE_SIGNAL_DELAY = 1;
    int TYPE_SIGNAL_STRENGTH = 2;
    int TYPE_SIGNAL_PATTERN = 3;
    int getType();
    /** Process input signal(s) into a length, usually in meters. */
    double findRange(Signal... signals);
}
