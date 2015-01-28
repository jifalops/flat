package com.flat.localization;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class LocalizationManager2 {


    private final Set<String> nodes = Collections.synchronizedSet(new HashSet<String>());
    private long firstConnectionTime;

    public boolean nodeConnected(String id) {
        if (nodes.add(id)) {
            if (nodes.size() == 1) {
                firstConnectionTime = System.nanoTime();
            } else {
                checkShouldSwitchToBeaconMode();
            }
            return true;
        }
        return false;
    }

    public void sendIdentityInfo()


    private void checkShouldSwitchToBeaconMode() {
        if (nodes.size() >= 3 && )
    }
}
