package com.essentiallocalization.util.lifecycle;

/**
 * Created by Jake on 1/27/14.
 */
public interface Cancelable {
    void cancel();
    boolean isCanceled();

    static interface Listener {
        void onCanceled();
    }
}
