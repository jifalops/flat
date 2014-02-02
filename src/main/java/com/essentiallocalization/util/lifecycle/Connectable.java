package com.essentiallocalization.util.lifecycle;

/**
 * Created by Jake on 1/28/14.
 */
public interface Connectable {
    boolean isConnected();

    static interface Listener {
        void onConnect();
        void onDisconnect();
    }
}
