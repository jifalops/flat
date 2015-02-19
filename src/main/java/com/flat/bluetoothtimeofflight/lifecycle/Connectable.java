package com.flat.bluetoothtimeofflight.lifecycle;

/**
 * Something that can be connected and disconnected
 */
public interface Connectable extends Startable {
    boolean isConnected();

    static interface ConnectionListener {
        void onConnect();
        void onDisconnect();
    }
}
