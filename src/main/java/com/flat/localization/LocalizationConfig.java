package com.flat.localization;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class LocalizationConfig {

    /** Minimum number of remote nodes required to do localization */
    public static final int MIN_NODES = 2;
    /** Minimum amount of time to wait for nodes to connect through NSD. */
    public static final int MIN_NSD_WAIT = 5000;
    /** Maximum amount of time to wait for expected beacons to be received. */
    public static final int MAX_BEACON_WAIT = MIN_NSD_WAIT * 3;






//    private final Set<String> nodes = Collections.synchronizedSet(new HashSet<String>());
//    private long firstConnectionTime;
//
//    public boolean nodeConnected(String id) {
//        if (nodes.add(id)) {
//            if (nodes.size() == 1) {
//                firstConnectionTime = System.nanoTime();
//            } else {
//                checkShouldSwitchToBeaconMode();
//            }
//            return true;
//        }
//        return false;
//    }
//
//    public void sendIdentityInfo()
//
//
//    private void checkShouldSwitchToBeaconMode() {
//        if (nodes.size() >= 3 && )
//    }
}
