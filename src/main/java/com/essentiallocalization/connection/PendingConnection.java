package com.essentiallocalization.connection;

/**
 * Created by Jake on 9/20/13.
 */
public interface PendingConnection {
    boolean isConnected();
    boolean isFinished();
    boolean isCanceled();
    void cancel();
    void start();
}
