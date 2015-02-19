package com.flat.bluetoothtimeofflight.io;

import com.flat.bluetoothtimeofflight.lifecycle.Connectable;

/**
 * Created by Jake on 1/28/14.
 */
public interface Connection extends Connectable {
    int STATE_NONE         = 0;
    int STATE_CONNECTING   = 1;
    int STATE_CONNECTED    = 2;
    int STATE_DISCONNECTED = 3;

    int getState();
    void setState(int state);

    boolean isConnecting();
    boolean isDisconnected();

    static interface StateChangeListener {
        void onStateChange(int oldState, int newState);
    }
}
