package com.essentiallocalization.util;

/**
 * Created by Jake on 1/27/14.
 */
public interface Startable {
    void start();
    void stop();
    boolean canRestart();
}
