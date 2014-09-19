package com.essentiallocalization.localization.distance;

import android.os.Bundle;

public interface Ranging {
    int TYPE_UNKNOWN = 0;
    int TYPE_SIGNAL_DELAY = 1;
    int TYPE_SIGNAL_STRENGTH = 2;
    int TYPE_SIGNAL_PATTERN = 3;
    int getType();
    double estimateDistance(Bundle args);
}
