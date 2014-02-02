package com.essentiallocalization.util.lifecycle;

/**
 * Created by Jake on 9/20/13.
 */
public interface Finishable {
    boolean isFinished();

    static interface Listener {
        void onFinished();
    }
}
