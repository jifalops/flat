package com.essentiallocalization.localization.signal;


import android.os.Bundle;

public class Accelerometer implements Signal {




    @Override
    public int getType() {
        return Signal.TYPE_INTERNAL;
    }

    @Override
    public void enable(Bundle args) {
    }

    @Override
    public void disable(Bundle args) {

    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
