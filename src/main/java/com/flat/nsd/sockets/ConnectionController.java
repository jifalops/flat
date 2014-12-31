package com.flat.nsd.sockets;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public interface ConnectionController {
    void start();
    void cancel();
    boolean isCanceled();
    boolean isConnected();
    boolean isFinished();
}
